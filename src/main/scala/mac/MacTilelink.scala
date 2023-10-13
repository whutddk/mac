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

    // Tx Status signals
    val RetryCntLatched  = Input(UInt(4.W))  // Latched Retry Counter
    val RetryLimit       = Input(Bool())     // Retry limit reached (Retry Max value +1 attempts were made)
    val LateCollLatched  = Input(Bool())     // Late collision occured
    val DeferLatched     = Input(Bool())     // Defer indication (Frame was defered before sucessfully sent)
    val CarrierSenseLost = Input(Bool())     // Carrier Sense was lost during the frame transmission

    // Tx
    val PerPacketCrcEn = Output(Bool())     // Per packet crc enable
    val PerPacketPad   = Output(Bool())     // Per packet pading

    //Register
    val r_TxEn    = Input(Bool())          // Transmit enable

    val BlockingTxStatusWrite = Output(Bool())

    val TxUsedData     = Input(Bool())      // Transmit packet used data    
   

    val TxValidBytesLatched = Output(UInt(2.W))
    


    val TxRetrySync  = Input(Bool())
    val TxAbortSync = Input(Bool())      // Transmit packet abort
    val TxDoneSync  = Input(Bool())      // Transmission ended





    val r_RxEn    = Input(Bool())         // Receive enable
    val RxDataLatched2_rxclk  = Input(UInt(32.W))
    val WriteRxDataToFifoSync = Input(Bool())
    val RxAbortSync           = Input(Bool())
    val LatchedRxLength_rxclk = Input(UInt(16.W))
    val RxStatusInLatched_rxclk = Input(UInt(9.W))
    val ShiftEndedSync = Input(Bool())
    val RxReady = Output(Bool())
    val rxDeq = new RevBuff_Enq_Bundle



    val TxStartFrm_wb = Output(Bool())
    val TxStartFrm_syncb = Input(Bool())
    val TxEndFrm_wb = Output(Bool())

    val TxData_wb = Output(UInt(32.W))
    val ReadTxDataFromFifo_sync = Input(Bool())

    val txEnq = Flipped(new TxBuff_Deq_Bundle)


  }


  
  val io = IO(new MacTileLinkIO)


  val ShiftEndedSyncPluse         = io.ShiftEndedSync          & ~RegNext(io.ShiftEndedSync, false.B)
  val RxAbortPluse                = io.RxAbortSync             & ~RegNext(io.RxAbortSync, false.B)
  val WriteRxDataToFifoSyncPluse  = io.WriteRxDataToFifoSync   & ~RegNext(io.WriteRxDataToFifoSync, false.B)

  val RxReady = RegInit(false.B); io.RxReady := RxReady


  val rxDeqCtrlValid = RegInit(false.B)
  io.rxDeq.ctrl.valid := rxDeqCtrlValid
  io.rxDeq.ctrl.bits.LatchedRxLength   := RegEnable(io.LatchedRxLength_rxclk,   ShiftEndedSyncPluse | RxAbortPluse)
  io.rxDeq.ctrl.bits.RxStatusInLatched := RegEnable(io.RxStatusInLatched_rxclk, ShiftEndedSyncPluse | RxAbortPluse)
  io.rxDeq.ctrl.bits.isRxAbort         := RegEnable(RxAbortPluse,      false.B, ShiftEndedSyncPluse | RxAbortPluse)

  when( io.rxDeq.ctrl.fire ){
    rxDeqCtrlValid := false.B
  } .elsewhen( ShiftEndedSyncPluse | RxAbortPluse ){
    rxDeqCtrlValid := true.B
  }



  // RxReady generation
  when(ShiftEndedSyncPluse | RxAbortPluse  ){
    RxReady := false.B
  } .elsewhen( io.r_RxEn & (io.rxDeq.data.ready) ){
    RxReady := true.B
  }


  io.rxDeq.data.bits := io.RxDataLatched2_rxclk
  io.rxDeq.data.valid := WriteRxDataToFifoSyncPluse 

  assert( (~io.rxDeq.data.valid & ~io.rxDeq.data.ready), "Assert Failed, rx overrun!" )





















  val TxStartFrm_wb = RegInit(false.B); io.TxStartFrm_wb := TxStartFrm_wb
  val TxStatus = RegInit(0.U(4.W)) //[14:11]


  when( io.txEnq.req.fire ){
    TxStartFrm_wb := true.B
  } .elsewhen(io.TxStartFrm_syncb){
    TxStartFrm_wb := false.B
  }


  when((TxLength === 0.U) & io.TxUsedData){
    TxEndFrm_wb := true.B
  } .elsewhen(TxRetryPulse | TxDonePulse | TxAbortPulse){
    TxEndFrm_wb := false.B
  }

  when( io.txEnq.req.fire ){
    TxStatus := 
      Cat(
        io.txEnq.req.bits.irq, io.txEnq.req.bits.wr, io.txEnq.req.bits.pad, io.txEnq.req.bits.crc
      )
    TxLength        := io.txEnq.req.bits.txLength
    LatchedTxLength := io.txEnq.req.bits.txLength
  } .elsewhen( io.txEnq.data.fire ){
    when( TxLength < 4.U ){
      TxLength := 0.U
    } .otherwise{
      TxLength := TxLength - 4.U    // Length is subtracted at the data request
    }
  }


  val txRespValid := RegInit(false.B)

  io.txEnq.resp.valid := txRespValid
  io.txEnq.resp.bits  := 
    RegEnable(
      Cat(LatchedTxLength, 0.U(1.W), TxStatus, 0.U(2.W), false.B, io.RetryCntLatched, io.RetryLimit, io.LateCollLatched, io.DeferLatched, io.CarrierSenseLost), 
      TxRetryPulse | TxDonePulse | TxAbortPulse
    )

  when( io.txEnq.resp.fire ){
    txRespValid := false.B
  } .elsewhen( TxRetryPulse | TxDonePulse | TxAbortPulse ){
    txRespValid := true.B
  }




  // // Latching READY status of the Tx buffer descriptor
  // when(TxBDRead){ // TxBDReady is sampled only once at the beginning.
  //   TxBDReady := txBuffDesc.rd & (txBuffDesc.len > 4.U)
  // } .elsewhen(ResetTxBDReady){ // Only packets larger then 4 bytes are transmitted.
  //   TxBDReady := false.B
  // }

  val txEnqReqReady = RegInit(true.B); io.txEnq.req.ready := txEnqReqReady

  val StartTxBDRead = (TxRetryPacket_NotCleared | TxDonePacket | TxAbortPacket) & ~TxBDReady // Reading the Tx buffer descriptor

  when(io.txEnq.req.fire){
    txEnqReqReady := false.B
  } .elsewhen(StartTxBDRead){
    txEnqReqReady := true.B
  }


  // Writing status back to the Tx buffer descriptor
  TxStatusWrite := (TxDonePacket | TxAbortPacket)


  // Status writing must occur only once. Meanwhile it is blocked.
  when(~io.TxDoneSync & ~io.TxAbortSync){
    BlockingTxStatusWrite := false.B
  } .elsewhen(TxStatusWrite){
    BlockingTxStatusWrite := true.B
  }















  io.txEnq.data.ready    := ReadTxDataFromFifoSyncPluse & ~tx_fifo.io.empty
  assert( ~(io.txEnq.data.ready & ~io.txEnq.data.valid), "Assert Failed, Tx should never under run!" )

  io.TxData_wb       := io.txEnq.data.bits

  tx_fifo.io.clear   := TxAbortPacket | TxRetryPacket









  val TxRetryPacket = RegInit(false.B)

  val TxDonePacket = RegInit(false.B)

  val TxAbortPacket = RegInit(false.B)

  val TxBDReady = RegInit(false.B)

  val ReadTxDataFromFifoSyncPluse = io.ReadTxDataFromFifo_sync & ~RegNext(io.ReadTxDataFromFifo_sync, false.B)


  val TxLength = RegInit(0.U(16.W))


  val TxRetryPulse                = io.TxRetrySync             & ~RegNext(io.TxRetrySync, false.B)
  val TxDonePulse                 = io.TxDoneSync              & ~RegNext(io.TxDoneSync,  false.B)
  val TxAbortPulse                = io.TxAbortSync             & ~RegNext(io.TxAbortSync, false.B)
  val LatchedTxLength = RegInit(0.U(16.W))


  val BlockingTxStatusWrite = RegInit(false.B); io.BlockingTxStatusWrite := BlockingTxStatusWrite



  val TxEndFrm_wb = RegInit(false.B); io.TxEndFrm_wb := TxEndFrm_wb

  // Delayed stage signals
  val r_TxEn_q = RegNext(io.r_TxEn, false.B) 


  val TxAbortPacketBlocked = RegInit(false.B)

  when( io.TxAbortSync & (~TxAbortPacketBlocked) ){
    TxAbortPacket := true.B
  } .otherwise{
    TxAbortPacket := false.B
  }



  when(~io.TxAbortSync & RegNext(io.TxAbortSync, false.B)){
    TxAbortPacketBlocked := false.B
  } .elsewhen(TxAbortPacket){
    TxAbortPacketBlocked := true.B
  }



  val TxRetryPacketBlocked = RegInit(false.B)

  when( io.TxRetrySync & ~TxRetryPacketBlocked ){
    TxRetryPacket := true.B
  } .otherwise{
    TxRetryPacket := false.B
  }

  when(StartTxBDRead){
    TxRetryPacket_NotCleared := false.B
  } .elsewhen( io.TxRetrySync & ~TxRetryPacketBlocked ){
    TxRetryPacket_NotCleared := true.B
  }


  when( ~io.TxRetrySync & RegNext(io.TxRetrySync, false.B) ){
    TxRetryPacketBlocked := false.B
  } .elsewhen(TxRetryPacket){
    TxRetryPacketBlocked := true.B
  }






  val TxDonePacketBlocked = RegInit(false.B)

  when( io.TxDoneSync & ~TxDonePacketBlocked ){
    TxDonePacket := true.B
  }.otherwise{
    TxDonePacket := false.B
  }




  when(~io.TxDoneSync & RegNext(io.TxDoneSync, false.B)){
    TxDonePacketBlocked := false.B
  } .elsewhen(TxDonePacket){
    TxDonePacketBlocked := true.B
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



  





}






class MacTileLink(edgeIn: TLEdgeIn, edgeOut: TLEdgeOut) extends MacTileLinkBase(edgeIn, edgeOut)




























