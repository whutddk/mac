package MAC

import chisel3._
import chisel3.util._
import Switch._

import freechips.rocketchip.tilelink._
import freechips.rocketchip.diplomacy._
import org.chipsalliance.cde.config._








abstract class MacTileLinkBase() extends Module{

  class MacTileLinkIO extends Bundle{

    val r_RxEn    = Input(Bool())         // Receive enable
    val RxDataLatched2_rxclk  = Input(UInt(32.W))
    val WriteRxDataToFifoSync = Input(Bool())
    val RxAbortSync           = Input(Bool())
    val LatchedRxLength_rxclk = Input(UInt(16.W))
    val RxStatusInLatched_rxclk = Input(UInt(9.W))
    val ShiftEndedSync = Input(Bool())
    val RxReady = Output(Bool())


    val TxStartFrm_wb = Output(Bool())
    val TxStartFrm_syncb = Input(Bool())
    val TxEndFrm_wb = Output(Bool())

    val TxData_wb = Output(UInt(32.W))
    val ReadTxDataFromFifo_sync = Input(Bool())

    // Tx Status signals
    val RetryCntLatched  = Input(UInt(4.W))  // Latched Retry Counter
    val RetryLimit       = Input(Bool())     // Retry limit reached (Retry Max value +1 attempts were made)
    val LateCollLatched  = Input(Bool())     // Late collision occured
    val DeferLatched     = Input(Bool())     // Defer indication (Frame was defered before sucessfully sent)
    val CarrierSenseLost = Input(Bool())     // Carrier Sense was lost during the frame transmission
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




  }


  
  val io = IO(new MacTileLinkIO)

  val txBuff = Module(new TxBuff)
  val rxBuff = Module(new RxBuff)

  val ShiftEndedSyncPluse         = io.ShiftEndedSync          & ~RegNext(io.ShiftEndedSync, false.B)
  val RxAbortPluse                = io.RxAbortSync             & ~RegNext(io.RxAbortSync, false.B)
  val WriteRxDataToFifoSyncPluse  = io.WriteRxDataToFifoSync   & ~RegNext(io.WriteRxDataToFifoSync, false.B)

  val RxReady = RegInit(false.B); io.RxReady := RxReady


  val rxBuffCtrlValid = RegInit(false.B)
  rxBuff.io.enq.ctrl.valid := rxBuffCtrlValid
  rxBuff.io.enq.ctrl.bits.LatchedRxLength   := RegEnable(io.LatchedRxLength_rxclk,   ShiftEndedSyncPluse | RxAbortPluse)
  rxBuff.io.enq.ctrl.bits.RxStatusInLatched := RegEnable(io.RxStatusInLatched_rxclk, ShiftEndedSyncPluse | RxAbortPluse)
  rxBuff.io.enq.ctrl.bits.isRxAbort         := RegEnable(RxAbortPluse,      false.B, ShiftEndedSyncPluse | RxAbortPluse)

  when( rxBuff.io.enq.ctrl.fire ){
    rxBuffCtrlValid := false.B
  } .elsewhen( ShiftEndedSyncPluse | RxAbortPluse ){
    rxBuffCtrlValid := true.B
  }



  // RxReady generation
  when(ShiftEndedSyncPluse | RxAbortPluse  ){
    RxReady := false.B
  } .elsewhen( io.r_RxEn & (rxBuff.io.enq.data.ready) ){
    RxReady := true.B
  }


  rxBuff.io.enq.data.bits := io.RxDataLatched2_rxclk
  rxBuff.io.enq.data.valid := WriteRxDataToFifoSyncPluse 

  assert( (~rxBuff.io.enq.data.valid & ~rxBuff.io.enq.data.ready), "Assert Failed, rx overrun!" )





















  val TxStartFrm_wb = RegInit(false.B); io.TxStartFrm_wb := TxStartFrm_wb
  val TxEndFrm_wb = RegInit(false.B); io.TxEndFrm_wb := TxEndFrm_wb

  val TxLength = RegInit(0.U(16.W))
  val LatchedTxLength = RegInit(0.U(16.W))

  val txRetryPulse                = io.TxRetrySync             & ~RegNext(io.TxRetrySync, false.B)
  val txDonePulse                 = io.TxDoneSync              & ~RegNext(io.TxDoneSync,  false.B)
  val txAbortPulse                = io.TxAbortSync             & ~RegNext(io.TxAbortSync, false.B)
  val ReadTxDataFromFifoSyncPluse = io.ReadTxDataFromFifo_sync & ~RegNext(io.ReadTxDataFromFifo_sync, false.B)

  
  io.PerPacketPad    := RegEnable(txBuff.io.deq.req.bits.PerPacketPad, false.B, txBuff.io.deq.req.fire)
  io.PerPacketCrcEn  := RegEnable(txBuff.io.deq.req.bits.PerPacketCrcEn, false.B, txBuff.io.deq.req.fire)

  when( txBuff.io.deq.req.fire ){
    TxStartFrm_wb := true.B
  } .elsewhen(io.TxStartFrm_syncb){
    TxStartFrm_wb := false.B
  }


  when((TxLength === 0.U) & io.TxUsedData){
    TxEndFrm_wb := true.B
  } .elsewhen(txRetryPulse | txDonePulse | txAbortPulse){
    TxEndFrm_wb := false.B
  }

  when( txBuff.io.deq.req.fire ){

    TxLength        := txBuff.io.deq.req.bits.txLength
    LatchedTxLength := txBuff.io.deq.req.bits.txLength
  } .elsewhen( txBuff.io.deq.data.fire ){
    when( TxLength < 4.U ){
      TxLength := 0.U
    } .otherwise{
      TxLength := TxLength - 4.U    // Length is subtracted at the data request
    }
  }


  val txRespValid = RegInit(false.B)
  val txRespBits  = Reg(new Transmit_Deq_Resp_Bundle)

  txBuff.io.deq.resp.valid := txRespValid
  txBuff.io.deq.resp.bits  := txRespBits

  when( txBuff.io.deq.resp.fire ){
    txRespValid := false.B
  } .elsewhen( txRetryPulse | txDonePulse | txAbortPulse ){
    txRespValid := true.B
    txRespBits.RetryCntLatched := io.RetryCntLatched
    txRespBits.RetryLimit := io.RetryLimit
    txRespBits.LateCollLatched := io.LateCollLatched
    txRespBits.DeferLatched := io.DeferLatched
    txRespBits.CarrierSenseLost := io.CarrierSenseLost
    txRespBits.isClear := txAbortPulse | txRetryPulse
  }






  val isTxBusy = RegInit(false.B); txBuff.io.deq.req.ready := ~isTxBusy & io.r_TxEn

  when(txBuff.io.deq.req.fire){
    isTxBusy := true.B
  } .elsewhen(txRetryPulse | txDonePulse | txAbortPulse){
    isTxBusy := false.B
  }







  txBuff.io.deq.data.ready    := ReadTxDataFromFifoSyncPluse
  assert( ~(txBuff.io.deq.data.ready & ~txBuff.io.deq.data.valid), "Assert Failed, Tx should never under run!" )

  io.TxData_wb       := txBuff.io.deq.data.bits








  val BlockingTxStatusWrite = RegInit(false.B); io.BlockingTxStatusWrite := BlockingTxStatusWrite

  when(~io.TxDoneSync & ~io.TxAbortSync){
    BlockingTxStatusWrite := false.B
  } .elsewhen(txDonePulse | txAbortPulse){
    BlockingTxStatusWrite := true.B
  }













  // Marks which bytes are valid within the word.
  val TxValidBytesLatched = RegInit(0.U(2.W)); io.TxValidBytesLatched := TxValidBytesLatched
  val LatchValidBytes = ShiftRegisters((TxLength < 4.U), 2, false.B, true.B)
  val LatchValidBytesPluse = LatchValidBytes(0) & ~LatchValidBytes(1)

  // Latching valid bytes
  when(LatchValidBytesPluse){
    TxValidBytesLatched := Mux(TxLength < 4.U, TxLength(1,0), 0.U)
  } .elsewhen(txRetryPulse | txDonePulse | txAbortPulse){
    TxValidBytesLatched := 0.U
  }





}



class MacTileLink() extends MacTileLinkBase(){
  txBuff.io.enq.ctrl.valid := false.B
  txBuff.io.enq.data.valid := false.B
  txBuff.io.enq.data.bits  := 0.U

  rxBuff.io.deq.data.ready := false.B
  rxBuff.io.deq.ctrl.ready := false.B
}




























