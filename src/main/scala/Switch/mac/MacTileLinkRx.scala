package MAC

import chisel3._
import chisel3.util._


class Mac_Stream_Bundle extends Bundle{
  val data = UInt(8.W)
  val isStart = Bool()
  val isLast = Bool()
}



class MacTileLinkRxIO extends Bundle{

  val rxReq = Decoupled(new Mac_Stream_Bundle)

  // val LatchedRxLength = Output(UInt(16.W))
  val RxStatusInLatched = Output( UInt(9.W) )

  // val RxLength = Input(UInt(16.W))
  val LoadRxStatus = Input(Bool())
      val RxStatusIn = Input(UInt(9.W))


  val Busy_IRQ_rck = Output(Bool())
  val Busy_IRQ_syncb = Input(Bool())

  val RxData     = Input(UInt(8.W))      // Received data byte (from PHY)
  val RxValid    = Input(Bool())
  val RxReady    = Input(Bool())
  val RxStartFrm = Input(Bool())
  val RxEndFrm   = Input(Bool())
}


class MacTileLinkRx extends Module with RequireAsyncReset{
  val io = IO(new MacTileLinkRxIO)


  val ShiftWillEnd = RegInit(false.B)

  val RxStatusInLatched = RegEnable(io.RxStatusIn, 0.U(9.W), io.LoadRxStatus); io.RxStatusInLatched := RxStatusInLatched

  when( io.LoadRxStatus ) {
    when( io.RxStatusIn.extract(4) ) { printf("Warning! DribbleNibble!\n"); }
    when( io.RxStatusIn.extract(1) ) { printf("Warning! LatchedCrcError!\n"); }
    when( io.RxStatusIn.extract(0) ) { printf("Warning! RxLateCollision!\n"); }    
  }


  // val ShiftEnded_rck = RegInit(false.B); io.ShiftEnded_rck := ShiftEnded_rck
  val RxEnableWindow = RegInit(false.B)
  
  val Busy_IRQ_rck = RegInit(false.B); io.Busy_IRQ_rck := Busy_IRQ_rck


  // Indicating start of the reception process
  val SetWriteRxDataToFifo =
    io.RxValid & io.RxReady & (io.RxStartFrm | RxEnableWindow )


    // Generation of the end-of-frame signal
    when(io.RxStartFrm){
      RxEnableWindow := true.B
    } .elsewhen(io.RxEndFrm ){
      RxEnableWindow := false.B
    }



    when(io.RxValid & io.RxStartFrm & ~io.RxReady){
      Busy_IRQ_rck := true.B
    } .elsewhen(io.Busy_IRQ_syncb){
      Busy_IRQ_rck := false.B
    }


  val rxFifo = Module(new Queue(new Mac_Stream_Bundle, 4))
  rxFifo.io.enq.valid := SetWriteRxDataToFifo
  rxFifo.io.enq.bits.data   := io.RxData
  rxFifo.io.enq.bits.isStart := io.RxStartFrm
  rxFifo.io.enq.bits.isLast := io.RxEndFrm
  assert( ~(rxFifo.io.enq.valid & ~rxFifo.io.enq.ready) )

  io.rxReq <> rxFifo.io.deq
}



