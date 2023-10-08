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
    val CarrierSenseLost = Input(Bool())     // Carrier Sense was lost during the frame transmission

    // Tx
    val TxUsedData     = Input(Bool())      // Transmit packet used data
    val TxUnderRun     = Input(Bool())     // Transmit packet under-run
    val PerPacketCrcEn = Output(Bool())     // Per packet crc enable
    val PerPacketPad   = Output(Bool())     // Per packet pading

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



    val BlockingTxStatusWrite = Output(Bool())
    val TxStartFrm_wb = Output(Bool())
    val ReadTxDataFromFifo_sync = Input(Bool())

    val TxStartFrm_syncb = Input(Bool())
    val TxUnderRun_wb = Output(Bool())
    val TxData_wb = Output(UInt(32.W))
    val TxValidBytesLatched = Output(UInt(2.W))
    val TxEndFrm_wb = Output(Bool())

    val TxRetrySync  = Input(Bool())
    val TxAbortSync = Input(Bool())      // Transmit packet abort
    val TxDoneSync  = Input(Bool())      // Transmission ended




    val RxDataLatched2_rxclk  = Input(UInt(32.W))
    val WriteRxDataToFifoSync = Input(Bool())
    val RxAbortSync           = Input(Bool())
    val LatchedRxLength_rxclk = Input(UInt(16.W))
    val RxStatusInLatched_rxclk = Input(UInt(9.W))
    val ShiftEndedSync = Input(Bool())
    val LatchedRxStartFrmSync = Input(Bool())
    val Busy_IRQ_sync = Input(Bool())

    val RxReady = Output(Bool())
    val RxStatusIn = Output(UInt(9.W))
    val RxStatusWriteLatched = Output(Bool())
    val RxStatusWriteLatchedSyncb = Input(Bool())
  }


  
  val io = IO(new MacTileLinkIO)
  val tx_fifo = Module( new MacFifo(dw = 32, dp = 16) )
  val rx_fifo = Module(new MacFifo(dw = 32, dp = 16))
  
  val (_, _, isLastD, transDCnt) = edgeOut.count(io.tlMst.D)

  val TxB_IRQ = RegInit(false.B); io.TxB_IRQ := TxB_IRQ
  val TxE_IRQ = RegInit(false.B); io.TxE_IRQ := TxE_IRQ
  val RxB_IRQ = RegInit(false.B); io.RxB_IRQ := RxB_IRQ
  val RxE_IRQ = RegInit(false.B); io.RxE_IRQ := RxE_IRQ




  val TxUnderRun_wb = RegInit(false.B); io.TxUnderRun_wb := TxUnderRun_wb

  val TxBDRead = RegInit(true.B)
  val TxStatusWrite = Wire(Bool())



  val TxLength = RegInit(0.U(16.W))
  val LatchedTxLength = RegInit(0.U(16.W))
  val TxStatus = RegInit(0.U(4.W)) //[14:11]
  val RxStatus = RegInit(0.U(2.W)) //[14:13]




  val TxStartFrm_wb = RegInit(false.B); io.TxStartFrm_wb := TxStartFrm_wb


  // Signals used for various purposes
  val TxRetryPulse                = io.TxRetrySync             & ~RegNext(io.TxRetrySync, false.B)
  val TxDonePulse                 = io.TxDoneSync              & ~RegNext(io.TxDoneSync,  false.B)
  val TxAbortPulse                = io.TxAbortSync             & ~RegNext(io.TxAbortSync, false.B)
  val ShiftEndedSyncPluse         = io.ShiftEndedSync          & ~RegNext(io.ShiftEndedSync, false.B)
  val ReadTxDataFromFifoSyncPluse = io.ReadTxDataFromFifo_sync & ~RegNext(io.ReadTxDataFromFifo_sync, false.B)
  val RxAbortPluse                = io.RxAbortSync             & ~RegNext(io.RxAbortSync, false.B)
  val WriteRxDataToFifoSyncPluse  = io.WriteRxDataToFifoSync   & ~RegNext(io.WriteRxDataToFifoSync, false.B)
  val LatchedRxStartFrmSyncPluse  = io.LatchedRxStartFrmSync   & ~RegNext(io.LatchedRxStartFrmSync, false.B)
  val Busy_IRQ_syncPluse          = io.Busy_IRQ_sync           & ~RegNext(io.Busy_IRQ_sync)

  val TxRetryPacket = RegInit(false.B)
  val TxRetryPacket_NotCleared = RegInit(false.B)
  val TxDonePacket = RegInit(false.B)
  val TxDonePacket_NotCleared = RegInit(false.B)
  val TxAbortPacket = RegInit(false.B)
  val TxAbortPacket_NotCleared = RegInit(false.B)
  val RxBDReady = RegInit(false.B)
  val RxReady = RegInit(false.B); io.RxReady := RxReady
  val TxBDReady = RegInit(false.B)

  val RxBDRead = RegInit(false.B)



  val BlockingTxStatusWrite = RegInit(false.B); io.BlockingTxStatusWrite := BlockingTxStatusWrite
  val BlockingTxBDRead = RegInit(false.B)


  val RxBDAddress = RegInit(0.U(7.W))   //[7:1]
  val TxBDAddress = RegInit(0.U(7.W))   //[7:1]






  val ShiftEnded = RegInit(false.B)
  val RxOverrun  = RegInit(false.B)

  val BDWrite = RegInit(0.U(4.W))     // BD Write Enable for access from WISHBONE side
  val BDRead  = RegInit(false.B)      // BD Read access from WISHBONE side

  val TxEndFrm_wb = RegInit(false.B); io.TxEndFrm_wb := TxEndFrm_wb






  val RxStatusWrite = Wire(Bool())





  // Delayed stage signals
  val r_TxEn_q = RegNext(io.r_TxEn, false.B) 
  val r_RxEn_q = RegNext(io.r_RxEn, false.B)

  def StateIdle = 0.U(3.W)
  def StateWB   = 1.U(3.W)
  def StateTX   = 2.U(3.W)
  def StateRX   = 3.U(3.W)

  val stateNxt = RegInit( StateWB )
  val stateCur = RegNext( stateNxt, StateIdle )

  
  val ram_addr = RegInit(0.U(8.W))
  val ram_di = RegInit(0.U(32.W))




  val TxPointerRead = RegInit(false.B)
  val TxEn_needed = RegInit(false.B)
  val RxEn_needed = RegInit(false.B)

  val RxPointerRead = RegInit(false.B)

  // RX shift ending signals
  val ShiftEndedSync3 = RegInit(false.B)



  

  val StartOccured      = RegInit(false.B)

  val BlockReadTxDataFromMemory = RegInit(false.B)


  val ReadTxDataFromMemory = RegInit(false.B)

  val MasterWbTX = RegInit(false.B)
  val MasterWbRX = RegInit(false.B)


  val TxPointerMSB = RegInit(0.U(30.W)) //[31:2]
  val RxPointerMSB = RegInit(0.U(30.W)) //[31:2]



  val RxStatusWriteLatched = RegInit(false.B); io.RxStatusWriteLatched := RxStatusWriteLatched




  // Generic synchronous single-port RAM interface
  val bd_ram = Module(new MacSRAM)
  val txBuffDesc = bd_ram.io.dato.asTypeOf(new TxBuffDesc)
  val rxBuffDesc = bd_ram.io.dato.asTypeOf(new RxBuffDesc)


  bd_ram.io.we :=
    Mux1H(Seq(
      (stateNxt === StateWB & stateCur === StateWB) -> BDWrite,
      (TxStatusWrite | RxStatusWrite)               -> "b1111".U
    )).asBools

  bd_ram.io.oe :=
    Mux1H(Seq(
      (( stateNxt === StateWB ) & ( stateCur === StateWB )) -> BDRead,
      (( stateNxt === StateTX ) & ( stateCur === StateTX )) -> (TxBDRead | TxPointerRead),
      (( stateNxt === StateRX ) & ( stateCur === StateRX )) -> (RxBDRead | RxPointerRead),
    ))


  bd_ram.io.addr := ram_addr
  bd_ram.io.di := ram_di




  when(~TxBDReady & io.r_TxEn & stateNxt === StateWB & stateCur =/= StateWB){
    TxEn_needed := true.B
  } .elsewhen(TxPointerRead & stateNxt === StateTX & stateCur === StateTX){
    TxEn_needed := false.B
  }


Fill(4, io.tlSlv.A.valid & io.tlSlv.A.bits.mask.orR & io.tlSlv.A.bits.address(10)) & io.tlSlv.A.bits.mask

  // Enabling access to the RAM for three devices.
  // Switching between three stages depends on enable signals
  switch( stateCur ){
    is(StateIdle){
      when( RxEn_needed === false.B & TxEn_needed === false.B ){
        stateNxt := StateWB // Idle state. We go to WbEn access stage.
        
        ram_addr := io.tlSlv.A.bits.address(9,2) // [11:2 ] -> [9:2]
        ram_di   := io.tlSlv.A.bits.data
        BDWrite  := io.tlSlv.A.bits.mask & Fill(4, io.tlSlv.A.valid & io.tlSlv.A.bits.address(10) & ((io.tlSlv.A.bits.opcode === 0.U) || (io.tlSlv.A.bits.opcode === 1.U)) )
        BDRead   := io.tlSlv.A.bits.mask.orR     & io.tlSlv.A.valid & io.tlSlv.A.bits.address(10) &  (io.tlSlv.A.bits.opcode === 4.U) // 0x400 - 0x7FF
      }

    }
    is(StateWB){
      when( RxEn_needed ){  // synopsys parallel_case
        stateNxt := StateRX // wb access stage and r_RxEn is enabled

        ram_addr := Cat(RxBDAddress, RxPointerRead)
        ram_di   := Cat(io.LatchedRxLength_rxclk, 0.U(1.W), RxStatus, 0.U(4.W), io.RxStatusInLatched_rxclk)
      } .elsewhen( TxEn_needed ){
        stateNxt := StateTX // wb access stage, r_RxEn is disabled but r_TxEn is enabled

        ram_addr := Cat(TxBDAddress, TxPointerRead) //[7,1] + [0]
        ram_di   := Cat(LatchedTxLength, 0.U(1.W), TxStatus, 0.U(2.W), io.TxUnderRun, io.RetryCntLatched, io.RetryLimit, io.LateCollLatched, io.DeferLatched, io.CarrierSenseLost)
      } .otherwise{
        stateNxt := StateIdle // WbEn access stage and there is no need for other stages. WbEn needs to be switched off for a bit
      }
    }
    is(StateRX){
      when( TxEn_needed ){
        stateNxt := StateTX  // RxEn access stage and r_TxEn is enabled

        ram_addr := Cat(TxBDAddress, TxPointerRead)
        ram_di   := Cat(LatchedTxLength, 0.U(1.W), TxStatus, 0.U(2.W), io.TxUnderRun, io.RetryCntLatched, io.RetryLimit, io.LateCollLatched, io.DeferLatched, io.CarrierSenseLost)
      } .otherwise{
        stateNxt := StateWB  // RxEn access stage and r_TxEn is disabled

        ram_addr := io.tlSlv.A.bits.address(9,2) // [11:2 ] -> [9:2];
        ram_di   := io.tlSlv.A.bits.data
        BDWrite  := io.tlSlv.A.bits.mask & Fill(4, io.tlSlv.A.valid & io.tlSlv.A.bits.address(10) & ((io.tlSlv.A.bits.opcode === 0.U) || (io.tlSlv.A.bits.opcode === 1.U)) )
        BDRead   := io.tlSlv.A.bits.mask.orR     & io.tlSlv.A.valid & io.tlSlv.A.bits.address(10) &  (io.tlSlv.A.bits.opcode === 4.U)
      }
    }
    is(StateTX){
      when( true.B ){
        stateNxt := StateWB  // TxEn access stage (we always go to wb access stage)

        ram_addr := io.tlSlv.A.bits.address(9,2) //[11:2 ] ->[9:2]
        ram_di   := io.tlSlv.A.bits.data
        BDWrite  := io.tlSlv.A.bits.mask & Fill(4, io.tlSlv.A.valid & io.tlSlv.A.bits.address(10) & ((io.tlSlv.A.bits.opcode === 0.U) || (io.tlSlv.A.bits.opcode === 1.U)) )
        BDRead   := io.tlSlv.A.bits.mask.orR     & io.tlSlv.A.valid & io.tlSlv.A.bits.address(10) &  (io.tlSlv.A.bits.opcode === 4.U)
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

  // Reading Tx BD Pointer
  when(TxBDRead & TxBDReady){
    TxPointerRead := true.B
  } .elsewhen(stateCur === StateTX){
    TxPointerRead := false.B
  }



  // Writing status back to the Tx buffer descriptor
  TxStatusWrite := (TxDonePacket_NotCleared | TxAbortPacket_NotCleared) & stateNxt === StateTX & stateCur === StateTX & ~BlockingTxStatusWrite


  // Status writing must occur only once. Meanwhile it is blocked.
  when(~io.TxDoneSync & ~io.TxAbortSync){
    BlockingTxStatusWrite := false.B
  } .elsewhen(TxStatusWrite){
    BlockingTxStatusWrite := true.B
  }






  // TxBDRead state is activated only once. 
  when(StartTxBDRead){
    BlockingTxBDRead := true.B
  } .elsewhen(~StartTxBDRead & ~TxBDReady){
    BlockingTxBDRead := false.B
  }



  





  
  when(stateNxt === StateTX & stateCur === StateTX & TxBDRead){
    TxStatus := Cat(txBuffDesc.irq, txBuffDesc.wr, txBuffDesc.pad, txBuffDesc.crc) // Latching status from the tx buffer descriptor Data is avaliable one cycle after the access is started (at that time signal TxEn is not active)
    TxLength        := txBuffDesc.len                                              //Latching length from the buffer descriptor;
    LatchedTxLength := txBuffDesc.len 
  } .elsewhen( MasterWbTX & io.tlMst.D.fire ){ //tx tileRead
    when( TxLength < 4.U ){
      TxLength := 0.U
    } .otherwise{
      TxLength := TxLength - 4.U    // Length is subtracted at the data request
    }
  }







  when(stateNxt === StateTX & stateCur === StateTX & TxPointerRead){
    TxPointerMSB := bd_ram.io.dato(31,2)  // Latching Tx buffer pointer from buffer descriptor. Only 30 MSB bits are latched because TxPointerMSB is only used for word-aligned accesses.
    when( bd_ram.io.dato(1,0) =/= 0.U ){
      printf("Warning, force to align at tx ram")
    }
  } .elsewhen( io.tlMst.D.fire & io.tlMst.D.bits.opcode === 1.U ){
    TxPointerMSB := TxPointerMSB + 1.U // TxPointer is word-aligned
  }
    


  val isTlMstBusy = RegInit(false.B)


  when( (TxLength === 0.U) | TxAbortPulse | TxRetryPulse){
    ReadTxDataFromMemory := false.B
  } .elsewhen(stateNxt === StateTX & stateCur === StateTX & TxPointerRead){
    ReadTxDataFromMemory := true.B
  }

  val ReadTxDataFromMemory_2 = ReadTxDataFromMemory & ~BlockReadTxDataFromMemory;

  when(
    (tx_fifo.io.almost_full | TxLength <= 4.U) & MasterWbTX & isTlMstBusy & (~(TxAbortPacket_NotCleared | TxRetryPacket_NotCleared))){
    BlockReadTxDataFromMemory := true.B
  } .elsewhen(ReadTxDataFromFifoSyncPluse | TxDonePacket | TxAbortPacket | TxRetryPacket){
    BlockReadTxDataFromMemory := false.B
  }
































































  tx_fifo.io.data_in := io.tlMst.D.bits.data
  tx_fifo.io.write   := io.tlMst.D.fire & io.tlMst.D.bits.opcode === 1.U


  tx_fifo.io.read    := ReadTxDataFromFifoSyncPluse & ~tx_fifo.io.empty
  tx_fifo.io.clear   := TxAbortPacket | TxRetryPacket
  io.TxData_wb       := tx_fifo.io.data_out





  // Start: Generation of the TxStartFrm_wb which is then synchronized to the MTxClk
  when(TxBDReady & ~StartOccured & (tx_fifo.io.full | TxLength === 0.U)){
    TxStartFrm_wb := true.B
  } .elsewhen(io.TxStartFrm_syncb){
    TxStartFrm_wb := false.B
  }


  // StartOccured: TxStartFrm_wb occurs only ones at the beginning. Then it's blocked.
  when(TxStartFrm_wb){
    StartOccured := true.B
  } .elsewhen(ResetTxBDReady){
    StartOccured := false.B
  }


  // TxEndFrm_wb: indicator of the end of frame
  when((TxLength === 0.U) & tx_fifo.io.almost_empty & io.TxUsedData){
    TxEndFrm_wb := true.B
  } .elsewhen(TxRetryPulse | TxDonePulse | TxAbortPulse){
    TxEndFrm_wb := false.B
  }


  // Marks which bytes are valid within the word.
  val TxValidBytesLatched = RegInit(0.U(2.W)); io.TxValidBytesLatched := TxValidBytesLatched


  val LatchValidBytes = ShiftRegisters((TxLength < 4.U) & TxBDReady, 2, false.B, true.B)
  val LatchValidBytesPluse = LatchValidBytes(0) & ~LatchValidBytes(1)

  // Latching valid bytes
  when(LatchValidBytesPluse){
    TxValidBytesLatched := Mux(TxLength < 4.U, TxLength(1,0), 0.U)
  } .elsewhen(TxRetryPulse | TxDonePulse | TxAbortPulse){
    TxValidBytesLatched := 0.U
  }


  val TxIRQEn         = TxStatus.extract(3) //[14:11]
  val WrapTxStatusBit = TxStatus.extract(2)
  io.PerPacketPad    := TxStatus.extract(1)
  io.PerPacketCrcEn  := TxStatus.extract(0)

  val RxIRQEn         = RxStatus.extract(1) //[14:13]
  val WrapRxStatusBit = RxStatus.extract(0)



  // Latching Tx buffer descriptor address
  when(io.r_TxEn & (~r_TxEn_q)){
    TxBDAddress := 0.U
  } .elsewhen(TxStatusWrite){
    when( TxStatusWrite & ~WrapTxStatusBit ){ //increase
      TxBDAddress := TxBDAddress + 1.U
    } .otherwise{ //wrap
      TxBDAddress := 0.U
    } 
  }

  // Latching Rx buffer descriptor address
  when(io.r_RxEn & (~r_RxEn_q)){
    RxBDAddress := io.r_TxBDNum(6,0)
  } .elsewhen(RxStatusWrite){
    when( WrapRxStatusBit ) {
      RxBDAddress := io.r_TxBDNum(6,0) // Using first Rx BD
    } .otherwise{
      RxBDAddress := (RxBDAddress + 1.U) //Using next Rx BD
    }  
  }











  val TxAbortPacketBlocked = RegInit(false.B)

  when(
    io.TxAbortSync & (~TxAbortPacketBlocked) &   MasterWbTX  & io.tlMst.D.fire & isLastD  |
    io.TxAbortSync & (~TxAbortPacketBlocked) & (~MasterWbTX) ){
    TxAbortPacket := true.B
  } .otherwise{
    TxAbortPacket := false.B
  }


  when(stateNxt === StateTX & stateCur === StateTX & TxAbortPacket_NotCleared){
    TxAbortPacket_NotCleared := false.B
  } .elsewhen(
    io.TxAbortSync & (~TxAbortPacketBlocked) &   MasterWbTX  & io.tlMst.D.fire & isLastD |
    io.TxAbortSync & (~TxAbortPacketBlocked) & (~MasterWbTX) ){
    TxAbortPacket_NotCleared := true.B
  }

  when(~io.TxAbortSync & RegNext(io.TxAbortSync, false.B)){
    TxAbortPacketBlocked := false.B
  } .elsewhen(TxAbortPacket){
    TxAbortPacketBlocked := true.B
  }



  val TxRetryPacketBlocked = RegInit(false.B)

  when(
    io.TxRetrySync & ~TxRetryPacketBlocked &  MasterWbTX & io.tlMst.D.fire & isLastD |
    io.TxRetrySync & ~TxRetryPacketBlocked & ~MasterWbTX ){
    TxRetryPacket := true.B
  } .otherwise{
    TxRetryPacket := false.B
  }





  when(StartTxBDRead){
    TxRetryPacket_NotCleared := false.B
  } .elsewhen(
    io.TxRetrySync & ~TxRetryPacketBlocked &  MasterWbTX & io.tlMst.D.fire & isLastD |
    io.TxRetrySync & ~TxRetryPacketBlocked & ~MasterWbTX ){
    TxRetryPacket_NotCleared := true.B
  }


  when( ~io.TxRetrySync & RegNext(io.TxRetrySync, false.B) ){
    TxRetryPacketBlocked := false.B
  } .elsewhen(TxRetryPacket){
    TxRetryPacketBlocked := true.B
  }



  val TxDonePacketBlocked = RegInit(false.B)

  when(
    io.TxDoneSync & ~TxDonePacketBlocked &  MasterWbTX & io.tlMst.D.fire & isLastD |
    io.TxDoneSync & ~TxDonePacketBlocked & ~MasterWbTX  ){
    TxDonePacket := true.B
  }.otherwise{
    TxDonePacket := false.B
  }

  when(stateNxt === StateTX & stateCur === StateTX & TxDonePacket_NotCleared){
    TxDonePacket_NotCleared := false.B
  } .elsewhen(
    io.TxDoneSync & ~TxDonePacketBlocked &  MasterWbTX & io.tlMst.D.fire & isLastD |
    io.TxDoneSync & ~TxDonePacketBlocked & ~MasterWbTX ){
    TxDonePacket_NotCleared := true.B
  }


  when(~io.TxDoneSync & RegNext(io.TxDoneSync, false.B)){
    TxDonePacketBlocked := false.B
  } .elsewhen(TxDonePacket){
    TxDonePacketBlocked := true.B
  }
  


  // Tx under run
  when(TxAbortPulse){
    TxUnderRun_wb := false.B    
  } .elsewhen(tx_fifo.io.empty & ReadTxDataFromFifoSyncPluse){
    TxUnderRun_wb := true.B
  }





  // Reading the Rx buffer descriptor
  when( (RxStatusWrite | RegNext(RxAbortPluse, false.B) | (io.r_RxEn & ~r_RxEn_q)) & ~RxReady){
    RxBDRead := true.B
  } .elsewhen(RxBDReady){
    RxBDRead := false.B
  }



  // Latching READY status of the Rx buffer descriptor
  when(RxPointerRead){
    RxBDReady := false.B
  } .elsewhen(stateNxt === StateRX & stateCur === StateRX & RxBDRead){
    RxBDReady := rxBuffDesc.e                           // RxBDReady is sampled only once at the beginning 
    RxStatus := Cat(rxBuffDesc.irq, rxBuffDesc.wrap)    // Latching Rx buffer descriptor status Data is avaliable one cycle after the access is started (at that time signal RxEn is not active)
  }



  // RxReady generation
  when(ShiftEnded | RxAbortPluse | ~io.r_RxEn & r_RxEn_q){
    RxReady := false.B
  } .elsewhen(stateNxt === StateRX & stateCur === StateRX & RxPointerRead){
    RxReady := true.B
  }

  // Reading Tx BD Pointer
  when(RxBDRead & RxBDReady){
    RxPointerRead := true.B
  } .elsewhen(stateNxt === StateRX & stateCur === StateRX){
    RxPointerRead := false.B
  }



  //Latching Rx buffer pointer from buffer descriptor;
  when(stateNxt === StateRX & stateCur === StateRX & RxPointerRead){
    RxPointerMSB := bd_ram.io.dato(31,2)    
  } .elsewhen(MasterWbRX & io.tlMst.A.fire ){
    RxPointerMSB := RxPointerMSB + 1.U // Word access (always word access. m_wb_sel_o are used for selecting bytes)
  }



  when(~RxReady & io.r_RxEn & stateNxt === StateWB & stateCur =/= StateWB){
    RxEn_needed := true.B
  } .elsewhen(RxPointerRead & stateNxt === StateRX & stateCur === StateRX){
    RxEn_needed := false.B
  }



  // Reception status is written back to the buffer descriptor after the end of frame is detected.
  RxStatusWrite := ShiftEnded & stateNxt === StateRX & stateCur === StateRX







  rx_fifo.io.data_in := io.RxDataLatched2_rxclk
  rx_fifo.io.write   := WriteRxDataToFifoSyncPluse & ~rx_fifo.io.full
  rx_fifo.io.read    := MasterWbRX & io.tlMst.A.fire
  rx_fifo.io.clear   := LatchedRxStartFrmSyncPluse









  when( ShiftEndedSyncPluse ){
    ShiftEndedSync3 := true.B
  } .elsewhen(ShiftEnded){
    ShiftEndedSync3 := false.B
  }


  // Generation of the end-of-frame signal
  when(ShiftEndedSync3 & MasterWbRX & io.tlMst.A.fire & rx_fifo.io.almost_empty & ~ShiftEnded){
    ShiftEnded := true.B
  } .elsewhen(RxStatusWrite){
    ShiftEnded := false.B
  }




  io.RxStatusIn := Cat(io.ReceivedPauseFrm, io.AddressMiss, RxOverrun, io.InvalidSymbol, io.DribbleNibble, io.ReceivedPacketTooBig, io.ShortFrame, io.LatchedCrcError, io.RxLateCollision)


  // Rx overrun
  when(RxStatusWrite){
    RxOverrun := false.B
  } .elsewhen(rx_fifo.io.full & WriteRxDataToFifoSyncPluse){
    RxOverrun := true.B
  }









  // Latching and synchronizing RxStatusWrite signal. This signal is used for clearing the ReceivedPauseFrm signal
  when(io.RxStatusWriteLatchedSyncb){
    RxStatusWriteLatched := false.B
  } .elsewhen(RxStatusWrite){
    RxStatusWriteLatched := true.B
  }


  // ShortFrame (RxStatusInLatched[2]) can not set an error because short frames are aborted when signal r_RecSmall is set to 0 in MODER register. 
  // AddressMiss is identifying that a frame was received because of the promiscous mode and is not an error
  val RxError = (io.RxStatusInLatched_rxclk(6,3).orR) | (io.RxStatusInLatched_rxclk(1,0).orR)

  val TxError = io.TxUnderRun | io.RetryLimit | io.LateCollLatched | io.CarrierSenseLost

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



  io.Busy_IRQ := Busy_IRQ_syncPluse



  
  val slvAInfo = RegEnable( io.tlSlv.A.bits, io.tlSlv.A.fire )
  val slvDValid = RegInit(false.B); io.tlSlv.D.valid := slvDValid
  val slvDDat = Reg(UInt(32.W))



  when( io.tlSlv.D.fire ){
    slvDValid := false.B
  } .elsewhen(io.tlSlv.A.fire){
    slvDValid := true.B
    slvDDat := bd_ram.io.dato
  }

  when(slvAInfo.opcode === 4.U) {
    io.tlSlv.D.bits := edgeIn.AccessAck(slvAInfo, slvDDat)
  } .otherwise {
    io.tlSlv.D.bits := edgeIn.AccessAck(slvAInfo)
  }

  io.tlSlv.A.ready := RegNext(stateNxt === StateWB & Mux( stateCur === StateWB , BDWrite.orR, BDRead ))
  
  assert( ~(io.tlSlv.A.ready & ~io.tlSlv.A.valid) )






  val mstAValid = RegInit(false.B)
  val mstABits  = Reg(new TLBundleA(edgeOut.bundle))


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
        mask = "b1111".U,
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
    when( ~rx_fifo.io.empty ){
      MasterWbRX := true.B
    } .elsewhen(ReadTxDataFromMemory_2) {
      MasterWbTX := true.B
    }
  } .elsewhen( ~MasterWbTX & MasterWbRX){ //1.4A + 1D fifo to memory
    when( io.tlMst.D.fire & isLastD & rx_fifo.io.empty ){
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






class MacTileLink(edgeIn: TLEdgeIn, edgeOut: TLEdgeOut) extends MacTileLinkBase(edgeIn, edgeOut)




























