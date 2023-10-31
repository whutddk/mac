package MAC

import chisel3._
import chisel3.util._

import freechips.rocketchip.util._


class Mac_Stream_Bundle extends Bundle{
  val data = UInt(8.W)
  val isStart = Bool()
  val isLast = Bool()
}



class MacTileLinkRxIO extends Bundle{

  val LoadRxStatus = Input(Bool())

  val RxData     = Input(UInt(8.W))      // Received data byte (from PHY)
  val RxValid    = Input(Bool())
  val RxStartFrm = Input(Bool())
  val RxEndFrm   = Input(Bool())

  val DribbleNibble = Input(Bool())
  val LatchedCrcError = Input(Bool())
  val RxLateCollision = Input(Bool())

  val asyncReset = Input(AsyncReset())
  val MRxClk     = Input(Bool())

  val Busy_IRQ = Output(Bool())


  val r_RxEn    = Input(Bool())         // Receive enable



  val rxEnq = Decoupled(new Mac_Stream_Bundle)



}


class MacTileLinkRx extends Module{
  val io = IO(new MacTileLinkRxIO)

  val syncReqBundle  = Wire(Decoupled(new Mac_Stream_Bundle))
  val asyncReqBundle = Wire(Decoupled(new Mac_Stream_Bundle))
  val req_ToAsync    = Wire(new AsyncBundle(new Mac_Stream_Bundle))

  val rxReady = RegInit(false.B)
  val rxReadyAsync = Wire(Bool())
  withClockAndReset( io.MRxClk.asClock, io.asyncReset ) {
    rxReadyAsync  := ShiftRegister( rxReady, 2, false.B, true.B )
  }


  withClockAndReset(io.MRxClk.asClock, io.asyncReset){

    when( io.LoadRxStatus ) {
      when( io.DribbleNibble ) { printf("Warning! DribbleNibble!\n"); }
      when( io.LatchedCrcError ) { printf("Warning! LatchedCrcError!\n"); }
      when( io.RxLateCollision ) { printf("Warning! RxLateCollision!\n"); }    
    }

    val RxEnableWindow = RegInit(false.B)
    



    // Indicating start of the reception process
    val SetWriteRxDataToFifo =
      io.RxValid & rxReadyAsync & (io.RxStartFrm | RxEnableWindow )


      // Generation of the end-of-frame signal
      when(io.RxStartFrm){
        RxEnableWindow := true.B
      } .elsewhen(io.RxEndFrm ){
        RxEnableWindow := false.B
      }






    val rxFifo = Module(new Queue(new Mac_Stream_Bundle, 4))
    rxFifo.io.enq.valid := SetWriteRxDataToFifo
    rxFifo.io.enq.bits.data   := io.RxData
    rxFifo.io.enq.bits.isStart := io.RxStartFrm
    rxFifo.io.enq.bits.isLast := io.RxEndFrm
    assert( ~(rxFifo.io.enq.valid & ~rxFifo.io.enq.ready) )

    asyncReqBundle <> rxFifo.io.deq


  }

































 
  syncReqBundle <> FromAsyncBundle( req_ToAsync )

  withClockAndReset(io.MRxClk.asClock, io.asyncReset) {  
    req_ToAsync <> ToAsyncBundle( asyncReqBundle )
  }

    
    





      

  
  // Busy Interrupt
  val Busy_IRQ_rck = withClockAndReset(io.MRxClk.asClock, io.asyncReset) {RegInit(false.B)}
  val Busy_IRQ_sync = ShiftRegister(Busy_IRQ_rck, 2, false.B, true.B)


  io.Busy_IRQ := Busy_IRQ_sync & ~RegNext(Busy_IRQ_sync, false.B)


  withClockAndReset( io.MRxClk.asClock, io.asyncReset ) {
    val Busy_IRQ_syncb = ShiftRegister( Busy_IRQ_sync,  2, false.B, true.B )

    when(io.RxValid & io.RxStartFrm & ~rxReadyAsync){
      Busy_IRQ_rck := true.B
    } .elsewhen(Busy_IRQ_syncb){
      Busy_IRQ_rck := false.B
    }

  }

























  






  when(syncReqBundle.bits.isLast & syncReqBundle.fire ){
    rxReady := false.B
  } .elsewhen( io.r_RxEn & (io.rxEnq.ready) ){
    rxReady := true.B
  }


  io.rxEnq.bits  := syncReqBundle.bits
  io.rxEnq.valid := syncReqBundle.valid
  syncReqBundle.ready      := io.rxEnq.ready



  assert( ~(io.rxEnq.valid & ~io.rxEnq.ready), "Assert Failed, rx overrun!" )













}



