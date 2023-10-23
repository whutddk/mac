package MAC

import chisel3._
import chisel3.util._


class MacTileLinkTxIO extends Bundle{

  val txReq  = Flipped(Decoupled(UInt(8.W)))
  // val txResp = Decoupled()



  val RstDeferLatched  = Output(Bool())
  val BlockingTxStatusWrite_sync = Input(Bool())
  val TxStartFrm_sync            = Input(Bool())

  val TxStartFrm     = Output(Bool())     // Transmit packet start frame
  val TxEndFrm       = Output(Bool())     // Transmit packet end frame
  val TxData         = Output(UInt(8.W))  // Transmit packet data byte

  val TxUsedData     = Input(Bool())      // Transmit packet used data
  val TxRetry        = Input(Bool())      // Transmit packet retry
  val TxAbort        = Input(Bool())      // Transmit packet abort
  val TxDone         = Input(Bool())      // Transmission ended

  // val ReadTxDataFromFifo_tck = Output(Bool())
  // val ReadTxDataFromFifo_syncb = Input(Bool())

  val TxEndFrm_wb = Input(Bool())
  // val TxData_wb = Input(UInt(8.W))
}



class MacTileLinkTx extends Module with RequireAsyncReset{
  val io = IO(new MacTileLinkTxIO)



  io.RstDeferLatched := io.BlockingTxStatusWrite_sync & ~RegNext(io.BlockingTxStatusWrite_sync, false.B)

  val TxStartFrm = RegInit(false.B);  io.TxStartFrm := TxStartFrm
  val TxEndFrm   = RegInit(false.B);  io.TxEndFrm   := TxEndFrm
  val TxData     = RegInit(0.U(8.W)); io.TxData     := TxData
  val LastWord = RegInit(false.B)
  val ReadTxDataFromFifo_tck = RegInit(false.B); 
  // io.ReadTxDataFromFifo_tck := ReadTxDataFromFifo_tck
  

  // Generating delayed signals
  val TxAbort_q    = RegNext( io.TxAbort, false.B)
  val TxRetry_q    = RegNext( io.TxRetry, false.B)
  val TxUsedData_q = RegNext( io.TxUsedData, false.B)

  // Changes for tx occur every second clock. Flop is used for this manner.
  val Flop = RegInit(false.B)
  when( io.TxDone | io.TxAbort | TxRetry_q){
    Flop := false.B
  } .elsewhen ( io.TxUsedData ){
    Flop := ~Flop
  }
  
  when(io.TxStartFrm_sync){
    TxStartFrm := true.B
  } .elsewhen(TxUsedData_q | ~io.TxStartFrm_sync & (io.TxRetry & (~TxRetry_q) | io.TxAbort & (~TxAbort_q))){
    TxStartFrm := false.B
  }

  // Indication of the last word
  when( (TxEndFrm | io.TxAbort | io.TxRetry) & Flop ){
    LastWord := false.B
  } .elsewhen( io.TxUsedData & Flop ){
    LastWord := io.TxEndFrm_wb
  }

  // Tx end frame generation
  when(Flop & TxEndFrm | io.TxAbort | TxRetry_q){
    TxEndFrm := false.B
  } .elsewhen(Flop & LastWord){
    TxEndFrm := true.B
  }



  // when(
  //   io.TxStartFrm_sync & ~TxStartFrm |
  //   io.TxUsedData & Flop & ~LastWord |
  //   TxStartFrm & io.TxUsedData & Flop ){
  //   ReadTxDataFromFifo_tck := true.B
  //   // TxData := io.TxData_wb( 7, 0)
  //   
  // } .elsewhen(io.ReadTxDataFromFifo_syncb & ~RegNext(io.ReadTxDataFromFifo_syncb, false.B)){
  //   ReadTxDataFromFifo_tck := false.B
  // }

  io.txReq.ready := 
    io.TxStartFrm_sync & ~TxStartFrm |
    io.TxUsedData & Flop & ~LastWord |
    TxStartFrm & io.TxUsedData & Flop 

  TxData := io.txReq.bits

  assert( ~(io.txReq.ready & ~io.txReq.valid) )

}

