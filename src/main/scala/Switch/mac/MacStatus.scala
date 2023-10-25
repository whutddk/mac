package MAC

import chisel3._
import chisel3.util._

class MacStatusIO extends Bundle{
  val asyncReset = Input(AsyncReset())

  val MRxClk              = Input(Bool())
  val RxCrcError          = Input(Bool())
  val MRxErr              = Input(Bool())
  val MRxDV               = Input(Bool())
  val RxStateSFD          = Input(Bool())
  val RxStateData         = Input(UInt(2.W))
  val RxStatePreamble     = Input(Bool())
  val RxStateIdle         = Input(Bool())
  val Transmitting        = Input(Bool())
  val RxByteCnt           = Input(UInt(16.W))
  val RxByteCntEq0        = Input(Bool())
  val RxByteCntGreat2     = Input(Bool())
  val RxByteCntMaxFrame   = Input(Bool())
  val MRxD                = Input(UInt(4.W))
  val Collision           = Input(Bool())
  val CollValid           = Input(UInt(6.W))
  val r_MinFL             = Input(UInt(16.W))
  val r_MaxFL             = Input(UInt(16.W))
  val r_HugEn             = Input(Bool())
  val StartTxDone         = Input(Bool())
  val StartTxAbort        = Input(Bool())
  val MTxClk              = Input(Bool())
  val MaxCollisionOccured = Input(Bool())
  val LateCollision       = Input(Bool())
  val DeferIndication     = Input(Bool())
  val TxStartFrm          = Input(Bool())
  val StatePreamble       = Input(Bool())
  val StateData           = Input(UInt(2.W))
  val CarrierSense        = Input(Bool())
  val Loopback            = Input(Bool())
  val r_FullD             = Input(Bool())
  val RstDeferLatched     = Input(Bool())

  val ReceivedLengthOK     = Output(Bool())
  val ReceiveEnd           = Output(Bool())
  val ReceivedPacketGood   = Output(Bool())
  val LatchedCrcError      = Output(Bool())
  val RxLateCollision      = Output(Bool())
  val DribbleNibble        = Output(Bool())
  val ReceivedPacketTooBig = Output(Bool())
  val LoadRxStatus         = Output(Bool())
  val RetryLimit           = Output(Bool())
  val LateCollLatched      = Output(Bool())
  val DeferLatched         = Output(Bool())
  val CarrierSenseLost     = Output(Bool())
}

class MacStatus extends RawModule{
  val io: MacStatusIO = IO(new MacStatusIO)

  withClockAndReset( io.MRxClk.asClock, io.asyncReset ) {

    val LatchedCrcError = RegInit(false.B); io.LatchedCrcError := LatchedCrcError // Crc error

    when(io.RxStateSFD){
      LatchedCrcError := false.B
    } .elsewhen(io.RxStateData.extract(0)){
      LatchedCrcError := io.RxCrcError & ~io.RxByteCntEq0;    
    }



    io.ReceivedPacketGood := ~LatchedCrcError                                        // ReceivedPacketGood
    io.ReceivedLengthOK := io.RxByteCnt >= io.r_MinFL & io.RxByteCnt <= io.r_MaxFL   // ReceivedLengthOK

    // Time to take a sample
    val TakeSample =
      (io.RxStateData.orR & ~io.MRxDV) |
      (io.RxStateData.extract(0) & io.MRxDV & io.RxByteCntMaxFrame)

    
    val LoadRxStatus = RegNext(TakeSample, false.B); io.LoadRxStatus := LoadRxStatus // LoadRxStatus
    val ReceiveEnd = RegNext(LoadRxStatus, false.B); io.ReceiveEnd := ReceiveEnd     // ReceiveEnd




    val RxLateCollision = RegInit(false.B); io.RxLateCollision := RxLateCollision // Late Collision
    val RxColWindow = RegInit(true.B)// Collision Window

    when(LoadRxStatus){
      RxLateCollision := false.B
    } .elsewhen(io.Collision & (~io.r_FullD) ){
      RxLateCollision := true.B
    }

    when(~io.Collision & io.RxByteCnt(5,0) === io.CollValid & io.RxStateData.extract(1)){
      RxColWindow := false.B
    } .elsewhen(io.RxStateIdle){
      RxColWindow := true.B
    }




    // DribbleNibble
    val DribbleNibble = RegInit(false.B); io.DribbleNibble := DribbleNibble

    when(io.RxStateSFD){
      DribbleNibble := false.B
    } .elsewhen(~io.MRxDV & io.RxStateData.extract(1)){
      DribbleNibble := true.B
    }


    val ReceivedPacketTooBig = RegInit(false.B); io.ReceivedPacketTooBig := ReceivedPacketTooBig

    when(LoadRxStatus){
      ReceivedPacketTooBig := false.B
    } .elsewhen(TakeSample){
      ReceivedPacketTooBig := ~io.r_HugEn & io.RxByteCnt > io.r_MaxFL
    }


  }



  withClockAndReset( io.MTxClk.asClock, io.asyncReset ) {
    val RetryLimit      = RegEnable( io.MaxCollisionOccured, false.B, io.StartTxDone | io.StartTxAbort ); io.RetryLimit := RetryLimit // Latched Retransmission limit
    val LateCollLatched = RegEnable( io.LateCollision, false.B, io.StartTxDone | io.StartTxAbort); io.LateCollLatched := LateCollLatched // Latched Late Collision


    val DeferLatched = RegInit(false.B); io.DeferLatched := DeferLatched // Latched Defer state

    when(io.DeferIndication){
      DeferLatched := true.B
    } .elsewhen(io.RstDeferLatched){
      DeferLatched := false.B
    }

    val CarrierSenseLost = RegInit(false.B); io.CarrierSenseLost := CarrierSenseLost // CarrierSenseLost

    when((io.StatePreamble | io.StateData.orR) & ~io.CarrierSense & ~io.Loopback & ~io.Collision & ~io.r_FullD){
      CarrierSenseLost := true.B
    } .elsewhen(io.TxStartFrm){
      CarrierSenseLost := false.B
    }




  }
}


