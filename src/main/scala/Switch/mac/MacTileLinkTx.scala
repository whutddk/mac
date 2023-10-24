package MAC

import chisel3._
import chisel3.util._


class TxFifo_Stream_Bundle extends Bundle{
  val data = UInt(8.W)
  val isStart = Bool()
  val isLast  = Bool()
}


class MacTileLinkTxIO extends Bundle{

  val txReq  = Flipped(Decoupled(new TxFifo_Stream_Bundle))
  // val txResp = Decoupled()


  val RstDeferLatched  = Output(Bool())
  val BlockingTxStatusWrite_sync = Input(Bool())

  val TxStartFrm     = Output(Bool())     // Transmit packet start frame
  val TxEndFrm       = Output(Bool())     // Transmit packet end frame
  val TxData         = Output(UInt(8.W))  // Transmit packet data byte

  val TxUsedData     = Input(Bool())      // Transmit packet used data
  val TxRetry        = Input(Bool())      // Transmit packet retry
  val TxAbort        = Input(Bool())      // Transmit packet abort
  val TxDone         = Input(Bool())      // Transmission ended


  // val TxEndFrm_wb = Input(Bool())
}



class MacTileLinkTx extends Module with RequireAsyncReset{
  val io = IO(new MacTileLinkTxIO)



  io.RstDeferLatched := io.BlockingTxStatusWrite_sync & ~RegNext(io.BlockingTxStatusWrite_sync, false.B)

  val TxStartFrm = RegInit(false.B);  io.TxStartFrm := TxStartFrm
  val TxEndFrm   = RegInit(false.B);  io.TxEndFrm   := TxEndFrm
  val TxData     = RegInit(0.U(8.W)); io.TxData     := TxData
  val LastWord = RegInit(false.B)
  val ReadTxDataFromFifo_tck = RegInit(false.B); 

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
  
  when( io.txReq.valid & io.txReq.bits.isStart ){
    TxStartFrm := true.B
  } .elsewhen(TxUsedData_q | io.TxRetry & (~TxRetry_q) | io.TxAbort & (~TxAbort_q) ){
    TxStartFrm := false.B
  }

  // Indication of the last word
  when( (TxEndFrm | io.TxAbort | io.TxRetry) & Flop ){
    LastWord := false.B
  } .elsewhen( io.txReq.fire ){
    LastWord := io.txReq.bits.isLast
  }

  // Tx end frame generation
  when(Flop & TxEndFrm | io.TxAbort | TxRetry_q){
    TxEndFrm := false.B
  } .elsewhen(Flop & LastWord){
    TxEndFrm := true.B
  }



  io.txReq.ready := 
      io.txReq.valid & io.txReq.bits.isStart & ~TxStartFrm |
      io.TxUsedData & Flop & ~LastWord |
      TxStartFrm & io.TxUsedData & Flop

  when( io.txReq.fire ){
    TxData := io.txReq.bits.data    
  }


  assert( ~(io.txReq.ready & ~io.txReq.valid) )

}

