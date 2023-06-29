package MAC

import chisel3._
import chisel3.util._


class TxBuffDesc extends Bundle{
  val len = UInt(16.W) //[31.16]
  val rd = Bool() //[15]
  val irq  = Bool() //14
  val wr = Bool()  //13
  val pad = Bool() //12
  val crc = Bool() //11
  val reserved1 = UInt(2.W) //10,9
  val ur = Bool()  //8
  val rtry = UInt(4.W)  //7 6 5 4
  val rl = Bool()  //3
  val lc = Bool()  //2
  val df = Bool()  //1
  val cs = Bool()  //0
}

class RxBuffDesc extends Bundle{
  val len = UInt(16.W) //[31.16]
  val e = Bool() //15
  val irq = Bool() //14
  val wrap = Bool() //13
  val reserved1 = UInt(4.W) //12 11 10 9
  val cf = Bool() //8
  val m = Bool() //7
  val or = Bool() //6
  val is = Bool() //5
  val dn = Bool() //4
  val tl = Bool() //3
  val sf = Bool() //2
  val crc = Bool() //1
  val lc = Bool() //0
}



class MacTileLinkIO extends Bundle{

  // WISHBONE common
  val WB_DAT_I = Input(UInt(32.W))       // WISHBONE data input

  val WB_DAT_O = Output(UInt(32.W))       // WISHBONE data output

  // WISHBONE slave
  val WB_ADR_I = Input(UInt(8.W))       // WISHBONE address input
  val WB_WE_I  = Input(Bool())        // WISHBONE write enable input
  val BDCs     = Input(UInt(4.W))          // Buffer descriptors are selected
  val WB_ACK_O = Output(Bool())       // WISHBONE acknowledge output

  // WISHBONE master
  val m_wb_adr_o = Output(UInt(30.W))
  val m_wb_sel_o = Output(UInt(4.W))
  val m_wb_we_o  = Output(Bool())
  val m_wb_dat_o = Output(UInt(32.W))
  val m_wb_cyc_o = Output(Bool())
  val m_wb_stb_o = Output(Bool())
  val m_wb_dat_i = Input(UInt(32.W))
  val m_wb_ack_i = Input(Bool())
  val m_wb_err_i = Input(Bool())

  val m_wb_cti_o = Output(UInt(3.W))     // Cycle Type Identifier
  val m_wb_bte_o = Output(UInt(2.W))     // Burst Type Extension

  // Rx Status signals
  val InvalidSymbol   = Input(Bool())             // Invalid symbol was received during reception in 100 Mbps mode
  val LatchedCrcError = Input(Bool())             // CRC error
  val RxLateCollision = Input(Bool())             // Late collision occured while receiving frame
  val ShortFrame      = Input(Bool())             // Frame shorter then the minimum size (r_MinFL) was received while small packets are enabled (r_RecSmall)
  val DribbleNibble        = Input(Bool())        // Extra nibble received
  val ReceivedPacketTooBig = Input(Bool())        // Received packet is bigger than r_MaxFL
  val RxLength             = Input(UInt(16.W))    // Length of the incoming frame
  val LoadRxStatus         = Input(Bool())        // Rx status was loaded
  val ReceivedPacketGood   = Input(Bool())        // Received packet's length and CRC are good
  val AddressMiss          = Input(Bool())        // When a packet is received AddressMiss status is written to the Rx BD
  val r_RxFlow             = Input(Bool())
  val r_PassAll            = Input(Bool())
  val ReceivedPauseFrm     = Input(Bool())

  // Tx Status signals
  val RetryCntLatched  = Input(UInt(4.W))  // Latched Retry Counter
  val RetryLimit       = Input(Bool())     // Retry limit reached (Retry Max value +1 attempts were made)
  val LateCollLatched  = Input(Bool())     // Late collision occured
  val DeferLatched     = Input(Bool())     // Defer indication (Frame was defered before sucessfully sent)
  val RstDeferLatched  = Output(Bool())
  val CarrierSenseLost = Input(Bool())     // Carrier Sense was lost during the frame transmission

  // Tx
  val MTxClk         = Input(Bool())      // Transmit clock (from PHY)
  val TxUsedData     = Input(Bool())      // Transmit packet used data
  val TxRetry        = Input(Bool())      // Transmit packet retry
  val TxAbort        = Input(Bool())      // Transmit packet abort
  val TxDone         = Input(Bool())      // Transmission ended
  val TxStartFrm     = Output(Bool())     // Transmit packet start frame
  val TxEndFrm       = Output(Bool())     // Transmit packet end frame
  val TxData         = Output(UInt(8.W))  // Transmit packet data byte
  val TxUnderRun     = Output(Bool())     // Transmit packet under-run
  val PerPacketCrcEn = Output(Bool())     // Per packet crc enable
  val PerPacketPad   = Output(Bool())     // Per packet pading

  // Rx
  val MRxClk     = Input(Bool())         // Receive clock (from PHY)
  val RxData     = Input(UInt(8.W))      // Received data byte (from PHY)
  val RxValid    = Input(Bool())
  val RxStartFrm = Input(Bool())
  val RxEndFrm   = Input(Bool())
  val RxAbort    = Input(Bool())        // This signal is set when address doesn't match.
  val RxStatusWriteLatched_sync2 = Output(Bool())

  //Register
  val r_TxEn    = Input(Bool())          // Transmit enable
  val r_RxEn    = Input(Bool())         // Receive enable
  val r_TxBDNum = Input(UInt(8.W))      // Receive buffer descriptor number

  // Interrupts
  val TxB_IRQ  = Output(Bool())
  val TxE_IRQ  = Output(Bool())
  val RxB_IRQ  = Output(Bool())
  val RxE_IRQ  = Output(Bool())
  val Busy_IRQ = Output(Bool())
}


abstract class MacTileLinkBase extends Module{
  val io: MacTileLinkIO = IO(new MacTileLinkIO)


  val TxB_IRQ = RegInit(false.B); io.TxB_IRQ := TxB_IRQ
  val TxE_IRQ = RegInit(false.B); io.TxE_IRQ := TxE_IRQ
  val RxB_IRQ = RegInit(false.B); io.RxB_IRQ := RxB_IRQ
  val RxE_IRQ = RegInit(false.B); io.RxE_IRQ := RxE_IRQ




  val TxUnderRun_wb = RegInit(false.B)

  val TxBDRead = RegInit(true.B)
  val TxStatusWrite = Wire(Bool())


  val TxValidBytesLatched = RegInit(0.U(2.W))

  val TxLength = RegInit(0.U(16.W))
  val LatchedTxLength = RegInit(0.U(16.W))
  val TxStatus = RegInit(0.U(4.W)) //[14:11]
  val RxStatus = RegInit(0.U(2.W)) //[14:13]

  // Synchronizing TxRetry TxDone_wb TxAbort signal (synchronized to WISHBONE clock)
  val TxRetrySync1 = RegNext(io.TxRetry, false.B)
  val TxAbortSync1 = RegNext(io.TxAbort, false.B)
  val TxDoneSync1  = RegNext(io.TxDone, false.B)

  val TxStartFrm_wb = RegInit(false.B)
  val TxRetry_wb = RegNext(TxRetrySync1, false.B)
  val TxAbort_wb = RegNext(TxAbortSync1, false.B)
  val TxDone_wb  = RegNext(TxDoneSync1,  false.B)

  // Generating delayed signals
  val TxDone_wb_q  = RegNext(TxDone_wb,  false.B)
  val TxAbort_wb_q = RegNext(TxAbort_wb, false.B)
  val TxRetry_wb_q = RegNext(TxRetry_wb, false.B)
  val TxRetryPacket = RegInit(false.B)
  val TxRetryPacket_NotCleared = RegInit(false.B)
  val TxDonePacket = RegInit(false.B)
  val TxDonePacket_NotCleared = RegInit(false.B)
  val TxAbortPacket = RegInit(false.B)
  val TxAbortPacket_NotCleared = RegInit(false.B)
  val RxBDReady = RegInit(false.B)
  val RxReady = RegInit(false.B)
  val TxBDReady = RegInit(false.B)

  val RxBDRead = RegInit(false.B)



  val BlockingTxStatusWrite = RegInit(false.B)
  val BlockingTxBDRead = RegInit(false.B)


  val RxBDAddress = RegInit(0.U(7.W))   //[7:1]
  val TxBDAddress = RegInit(0.U(7.W))   //[7:1]






  val ShiftEnded = RegInit(false.B)
  val RxOverrun  = RegInit(false.B)

  val BDWrite = RegInit(0.U(4.W))                  // BD Write Enable for access from WISHBONE side
  val BDRead  = RegInit(false.B)                     // BD Read access from WISHBONE side
  val RxBDDataIn = Wire(UInt(32.W))   // Rx BD data in
  val TxBDDataIn = Wire(UInt(32.W))   // Tx BD data in

  val TxEndFrm_wb = RegInit(false.B)

  val TxRetryPulse = Wire(Bool())
  val TxDonePulse  = Wire(Bool())
  val TxAbortPulse = Wire(Bool())

  val StartRxBDRead = Wire(Bool())
  val StartTxBDRead = Wire(Bool())

  val TxIRQEn = Wire(Bool())
  val WrapTxStatusBit = Wire(Bool())

  val RxIRQEn = Wire(Bool())
  val WrapRxStatusBit = Wire(Bool())

  val TxValidBytes = Wire(UInt(2.W))

  val TempTxBDAddress = Wire(UInt(7.W)) //[7:1]
  val TempRxBDAddress = Wire(UInt(7.W)) //[7:1]

  val RxStatusWrite = Wire(Bool())
  val RxBufferFull = Wire(Bool())
  val RxBufferAlmostEmpty = Wire(Bool())
  val RxBufferEmpty = Wire(Bool())

  val WB_ACK_O = Reg(Bool()); io.WB_ACK_O := WB_ACK_O

  val RxStatusIn = Wire(UInt(9.W))


  // Delayed stage signals
  val WbEn    = RegInit(true.B)
  val WbEn_q  = RegNext(WbEn, false.B)
  val RxEn   = RegInit(false.B)
  val RxEn_q = RegNext(RxEn, false.B)
  val TxEn   = RegInit(false.B)
  val TxEn_q = RegNext(TxEn, false.B)
  val r_TxEn_q = RegNext(io.r_TxEn, false.B) 
  val r_RxEn_q = RegNext(io.r_RxEn, false.B)

  val ram_ce = true.B
  val ram_we = Wire(UInt(4.W))
  val ram_oe = Wire(Bool())
  val ram_addr = RegInit(0.U(8.W))
  val ram_di = RegInit(0.U(32.W))
  val ram_do = Wire(UInt(32.W))
  val txBuffDesc = ram_do.asTypeOf(new TxBuffDesc)
  val rxBuffDesc = ram_do.asTypeOf(new RxBuffDesc)

  val StartTxPointerRead = Wire(Bool())
  val TxPointerRead = RegInit(false.B)
  val TxEn_needed = RegInit(false.B)
  val RxEn_needed = RegInit(false.B)

  val StartRxPointerRead = Wire(Bool())
  val RxPointerRead = RegInit(false.B)

  // RX shift ending signals
  val ShiftEnded_rck_txclk = Wire(Bool())
  val ShiftEndedSync1 = RegNext( ShiftEnded_rck_txclk, false.B)
  val ShiftEndedSync2 = RegNext( ShiftEndedSync1, false.B)
  val ShiftEndedSync3 = RegInit(false.B)



  val StartShiftWillEnd = Wire(Bool())

  val StartOccured      = RegInit(false.B)
  val TxStartFrm_syncb1 = RegInit(false.B)
  val TxStartFrm_syncb2 = RegInit(false.B)

  val TxFifoClear = Wire(Bool())
  val TxBufferAlmostFull = Wire(Bool())
  val TxBufferFull  = Wire(Bool())
  val TxBufferEmpty = Wire(Bool())
  val TxBufferAlmostEmpty = Wire(Bool())
  val SetReadTxDataFromMemory = Wire(Bool())
  val BlockReadTxDataFromMemory = RegInit(false.B)

  val tx_burst_en = RegInit(true.B)
  val rx_burst_en = RegInit(false.B)
  val tx_burst_cnt = RegInit(0.U(3.W))

  val ReadTxDataFromMemory_2 = Wire(Bool())
  val tx_burst = Wire(Bool())
  val m_wb_cti_o = RegInit(0.U(3.W)); io.m_wb_cti_o := m_wb_cti_o    // Cycle Type Identifier

  val TxData_wb = Wire(UInt(32.W))
  val ReadTxDataFromFifo_wb = Wire(Bool())

  val txfifo_cnt = Wire(UInt(5.W))
  val rxfifo_cnt = Wire(UInt(5.W))

  val rx_burst_cnt = RegInit(0.U(3.W))

  val rx_burst = Wire(Bool())
  val enough_data_in_rxfifo_for_burst = Wire(Bool())
  val enough_data_in_rxfifo_for_burst_plus1 = Wire(Bool())

  val ReadTxDataFromMemory = RegInit(false.B)
  val WriteRxDataToMemory = Wire(Bool())

  val MasterWbTX = RegInit(false.B)
  val MasterWbRX = RegInit(false.B)

  val m_wb_adr_o = RegInit(0.U(30.W)); io.m_wb_adr_o := m_wb_adr_o
  val m_wb_cyc_o = RegInit(false.B); io.m_wb_cyc_o := m_wb_cyc_o
  val m_wb_sel_o = RegInit(0.U(4.W)); io.m_wb_sel_o := m_wb_sel_o
  val m_wb_we_o  = RegInit(false.B); io.m_wb_we_o := m_wb_we_o

  val TxLengthEq0 = Wire(Bool())
  val TxLengthLt4 = Wire(Bool())

  val BlockingIncrementTxPointer = RegInit(false.B)
  val TxPointerMSB = RegInit(0.U(30.W)) //[31:2]
  val TxPointerLSB = RegInit(0.U(2.W))
  val TxPointerLSB_rst = RegInit(0.U(2.W))
  val RxPointerMSB = RegInit(0.U(30.W)) //[31:2]
  val RxPointerLSB_rst = RegInit(0.U(2.W))




  val ResetTxBDReady = Wire(Bool())


  val cyc_cleared = RegInit(false.B)
  val IncrTxPointer = RegInit(false.B)

  val RxByteSel = Wire(UInt(4.W))
  val MasterAccessFinished = Wire(Bool())

  val LatchValidBytes   = RegInit(false.B)
  val LatchValidBytes_q = RegNext(LatchValidBytes, false.B)

  // Start: Generation of the ReadTxDataFromFifo_tck signal and synchronization to the WB_CLK_I
  val ReadTxDataFromFifo_tck_txclk = Wire(Bool())
  val ReadTxDataFromFifo_sync1 = RegNext(ReadTxDataFromFifo_tck_txclk, false.B)
  val ReadTxDataFromFifo_sync2 = RegNext(ReadTxDataFromFifo_sync1, false.B)
  val ReadTxDataFromFifo_sync3 = RegNext(ReadTxDataFromFifo_sync2, false.B)

  val RxAbortLatched_rxclk = Wire(Bool())
  val RxAbortSync1 = RegNext( RxAbortLatched_rxclk, false.B )
  val RxAbortSync2 = RegNext( RxAbortSync1, false.B )
  val RxAbortSync3 = RegNext( RxAbortSync2, false.B )
  val RxAbortSync4 = RegNext( RxAbortSync3, false.B )


  val SetWriteRxDataToFifo = Wire(Bool())
  val WriteRxDataToFifo_rxclk = Wire(Bool())
  val WriteRxDataToFifoSync1 = RegNext( WriteRxDataToFifo_rxclk, false.B)
  val WriteRxDataToFifoSync2 = RegNext( WriteRxDataToFifoSync1, false.B)
  val WriteRxDataToFifoSync3 = RegNext( WriteRxDataToFifoSync2, false.B)

  val WriteRxDataToFifo_wb = Wire(Bool())
  val LatchedRxStartFrm_rxclk = Wire(Bool())
  val SyncRxStartFrm    = RegNext( LatchedRxStartFrm_rxclk, false.B)
  val SyncRxStartFrm_q  = RegNext( SyncRxStartFrm, false.B)
  val SyncRxStartFrm_q2 = RegNext( SyncRxStartFrm_q, false.B)


  val RxFifoReset = Wire(Bool())

  val TxError = Wire(Bool())
  val RxError = Wire(Bool())

  val RxStatusWriteLatched = RegInit(false.B)

  val RxStatusWriteLatched_syncb1 = RegNext(io.RxStatusWriteLatched_sync2, false.B)
  val RxStatusWriteLatched_syncb2 = RegNext(RxStatusWriteLatched_syncb1, false.B)


  io.m_wb_bte_o := "b00".U    // Linear burst
  io.m_wb_stb_o := m_wb_cyc_o


  when(true.B){
    WB_ACK_O := (BDWrite.orR & WbEn & WbEn_q) | (BDRead & WbEn & ~WbEn_q)
  }

  // Generic synchronous single-port RAM interface
  val bd_ram = Module(new MacSRAM)


  io.WB_DAT_O := ram_do



  bd_ram.io.ce := ram_ce
  bd_ram.io.we := ram_we.asBools
  bd_ram.io.oe := ram_oe
  bd_ram.io.addr := ram_addr
  bd_ram.io.di := ram_di
  ram_do := bd_ram.io.dato

  ram_we :=
    (BDWrite & Fill(4,(WbEn & WbEn_q)) ) |
    Fill(4, (TxStatusWrite | RxStatusWrite) )

  ram_oe :=
    (BDRead & WbEn & WbEn_q) |
    (TxEn & TxEn_q & (TxBDRead | TxPointerRead)) |
    (RxEn & RxEn_q & (RxBDRead | RxPointerRead))


  when(~TxBDReady & io.r_TxEn & WbEn & ~WbEn_q){
    TxEn_needed := true.B
  } .elsewhen(TxPointerRead & TxEn & TxEn_q){
    TxEn_needed := false.B
  }



  // Enabling access to the RAM for three devices.
  val RAMAccessEnable = 
    Cat(WbEn_q, RxEn_q, TxEn_q, RxEn_needed, TxEn_needed)

  // Switching between three stages depends on enable signals
  when( RAMAccessEnable === "b10010".U | RAMAccessEnable === "b10011".U ){  // synopsys parallel_case
    WbEn := false.B
    RxEn := true.B  // wb access stage and r_RxEn is enabled
    TxEn := false.B
    ram_addr := Cat(RxBDAddress, RxPointerRead)
    ram_di := RxBDDataIn
  } .elsewhen( RAMAccessEnable === "b10001".U ){
    WbEn := false.B
    RxEn := false.B
    TxEn := true.B  // wb access stage, r_RxEn is disabled but r_TxEn is enabled
    ram_addr := Cat(TxBDAddress, TxPointerRead)
    ram_di := TxBDDataIn;
  } .elsewhen( RAMAccessEnable === "b01000".U | RAMAccessEnable === "b01010".U ){
    WbEn := true.B  // RxEn access stage and r_TxEn is disabled
    RxEn := false.B
    TxEn := false.B
                ram_addr := io.WB_ADR_I // [9:2];
                ram_di  := io.WB_DAT_I;
                BDWrite := io.BDCs & Fill(4,io.WB_WE_I)
                BDRead  := io.BDCs.orR & ~io.WB_WE_I
  } .elsewhen( RAMAccessEnable === "b01001".U | RAMAccessEnable === "b01011".U ){
    WbEn := false.B
    RxEn := false.B
    TxEn := true.B  // RxEn access stage and r_TxEn is enabled
    ram_addr := Cat(TxBDAddress, TxPointerRead)
    ram_di := TxBDDataIn;      
  } .elsewhen( RAMAccessEnable === "b00100".U | RAMAccessEnable === "b00101".U | RAMAccessEnable === "b00110".U | RAMAccessEnable === "b00111".U ){
    WbEn := true.B  // TxEn access stage (we always go to wb access stage)
    RxEn := false.B
    TxEn := false.B
    ram_addr := io.WB_ADR_I //[9:2]
    ram_di  := io.WB_DAT_I
    BDWrite := io.BDCs & Fill(4,io.WB_WE_I)
    BDRead  := io.BDCs.orR & ~io.WB_WE_I
  } .elsewhen( RAMAccessEnable === "b10000".U ){
    WbEn := false.B // WbEn access stage and there is no need for other stages. WbEn needs to be switched off for a bit
  } .elsewhen( RAMAccessEnable === "b00000".U ){
    WbEn := true.B  // Idle state. We go to WbEn access stage.
    RxEn := false.B
    TxEn := false.B
    ram_addr := io.WB_ADR_I //[9:2]
    ram_di  := io.WB_DAT_I
    BDWrite := io.BDCs & Fill(4,io.WB_WE_I)
    BDRead  := io.BDCs.orR & ~io.WB_WE_I   
  }






























  ResetTxBDReady := TxDonePulse | TxAbortPulse | TxRetryPulse


  // Latching READY status of the Tx buffer descriptor
  when(TxEn & TxEn_q & TxBDRead){ // TxBDReady is sampled only once at the beginning.
    TxBDReady := txBuffDesc.rd & (txBuffDesc.len > 4.U)
  } .elsewhen(ResetTxBDReady){ // Only packets larger then 4 bytes are transmitted.
    TxBDReady := false.B
  }


  StartTxBDRead := (TxRetryPacket_NotCleared | TxStatusWrite) & ~BlockingTxBDRead & ~TxBDReady // Reading the Tx buffer descriptor

  when(StartTxBDRead){
    TxBDRead := true.B
  } .elsewhen(TxBDReady){
    TxBDRead := false.B
  }

  StartTxPointerRead := TxBDRead & TxBDReady  // Reading Tx BD pointer

  // Reading Tx BD Pointer
  when(StartTxPointerRead){
    TxPointerRead := true.B
  } .elsewhen(TxEn_q){
    TxPointerRead := false.B
  }



  // Writing status back to the Tx buffer descriptor
  TxStatusWrite := (TxDonePacket_NotCleared | TxAbortPacket_NotCleared) & TxEn & TxEn_q & ~BlockingTxStatusWrite


// Status writing must occur only once. Meanwhile it is blocked.
  when(~TxDone_wb & ~TxAbort_wb){
    BlockingTxStatusWrite := false.B
  } .elsewhen(TxStatusWrite){
    BlockingTxStatusWrite := true.B
  }




  val BlockingTxStatusWrite_sync2_txclk = Wire(Bool())
  val BlockingTxStatusWrite_sync3_txclk = Wire(Bool())
  io.RstDeferLatched := BlockingTxStatusWrite_sync2_txclk & ~BlockingTxStatusWrite_sync3_txclk

  // TxBDRead state is activated only once. 
  when(StartTxBDRead){
    BlockingTxBDRead := true.B
  } .elsewhen(~StartTxBDRead & ~TxBDReady){
    BlockingTxBDRead := false.B
  }



// Latching status from the tx buffer descriptor
// Data is avaliable one cycle after the access is started (at that time
// signal TxEn is not active)
  when(TxEn & TxEn_q & TxBDRead){
    TxStatus := Cat(txBuffDesc.irq, txBuffDesc.wr, txBuffDesc.pad, txBuffDesc.crc)
  }




  //Latching length from the buffer descriptor;
  when(TxEn & TxEn_q & TxBDRead){
    TxLength := txBuffDesc.len   
  } .elsewhen(MasterWbTX & io.m_wb_ack_i){
    when(TxLengthLt4){
      TxLength := 0.U
    } .elsewhen(TxPointerLSB_rst === 0.U){
      TxLength := TxLength - 4.U    // Length is subtracted at the data request
    } .elsewhen(TxPointerLSB_rst === 1.U){
      TxLength := TxLength - 3.U    // Length is subtracted at the data request
    } .elsewhen(TxPointerLSB_rst === 2.U){
      TxLength := TxLength - 2.U    // Length is subtracted at the data request
    } .elsewhen(TxPointerLSB_rst === 3.U){
      TxLength := TxLength - 1.U    // Length is subtracted at the data request
    }
  }


  //Latching length from the buffer descriptor;
  when(TxEn & TxEn_q & TxBDRead){
    LatchedTxLength := txBuffDesc.len   
  }



  TxLengthEq0 := TxLength === 0.U
  TxLengthLt4 := TxLength < 4.U


  // Latching Tx buffer pointer from buffer descriptor. Only 30 MSB bits are
  // latched because TxPointerMSB is only used for word-aligned accesses.
  when(TxEn & TxEn_q & TxPointerRead){
    TxPointerMSB := ram_do(31,2)    
  } .elsewhen(IncrTxPointer & ~BlockingIncrementTxPointer){
    TxPointerMSB := TxPointerMSB + 1.U // TxPointer is word-aligned
  }
    



  // Latching 2 MSB bits of the buffer descriptor. Since word accesses are performed, valid data does not necesserly start at byte 0 (could be byte 0, 1, 2 or 3). This signals are used for proper selection of the star byte (TxData and TxByteCnt) are set by this two bits.
  when(TxEn & TxEn_q & TxPointerRead){
    TxPointerLSB := ram_do(1,0)    
  }



  // Latching 2 MSB bits of the buffer descriptor.  After the read access, TxLength needs to be decremented for the number of the valid bytes (1 to 4 bytes are valid in the first word). After the first read all bytes are valid so this two bits are reset to zero. 
  when(TxEn & TxEn_q & TxPointerRead){
    TxPointerLSB_rst := ram_do(1,0)    
  } .elsewhen(MasterWbTX & io.m_wb_ack_i){ // After first access pointer is word alligned
    TxPointerLSB_rst := 0.U
  }



  when(MasterAccessFinished){
    BlockingIncrementTxPointer := false.B
  } .elsewhen(IncrTxPointer){
    BlockingIncrementTxPointer := true.B
  }


  SetReadTxDataFromMemory := TxEn & TxEn_q & TxPointerRead;

  when(TxLengthEq0 | TxAbortPulse | TxRetryPulse){
    ReadTxDataFromMemory := false.B
  } .elsewhen(SetReadTxDataFromMemory){
    ReadTxDataFromMemory := true.B
  }


  ReadTxDataFromMemory_2 := ReadTxDataFromMemory & ~BlockReadTxDataFromMemory

  tx_burst := ReadTxDataFromMemory_2 & tx_burst_en



  when((TxBufferAlmostFull | TxLength <= 4.U) & MasterWbTX & (~cyc_cleared) & (~(TxAbortPacket_NotCleared | TxRetryPacket_NotCleared))){
    BlockReadTxDataFromMemory := true.B
  } .elsewhen(ReadTxDataFromFifo_wb | TxDonePacket | TxAbortPacket | TxRetryPacket){
    BlockReadTxDataFromMemory := false.B
  }

  MasterAccessFinished := io.m_wb_ack_i | io.m_wb_err_i



// Enabling master wishbone access to the memory for two devices TX and RX.

val masterStage = Cat(MasterWbTX, MasterWbRX, ReadTxDataFromMemory_2, WriteRxDataToMemory, MasterAccessFinished, cyc_cleared, tx_burst, rx_burst)

      // Switching between two stages depends on enable signals


  when(
    masterStage === BitPat("b00100010") | // Idle and MRB needed
    masterStage === BitPat("b101?101?") | // MRB continues
    masterStage === BitPat("b10100110") | // Clear (previously MR) and MRB needed
    masterStage === BitPat("b011?011?")
  ){ // Clear (previously MW) and MRB needed
    MasterWbTX    := true.B  // tx burst
    MasterWbRX    := false.B
    m_wb_cyc_o    := true.B
    m_wb_we_o     := false.B
    m_wb_sel_o    := "hf".U
    cyc_cleared   := false.B
    IncrTxPointer := true.B
    tx_burst_cnt  := tx_burst_cnt + 1.U
    when(tx_burst_cnt === 0.U){
      m_wb_adr_o := TxPointerMSB
    } .otherwise{
      m_wb_adr_o := m_wb_adr_o + 1.U
    }

    when(tx_burst_cnt === 3.U) {
      tx_burst_en := false.B
      m_wb_cti_o  := "b111".U
    } .otherwise{
      m_wb_cti_o  := "b010".U
    }
  } .elsewhen(
    masterStage === BitPat("b00?100?1") |            // Idle and MWB needed
    masterStage === BitPat("b01?110?1") |            // MWB continues
    masterStage === BitPat("b01010101") |            // Clear (previously MW) and MWB needed
    masterStage === BitPat("b10?101?1")              // Clear (previously MR) and MWB needed
  ){
    MasterWbTX    := false.B  // rx burst
    MasterWbRX    := true.B
    m_wb_cyc_o    := true.B
    m_wb_we_o     := true.B
    m_wb_sel_o    := RxByteSel
    IncrTxPointer := false.B
    cyc_cleared   := false.B
    rx_burst_cnt  := rx_burst_cnt + 1.U

    when(rx_burst_cnt === 0.U ){
      m_wb_adr_o := RxPointerMSB
    } .otherwise{
      m_wb_adr_o := m_wb_adr_o + 1.U
    }

    when(rx_burst_cnt === 3.U ){
      rx_burst_en := false.B
      m_wb_cti_o  := "b111".U
    } .otherwise{
      m_wb_cti_o := "b010".U
    }       
  }.elsewhen( masterStage === BitPat("b00?100?0") ){ // idle and MW is needed (data write to rx buffer)
    MasterWbTX    := false.B
    MasterWbRX    := true.B
    m_wb_adr_o    := RxPointerMSB
    m_wb_cyc_o    := true.B
    m_wb_we_o     := true.B
    m_wb_sel_o    := RxByteSel
    IncrTxPointer := false.B
  }.elsewhen( masterStage === BitPat("b00100000") ){ // idle and MR is needed (data read from tx buffer)
    MasterWbTX    := true.B
    MasterWbRX    := false.B
    m_wb_adr_o    := TxPointerMSB;
    m_wb_cyc_o    := true.B
    m_wb_we_o     := false.B
    m_wb_sel_o    := "hf".U
    IncrTxPointer := true.B
  }.elsewhen( 
    masterStage === BitPat("b10100100") | // MR and MR is needed (data read from tx buffer)
    masterStage === BitPat("b011?010?")   // MW and MR is needed (data read from tx buffer)
  ){
    MasterWbTX    := true.B
    MasterWbRX    := false.B
    m_wb_adr_o    := TxPointerMSB;
    m_wb_cyc_o    := true.B
    m_wb_we_o     := false.B
    m_wb_sel_o    := "hf".U
    cyc_cleared   := false.B
    IncrTxPointer := true.B
  }.elsewhen( 
    masterStage === BitPat("b01010100") | // MW and MW needed (data write to rx buffer)
    masterStage === BitPat("b10?101?0")   // MR and MW is needed (data write to rx buffer)
  ){
    MasterWbTX    := false.B
    MasterWbRX    := true.B
    m_wb_adr_o    := RxPointerMSB;
    m_wb_cyc_o    := true.B
    m_wb_we_o     := true.B
    m_wb_sel_o    := RxByteSel;
    cyc_cleared   := false.B
    IncrTxPointer := false.B
  }.elsewhen( 
    masterStage === BitPat("b01011000") | // MW and MW needed (cycle is cleared between previous and next access)
    masterStage === BitPat("b011?10?0") | // MW and MW or MR or MRB needed (cycle is cleared between previous and next access)
    masterStage === BitPat("b10101000") | // MR and MR needed (cycle is cleared between previous and next access)
    masterStage === BitPat("b10?1100?")   // MR and MR or MW or MWB (cycle is cleared between previous and next access)
  ){
    m_wb_cyc_o    := false.B// whatever and master read or write is needed. We need to clear m_wb_cyc_o before next access is started
    cyc_cleared   := true.B
    IncrTxPointer := false.B
    tx_burst_cnt  := 0.U
    tx_burst_en   := (txfifo_cnt < 12.U) & (TxLength > 20.U)
    rx_burst_cnt  := 0.U
    rx_burst_en   := Mux(MasterWbRX, enough_data_in_rxfifo_for_burst_plus1, enough_data_in_rxfifo_for_burst)  // Counter is not decremented, yet, so plus1 is used.
    m_wb_cti_o    := 0.U
  }.elsewhen( 
    masterStage === BitPat("b??001000") | // whatever and no master read or write is needed (ack or err comes finishing previous access)
    masterStage === BitPat("b??000100")   // Between cyc_cleared request was cleared
  ){
    MasterWbTX    := false.B
    MasterWbRX    := false.B
    m_wb_cyc_o    := false.B
    cyc_cleared   := false.B
    IncrTxPointer := false.B
    rx_burst_cnt  := 0.U
    // Counter is not decremented, yet, so plus1 is used.
    rx_burst_en   := Mux(MasterWbRX, enough_data_in_rxfifo_for_burst_plus1, enough_data_in_rxfifo_for_burst)
    m_wb_cti_o    := 0.U
  }.elsewhen( masterStage === BitPat("b00000000") ){  // whatever and no master read or write is needed (ack or err comes finishing previous access)
    tx_burst_cnt := 0.U
    tx_burst_en  := (txfifo_cnt < 12.U) & (TxLength > 20.U)
  } .otherwise{

  }



  TxFifoClear := (TxAbortPacket | TxRetryPacket)

  val tx_fifo = Module( new MacFifo(dw = 32, dp = 16) )
  tx_fifo.io.data_in := io.m_wb_dat_i
  tx_fifo.io.write   := MasterWbTX & io.m_wb_ack_i
  tx_fifo.io.read    := ReadTxDataFromFifo_wb & ~TxBufferEmpty
  tx_fifo.io.clear   := TxFifoClear
  TxData_wb           := tx_fifo.io.data_out
  TxBufferFull        := tx_fifo.io.full
  TxBufferAlmostFull  := tx_fifo.io.almost_full
  TxBufferAlmostEmpty := tx_fifo.io.almost_empty
  TxBufferEmpty       := tx_fifo.io.empty
  txfifo_cnt          := tx_fifo.io.cnt



// Start: Generation of the TxStartFrm_wb which is then synchronized to the MTxClk
  when(TxBDReady & ~StartOccured & (TxBufferFull | TxLengthEq0)){
    TxStartFrm_wb := true.B
  } .elsewhen(TxStartFrm_syncb2){
    TxStartFrm_wb := false.B
  }


// StartOccured: TxStartFrm_wb occurs only ones at the beginning. Then it's blocked.
  when(TxStartFrm_wb){
    StartOccured := true.B
  } .elsewhen(ResetTxBDReady){
    StartOccured := false.B
  }


  val TxStartFrm_sync2_txclk = Wire(Bool())
  when(true.B){
    TxStartFrm_syncb1 := TxStartFrm_sync2_txclk
    TxStartFrm_syncb2 := TxStartFrm_syncb1
  }



  // TxEndFrm_wb: indicator of the end of frame
  when(TxLengthEq0 & TxBufferAlmostEmpty & io.TxUsedData){
    TxEndFrm_wb := true.B
  } .elsewhen(TxRetryPulse | TxDonePulse | TxAbortPulse){
    TxEndFrm_wb := false.B
  }


  // Marks which bytes are valid within the word.
  TxValidBytes := Mux(TxLengthLt4, TxLength(1,0), 0.U)



  when(TxLengthLt4 & TxBDReady){
    LatchValidBytes := true.B
  }.otherwise{
    LatchValidBytes := false.B
  }


  // Latching valid bytes
  when(LatchValidBytes & ~LatchValidBytes_q){
    TxValidBytesLatched := TxValidBytes
  } .elsewhen(TxRetryPulse | TxDonePulse | TxAbortPulse){
    TxValidBytesLatched := 0.U
  }


  // dontTouch(TxStatus)
  TxIRQEn         := TxStatus.extract(3) //[14:11]
  WrapTxStatusBit := TxStatus.extract(2)
  io.PerPacketPad    := TxStatus.extract(1)
  io.PerPacketCrcEn  := TxStatus.extract(0)

  RxIRQEn         := RxStatus.extract(1) //[14:13]
  WrapRxStatusBit := RxStatus.extract(0)


  // Temporary Tx and Rx buffer descriptor address
  TempTxBDAddress := Fill(7, TxStatusWrite & ~WrapTxStatusBit) & (TxBDAddress + 1.U) // Tx BD increment or wrap (last BD)

  TempRxBDAddress := 
    ( Fill(7, WrapRxStatusBit) & io.r_TxBDNum(6,0)  ) |   // Using first Rx BD
    ( Fill(7,~WrapRxStatusBit) & (RxBDAddress + 1.U)) // Using next Rx BD
                                                  // (increment address)

  // Latching Tx buffer descriptor address
  when(io.r_TxEn & (~r_TxEn_q)){
    TxBDAddress := 0.U
  } .elsewhen(TxStatusWrite){
    TxBDAddress := TempTxBDAddress  
  }

  // Latching Rx buffer descriptor address
  when(io.r_RxEn & (~r_RxEn_q)){
    RxBDAddress := io.r_TxBDNum(6,0)
  } .elsewhen(RxStatusWrite){
    RxBDAddress := TempRxBDAddress;    
  }


  val TxStatusInLatched = Cat(io.TxUnderRun, io.RetryCntLatched, io.RetryLimit, io.LateCollLatched, io.DeferLatched, io.CarrierSenseLost)
  val LatchedRxLength_rxclk = Wire(UInt(16.W))
  val RxStatusInLatched_rxclk = Wire(UInt(9.W))
  RxBDDataIn := Cat(LatchedRxLength_rxclk, 0.U(1.W), RxStatus, 0.U(4.W), RxStatusInLatched_rxclk)
  TxBDDataIn := Cat(LatchedTxLength, 0.U(1.W), TxStatus, 0.U(2.W), TxStatusInLatched)


  // Signals used for various purposes
  TxRetryPulse := TxRetry_wb   & ~TxRetry_wb_q
  TxDonePulse  := TxDone_wb    & ~TxDone_wb_q
  TxAbortPulse := TxAbort_wb   & ~TxAbort_wb_q









  val TxAbortPacketBlocked = RegInit(false.B)

  when(TxAbort_wb & (~tx_burst_en) & MasterWbTX & MasterAccessFinished &
    (~TxAbortPacketBlocked) | TxAbort_wb & (~MasterWbTX) &
    (~TxAbortPacketBlocked)){
    TxAbortPacket := true.B
  } .otherwise{
    TxAbortPacket := false.B
  }


  when(TxEn & TxEn_q & TxAbortPacket_NotCleared){
    TxAbortPacket_NotCleared := false.B
  } .elsewhen(TxAbort_wb & (~tx_burst_en) & MasterWbTX & MasterAccessFinished &
    (~TxAbortPacketBlocked) | TxAbort_wb & (~MasterWbTX) &
    (~TxAbortPacketBlocked)){
    TxAbortPacket_NotCleared := true.B
  }

  when(~TxAbort_wb & TxAbort_wb_q){
    TxAbortPacketBlocked := false.B
  } .elsewhen(TxAbortPacket){
    TxAbortPacketBlocked := true.B
  }






  val TxRetryPacketBlocked = RegInit(false.B)

  when(
    TxRetry_wb & ~tx_burst_en & MasterWbTX & MasterAccessFinished & ~TxRetryPacketBlocked |
    TxRetry_wb & ~MasterWbTX & ~TxRetryPacketBlocked){
    TxRetryPacket := true.B
  } .otherwise{
    TxRetryPacket := false.B
  }





  when(StartTxBDRead){
    TxRetryPacket_NotCleared := false.B
  } .elsewhen(
    TxRetry_wb & ~tx_burst_en & MasterWbTX & MasterAccessFinished & ~TxRetryPacketBlocked |
    TxRetry_wb & ~MasterWbTX & ~TxRetryPacketBlocked){
    TxRetryPacket_NotCleared := true.B
  }


  when(~TxRetry_wb & TxRetry_wb_q){
    TxRetryPacketBlocked := false.B
  } .elsewhen(TxRetryPacket){
    TxRetryPacketBlocked := true.B
  }



  val TxDonePacketBlocked = RegInit(false.B)

  when(
    TxDone_wb & ~tx_burst_en & MasterWbTX & MasterAccessFinished & ~TxDonePacketBlocked |
    TxDone_wb & ~MasterWbTX  & ~TxDonePacketBlocked){
    TxDonePacket := true.B
  }.otherwise{
    TxDonePacket := false.B
  }

  when(TxEn & TxEn_q & TxDonePacket_NotCleared){
    TxDonePacket_NotCleared := false.B
  } .elsewhen(
    TxDone_wb & ~tx_burst_en & MasterWbTX & MasterAccessFinished & (~TxDonePacketBlocked) |
    TxDone_wb & ~MasterWbTX & (~TxDonePacketBlocked)){
    TxDonePacket_NotCleared := true.B
  }


  when(~TxDone_wb & TxDone_wb_q){
    TxDonePacketBlocked := false.B
  } .elsewhen(TxDonePacket){
    TxDonePacketBlocked := true.B
  }
  


  // Tx under run
  when(TxAbortPulse){
    TxUnderRun_wb := false.B    
  } .elsewhen(TxBufferEmpty & ReadTxDataFromFifo_wb){
    TxUnderRun_wb := true.B
  }


  ReadTxDataFromFifo_wb := ReadTxDataFromFifo_sync2 & ~ReadTxDataFromFifo_sync3
  // End: Generation of the ReadTxDataFromFifo_tck signal and synchronization to the WB_CLK_I


  StartRxBDRead :=
    RxStatusWrite |
    RxAbortSync3 & ~RxAbortSync4 |
    io.r_RxEn & ~r_RxEn_q

  // Reading the Rx buffer descriptor
  when(StartRxBDRead & ~RxReady){
    RxBDRead := true.B
  } .elsewhen(RxBDReady){
    RxBDRead := false.B
  }



// Reading of the next receive buffer descriptor starts after reception status
// is written to the previous one.

  // Latching READY status of the Rx buffer descriptor
  when(RxPointerRead){
    RxBDReady := false.B
  } .elsewhen(RxEn & RxEn_q & RxBDRead){
    RxBDReady := rxBuffDesc.e// RxBDReady is sampled only once at the beginning    
  }


  // Latching Rx buffer descriptor status
  // Data is avaliable one cycle after the access is started (at that time
  // signal RxEn is not active)
  when(RxEn & RxEn_q & RxBDRead){
    RxStatus := Cat(rxBuffDesc.irq, rxBuffDesc.wrap)
  }



  // RxReady generation
  when(ShiftEnded | RxAbortSync2 & ~RxAbortSync3 | ~io.r_RxEn & r_RxEn_q){
    RxReady := false.B
  } .elsewhen(RxEn & RxEn_q & RxPointerRead){
    RxReady := true.B
  }

  // Reading Rx BD pointer
  StartRxPointerRead := RxBDRead & RxBDReady

  // Reading Tx BD Pointer
  when(StartRxPointerRead){
    RxPointerRead := true.B
  } .elsewhen(RxEn & RxEn_q){
    RxPointerRead := false.B
  }



  //Latching Rx buffer pointer from buffer descriptor;
  when(RxEn & RxEn_q & RxPointerRead){
    RxPointerMSB := ram_do(31,2)    
  } .elsewhen(MasterWbRX & io.m_wb_ack_i){
    RxPointerMSB := RxPointerMSB + 1.U // Word access (always word access. m_wb_sel_o are used for selecting bytes)
  }

  //Latching last addresses from buffer descriptor (used as byte-half-word indicator);
  when(MasterWbRX & io.m_wb_ack_i){// After first write all RxByteSel are active
    RxPointerLSB_rst := 0.U
  } .elsewhen(RxEn & RxEn_q & RxPointerRead){
    RxPointerLSB_rst := ram_do(1,0)    
  }

  RxByteSel := Mux1H(Seq(
    (RxPointerLSB_rst === 0.U) -> "hf".U,
    (RxPointerLSB_rst === 1.U) -> "h7".U,
    (RxPointerLSB_rst === 2.U) -> "h3".U,
    (RxPointerLSB_rst === 3.U) -> "h1".U,
  ))


  when(~RxReady & io.r_RxEn & WbEn & ~WbEn_q){
    RxEn_needed := true.B
  } .elsewhen(RxPointerRead & RxEn & RxEn_q){
    RxEn_needed := false.B
  }



  // Reception status is written back to the buffer descriptor after the end of frame is detected.
  RxStatusWrite := ShiftEnded & RxEn & RxEn_q




  val LastByteIn_rxclk = Wire(Bool())
  val RxByteCnt_rxclk  = Wire(UInt(2.W))
  val RxEnableWindow_rxclk = Wire(Bool())
  StartShiftWillEnd := LastByteIn_rxclk | io.RxValid & io.RxEndFrm & RxByteCnt_rxclk.andR & RxEnableWindow_rxclk

  // Indicating start of the reception process
  val ShiftWillEnd_rxclk = Wire(Bool())
  SetWriteRxDataToFifo :=
    (io.RxValid & RxReady & ~io.RxStartFrm & RxEnableWindow_rxclk & (RxByteCnt_rxclk.andR)) |
    (io.RxValid & RxReady &  io.RxStartFrm & (RxPointerLSB_rst.andR)) |
    (ShiftWillEnd_rxclk & LastByteIn_rxclk & (RxByteCnt_rxclk.andR))



  WriteRxDataToFifo_wb := WriteRxDataToFifoSync2 & ~WriteRxDataToFifoSync3



  RxFifoReset := SyncRxStartFrm_q & ~SyncRxStartFrm_q2

  val rx_fifo = Module(new MacFifo(dw = 32, dp = 16))
  val RxDataLatched2_rxclk = Wire(UInt(32.W))
  rx_fifo.io.data_in := RxDataLatched2_rxclk
  rx_fifo.io.write   := WriteRxDataToFifo_wb & ~RxBufferFull
  rx_fifo.io.read    := MasterWbRX & io.m_wb_ack_i
  rx_fifo.io.clear   := RxFifoReset

  io.m_wb_dat_o := rx_fifo.io.data_out
  RxBufferFull := rx_fifo.io.full
  RxBufferAlmostEmpty := rx_fifo.io.almost_empty
  RxBufferEmpty := rx_fifo.io.empty
  rxfifo_cnt := rx_fifo.io.cnt



  enough_data_in_rxfifo_for_burst       := rxfifo_cnt >= 4.U
  enough_data_in_rxfifo_for_burst_plus1 := rxfifo_cnt > 4.U

  WriteRxDataToMemory := ~RxBufferEmpty
  rx_burst            := rx_burst_en & WriteRxDataToMemory
  




  when(ShiftEndedSync1 & ~ShiftEndedSync2){
    ShiftEndedSync3 := true.B
  } .elsewhen(ShiftEnded){
    ShiftEndedSync3 := false.B
  }


  // Generation of the end-of-frame signal
  when(ShiftEndedSync3 & MasterWbRX & io.m_wb_ack_i & RxBufferAlmostEmpty & ~ShiftEnded){
    ShiftEnded := true.B
  } .elsewhen(RxStatusWrite){
    ShiftEnded := false.B
  }





  RxStatusIn := Cat(io.ReceivedPauseFrm, io.AddressMiss, RxOverrun, io.InvalidSymbol, io.DribbleNibble, io.ReceivedPacketTooBig, io.ShortFrame, io.LatchedCrcError, io.RxLateCollision)


  // Rx overrun
  when(RxStatusWrite){
    RxOverrun := false.B
  } .elsewhen(RxBufferFull & WriteRxDataToFifo_wb){
    RxOverrun := true.B
  }




  TxError := io.TxUnderRun | io.RetryLimit | io.LateCollLatched | io.CarrierSenseLost


  // ShortFrame (RxStatusInLatched[2]) can not set an error because short frames are aborted when signal r_RecSmall is set to 0 in MODER register. 
  // AddressMiss is identifying that a frame was received because of the promiscous mode and is not an error
  RxError := (RxStatusInLatched_rxclk(6,3).orR) | (RxStatusInLatched_rxclk(1,0).orR)


  // Latching and synchronizing RxStatusWrite signal. This signal is used for clearing the ReceivedPauseFrm signal
  when(RxStatusWriteLatched_syncb2){
    RxStatusWriteLatched := false.B
  } .elsewhen(RxStatusWrite){
    RxStatusWriteLatched := true.B
  }






  // Tx Done Interrupt
  when(TxStatusWrite & TxIRQEn){
    TxB_IRQ := ~TxError
  } .otherwise{
    TxB_IRQ := false.B
  }

  // Tx Error Interrupt
  when(TxStatusWrite & TxIRQEn){
    TxE_IRQ := TxError
  } .otherwise{
    TxE_IRQ := false.B
  }

  // Rx Done Interrupt
  when(RxStatusWrite & RxIRQEn & io.ReceivedPacketGood & (~io.ReceivedPauseFrm | io.ReceivedPauseFrm & io.r_PassAll & (~io.r_RxFlow))){
    RxB_IRQ := (~RxError)
  } .otherwise{
    RxB_IRQ := false.B
  }

  // Rx Error Interrupt
  when(RxStatusWrite & RxIRQEn & (~io.ReceivedPauseFrm | io.ReceivedPauseFrm & io.r_PassAll & (~io.r_RxFlow))){
    RxE_IRQ := RxError
  } .otherwise{
    RxE_IRQ := false.B
  }




  // Busy Interrupt
  val Busy_IRQ_rck_rxclk = Wire(Bool())
  val Busy_IRQ_sync1 = RegNext(Busy_IRQ_rck_rxclk)
  val Busy_IRQ_sync2 = RegNext(Busy_IRQ_sync1)
  val Busy_IRQ_sync3 = RegNext(Busy_IRQ_sync2)
  val Busy_IRQ_syncb1 = RegNext(Busy_IRQ_sync2, false.B)
  val Busy_IRQ_syncb2 = RegNext(Busy_IRQ_syncb1, false.B)

  



  io.Busy_IRQ := Busy_IRQ_sync2 & ~Busy_IRQ_sync3


}






















trait MacTileLinkTXClk{ this: MacTileLinkBase =>
  withClockAndReset( io.MTxClk.asClock, reset ) {

    val Flop = RegInit(false.B)
    // Synchronizing BlockingTxStatusWrite to MTxClk
    val BlockingTxStatusWrite_sync1 = RegNext(BlockingTxStatusWrite, false.B)
    val BlockingTxStatusWrite_sync2 = RegNext(BlockingTxStatusWrite_sync1, false.B); BlockingTxStatusWrite_sync2_txclk := BlockingTxStatusWrite_sync2
    val BlockingTxStatusWrite_sync3 = RegNext(BlockingTxStatusWrite_sync2, false.B); BlockingTxStatusWrite_sync3_txclk := BlockingTxStatusWrite_sync3
    // Synchronizing TxStartFrm_wb to MTxClk
    val TxStartFrm_sync1  = RegNext( TxStartFrm_wb, false.B)
    val TxStartFrm_sync2  = RegNext( TxStartFrm_sync1, false.B); TxStartFrm_sync2_txclk := TxStartFrm_sync2
    val TxStartFrm = RegInit(false.B);   io.TxStartFrm := TxStartFrm
    val TxEndFrm   = RegInit(false.B);   io.TxEndFrm   := TxEndFrm
    val TxData     = RegInit(0.U(8.W)); io.TxData := TxData
    val TxUnderRun = RegInit(false.B); io.TxUnderRun := TxUnderRun
    val TxDataLatched = RegInit(0.U(32.W))
    val TxByteCnt = RegInit(0.U(2.W))
    val LastWord = RegInit(false.B)
    val ReadTxDataFromFifo_tck = RegInit(false.B); ReadTxDataFromFifo_tck_txclk := ReadTxDataFromFifo_tck

    // Generating delayed signals
    val TxAbort_q    = RegNext( io.TxAbort, false.B)
    val TxRetry_q    = RegNext( io.TxRetry, false.B)
    val TxUsedData_q = RegNext( io.TxUsedData, false.B)

    val ReadTxDataFromFifo_syncb1 = RegNext( ReadTxDataFromFifo_sync2, false.B)
    val ReadTxDataFromFifo_syncb2 = RegNext( ReadTxDataFromFifo_syncb1, false.B)
    val ReadTxDataFromFifo_syncb3 = RegNext( ReadTxDataFromFifo_syncb2, false.B)

    // Changes for tx occur every second clock. Flop is used for this manner.
    when( io.TxDone | io.TxAbort | TxRetry_q){
      Flop := false.B
    } .elsewhen ( io.TxUsedData ){
      Flop := ~Flop
    }

    when(TxStartFrm_sync2){
      TxStartFrm := true.B
    } .elsewhen(TxUsedData_q | ~TxStartFrm_sync2 & (io.TxRetry & (~TxRetry_q) | io.TxAbort & (~TxAbort_q))){
      TxStartFrm := false.B
    }


    // Indication of the last word
    when( (TxEndFrm | io.TxAbort | io.TxRetry) & Flop ){
      LastWord := false.B
    } .elsewhen( io.TxUsedData & Flop & TxByteCnt === 3.U ){
      LastWord := TxEndFrm_wb
    }

    // Tx end frame generation
    when(Flop & TxEndFrm | io.TxAbort | TxRetry_q){
      TxEndFrm := false.B
    } .elsewhen(Flop & LastWord){
      TxEndFrm := 
        Mux1H(Seq(
          (TxValidBytesLatched === 1.U) -> (TxByteCnt === 0.U),
          (TxValidBytesLatched === 2.U) -> (TxByteCnt === 1.U),
          (TxValidBytesLatched === 3.U) -> (TxByteCnt === 2.U),
          (TxValidBytesLatched === 0.U) -> (TxByteCnt === 3.U),
        ))  
    }



    // Tx data selection (latching)
    when( TxStartFrm_sync2 & ~TxStartFrm ){
      TxData := Mux1H(Seq(
        ( TxPointerLSB === 0.U ) -> TxData_wb(31,24),// Big Endian Byte Ordering
        ( TxPointerLSB === 1.U ) -> TxData_wb(23,16),// Big Endian Byte Ordering
        ( TxPointerLSB === 2.U ) -> TxData_wb(15, 8),// Big Endian Byte Ordering
        ( TxPointerLSB === 3.U ) -> TxData_wb( 7, 0),// Big Endian Byte Ordering
      ))     
    } .elsewhen( TxStartFrm & io.TxUsedData & TxPointerLSB === 3.U ){
      TxData := TxData_wb(31,24) // Big Endian Byte Ordering        
    } .elsewhen(io.TxUsedData & Flop){
      TxData := Mux1H(Seq(
        (TxByteCnt === 0.U) -> TxDataLatched(31,24),// Big Endian Byte Ordering
        (TxByteCnt === 1.U) -> TxDataLatched(23,16),
        (TxByteCnt === 2.U) -> TxDataLatched(15, 8),
        (TxByteCnt === 3.U) -> TxDataLatched( 7, 0),
      ))
    }



    // Latching tx data
    when(
      TxStartFrm_sync2 & ~TxStartFrm |
      io.TxUsedData & Flop & TxByteCnt === 3.U |
      TxStartFrm & io.TxUsedData & Flop & TxByteCnt === 0.U){
      TxDataLatched := TxData_wb
    }


    val TxUnderRun_sync1 = RegInit(false.B)

    // Tx under run
    when(TxUnderRun_wb){
      TxUnderRun_sync1 := true.B
    } .elsewhen(BlockingTxStatusWrite_sync2){
      TxUnderRun_sync1 := false.B
    }


    // Tx under run
    when(BlockingTxStatusWrite_sync2){
      TxUnderRun := false.B
    } .elsewhen(TxUnderRun_sync1){
      TxUnderRun := true.B
    }



    // Tx Byte counter
    when(TxAbort_q | TxRetry_q){
      TxByteCnt := 0.U
    } .elsewhen(TxStartFrm & ~io.TxUsedData){
      TxByteCnt := Mux1H(Seq(
        ( TxPointerLSB === 0.U ) -> 1.U,
        ( TxPointerLSB === 1.U ) -> 2.U,
        ( TxPointerLSB === 2.U ) -> 3.U,
        ( TxPointerLSB === 3.U ) -> 0.U,
      ))
    } .elsewhen(io.TxUsedData & Flop){
      TxByteCnt := TxByteCnt + 1.U
    }

    when(TxStartFrm_sync2 & ~TxStartFrm | io.TxUsedData & Flop & TxByteCnt === 3.U &
      ~LastWord | TxStartFrm & io.TxUsedData & Flop & TxByteCnt === 0.U ){
      ReadTxDataFromFifo_tck := true.B
    } .elsewhen(ReadTxDataFromFifo_syncb2 & ~ReadTxDataFromFifo_syncb3){
      ReadTxDataFromFifo_tck := false.B
    }


  }
}




































trait MacTileLinkRXClk{ this: MacTileLinkBase =>

  withClockAndReset( io.MRxClk.asClock, reset ){

    val RxDataLatched2 = RegInit(0.U(32.W)); RxDataLatched2_rxclk := RxDataLatched2
    val RxDataLatched1 = RegInit(0.U(24.W))     // Big Endian Byte Ordering[31:8] 
    val RxValidBytes = RegInit(1.U(2.W))
    val RxByteCnt    = RegInit(0.U(2.W)); RxByteCnt_rxclk := RxByteCnt
    val LastByteIn   = RegInit(false.B); LastByteIn_rxclk := LastByteIn
    val ShiftWillEnd = RegInit(false.B); ShiftWillEnd_rxclk := ShiftWillEnd
    val WriteRxDataToFifo = RegInit(false.B); WriteRxDataToFifo_rxclk := WriteRxDataToFifo
    val LatchedRxLength = RegInit(0.U(16.W)); LatchedRxLength_rxclk := LatchedRxLength
    val RxAbortLatched = RegInit(false.B); RxAbortLatched_rxclk := RxAbortLatched
    val RxStatusInLatched = RegInit(0.U(9.W)); RxStatusInLatched_rxclk := RxStatusInLatched
    val ShiftEnded_rck = RegInit(false.B); ShiftEnded_rck_txclk := ShiftEnded_rck
    val ShiftEndedSync_c1 = RegNext( ShiftEndedSync2, false.B)
    val ShiftEndedSync_c2 = RegNext( ShiftEndedSync_c1, false.B)
    val RxAbortSyncb1 = RegNext( RxAbortSync2, false.B)
    val RxAbortSyncb2 = RegNext( RxAbortSyncb1, false.B)
    val RxEnableWindow = RegInit(false.B); RxEnableWindow_rxclk := RxEnableWindow
    val LatchedRxStartFrm = RegInit(false.B); LatchedRxStartFrm_rxclk := LatchedRxStartFrm
    val RxStatusWriteLatched_sync1 = RegNext(RxStatusWriteLatched, false.B)
    val RxStatusWriteLatched_sync2 = RegNext(RxStatusWriteLatched_sync1, false.B); io.RxStatusWriteLatched_sync2 := RxStatusWriteLatched_sync2
    // Indicating that last byte is being reveived

    when(ShiftWillEnd & RxByteCnt.andR | io.RxAbort){
      LastByteIn := false.B
    } .elsewhen(io.RxValid & RxReady & io.RxEndFrm & ~(RxByteCnt.andR) & RxEnableWindow){
      LastByteIn := true.B
    }

    // Indicating that data reception will end
    when(ShiftEnded_rck | io.RxAbort){
      ShiftWillEnd := false.B
    } .elsewhen(StartShiftWillEnd){
      ShiftWillEnd := true.B
    }

    // Receive byte counter
    when(ShiftEnded_rck | io.RxAbort){
      RxByteCnt := 0.U
    } .elsewhen(io.RxValid & io.RxStartFrm & RxReady){
      RxByteCnt := Mux1H(Seq(
        ( RxPointerLSB_rst === 0.U ) -> 1.U,
        ( RxPointerLSB_rst === 1.U ) -> 2.U,
        ( RxPointerLSB_rst === 2.U ) -> 3.U,
        ( RxPointerLSB_rst === 3.U ) -> 0.U,
      ))       
    } .elsewhen(io.RxValid & RxEnableWindow & RxReady | LastByteIn){
      RxByteCnt := RxByteCnt + 1.U
    }

    // Indicates how many bytes are valid within the last word
    when(io.RxValid & io.RxStartFrm){
      RxValidBytes := Mux1H(Seq(
        ( RxPointerLSB_rst === 0.U ) -> 1.U,
        ( RxPointerLSB_rst === 1.U ) -> 2.U,
        ( RxPointerLSB_rst === 2.U ) -> 3.U,
        ( RxPointerLSB_rst === 3.U ) -> 0.U,
      ))
    } .elsewhen(io.RxValid & ~LastByteIn & ~io.RxStartFrm & RxEnableWindow){
      RxValidBytes := RxValidBytes + 1.U        
    }

    when(io.RxValid & RxReady & ~LastByteIn){
      when(io.RxStartFrm){
        RxDataLatched1 := Mux1H(Seq(
          ( RxPointerLSB_rst === 0.U ) -> Cat(                       io.RxData, RxDataLatched1(15,0)),// Big Endian Byte Ordering
          ( RxPointerLSB_rst === 1.U ) -> Cat(RxDataLatched1(23,16), io.RxData, RxDataLatched1( 7,0)),
          ( RxPointerLSB_rst === 2.U ) -> Cat(RxDataLatched1(23, 8), io.RxData),
          ( RxPointerLSB_rst === 3.U ) -> RxDataLatched1,
        ))
      } .elsewhen(RxEnableWindow){
        RxDataLatched1 := Mux1H(Seq(
          ( RxByteCnt === 0.U ) -> Cat(                       io.RxData, RxDataLatched1(15,0)),// Big Endian Byte Ordering
          ( RxByteCnt === 1.U ) -> Cat(RxDataLatched1(23,16), io.RxData, RxDataLatched1( 7,0)),
          ( RxByteCnt === 2.U ) -> Cat(RxDataLatched1(23, 8), io.RxData),
          ( RxByteCnt === 3.U ) -> RxDataLatched1,
        ))
      }
    }


    // Assembling data that will be written to the rx_fifo
    when(SetWriteRxDataToFifo & ~ShiftWillEnd){
      RxDataLatched2 := Cat(RxDataLatched1, io.RxData)// Big Endian Byte Ordering
    } .elsewhen(SetWriteRxDataToFifo & ShiftWillEnd){
      RxDataLatched2 := Mux1H(Seq(
        ( RxValidBytes === 0.U ) -> Cat(RxDataLatched1,        io.RxData),
        ( RxValidBytes === 1.U ) -> Cat(RxDataLatched1(23,16), 0.U(24.W)),
        ( RxValidBytes === 2.U ) -> Cat(RxDataLatched1(23, 8), 0.U(16.W)),
        ( RxValidBytes === 3.U ) -> Cat(RxDataLatched1,        0.U(8.W)),       
      ))
    }

    when(SetWriteRxDataToFifo & ~io.RxAbort){
      WriteRxDataToFifo := true.B
    } .elsewhen(WriteRxDataToFifoSync2 | io.RxAbort){
      WriteRxDataToFifo := false.B
    }


    when(io.RxStartFrm & ~SyncRxStartFrm_q){
      LatchedRxStartFrm := true.B
    } .elsewhen(SyncRxStartFrm_q){
      LatchedRxStartFrm := false.B
    }


    // Generation of the end-of-frame signal
    when(~io.RxAbort & SetWriteRxDataToFifo & StartShiftWillEnd){
      ShiftEnded_rck := true.B
    } .elsewhen(io.RxAbort | ShiftEndedSync_c1 & ShiftEndedSync_c2){
      ShiftEnded_rck := false.B
    }

  // Generation of the end-of-frame signal
    when(io.RxStartFrm){
      RxEnableWindow := true.B
    } .elsewhen(io.RxEndFrm | io.RxAbort){
      RxEnableWindow := false.B
    }


    when(RxAbortSyncb2){
      RxAbortLatched := false.B
    } .elsewhen(io.RxAbort){
      RxAbortLatched := true.B
    }

    when(io.LoadRxStatus){
      LatchedRxLength := io.RxLength
    }


    when(io.LoadRxStatus){
      RxStatusInLatched := RxStatusIn
    }


    val Busy_IRQ_rck = RegInit(false.B); Busy_IRQ_rck_rxclk := Busy_IRQ_rck

    when(io.RxValid & io.RxStartFrm & ~RxReady){
      Busy_IRQ_rck := true.B
    } .elsewhen(Busy_IRQ_syncb2){
      Busy_IRQ_rck := false.B
    }

    when(true.B){
      Busy_IRQ_syncb1 := Busy_IRQ_sync2
      Busy_IRQ_syncb2 := Busy_IRQ_syncb1
    }

  }
}

class MacTileLink extends MacTileLinkBase with MacTileLinkTXClk with MacTileLinkRXClk

















// trait MacTileLinkSlave{ this: MacTileLinkBase =>

//   val a = Flipped(new DecoupledIO(new TLBundleA(edge.bundle)))
//   val d = new DecoupledIO(new TLBundleD(edge.bundle))

//   val tlSlvDValid = RegInit(false.B); io.d.valid := tlSlvDValid


// }

