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

    val rxReq = Flipped(Decoupled(new Mac_Stream_Bundle))
    val rxResp = Decoupled(new Bool())

    // val LatchedRxLength_rxclk = Input(UInt(16.W))
    val RxStatusInLatched_rxclk = Input(UInt(9.W))
    val RxReady = Output(Bool())



    val txReq = Decoupled(new Mac_Stream_Bundle)

    // val TxEndFrm_wb = Output(Bool())


    // Tx Status signals
    val RetryLimit       = Input(Bool())     // Retry limit reached (Retry Max value +1 attempts were made)
    val LateCollLatched  = Input(Bool())     // Late collision occured
    val DeferLatched     = Input(Bool())     // Defer indication (Frame was defered before sucessfully sent)
    val CarrierSenseLost = Input(Bool())     // Carrier Sense was lost during the frame transmission
    val PerPacketCrcEn = Output(Bool())     // Per packet crc enable

    //Register
    val r_TxEn    = Input(Bool())          // Transmit enable
    val BlockingTxStatusWrite = Output(Bool())
    val TxUsedData     = Input(Bool())      // Transmit packet used data    
    
    val TxAbortSync = Input(Bool())      // Transmit packet abort
    val TxDoneSync  = Input(Bool())      // Transmission ended


    val rxEnq = new Receive_Enq_Bundle
    val txDeq = Flipped(new Transmit_Bundle)

  }


  
  val io = IO(new MacTileLinkIO)




  val ShiftEndedSyncPluse         = io.rxReq.bits.isLast & io.rxReq.fire

  val RxReady = RegInit(false.B); io.RxReady := RxReady


  val rxBuffCtrlValid = RegInit(false.B)
  io.rxEnq.ctrl.valid := rxBuffCtrlValid
  // io.rxEnq.ctrl.bits.LatchedRxLength   := RegEnable(io.LatchedRxLength_rxclk,   ShiftEndedSyncPluse )
  io.rxEnq.ctrl.bits.RxStatusInLatched := RegEnable(io.RxStatusInLatched_rxclk, ShiftEndedSyncPluse )


  when( io.rxEnq.ctrl.fire ){
    rxBuffCtrlValid := false.B
  } .elsewhen( ShiftEndedSyncPluse ){
    rxBuffCtrlValid := true.B
  }



  // RxReady generation
  when(ShiftEndedSyncPluse ){
    RxReady := false.B
  } .elsewhen( io.r_RxEn & (io.rxEnq.req.ready) ){
    RxReady := true.B
  }


  io.rxEnq.req.bits  := io.rxReq.bits
  io.rxEnq.req.valid := io.rxReq.valid
  io.rxReq.ready      := io.rxEnq.req.ready

  val rxRespValid = RegInit(false.B)
  when( io.rxResp.fire ){
    rxRespValid := false.B
  } .elsewhen( ShiftEndedSyncPluse ){
    rxRespValid := true.B
  }
  io.rxResp.valid := rxRespValid
  io.rxResp.bits  := DontCare

  assert( ~(io.rxEnq.req.valid & ~io.rxEnq.req.ready), "Assert Failed, rx overrun!" )





















  // val TxStartFrm_wb = RegInit(false.B)
  // val TxEndFrm_wb = RegInit(false.B)

  // val TxLength = RegInit(0.U(16.W))

  val txDonePulse                 = io.TxDoneSync              & ~RegNext(io.TxDoneSync,  false.B)
  val txAbortPulse                = io.TxAbortSync             & ~RegNext(io.TxAbortSync, false.B)

  
  io.PerPacketCrcEn  := false.B

  // when( io.txDeq.req.fire ){
  //   TxStartFrm_wb := true.B
  // } .elsewhen(io.txReq.fire){
  //   TxStartFrm_wb := false.B
  // }


    // when(((TxLength - 1.U) === 0.U) & io.TxUsedData){
    //   TxEndFrm_wb := true.B
    // } .elsewhen(txDonePulse | txAbortPulse){
    //   TxEndFrm_wb := false.B
    // }

  //     val TxLength = RegInit(0.U(16.W))
  // when( io.txDeq.req.fire ){
  //   TxLength        := io.txDeq.req.bits.txLength
  // } .elsewhen( io.txDeq.data.fire ){
  //   when( TxLength < 1.U ){
  //     TxLength := 0.U
  //   } .otherwise{
  //     TxLength := TxLength - 1.U    // Length is subtracted at the data request
  //   }
  // }


  val txRespValid = RegInit(false.B)
  val txRespBits  = Reg(new Transmit_Resp_Bundle)

  io.txDeq.resp.valid := txRespValid
  io.txDeq.resp.bits  := txRespBits

  when( io.txDeq.resp.fire ){
    txRespValid := false.B
  } .elsewhen( txDonePulse | txAbortPulse ){
    txRespValid := true.B
    txRespBits.RetryLimit := io.RetryLimit
    txRespBits.LateCollLatched := io.LateCollLatched
    txRespBits.DeferLatched := io.DeferLatched
    txRespBits.CarrierSenseLost := io.CarrierSenseLost
    txRespBits.isClear := txAbortPulse
  }






  val isTxBusy = RegInit(false.B); io.txDeq.req.ready := ~isTxBusy & io.r_TxEn

  when( io.txDeq.req.fire){
    isTxBusy := true.B
  } .elsewhen(txDonePulse | txAbortPulse){
    isTxBusy := false.B
  }



  // io.txReq.bits.isLast  := TxEndFrm_wb

  // io.txReq.bits.isStart := TxStartFrm_wb
  // io.txReq.valid        := io.txDeq.data.valid
  // io.txDeq.data.ready   := io.txReq.ready

  io.txReq <> io.txDeq.req







  val BlockingTxStatusWrite = RegInit(false.B); io.BlockingTxStatusWrite := BlockingTxStatusWrite

  when(~io.TxDoneSync & ~io.TxAbortSync){
    BlockingTxStatusWrite := false.B
  } .elsewhen(txDonePulse | txAbortPulse){
    BlockingTxStatusWrite := true.B
  }













}



class MacTileLink() extends MacTileLinkBase(){




}




























