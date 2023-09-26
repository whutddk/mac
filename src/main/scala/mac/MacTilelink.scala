package MAC

import chisel3._
import chisel3.util._

import freechips.rocketchip.tilelink._
import freechips.rocketchip.diplomacy._
import org.chipsalliance.cde.config._








abstract class MacTileLinkBase(edgeIn: TLEdgeIn, edgeOut: TLEdgeOut) extends Module{

  class MacTileLinkSlaveIO extends Bundle{
    val A = Flipped(Decoupled(new TLBundleA(edgeIn.bundle)))
    val D = Decoupled(new TLBundleD(edgeIn.bundle))
  }

  class MacTileLinkMasterIO extends Bundle{
    val A = Decoupled(new TLBundleA(edgeOut.bundle))
    val D = Flipped(Decoupled(new TLBundleD(edgeOut.bundle)))
  }

  class MacTileLinkIO extends Bundle{

    val tlSlv = new MacTileLinkSlaveIO
    val tlMst = new MacTileLinkMasterIO

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

    val asyncReset = Input(AsyncReset())
  }


  
  val io = IO(new MacTileLinkIO)
    

  val (_, _, isLastD, transDCnt) = edgeOut.count(io.tlMst.D)


  val BDCs = Wire(UInt(4.W))

  val TxB_IRQ = RegInit(false.B); io.TxB_IRQ := TxB_IRQ
  val TxE_IRQ = RegInit(false.B); io.TxE_IRQ := TxE_IRQ
  val RxB_IRQ = RegInit(false.B); io.RxB_IRQ := RxB_IRQ
  val RxE_IRQ = RegInit(false.B); io.RxE_IRQ := RxE_IRQ




  val TxUnderRun_wb = RegInit(false.B)

  val TxBDRead = RegInit(true.B)
  val TxStatusWrite = Wire(Bool())



  val TxLength = RegInit(0.U(16.W))
  val LatchedTxLength = RegInit(0.U(16.W))
  val TxStatus = RegInit(0.U(4.W)) //[14:11]
  val RxStatus = RegInit(0.U(2.W)) //[14:13]




  val TxStartFrm_wb = RegInit(false.B)

  // Synchronizing TxRetry TxDone_wb TxAbort signal (synchronized to WISHBONE clock)
  val TxRetry_wb = ShiftRegisters( io.TxRetry, 3, false.B, true.B )
  val TxAbort_wb = ShiftRegisters( io.TxAbort, 3, false.B, true.B )
  val TxDone_wb  = ShiftRegisters( io.TxDone,  3, false.B, true.B )

  // Signals used for various purposes
  val TxRetryPulse = TxRetry_wb(1) & ~TxRetry_wb(2)
  val TxDonePulse  =  TxDone_wb(1) &  ~TxDone_wb(2)
  val TxAbortPulse = TxAbort_wb(1) & ~TxAbort_wb(2)

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











  val RxStatusWrite = Wire(Bool())
  val RxBufferFull = Wire(Bool())
  val RxBufferAlmostEmpty = Wire(Bool())
  val RxBufferEmpty = Wire(Bool())

  val BDAck = Reg(Bool());



  // Delayed stage signals
    // val WbEn    = RegInit(true.B)
    // val WbEn_q  = RegNext(WbEn, false.B)
    // val RxEn   = RegInit(false.B)
    // val RxEn_q = RegNext(RxEn, false.B)
    // val TxEn   = RegInit(false.B)
    // val TxEn_q = RegNext(TxEn, false.B)
  val r_TxEn_q = RegNext(io.r_TxEn, false.B) 
  val r_RxEn_q = RegNext(io.r_RxEn, false.B)

  def StateIdle = 0.U(3.W)
  def StateWB   = 1.U(3.W)
  def StateTX   = 2.U(3.W)
  def StateRX   = 3.U(3.W)

  val stateNxt = RegInit( StateWB )
  val stateCur = RegNext( stateNxt, StateIdle )

  


  val ram_ce = true.B
  val ram_we = Wire(UInt(4.W))
  val ram_oe = Wire(Bool())
  val ram_addr = RegInit(0.U(8.W))
  val ram_di = RegInit(0.U(32.W))
  val ram_do = Wire(UInt(32.W))
  val txBuffDesc = ram_do.asTypeOf(new TxBuffDesc)
  val rxBuffDesc = ram_do.asTypeOf(new RxBuffDesc)


  val TxPointerRead = RegInit(false.B)
  val TxEn_needed = RegInit(false.B)
  val RxEn_needed = RegInit(false.B)

  val RxPointerRead = RegInit(false.B)

  // RX shift ending signals
  val ShiftEnded_rck_txclk = Wire(Bool())
  val ShiftEndedSync1 = RegNext( ShiftEnded_rck_txclk, false.B)
  val ShiftEndedSync2 = RegNext( ShiftEndedSync1, false.B)
  val ShiftEndedSync3 = RegInit(false.B)


  

  val StartOccured      = RegInit(false.B)
  val TxStartFrm_sync_txclk = Wire(Bool())
  val TxStartFrm_syncb1 = RegNext(TxStartFrm_sync_txclk, false.B)
  val TxStartFrm_syncb2 = RegNext(TxStartFrm_syncb1, false.B)

  val TxFifoClear = Wire(Bool())
  val TxBufferAlmostFull = Wire(Bool())
  val TxBufferFull  = Wire(Bool())
  val TxBufferEmpty = Wire(Bool())
  val TxBufferAlmostEmpty = Wire(Bool())
  val BlockReadTxDataFromMemory = RegInit(false.B)


  val TxData_wb = Wire(UInt(32.W))
  val ReadTxDataFromFifo_wb = Wire(Bool())

  val txfifo_cnt = Wire(UInt(5.W))
  val rxfifo_cnt = Wire(UInt(5.W))

  val ReadTxDataFromMemory = RegInit(false.B)
  val WriteRxDataToMemory = Wire(Bool())

  val MasterWbTX = RegInit(false.B)
  val MasterWbRX = RegInit(false.B)


  val TxPointerMSB = RegInit(0.U(30.W)) //[31:2]
  val TxPointerLSB = RegInit(0.U(2.W))
  val TxPointerLSB_rst = RegInit(0.U(2.W))
  val RxPointerMSB = RegInit(0.U(30.W)) //[31:2]
  val RxPointerLSB_rst = RegInit(0.U(2.W))






  val cyc_cleared = RegInit(false.B)

  val RxByteSel = Wire(UInt(4.W))




  // Start: Generation of the ReadTxDataFromFifo_tck signal and synchronization to the WB_CLK_I
  val ReadTxDataFromFifo_tck_txclk = Wire(Bool())

  val ReadTxDataFromFifo_sync = ShiftRegisters( ReadTxDataFromFifo_tck_txclk, 3, false.B, true.B)


  val RxAbortLatched_rxclk = Wire(Bool())
  val RxAbortSync = ShiftRegisters( RxAbortLatched_rxclk, 4, false.B, true.B )

  val WriteRxDataToFifo_rxclk = Wire(Bool())
  val WriteRxDataToFifoSync = ShiftRegisters(WriteRxDataToFifo_rxclk, 3, false.B, true.B)


  val LatchedRxStartFrm_rxclk = Wire(Bool())
  val SyncRxStartFrm = ShiftRegisters(LatchedRxStartFrm_rxclk, 3, false.B, true.B)


  val RxStatusWriteLatched = RegInit(false.B)

  val RxStatusWriteLatched_syncb = ShiftRegister(io.RxStatusWriteLatched_sync2, 2, false.B, true.B)





  when(true.B){
    BDAck := stateNxt === StateWB & Mux( stateCur === StateWB , BDWrite.orR, BDRead )
  }

  // Generic synchronous single-port RAM interface
  val bd_ram = Module(new MacSRAM)


  val BD_WB_DAT_O = ram_do



  bd_ram.io.ce := ram_ce
  bd_ram.io.we := ram_we.asBools
  bd_ram.io.oe := ram_oe
  bd_ram.io.addr := ram_addr
  bd_ram.io.di := ram_di
  ram_do := bd_ram.io.dato

  ram_we :=
    (Fill(4, (stateNxt === StateWB & stateCur === StateWB)) & BDWrite  ) |
    (Fill(4, (TxStatusWrite | RxStatusWrite)              )            )

  ram_oe :=
    (   BDRead                  & ( stateNxt === StateWB ) & ( stateCur === StateWB ) ) |
    ((TxBDRead | TxPointerRead) & ( stateNxt === StateTX ) & ( stateCur === StateTX ) ) |
    ((RxBDRead | RxPointerRead) & ( stateNxt === StateRX ) & ( stateCur === StateRX ) )


  when(~TxBDReady & io.r_TxEn & stateNxt === StateWB & stateCur =/= StateWB){
    TxEn_needed := true.B
  } .elsewhen(TxPointerRead & stateNxt === StateTX & stateCur === StateTX){
    TxEn_needed := false.B
  }



  // Enabling access to the RAM for three devices.
  // Switching between three stages depends on enable signals
  switch( stateCur ){
    is(StateIdle){
      when( RxEn_needed === false.B & TxEn_needed === false.B ){
        stateNxt := StateWB // Idle state. We go to WbEn access stage.
        
        ram_addr := io.tlSlv.A.bits.address(9,2) // [11:2 ] -> [9:2]
        ram_di   := io.tlSlv.A.bits.data
        BDWrite  := BDCs & Fill(4,(io.tlSlv.A.bits.opcode === 0.U) || (io.tlSlv.A.bits.opcode === 1.U))
        BDRead   := BDCs.orR & (io.tlSlv.A.bits.opcode === 4.U)
      }

    }
    is(StateWB){
      when( RxEn_needed ){  // synopsys parallel_case
        stateNxt := StateRX // wb access stage and r_RxEn is enabled

        ram_addr := Cat(RxBDAddress, RxPointerRead)
        ram_di := RxBDDataIn
      } .elsewhen( TxEn_needed ){
        stateNxt := StateTX // wb access stage, r_RxEn is disabled but r_TxEn is enabled

        ram_addr := Cat(TxBDAddress, TxPointerRead) //[7,1] + [0]
        ram_di := TxBDDataIn
      } .otherwise{
        stateNxt := StateIdle // WbEn access stage and there is no need for other stages. WbEn needs to be switched off for a bit
      }
    }
    is(StateRX){
      when( TxEn_needed ){
        stateNxt := StateTX  // RxEn access stage and r_TxEn is enabled

        ram_addr := Cat(TxBDAddress, TxPointerRead)
        ram_di := TxBDDataIn
      } .otherwise{
        stateNxt := StateWB  // RxEn access stage and r_TxEn is disabled

        ram_addr := io.tlSlv.A.bits.address(9,2) // [11:2 ] -> [9:2];
        ram_di   := io.tlSlv.A.bits.data
        BDWrite  := BDCs & Fill(4,(io.tlSlv.A.bits.opcode === 0.U) || (io.tlSlv.A.bits.opcode === 1.U))
        BDRead   := BDCs.orR & (io.tlSlv.A.bits.opcode === 4.U)
      }
    }
    is(StateTX){
      when( true.B ){
        stateNxt := StateWB  // TxEn access stage (we always go to wb access stage)

        ram_addr := io.tlSlv.A.bits.address(9,2) //[11:2 ] ->[9:2]
        ram_di   := io.tlSlv.A.bits.data
        BDWrite  := BDCs & Fill(4,(io.tlSlv.A.bits.opcode === 0.U) || (io.tlSlv.A.bits.opcode === 1.U)) 
        BDRead   := BDCs.orR & (io.tlSlv.A.bits.opcode === 4.U)
      }
    }
  }





  val ResetTxBDReady = TxDonePulse | TxAbortPulse | TxRetryPulse


  // Latching READY status of the Tx buffer descriptor
  when(stateNxt === StateTX & stateCur === StateTX & TxBDRead){ // TxBDReady is sampled only once at the beginning.
    TxBDReady := txBuffDesc.rd & (txBuffDesc.len > 4.U)
  } .elsewhen(ResetTxBDReady){ // Only packets larger then 4 bytes are transmitted.
    TxBDReady := false.B
  }


  val StartTxBDRead = (TxRetryPacket_NotCleared | TxStatusWrite) & ~BlockingTxBDRead & ~TxBDReady // Reading the Tx buffer descriptor

  when(StartTxBDRead){
    TxBDRead := true.B
  } .elsewhen(TxBDReady){
    TxBDRead := false.B
  }

  val StartTxPointerRead = TxBDRead & TxBDReady  // Reading Tx BD pointer

  // Reading Tx BD Pointer
  when(StartTxPointerRead){
    TxPointerRead := true.B
  } .elsewhen(stateCur === StateTX){
    TxPointerRead := false.B
  }



  // Writing status back to the Tx buffer descriptor
  TxStatusWrite := (TxDonePacket_NotCleared | TxAbortPacket_NotCleared) & stateNxt === StateTX & stateCur === StateTX & ~BlockingTxStatusWrite


  // Status writing must occur only once. Meanwhile it is blocked.
  when(~TxDone_wb(1) & ~TxAbort_wb(1)){
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



  // Latching status from the tx buffer descriptor Data is avaliable one cycle after the access is started (at that time signal TxEn is not active)
  when(stateNxt === StateTX & stateCur === StateTX & TxBDRead){
    TxStatus := Cat(txBuffDesc.irq, txBuffDesc.wr, txBuffDesc.pad, txBuffDesc.crc)
  }




  //Latching length from the buffer descriptor;
  when(stateNxt === StateTX & stateCur === StateTX & TxBDRead){
    TxLength := txBuffDesc.len   
  } 
  .elsewhen( MasterWbTX & io.tlMst.D.fire ){ //tx tileRead
    when( TxLength < 4.U ){
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
  when(stateNxt === StateTX & stateCur === StateTX & TxBDRead){
    LatchedTxLength := txBuffDesc.len   
  }




  when(stateNxt === StateTX & stateCur === StateTX & TxPointerRead){
    TxPointerMSB := ram_do(31,2)  // Latching Tx buffer pointer from buffer descriptor. Only 30 MSB bits are latched because TxPointerMSB is only used for word-aligned accesses.
    TxPointerLSB := ram_do(1,0)   // Latching 2 MSB bits of the buffer descriptor. Since word accesses are performed, valid data does not necesserly start at byte 0 (could be byte 0, 1, 2 or 3). This signals are used for proper selection of the star byte (TxData and TxByteCnt) are set by this two bits.
  } .elsewhen( io.tlMst.D.fire & io.tlMst.D.bits.opcode === 1.U ){
    TxPointerMSB := TxPointerMSB + 1.U // TxPointer is word-aligned
  }
    

  // Latching 2 MSB bits of the buffer descriptor.  After the read access, TxLength needs to be decremented for the number of the valid bytes (1 to 4 bytes are valid in the first word). After the first read all bytes are valid so this two bits are reset to zero. 
  when(stateNxt === StateTX & stateCur === StateTX & TxPointerRead){
    TxPointerLSB_rst := ram_do(1,0)    
  } .elsewhen( MasterWbTX & io.tlMst.D.fire ){ // After first access pointer is word alligned
    TxPointerLSB_rst := 0.U
  }


  val isTlMstBusy = RegInit(false.B)


  when( (TxLength === 0.U) | TxAbortPulse | TxRetryPulse){
    ReadTxDataFromMemory := false.B
  } .elsewhen(stateNxt === StateTX & stateCur === StateTX & TxPointerRead){
    ReadTxDataFromMemory := true.B
  }

  val ReadTxDataFromMemory_2 = ReadTxDataFromMemory & ~BlockReadTxDataFromMemory;

  when(
    (TxBufferAlmostFull | TxLength <= 4.U) & MasterWbTX & isTlMstBusy & (~(TxAbortPacket_NotCleared | TxRetryPacket_NotCleared))){
    BlockReadTxDataFromMemory := true.B
  } .elsewhen(ReadTxDataFromFifo_wb | TxDonePacket | TxAbortPacket | TxRetryPacket){
    BlockReadTxDataFromMemory := false.B
  }
































































  TxFifoClear := (TxAbortPacket | TxRetryPacket)

  val tx_fifo = Module( new MacFifo(dw = 32, dp = 16) )
    tx_fifo.io.data_in := io.tlMst.D.bits.data
    tx_fifo.io.write   := io.tlMst.D.fire & io.tlMst.D.bits.opcode === 1.U


  tx_fifo.io.read    := ReadTxDataFromFifo_wb & ~TxBufferEmpty
  tx_fifo.io.clear   := TxFifoClear
  TxData_wb           := tx_fifo.io.data_out
  TxBufferFull        := tx_fifo.io.full
  TxBufferAlmostFull  := tx_fifo.io.almost_full
  TxBufferAlmostEmpty := tx_fifo.io.almost_empty
  TxBufferEmpty       := tx_fifo.io.empty
  txfifo_cnt          := tx_fifo.io.cnt



  // Start: Generation of the TxStartFrm_wb which is then synchronized to the MTxClk
  when(TxBDReady & ~StartOccured & (TxBufferFull | TxLength === 0.U)){
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


  // TxEndFrm_wb: indicator of the end of frame
  when((TxLength === 0.U) & TxBufferAlmostEmpty & io.TxUsedData){
    TxEndFrm_wb := true.B
  } .elsewhen(TxRetryPulse | TxDonePulse | TxAbortPulse){
    TxEndFrm_wb := false.B
  }


  // Marks which bytes are valid within the word.
  val TxValidBytes = Mux(TxLength < 4.U, TxLength(1,0), 0.U)
  val TxValidBytesLatched = RegInit(0.U(2.W))


  val LatchValidBytes   = ShiftRegisters((TxLength < 4.U) & TxBDReady, 2, false.B, true.B)

  // Latching valid bytes
  when(LatchValidBytes(0) & ~LatchValidBytes(1)){
    TxValidBytesLatched := TxValidBytes
  } .elsewhen(TxRetryPulse | TxDonePulse | TxAbortPulse){
    TxValidBytesLatched := 0.U
  }


  // dontTouch(TxStatus)
  val TxIRQEn         = TxStatus.extract(3) //[14:11]
  val WrapTxStatusBit = TxStatus.extract(2)
  io.PerPacketPad    := TxStatus.extract(1)
  io.PerPacketCrcEn  := TxStatus.extract(0)

  val RxIRQEn         = RxStatus.extract(1) //[14:13]
  val WrapRxStatusBit = RxStatus.extract(0)


  // Temporary Tx and Rx buffer descriptor address//[7:1]
  val TempTxBDAddress = Mux( TxStatusWrite & ~WrapTxStatusBit, (TxBDAddress + 1.U), 0.U ) // Tx BD increment or wrap (last BD)
  val TempRxBDAddress = Mux( WrapRxStatusBit,                    io.r_TxBDNum(6,0), (RxBDAddress + 1.U) ) // Using first Rx BD / Using next Rx BD


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




  val TxError = io.TxUnderRun | io.RetryLimit | io.LateCollLatched | io.CarrierSenseLost







  val TxAbortPacketBlocked = RegInit(false.B)

  when(
    TxAbort_wb(1) & (~TxAbortPacketBlocked) &   MasterWbTX  & io.tlMst.D.fire & isLastD  |
    TxAbort_wb(1) & (~TxAbortPacketBlocked) & (~MasterWbTX) ){
    TxAbortPacket := true.B
  } .otherwise{
    TxAbortPacket := false.B
  }


  when(stateNxt === StateTX & stateCur === StateTX & TxAbortPacket_NotCleared){
    TxAbortPacket_NotCleared := false.B
  } .elsewhen(
    TxAbort_wb(1) & (~TxAbortPacketBlocked) &   MasterWbTX  & io.tlMst.D.fire & isLastD |
    TxAbort_wb(1) & (~TxAbortPacketBlocked) & (~MasterWbTX) ){
    TxAbortPacket_NotCleared := true.B
  }

  when(~TxAbort_wb(1) & TxAbort_wb(2)){
    TxAbortPacketBlocked := false.B
  } .elsewhen(TxAbortPacket){
    TxAbortPacketBlocked := true.B
  }



  val TxRetryPacketBlocked = RegInit(false.B)

  when(
    TxRetry_wb(1) & ~TxRetryPacketBlocked &  MasterWbTX & io.tlMst.D.fire & isLastD |
    TxRetry_wb(1) & ~TxRetryPacketBlocked & ~MasterWbTX ){
    TxRetryPacket := true.B
  } .otherwise{
    TxRetryPacket := false.B
  }





  when(StartTxBDRead){
    TxRetryPacket_NotCleared := false.B
  } .elsewhen(
    TxRetry_wb(1) & ~TxRetryPacketBlocked &  MasterWbTX & io.tlMst.D.fire & isLastD |
    TxRetry_wb(1) & ~TxRetryPacketBlocked & ~MasterWbTX ){
    TxRetryPacket_NotCleared := true.B
  }


  when(~TxRetry_wb(1) & TxRetry_wb(2)){
    TxRetryPacketBlocked := false.B
  } .elsewhen(TxRetryPacket){
    TxRetryPacketBlocked := true.B
  }



  val TxDonePacketBlocked = RegInit(false.B)

  when(
    TxDone_wb(1) & ~TxDonePacketBlocked &  MasterWbTX & io.tlMst.D.fire & isLastD |
    TxDone_wb(1) & ~TxDonePacketBlocked & ~MasterWbTX  ){
    TxDonePacket := true.B
  }.otherwise{
    TxDonePacket := false.B
  }

  when(stateNxt === StateTX & stateCur === StateTX & TxDonePacket_NotCleared){
    TxDonePacket_NotCleared := false.B
  } .elsewhen(
    TxDone_wb(1) & ~TxDonePacketBlocked &  MasterWbTX & io.tlMst.D.fire & isLastD |
    TxDone_wb(1) & ~TxDonePacketBlocked & ~MasterWbTX ){
    TxDonePacket_NotCleared := true.B
  }


  when(~TxDone_wb(1) & TxDone_wb(2)){
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


  ReadTxDataFromFifo_wb := ReadTxDataFromFifo_sync(1) & ~ReadTxDataFromFifo_sync(2)

  val StartRxBDRead = RxStatusWrite | (RxAbortSync(2) & ~RxAbortSync(3)) | (io.r_RxEn & ~r_RxEn_q)

  // Reading the Rx buffer descriptor
  when(StartRxBDRead & ~RxReady){
    RxBDRead := true.B
  } .elsewhen(RxBDReady){
    RxBDRead := false.B
  }



  // Reading of the next receive buffer descriptor starts after reception status is written to the previous one.

  // Latching READY status of the Rx buffer descriptor
  when(RxPointerRead){
    RxBDReady := false.B
  } .elsewhen(stateNxt === StateRX & stateCur === StateRX & RxBDRead){
    RxBDReady := rxBuffDesc.e  // RxBDReady is sampled only once at the beginning    
  }


  // Latching Rx buffer descriptor status Data is avaliable one cycle after the access is started (at that time signal RxEn is not active)
  when(stateNxt === StateRX & stateCur === StateRX & RxBDRead){
    RxStatus := Cat(rxBuffDesc.irq, rxBuffDesc.wrap)
  }



  // RxReady generation
  when(ShiftEnded | RxAbortSync(1) & ~RxAbortSync(2) | ~io.r_RxEn & r_RxEn_q){
    RxReady := false.B
  } .elsewhen(stateNxt === StateRX & stateCur === StateRX & RxPointerRead){
    RxReady := true.B
  }

  // Reading Rx BD pointer
  val StartRxPointerRead = RxBDRead & RxBDReady

  // Reading Tx BD Pointer
  when(StartRxPointerRead){
    RxPointerRead := true.B
  } .elsewhen(stateNxt === StateRX & stateCur === StateRX){
    RxPointerRead := false.B
  }



  //Latching Rx buffer pointer from buffer descriptor;
  when(stateNxt === StateRX & stateCur === StateRX & RxPointerRead){
    RxPointerMSB := ram_do(31,2)    
  } .elsewhen(MasterWbRX & io.tlMst.A.fire ){
    RxPointerMSB := RxPointerMSB + 1.U // Word access (always word access. m_wb_sel_o are used for selecting bytes)
  }

  //Latching last addresses from buffer descriptor (used as byte-half-word indicator);
  when(MasterWbRX & io.tlMst.A.fire ){// After first write all RxByteSel are active
    RxPointerLSB_rst := 0.U
  } .elsewhen(stateNxt === StateRX & stateCur === StateRX & RxPointerRead){
    RxPointerLSB_rst := ram_do(1,0)    
  }

  RxByteSel := Mux1H(Seq(
    (RxPointerLSB_rst === 0.U) -> "hf".U,
    (RxPointerLSB_rst === 1.U) -> "h7".U,
    (RxPointerLSB_rst === 2.U) -> "h3".U,
    (RxPointerLSB_rst === 3.U) -> "h1".U,
  ))


  when(~RxReady & io.r_RxEn & stateNxt === StateWB & stateCur =/= StateWB){
    RxEn_needed := true.B
  } .elsewhen(RxPointerRead & stateNxt === StateRX & stateCur === StateRX){
    RxEn_needed := false.B
  }



  // Reception status is written back to the buffer descriptor after the end of frame is detected.
  RxStatusWrite := ShiftEnded & stateNxt === StateRX & stateCur === StateRX




  val LastByteIn_rxclk = Wire(Bool())
  val RxByteCnt_rxclk  = Wire(UInt(2.W))
  val RxEnableWindow_rxclk = Wire(Bool())
  val StartShiftWillEnd = LastByteIn_rxclk | io.RxValid & io.RxEndFrm & RxByteCnt_rxclk.andR & RxEnableWindow_rxclk

  // Indicating start of the reception process
  val ShiftWillEnd_rxclk = Wire(Bool())
  val SetWriteRxDataToFifo =
    (io.RxValid & RxReady & ~io.RxStartFrm & RxEnableWindow_rxclk & (RxByteCnt_rxclk.andR)) |
    (io.RxValid & RxReady &  io.RxStartFrm & (RxPointerLSB_rst.andR)) |
    (ShiftWillEnd_rxclk & LastByteIn_rxclk & (RxByteCnt_rxclk.andR))


  val WriteRxDataToFifo_wb  = WriteRxDataToFifoSync(1) & ~WriteRxDataToFifoSync(2)
  val RxFifoReset = SyncRxStartFrm(1) & ~SyncRxStartFrm(2)

  val rx_fifo = Module(new MacFifo(dw = 32, dp = 16))
  val RxDataLatched2_rxclk = Wire(UInt(32.W))
  rx_fifo.io.data_in := RxDataLatched2_rxclk
  rx_fifo.io.write   := WriteRxDataToFifo_wb & ~RxBufferFull
  rx_fifo.io.read    := MasterWbRX & io.tlMst.A.fire
  rx_fifo.io.clear   := RxFifoReset



  RxBufferFull := rx_fifo.io.full
  RxBufferAlmostEmpty := rx_fifo.io.almost_empty
  RxBufferEmpty := rx_fifo.io.empty
  rxfifo_cnt := rx_fifo.io.cnt





  WriteRxDataToMemory := ~RxBufferEmpty
 




  when(ShiftEndedSync1 & ~ShiftEndedSync2){
    ShiftEndedSync3 := true.B
  } .elsewhen(ShiftEnded){
    ShiftEndedSync3 := false.B
  }


  // Generation of the end-of-frame signal
  when(ShiftEndedSync3 & MasterWbRX & io.tlMst.A.fire & RxBufferAlmostEmpty & ~ShiftEnded){
    ShiftEnded := true.B
  } .elsewhen(RxStatusWrite){
    ShiftEnded := false.B
  }




  val RxStatusIn = Cat(io.ReceivedPauseFrm, io.AddressMiss, RxOverrun, io.InvalidSymbol, io.DribbleNibble, io.ReceivedPacketTooBig, io.ShortFrame, io.LatchedCrcError, io.RxLateCollision)


  // Rx overrun
  when(RxStatusWrite){
    RxOverrun := false.B
  } .elsewhen(RxBufferFull & WriteRxDataToFifo_wb){
    RxOverrun := true.B
  }






  // ShortFrame (RxStatusInLatched[2]) can not set an error because short frames are aborted when signal r_RecSmall is set to 0 in MODER register. 
  // AddressMiss is identifying that a frame was received because of the promiscous mode and is not an error
  val RxError = (RxStatusInLatched_rxclk(6,3).orR) | (RxStatusInLatched_rxclk(1,0).orR)


  // Latching and synchronizing RxStatusWrite signal. This signal is used for clearing the ReceivedPauseFrm signal
  when(RxStatusWriteLatched_syncb){
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
  val Busy_IRQ_sync = ShiftRegisters(Busy_IRQ_rck_rxclk, 3)


  io.Busy_IRQ := Busy_IRQ_sync(1) & ~Busy_IRQ_sync(2)



  // Connected to registers
  // val CsMiss = Wire(Bool())



   
       BDCs  := Fill(4, io.tlSlv.A.valid & io.tlSlv.A.bits.mask.orR & io.tlSlv.A.bits.address(10)) & io.tlSlv.A.bits.mask // 0x400 - 0x7FF
      // CsMiss :=         io.tlSlv.A.valid & io.tlSlv.A.bits.mask.orR &  io.tlSlv.A.bits.address(11)    // 0x800 - 0xfFF     // When access to the address between 0x800 and 0xfff occurs, acknowledge is set but data is not valid.
    
    val slvAInfo = RegEnable( io.tlSlv.A.bits, io.tlSlv.A.fire )
    val slvDValid = RegInit(false.B); io.tlSlv.D.valid := slvDValid
    val slvDDat = Reg(UInt(32.W))



    when( io.tlSlv.D.fire ){
      slvDValid := false.B
    } .elsewhen(io.tlSlv.A.fire){
      slvDValid := true.B
      slvDDat := BD_WB_DAT_O
    }

    when(slvAInfo.opcode === 4.U) {
      io.tlSlv.D.bits := edgeIn.AccessAck(slvAInfo, slvDDat)
    } .otherwise {
      io.tlSlv.D.bits := edgeIn.AccessAck(slvAInfo)
    }

    io.tlSlv.A.ready := BDAck
    assert( ~(io.tlSlv.A.ready & ~io.tlSlv.A.valid) )

    // when( io.tlSlv.A.fire & (~(io.tlSlv.A.bits.mask.orR) | CsMiss) ){
    //   assert( false.B, "Assert Failed, tileLink access an undefine region!" )
    // }















  val mstAValid = RegInit(false.B)
  val mstABits  = Reg(new TLBundleA(edgeOut.bundle))



  // val tlMstStateDnxt = WireDefault()
  // val tlMstState     = RegNext(  )



  when( io.tlMst.A.fire ){
    mstAValid := false.B
  } 
  .elsewhen( MasterWbRX & ~isTlMstBusy ) {
    mstAValid := true.B
    mstABits :=
      edgeOut.Put(
        fromSource = 0.U,
        toAddress = RxPointerMSB << 2,
        lgSize = log2Ceil(32/8).U,
        data = rx_fifo.io.data_out,
        mask = RxByteSel,
      )._2
  }
  .elsewhen( MasterWbTX & ~isTlMstBusy ){
    mstAValid := true.B
    mstABits :=
      edgeOut.Get(
        fromSource = 0.U,
        toAddress = TxPointerMSB << 2,
        lgSize = log2Ceil(32/8).U,
      )._2
  }

  when( io.tlMst.A.fire ){
    isTlMstBusy := true.B
  } .elsewhen( io.tlMst.D.fire ){
    isTlMstBusy := false.B
  }

  when( ~MasterWbTX & ~MasterWbRX ){
    when( WriteRxDataToMemory ){
      MasterWbRX := true.B
    } .elsewhen(ReadTxDataFromMemory_2) {
      MasterWbTX := true.B
    }
  } .elsewhen( ~MasterWbTX & MasterWbRX){ //1.4A + 1D fifo to memory
    when( io.tlMst.D.fire & isLastD & ~WriteRxDataToMemory ){
      MasterWbRX := false.B
    }
  } .elsewhen( MasterWbTX & ~MasterWbRX){ //1 A + 1.4D memory to fifo
    when( io.tlMst.D.fire & isLastD & ~ReadTxDataFromMemory_2 ){
      MasterWbTX := false.B
    }
  }


  when(io.tlMst.D.fire & io.tlMst.D.bits.opcode === 1.U) { assert( MasterWbTX ) }
  when(io.tlMst.D.fire & io.tlMst.D.bits.opcode === 0.U) { assert( MasterWbRX ) }


    val tlMstAValid_dbg = RegInit(true.B)
    io.tlMst.A.valid := mstAValid & tlMstAValid_dbg
    io.tlMst.A.bits  := mstABits


    val tlMstDReady = RegInit(true.B)

    dontTouch(tlMstDReady)
    dontTouch(tlMstAValid_dbg)
    io.tlMst.D.ready := tlMstDReady












}



















trait MacTileLinkTXClk{ this: MacTileLinkBase =>
  withClockAndReset( io.MTxClk.asClock, io.asyncReset ) {

    val Flop = RegInit(false.B)
    // Synchronizing BlockingTxStatusWrite to MTxClk
    val BlockingTxStatusWrite_sync1 = RegNext(BlockingTxStatusWrite, false.B)
    val BlockingTxStatusWrite_sync2 = RegNext(BlockingTxStatusWrite_sync1, false.B); BlockingTxStatusWrite_sync2_txclk := BlockingTxStatusWrite_sync2
    val BlockingTxStatusWrite_sync3 = RegNext(BlockingTxStatusWrite_sync2, false.B); BlockingTxStatusWrite_sync3_txclk := BlockingTxStatusWrite_sync3

    // Synchronizing TxStartFrm_wb to MTxClk
    val TxStartFrm_sync = ShiftRegister( TxStartFrm_wb, 2, false.B, true.B ); TxStartFrm_sync_txclk := TxStartFrm_sync

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

    val ReadTxDataFromFifo_syncb = ShiftRegisters(ReadTxDataFromFifo_sync(1), 3, false.B, true.B)

    // Changes for tx occur every second clock. Flop is used for this manner.
    when( io.TxDone | io.TxAbort | TxRetry_q){
      Flop := false.B
    } .elsewhen ( io.TxUsedData ){
      Flop := ~Flop
    }

    when(TxStartFrm_sync){
      TxStartFrm := true.B
    } .elsewhen(TxUsedData_q | ~TxStartFrm_sync & (io.TxRetry & (~TxRetry_q) | io.TxAbort & (~TxAbort_q))){
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
    when( TxStartFrm_sync & ~TxStartFrm ){
      TxData := Mux1H(Seq(
        ( TxPointerLSB === 0.U ) -> TxData_wb( 7, 0),// little Endian Byte Ordering
        ( TxPointerLSB === 1.U ) -> TxData_wb(15, 8),// little Endian Byte Ordering
        ( TxPointerLSB === 2.U ) -> TxData_wb(23,16),// little Endian Byte Ordering
        ( TxPointerLSB === 3.U ) -> TxData_wb(31,24),// little Endian Byte Ordering
      ))     
    } .elsewhen( TxStartFrm & io.TxUsedData & TxPointerLSB === 3.U ){
      TxData := TxData_wb( 7, 0) // little Endian Byte Ordering        
    } .elsewhen(io.TxUsedData & Flop){
      TxData := Mux1H(Seq(
        (TxByteCnt === 0.U) -> TxDataLatched( 7, 0),// little Endian Byte Ordering
        (TxByteCnt === 1.U) -> TxDataLatched(15, 8),
        (TxByteCnt === 2.U) -> TxDataLatched(23,16),
        (TxByteCnt === 3.U) -> TxDataLatched(31,24),
      ))
    }



    // Latching tx data
    when(
      TxStartFrm_sync & ~TxStartFrm |
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

    when(TxStartFrm_sync & ~TxStartFrm | io.TxUsedData & Flop & TxByteCnt === 3.U &
      ~LastWord | TxStartFrm & io.TxUsedData & Flop & TxByteCnt === 0.U ){
      ReadTxDataFromFifo_tck := true.B
    } .elsewhen(ReadTxDataFromFifo_syncb(1) & ~ReadTxDataFromFifo_syncb(2)){
      ReadTxDataFromFifo_tck := false.B
    }


  }
}



































trait MacTileLinkRXClk{ this: MacTileLinkBase =>

  withClockAndReset( io.MRxClk.asClock, io.asyncReset ){

    val RxDataLatched2 = RegInit(0.U(32.W)); RxDataLatched2_rxclk := RxDataLatched2
    val RxDataLatched1 = RegInit(0.U(24.W))     // Little Endian Byte Ordering[23:0] 
    val RxValidBytes = RegInit(1.U(2.W))
    val RxByteCnt    = RegInit(0.U(2.W)); RxByteCnt_rxclk := RxByteCnt
    val LastByteIn   = RegInit(false.B); LastByteIn_rxclk := LastByteIn
    val ShiftWillEnd = RegInit(false.B); ShiftWillEnd_rxclk := ShiftWillEnd
    val WriteRxDataToFifo = RegInit(false.B); WriteRxDataToFifo_rxclk := WriteRxDataToFifo
    val RxAbortLatched = RegInit(false.B); RxAbortLatched_rxclk := RxAbortLatched

    val LatchedRxLength = RegEnable(io.RxLength, 0.U(16.W), io.LoadRxStatus); LatchedRxLength_rxclk := LatchedRxLength
    val RxStatusInLatched = RegEnable(RxStatusIn, 0.U(9.W), io.LoadRxStatus); RxStatusInLatched_rxclk := RxStatusInLatched
    val ShiftEnded_rck = RegInit(false.B); ShiftEnded_rck_txclk := ShiftEnded_rck

    val ShiftEndedSync = ShiftRegisters(ShiftEndedSync2, 2, false.B, true.B)

    val RxAbortSyncb = ShiftRegister( RxAbortSync(1), 2, false.B, true.B )

    val RxEnableWindow = RegInit(false.B); RxEnableWindow_rxclk := RxEnableWindow
    val LatchedRxStartFrm = RegInit(false.B); LatchedRxStartFrm_rxclk := LatchedRxStartFrm

    val RxStatusWriteLatched_sync = ShiftRegister(RxStatusWriteLatched, 2, false.B, true.B); io.RxStatusWriteLatched_sync2 := RxStatusWriteLatched_sync
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
          ( RxPointerLSB_rst === 0.U ) -> Cat(RxDataLatched1(23, 8), io.RxData),// Little Endian Byte Ordering
          ( RxPointerLSB_rst === 1.U ) -> Cat(RxDataLatched1(23,16), io.RxData, RxDataLatched1( 7,0)),
          ( RxPointerLSB_rst === 2.U ) -> Cat(                       io.RxData, RxDataLatched1(15,0)),
          ( RxPointerLSB_rst === 3.U ) -> RxDataLatched1,
        ))
      } .elsewhen(RxEnableWindow){
        RxDataLatched1 := Mux1H(Seq(
          ( RxByteCnt === 0.U ) -> Cat(RxDataLatched1(23, 8), io.RxData),// Little Endian Byte Ordering
          ( RxByteCnt === 1.U ) -> Cat(RxDataLatched1(23,16), io.RxData, RxDataLatched1( 7,0)),
          ( RxByteCnt === 2.U ) -> Cat(                       io.RxData, RxDataLatched1(15,0)),
          ( RxByteCnt === 3.U ) -> RxDataLatched1,
        ))
      }
    }


    // Assembling data that will be written to the rx_fifo
    when(SetWriteRxDataToFifo & ~ShiftWillEnd){
      RxDataLatched2 := Cat(io.RxData, RxDataLatched1)// Little Endian Byte Ordering
    } .elsewhen(SetWriteRxDataToFifo & ShiftWillEnd){
      RxDataLatched2 := Mux1H(Seq(                    // Little Endian Byte Ordering
        ( RxValidBytes === 0.U ) -> Cat(io.RxData, RxDataLatched1),
        ( RxValidBytes === 1.U ) -> Cat(0.U(24.W), RxDataLatched1(7,0) ),
        ( RxValidBytes === 2.U ) -> Cat(0.U(16.W), RxDataLatched1(15, 0) ),
        ( RxValidBytes === 3.U ) -> Cat(0.U(8.W),  RxDataLatched1        ),       
      ))
    }

    when(SetWriteRxDataToFifo & ~io.RxAbort){
      WriteRxDataToFifo := true.B
    } .elsewhen(WriteRxDataToFifoSync(1) | io.RxAbort){
      WriteRxDataToFifo := false.B
    }


    when(io.RxStartFrm & ~SyncRxStartFrm(1)){
      LatchedRxStartFrm := true.B
    } .elsewhen(SyncRxStartFrm(1)){
      LatchedRxStartFrm := false.B
    }


    // Generation of the end-of-frame signal
    when(~io.RxAbort & SetWriteRxDataToFifo & StartShiftWillEnd){
      ShiftEnded_rck := true.B
    } .elsewhen(io.RxAbort | ShiftEndedSync(0) & ShiftEndedSync(1)){
      ShiftEnded_rck := false.B
    }

    // Generation of the end-of-frame signal
    when(io.RxStartFrm){
      RxEnableWindow := true.B
    } .elsewhen(io.RxEndFrm | io.RxAbort){
      RxEnableWindow := false.B
    }


    when(RxAbortSyncb){
      RxAbortLatched := false.B
    } .elsewhen(io.RxAbort){
      RxAbortLatched := true.B
    }







    val Busy_IRQ_rck = RegInit(false.B); Busy_IRQ_rck_rxclk := Busy_IRQ_rck
    val Busy_IRQ_syncb = ShiftRegister( Busy_IRQ_sync(1), 2, false.B, true.B )

    when(io.RxValid & io.RxStartFrm & ~RxReady){
      Busy_IRQ_rck := true.B
    } .elsewhen(Busy_IRQ_syncb){
      Busy_IRQ_rck := false.B
    }



  }
}

class MacTileLink(edgeIn: TLEdgeIn, edgeOut: TLEdgeOut) extends MacTileLinkBase(edgeIn, edgeOut) with MacTileLinkTXClk with MacTileLinkRXClk












