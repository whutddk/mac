package MAC

import chisel3._
import chisel3.util._





class MacTileLinkTxIO extends Bundle{

  val txReq  = Flipped(Decoupled(new Mac_Stream_Bundle))
  // val txResp = Decoupled()


  val RstDeferLatched  = Output(Bool())
  val BlockingTxStatusWrite_sync = Input(Bool())

  val TxStartFrm     = Output(Bool())     // Transmit packet start frame
  val TxEndFrm       = Output(Bool())     // Transmit packet end frame
  val TxData         = Output(UInt(8.W))  // Transmit packet data byte

  val TxUsedData     = Input(Bool())      // Transmit packet used data
  val TxAbort        = Input(Bool())      // Transmit packet abort
  val TxDone         = Input(Bool())      // Transmission ended

}



class MacTileLinkTx extends Module with RequireAsyncReset{
  val io = IO(new MacTileLinkTxIO)



  io.RstDeferLatched := io.BlockingTxStatusWrite_sync & ~RegNext(io.BlockingTxStatusWrite_sync, false.B)

  val TxStartFrm = RegInit(false.B);  io.TxStartFrm := TxStartFrm
  val TxEndFrm   = RegInit(false.B);  io.TxEndFrm   := TxEndFrm
  val TxData     = RegInit(0.U(8.W)); io.TxData     := TxData
  // val LastWord = RegInit(false.B)
  val ReadTxDataFromFifo_tck = RegInit(false.B); 

  // Generating delayed signals
  val TxAbort_q    = RegNext( io.TxAbort, false.B)
  val TxUsedData_q = RegNext( io.TxUsedData, false.B)

  // Changes for tx occur every second clock. Flop is used for this manner.
  val Flop = RegInit(false.B)
  when( io.TxDone | io.TxAbort){
    Flop := false.B
  } .elsewhen ( io.TxUsedData ){
    Flop := ~Flop
  }
  
  when( io.txReq.valid & io.txReq.bits.isStart ){
    TxStartFrm := true.B
  } .elsewhen(TxUsedData_q | io.TxAbort & (~TxAbort_q) ){
    TxStartFrm := false.B
  }



  // Tx end frame generation
  when(Flop & TxEndFrm | io.TxAbort){
    TxEndFrm := false.B
  } .elsewhen( io.txReq.fire & io.txReq.bits.isLast ){
    TxEndFrm := true.B
  }



  io.txReq.ready := 
      io.txReq.valid & io.txReq.bits.isStart & ~TxStartFrm |
      io.TxUsedData & Flop & ~TxEndFrm |
      TxStartFrm & io.TxUsedData & Flop

  when( io.txReq.fire ){
    TxData := io.txReq.bits.data    
  }


  assert( ~(io.txReq.ready & ~io.txReq.valid) )

}

