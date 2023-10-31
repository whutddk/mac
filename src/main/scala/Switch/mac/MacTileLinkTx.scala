package MAC

import chisel3._
import chisel3.util._


import freechips.rocketchip.util._


class MacTileLinkTxIO extends Bundle{



  val RstDeferLatched  = Output(Bool())

  val TxStartFrm     = Output(Bool())     // Transmit packet start frame
  val TxEndFrm       = Output(Bool())     // Transmit packet end frame
  val TxData         = Output(UInt(8.W))  // Transmit packet data byte

  val TxUsedData     = Input(Bool())      // Transmit packet used data
  val TxAbort        = Input(Bool())      // Transmit packet abort
  val TxDone         = Input(Bool())      // Transmission ended

  val MTxClk     = Input(Bool())
  val asyncReset = Input(AsyncReset())



  // Tx Status signals
  val RetryLimit       = Input(Bool())     // Retry limit reached (Retry Max value +1 attempts were made)
  val LateCollLatched  = Input(Bool())     // Late collision occured
  val DeferLatched     = Input(Bool())     // Defer indication (Frame was defered before sucessfully sent)
  val CarrierSenseLost = Input(Bool())     // Carrier Sense was lost during the frame transmission
  val PerPacketCrcEn = Output(Bool())     // Per packet crc enable

  //Register
  val r_TxEn    = Input(Bool())          // Transmit enable 
  
  val txDeq = Flipped(Decoupled(new Mac_Stream_Bundle))

}



class MacTileLinkTx extends Module{
  val io = IO(new MacTileLinkTxIO)

  val syncReqBundle  = Wire(Decoupled(new Mac_Stream_Bundle))
  val asyncReqBundle = Wire(Decoupled(new Mac_Stream_Bundle))
  val txReq_ToAsync = Wire(new AsyncBundle(new Mac_Stream_Bundle))



  withClockAndReset( io.MTxClk.asClock, io.asyncReset ){



    val TxStartFrm = RegInit(false.B);  io.TxStartFrm := TxStartFrm
    val TxEndFrm   = RegInit(false.B);  io.TxEndFrm   := TxEndFrm
    val TxData     = RegInit(0.U(8.W)); io.TxData     := TxData

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
    
    when( asyncReqBundle.valid & asyncReqBundle.bits.isStart ){
      TxStartFrm := true.B
    } .elsewhen(TxUsedData_q | io.TxAbort & (~TxAbort_q) ){
      TxStartFrm := false.B
    }



    // Tx end frame generation
    when(Flop & TxEndFrm | io.TxAbort){
      TxEndFrm := false.B
    } .elsewhen( asyncReqBundle.fire & asyncReqBundle.bits.isLast ){
      TxEndFrm := true.B
    }



    asyncReqBundle.ready := 
        asyncReqBundle.valid & asyncReqBundle.bits.isStart & ~TxStartFrm |
        io.TxUsedData & Flop & ~TxEndFrm |
        TxStartFrm & io.TxUsedData & Flop

    when( asyncReqBundle.fire ){
      TxData := asyncReqBundle.bits.data    
    }


    assert( ~(asyncReqBundle.ready & ~asyncReqBundle.valid) )



  }











  




  txReq_ToAsync <> ToAsyncBundle( syncReqBundle )
  withClockAndReset(io.MTxClk.asClock, io.asyncReset) {  
    asyncReqBundle <> FromAsyncBundle( txReq_ToAsync )  
  }

  val BlockingTxStatusWrite = RegInit(false.B)
  
  withClockAndReset( io.MTxClk.asClock, io.asyncReset ){
    val BlockingTxStatusWrite_sync = ShiftRegister( BlockingTxStatusWrite, 2, false.B, true.B)
    io.RstDeferLatched := BlockingTxStatusWrite_sync & ~RegNext(BlockingTxStatusWrite_sync, false.B)
  }



  val TxAbortSync = ShiftRegister( io.TxAbort, 2, false.B, true.B )
  val TxDoneSync  = ShiftRegister( io.TxDone,  2, false.B, true.B )






























  val txDonePulse                 = TxDoneSync              & ~RegNext(TxDoneSync,  false.B)
  val txAbortPulse                = TxAbortSync             & ~RegNext(TxAbortSync, false.B)

  
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


  io.txDeq.ready := Mux( killTrans, true.B,  isTxEnable & syncReqBundle.ready )
  syncReqBundle.valid := Mux( killTrans, false.B, isTxEnable & io.txDeq.valid )
  syncReqBundle.bits  := io.txDeq.bits











  when(~TxDoneSync & ~TxAbortSync){
    BlockingTxStatusWrite := false.B
  } .elsewhen(txDonePulse | txAbortPulse){
    BlockingTxStatusWrite := true.B
  }


  



}

