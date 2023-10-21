package MAC

import chisel3._
import chisel3.util._



class MacTileLinkRxIO extends Bundle{
    // val RxDataLatched2 = Output(UInt(8.W))
    // val WriteRxDataToFifo = Output(Bool())

  val asyncToBuf = Decoupled(UInt(8.W))

  val RxAbortLatched = Output(Bool())
  val LatchedRxLength = Output(UInt(16.W))
  val RxStatusInLatched = Output( UInt(9.W) )
  val ShiftEnded_rck = Output(Bool())

  val RxLength = Input(UInt(16.W))
  val LoadRxStatus = Input(Bool())
      val RxStatusIn = Input(UInt(9.W))
  val ShiftEndedSyncb = Input(Bool())
  val RxAbortSyncb = Input(Bool())
  // val WriteRxDataToFifoSyncb = Input(Bool())

  val Busy_IRQ_rck = Output(Bool())
  val Busy_IRQ_syncb = Input(Bool())

  val RxData     = Input(UInt(8.W))      // Received data byte (from PHY)
  val RxAbort    = Input(Bool())
  val RxValid    = Input(Bool())
  val RxReady    = Input(Bool())
  val RxStartFrm = Input(Bool())
  val RxEndFrm   = Input(Bool())
}


class MacTileLinkRx extends Module with RequireAsyncReset{
  val io = IO(new MacTileLinkRxIO)


  val ShiftWillEnd = RegInit(false.B)
  val RxAbortLatched = RegInit(false.B); io.RxAbortLatched := RxAbortLatched

  val LatchedRxLength = RegEnable(io.RxLength, 0.U(16.W), io.LoadRxStatus); io.LatchedRxLength   := LatchedRxLength
  val RxStatusInLatched = RegEnable(io.RxStatusIn, 0.U(9.W), io.LoadRxStatus); io.RxStatusInLatched := RxStatusInLatched

  val ShiftEnded_rck = RegInit(false.B); io.ShiftEnded_rck := ShiftEnded_rck
  val RxEnableWindow = RegInit(false.B)
  
  val Busy_IRQ_rck = RegInit(false.B); io.Busy_IRQ_rck := Busy_IRQ_rck


  // Indicating start of the reception process
  val SetWriteRxDataToFifo =
    io.RxValid & io.RxReady & (io.RxStartFrm | RxEnableWindow )



        // when(SetWriteRxDataToFifo & ~io.RxAbort){
        //   WriteRxDataToFifo := true.B
        //   RxDataLatched2 := io.RxData
        // } .elsewhen(io.WriteRxDataToFifoSyncb | io.RxAbort){
        //   WriteRxDataToFifo := false.B
        // }

    // Generation of the end-of-frame signal
    when(SetWriteRxDataToFifo & ~io.RxAbort & io.RxEndFrm){
      ShiftEnded_rck := true.B
    } .elsewhen(io.RxAbort | io.ShiftEndedSyncb & RegNext(io.ShiftEndedSyncb, false.B) ){
      ShiftEnded_rck := false.B
    }

    // Generation of the end-of-frame signal
    when(io.RxStartFrm){
      RxEnableWindow := true.B
    } .elsewhen(io.RxEndFrm | io.RxAbort){
      RxEnableWindow := false.B
    }


    when(io.RxAbortSyncb){
      RxAbortLatched := false.B
    } .elsewhen(io.RxAbort){
      RxAbortLatched := true.B
    }

    when(io.RxValid & io.RxStartFrm & ~io.RxReady){
      Busy_IRQ_rck := true.B
    } .elsewhen(io.Busy_IRQ_syncb){
      Busy_IRQ_rck := false.B
    }


  val rxFifo = Module(new Queue(UInt(8.W), 4))
  rxFifo.io.enq.valid := SetWriteRxDataToFifo & ~io.RxAbort
  rxFifo.io.enq.bits  := io.RxData
  assert( ~(rxFifo.io.enq.valid & ~rxFifo.io.enq.ready) )

  io.asyncToBuf <> rxFifo.io.deq
}



