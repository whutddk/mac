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

    val RxStatusInLatched_rxclk = Input(UInt(9.W))
    val RxReady = Output(Bool())



    val txReq = Decoupled(new Mac_Stream_Bundle)



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


    val rxEnq = Decoupled(new Mac_Stream_Bundle)
    val txDeq = Flipped(Decoupled(new Mac_Stream_Bundle))

  }


  
  val io = IO(new MacTileLinkIO)



  val RxReady = RegInit(false.B); io.RxReady := RxReady






  // RxReady generation
  when(io.rxReq.bits.isLast & io.rxReq.fire ){
    RxReady := false.B
  } .elsewhen( io.r_RxEn & (io.rxEnq.ready) ){
    RxReady := true.B
  }


  io.rxEnq.bits  := io.rxReq.bits
  io.rxEnq.valid := io.rxReq.valid
  io.rxReq.ready      := io.rxEnq.ready



  assert( ~(io.rxEnq.valid & ~io.rxEnq.ready), "Assert Failed, rx overrun!" )


















  val txDonePulse                 = io.TxDoneSync              & ~RegNext(io.TxDoneSync,  false.B)
  val txAbortPulse                = io.TxAbortSync             & ~RegNext(io.TxAbortSync, false.B)

  
  io.PerPacketCrcEn  := false.B




  when( txDonePulse | txAbortPulse ){

    when(io.RetryLimit) {printf("Warning, io.RetryLimit\n")}
    when(io.LateCollLatched) {printf("Warning, io.LateCollLatched\n")}
    when(io.DeferLatched) {printf("Warning, io.DeferLatched\n")}
    when(io.CarrierSenseLost) {printf("Warning, io.CarrierSenseLost\n")}
    when(txAbortPulse) {printf("Warning, txAbortPulse\n")}
  }

  val killTrans = RegInit(false.B)

  when( (io.txDeq.fire & io.txDeq.bits.isLast) | ~io.txDeq.valid ){
    killTrans := false.B
  } .elsewhen( txAbortPulse ){
    killTrans := true.B
  }






  val isTxEnable = RegInit(false.B)

 

  when( io.txDeq.fire & io.txDeq.bits.isLast & ~io.r_TxEn ){
    isTxEnable := false.B
  } .elsewhen( io.r_TxEn ){
    isTxEnable := true.B
  }


  io.txDeq.ready := Mux( killTrans, true.B,  isTxEnable & io.txReq.ready )
  io.txReq.valid := Mux( killTrans, false.B, isTxEnable & io.txDeq.valid )
  io.txReq.bits  := io.txDeq.bits









  val BlockingTxStatusWrite = RegInit(false.B); io.BlockingTxStatusWrite := BlockingTxStatusWrite

  when(~io.TxDoneSync & ~io.TxAbortSync){
    BlockingTxStatusWrite := false.B
  } .elsewhen(txDonePulse | txAbortPulse){
    BlockingTxStatusWrite := true.B
  }













}



class MacTileLink() extends MacTileLinkBase(){




}




























