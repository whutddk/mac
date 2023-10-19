package MAC

import chisel3._
import chisel3.util._



class MacTileLinkRxIO extends Bundle{
  val RxDataLatched2 = Output(UInt(32.W))
  val WriteRxDataToFifo = Output(Bool())
  val RxAbortLatched = Output(Bool())
  val LatchedRxLength = Output(UInt(16.W))
  val RxStatusInLatched = Output( UInt(9.W) )
  val ShiftEnded_rck = Output(Bool())

  val RxLength = Input(UInt(16.W))
  val LoadRxStatus = Input(Bool())
      val RxStatusIn = Input(UInt(9.W))
  val ShiftEndedSyncb = Input(Bool())
  val RxAbortSyncb = Input(Bool())
  val WriteRxDataToFifoSyncb = Input(Bool())

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

  val RxDataLatched2 = RegInit(0.U(32.W)); io.RxDataLatched2 := RxDataLatched2
  val RxDataLatched1 = RegInit(0.U(24.W))     // Little Endian Byte Ordering[23:0] 
  val RxValidBytes = RegInit(1.U(2.W))
  val RxByteCnt    = RegInit(0.U(2.W))
  val LastByteIn   = RegInit(false.B)
  val ShiftWillEnd = RegInit(false.B)
  val WriteRxDataToFifo = RegInit(false.B); io.WriteRxDataToFifo := WriteRxDataToFifo
  val RxAbortLatched = RegInit(false.B); io.RxAbortLatched := RxAbortLatched

  val LatchedRxLength = RegEnable(io.RxLength, 0.U(16.W), io.LoadRxStatus); io.LatchedRxLength   := LatchedRxLength
  val RxStatusInLatched = RegEnable(io.RxStatusIn, 0.U(9.W), io.LoadRxStatus); io.RxStatusInLatched := RxStatusInLatched

  val ShiftEnded_rck = RegInit(false.B); io.ShiftEnded_rck := ShiftEnded_rck
  val RxEnableWindow = RegInit(false.B)
  
  val Busy_IRQ_rck = RegInit(false.B); io.Busy_IRQ_rck := Busy_IRQ_rck




  // Indicating that last byte is being reveived
  when(ShiftWillEnd & RxByteCnt.andR | io.RxAbort){
    LastByteIn := false.B
  } .elsewhen(io.RxValid & io.RxReady & io.RxEndFrm & ~(RxByteCnt.andR) & RxEnableWindow){
    LastByteIn := true.B
  }

  // Indicating that data reception will end
  val StartShiftWillEnd = LastByteIn | io.RxValid & io.RxEndFrm & RxByteCnt.andR & RxEnableWindow
  when(ShiftEnded_rck | io.RxAbort){
    ShiftWillEnd := false.B
  } .elsewhen(StartShiftWillEnd){
    ShiftWillEnd := true.B
  }

  // Receive byte counter
  when(ShiftEnded_rck | io.RxAbort){
    RxByteCnt := 0.U
  } .elsewhen(io.RxValid & io.RxStartFrm & io.RxReady){
    RxByteCnt := 1.U     
  } .elsewhen(io.RxValid & RxEnableWindow & io.RxReady | LastByteIn){
    RxByteCnt := RxByteCnt + 1.U
  }

  // Indicates how many bytes are valid within the last word
  when(io.RxValid & io.RxStartFrm){
    RxValidBytes := 1.U
  } .elsewhen(io.RxValid & ~LastByteIn & ~io.RxStartFrm & RxEnableWindow){
    RxValidBytes := RxValidBytes + 1.U        
  }

  when(io.RxValid & io.RxReady & ~LastByteIn){
    when(io.RxStartFrm){
      RxDataLatched1 := Cat(RxDataLatched1(23, 8), io.RxData)// Little Endian Byte Ordering
    } .elsewhen(RxEnableWindow){
      RxDataLatched1 := Mux1H(Seq(
        ( RxByteCnt === 0.U ) -> Cat(RxDataLatched1(23, 8), io.RxData),// Little Endian Byte Ordering
        ( RxByteCnt === 1.U ) -> Cat(RxDataLatched1(23,16), io.RxData, RxDataLatched1( 7,0)),
        ( RxByteCnt === 2.U ) -> Cat(                       io.RxData, RxDataLatched1(15,0)),
        ( RxByteCnt === 3.U ) -> RxDataLatched1,
      ))
    }
  }

  // Indicating start of the reception process
  val SetWriteRxDataToFifo =
    (io.RxValid & io.RxReady & ~io.RxStartFrm & RxEnableWindow & (RxByteCnt.andR)) |
    (ShiftWillEnd & LastByteIn & (RxByteCnt.andR))

  // Assembling data that will be written to the rx_fifo
  when(SetWriteRxDataToFifo & ~ShiftWillEnd){
    RxDataLatched2 := Cat(io.RxData, RxDataLatched1)// Little Endian Byte Ordering
  } .elsewhen(SetWriteRxDataToFifo & ShiftWillEnd){
    RxDataLatched2 := Mux1H(Seq(                    // Little Endian Byte Ordering
      ( RxValidBytes === 0.U ) -> Cat(io.RxData, RxDataLatched1),
      ( RxValidBytes === 1.U ) -> Cat(0.U(24.W), RxDataLatched1(7,0) ),
      ( RxValidBytes === 2.U ) -> Cat(0.U(16.W), RxDataLatched1(15, 0) ),
      ( RxValidBytes === 3.U ) -> Cat(0.U(8.W),  RxDataLatched1        ),       
    ))
  }

    when(SetWriteRxDataToFifo & ~io.RxAbort){
      WriteRxDataToFifo := true.B
    } .elsewhen(io.WriteRxDataToFifoSyncb | io.RxAbort){
      WriteRxDataToFifo := false.B
    }

    // Generation of the end-of-frame signal
    when(~io.RxAbort & SetWriteRxDataToFifo & StartShiftWillEnd){
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


}



