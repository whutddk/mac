package MAC

import chisel3._
import chisel3.util._


class MacTileLinkTxIO extends Bundle{
  val RstDeferLatched  = Output(Bool())
  val BlockingTxStatusWrite_sync = Input(Bool())
  val TxStartFrm_sync            = Input(Bool())

  val TxStartFrm     = Output(Bool())     // Transmit packet start frame
  val TxEndFrm       = Output(Bool())     // Transmit packet end frame
  val TxData         = Output(UInt(8.W))  // Transmit packet data byte
  val TxUnderRun     = Output(Bool())     // Transmit packet under-run

  val TxUsedData     = Input(Bool())      // Transmit packet used data
  val TxRetry        = Input(Bool())      // Transmit packet retry
  val TxAbort        = Input(Bool())      // Transmit packet abort
  val TxDone         = Input(Bool())      // Transmission ended

  val ReadTxDataFromFifo_tck = Output(Bool())
  val ReadTxDataFromFifo_syncb = Input(Bool())

  val TxEndFrm_wb = Input(Bool())
  val TxValidBytesLatched = Input(UInt(2.W))
  val TxData_wb = Input(UInt(32.W))
  val TxUnderRun_wb = Input(Bool())
}



class MacTileLinkTx extends Module with RequireAsyncReset{
  val io = IO(new MacTileLinkTxIO)



  io.RstDeferLatched := io.BlockingTxStatusWrite_sync & ~RegNext(io.BlockingTxStatusWrite_sync, false.B)

  val TxStartFrm = RegInit(false.B);  io.TxStartFrm := TxStartFrm
  val TxEndFrm   = RegInit(false.B);  io.TxEndFrm   := TxEndFrm
  val TxData     = RegInit(0.U(8.W)); io.TxData     := TxData
  val TxUnderRun = RegInit(false.B);  io.TxUnderRun := TxUnderRun
  val TxDataLatched = RegInit(0.U(32.W))
  val TxByteCnt = RegInit(0.U(2.W))
  val LastWord = RegInit(false.B)
  val ReadTxDataFromFifo_tck = RegInit(false.B); io.ReadTxDataFromFifo_tck := ReadTxDataFromFifo_tck

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
  } .elsewhen( io.TxUsedData & Flop & TxByteCnt === 3.U ){
    LastWord := io.TxEndFrm_wb
  }

  // Tx end frame generation
  when(Flop & TxEndFrm | io.TxAbort | TxRetry_q){
    TxEndFrm := false.B
  } .elsewhen(Flop & LastWord){
    TxEndFrm := 
      Mux1H(Seq(
        (io.TxValidBytesLatched === 1.U) -> (TxByteCnt === 0.U),
        (io.TxValidBytesLatched === 2.U) -> (TxByteCnt === 1.U),
        (io.TxValidBytesLatched === 3.U) -> (TxByteCnt === 2.U),
        (io.TxValidBytesLatched === 0.U) -> (TxByteCnt === 3.U),
      ))  
  }

  // Tx data selection (latching)
  when( io.TxStartFrm_sync & ~TxStartFrm ){
    TxData := io.TxData_wb( 7, 0) // little Endian Byte Ordering
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
    io.TxStartFrm_sync & ~TxStartFrm |
    io.TxUsedData & Flop & TxByteCnt === 3.U |
    TxStartFrm & io.TxUsedData & Flop & TxByteCnt === 0.U){
    TxDataLatched := io.TxData_wb
  }

  val TxUnderRun_sync1 = RegInit(false.B)

  // Tx under run
  when(io.TxUnderRun_wb){
    TxUnderRun_sync1 := true.B
  } .elsewhen(io.BlockingTxStatusWrite_sync){
    TxUnderRun_sync1 := false.B
  }

  // Tx under run
  when(io.BlockingTxStatusWrite_sync){
    TxUnderRun := false.B
  } .elsewhen(TxUnderRun_sync1){
    TxUnderRun := true.B
  }

  // Tx Byte counter
  when(TxAbort_q | TxRetry_q){
    TxByteCnt := 0.U
  } .elsewhen(TxStartFrm & ~io.TxUsedData){
    TxByteCnt := 1.U
  } .elsewhen(io.TxUsedData & Flop){
    TxByteCnt := TxByteCnt + 1.U
  }

  when(io.TxStartFrm_sync & ~TxStartFrm | io.TxUsedData & Flop & TxByteCnt === 3.U &
    ~LastWord | TxStartFrm & io.TxUsedData & Flop & TxByteCnt === 0.U ){
    ReadTxDataFromFifo_tck := true.B
  } .elsewhen(io.ReadTxDataFromFifo_syncb & ~RegNext(io.ReadTxDataFromFifo_syncb, false.B)){
    ReadTxDataFromFifo_tck := false.B
  }

}

