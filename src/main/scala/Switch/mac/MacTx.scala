package MAC

import chisel3._
import chisel3.util._


class MacTxIO extends Bundle{

  val TxStartFrm      = Input(Bool())         // Transmit packet start frame
  val TxEndFrm        = Input(Bool())         // Transmit packet end frame
  val TxData          = Input(UInt(8.W))         // Transmit packet data byte
  val CarrierSense    = Input(Bool())         // Carrier sense (synchronized)
  val Collision       = Input(Bool())         // Collision (synchronized)
  val Pad             = Input(Bool())         // Pad enable (from register)
  val CrcEn           = Input(Bool())         // Crc enable (from register)
  val FullD           = Input(Bool())         // Full duplex (from register)
  val HugEn           = Input(Bool())         // Huge packets enable (from register)
  val DlyCrcEn        = Input(Bool())         // Delayed Crc enabled (from register)
  val MinFL           = Input(UInt(16.W))         // Minimum frame length (from register)
  val MaxFL           = Input(UInt(16.W))         // Maximum frame length (from register)
  val IPGT            = Input(UInt(7.W))         // Back to back transmit inter packet gap parameter (from register)
  val IPGR1           = Input(UInt(7.W))         // Non back to back transmit inter packet gap parameter IPGR1 (from register)
  val IPGR2           = Input(UInt(7.W))         // Non back to back transmit inter packet gap parameter IPGR2 (from register)
  val CollValid       = Input(UInt(6.W))         // Valid collision window (from register)
  val MaxRet          = Input(UInt(4.W))         // Maximum retry number (from register)
  val NoBckof         = Input(Bool())         // No backoff (from register)
  val ExDfrEn         = Input(Bool())         // Excessive defferal enable (from register)

  val MTxD               = Output(UInt(4.W))     // Transmit nibble (to PHY)
  val MTxEn              = Output(Bool())     // Transmit enable (to PHY)
  val MTxErr             = Output(Bool())     // Transmit error (to PHY)
  val TxDone             = Output(Bool())     // Transmit packet done (to RISC)
  val TxRetry            = Output(Bool())     // Transmit packet retry (to RISC)
  val TxAbort            = Output(Bool())     // Transmit packet abort (to RISC)
  val TxUsedData         = Output(Bool())     // Transmit packet used data (to RISC)
  val WillTransmit       = Output(Bool())     // Will transmit (to RxEthMAC)
  val ResetCollision     = Output(Bool())     // Reset Collision (for synchronizing collision)
  val RetryCnt           = Output(UInt(4.W))     // Latched Retry Counter for tx status purposes
  val StartTxDone        = Output(Bool())
  val StartTxAbort       = Output(Bool())
  val MaxCollisionOccured= Output(Bool())
  val LateCollision      = Output(Bool())
  val DeferIndication    = Output(Bool())
  val StatePreamble      = Output(Bool())
  val StateData          = Output(UInt(2.W))    
}

abstract class MacTxBase extends Module with RequireAsyncReset{
  val io: MacTxIO = IO(new MacTxIO)




  val StartIPG              = Wire(Bool())
  val StartPreamble         = Wire(Bool())
  val StartData             = Wire( Vec(2,Bool()))
  val StartFCS              = Wire(Bool())
  val StartJam              = Wire(Bool())
  val StartDefer            = Wire(Bool())
  val StartBackoff          = Wire(Bool())




  val StateSFD              = Wire(Bool())

  val TooBig                = Wire(Bool())

  val CrcError              = Wire(Bool())


  val NibbleMinFl           = Wire(Bool())
  val ExcessiveDefer        = Wire(Bool())

  val MaxFrame              = Wire(Bool())
  val RetryMax              = Wire(Bool())
  val RandomEq0             = Wire(Bool())
  val RandomEqByteCnt       = Wire(Bool())

  val StateIPG  = RegInit(false.B)
  val StateIdle = RegInit(false.B)
  val StatePreamble = RegInit(false.B); io.StatePreamble := StatePreamble
  val StateData = RegNext( Cat(StartData(1), StartData(0)), 0.U(2.W)); io.StateData := StateData
  val StatePAD = RegInit(false.B)
  val StateFCS = RegInit(false.B)
  val StateJam = RegInit(false.B)
  val StateJam_q = RegNext(StateJam, false.B)
  val StateBackOff = RegInit(false.B)
  val StateDefer = RegInit(true.B)
  val Rule1 = RegInit(false.B)

  val ColWindow = RegInit(true.B)
  val DlyCrcCnt = RegInit(0.U(3.W)) // Delayed CRC counter
  val PacketFinished_q = RegInit(false.B)
  val ByteCnt = RegInit(0.U(16.W)) // Transmit Byte Counter
  val NibCnt = RegInit(0.U(16.W)) // Nibble Counter
  val NibCntEq7  = NibCnt === 7.U
  val NibCntEq15 = NibCnt === 15.U

  val RetryCnt = RegInit(0.U(4.W)); io.RetryCnt := RetryCnt
}



trait MacTxFSM { this: MacTxBase =>

  // Defining the next state
  StartIPG := StateDefer & ~ExcessiveDefer & ~io.CarrierSense

  val StartIdle = StateIPG & (Rule1 & NibCnt(6,0) >= io.IPGT | ~Rule1 & NibCnt(6,0) >= io.IPGR2)

  StartPreamble := StateIdle & io.TxStartFrm & ~io.CarrierSense

  StartData(0) := ~io.Collision & (StatePreamble & NibCntEq15 | StateData(1) & ~io.TxEndFrm)
  StartData(1) := ~io.Collision & StateData(0) & ~MaxFrame

  val StartPAD = ~io.Collision & StateData(1) & io.TxEndFrm & io.Pad & ~NibbleMinFl

  StartFCS :=
    (~io.Collision & StateData(1) & io.TxEndFrm & (~io.Pad | io.Pad & NibbleMinFl) & io.CrcEn) |
    (~io.Collision & StatePAD & NibbleMinFl & io.CrcEn)

  StartJam := (io.Collision ) & ((StatePreamble & NibCntEq15) | (StateData(1) | StateData(0)) | StatePAD | StateFCS)

  StartBackoff := StateJam & ~RandomEq0 & ColWindow & ~RetryMax & NibCntEq7 & ~io.NoBckof

  StartDefer :=
    (StateIPG & ~Rule1 & io.CarrierSense & NibCnt(6,0) <= io.IPGR1 & NibCnt(6,0) =/= io.IPGR2) |
    (StateIdle & io.CarrierSense) |
    (StateJam & NibCntEq7 & (io.NoBckof | RandomEq0 | ~ColWindow | RetryMax)) |
    (StateBackOff & (RandomEqByteCnt)) |
    io.StartTxDone |
    TooBig

  io.DeferIndication := StateIdle & io.CarrierSense



  when(StartDefer | StartIdle){
    StateIPG := false.B
  }.elsewhen(StartIPG){
    StateIPG := true.B
  }

  when(StartDefer | StartPreamble){
    StateIdle := false.B
  }.elsewhen(StartIdle){
    StateIdle := true.B
  }

  when(StartData(0) | StartJam){
    StatePreamble := false.B
  } .elsewhen(StartPreamble){
    StatePreamble := true.B
  }

  when(StartFCS | StartJam){
    StatePAD := false.B
  }.elsewhen(StartPAD){
    StatePAD := true.B
  }

  when(StartJam | StartDefer){
    StateFCS := false.B
  } .elsewhen(StartFCS){
    StateFCS := true.B
  }

  when(StartBackoff | StartDefer){
    StateJam := false.B
  } .elsewhen(StartJam){
    StateJam := true.B
  }

  when(StartDefer){
    StateBackOff := false.B
  } .elsewhen(StartBackoff){
    StateBackOff := true.B
  }

  when(StartIPG){
    StateDefer := false.B
  } .elsewhen(StartDefer){
    StateDefer := true.B
  }

  // This sections defines which interpack gap rule to use
  when(StateIdle | StateBackOff){
    Rule1 := false.B
  } .elsewhen(StatePreamble | io.FullD){
    Rule1 := true.B
  }


}

trait MacTxCounter { this: MacTxBase =>

  val ByteCntMax = Wire(Bool())

  val IncrementNibCnt =
    StateIPG | StatePreamble |
    (StateData(1) | StateData(0)) |
    StatePAD | StateFCS | StateJam | StateBackOff |
    (StateDefer & ~ExcessiveDefer & io.TxStartFrm)


  val ResetNibCnt =
    (StateDefer & ExcessiveDefer & ~io.TxStartFrm) |
    (StatePreamble & NibCntEq15) |
    (StateJam & NibCntEq7) |
    StateIdle | StartDefer | StartIPG | StartFCS | StartJam


  when(ResetNibCnt){
    NibCnt := 0.U
  } .elsewhen(IncrementNibCnt){
    NibCnt := NibCnt + 1.U
  }

  NibbleMinFl := NibCnt >= (((io.MinFL-4.U)<<1) - 1.U)  // FCS should not be included in NibbleMinFl

  ExcessiveDefer := NibCnt(13,0) === "h17b7".U & ~io.ExDfrEn;   // 6071 nibbles

  val IncrementByteCnt =
    (StateData(1) & ~ByteCntMax) |
    (StateBackOff & (NibCnt(6,0) === 127.U)) |
    ((StatePAD | StateFCS) & NibCnt.extract(0) & ~ByteCntMax)

  val ResetByteCnt =
    StartBackoff | 
    (StateIdle & io.TxStartFrm) |
    PacketFinished_q


  ByteCntMax := ByteCnt.andR
  when(ResetByteCnt){
    ByteCnt := 0.U
  } .elsewhen(IncrementByteCnt){
    ByteCnt := ByteCnt + 1.U
  }

  MaxFrame := (ByteCnt === io.MaxFL) & ~io.HugEn;
       
  when((StateData(1) & DlyCrcCnt === 4.U) | StartJam | PacketFinished_q){
    DlyCrcCnt := 0.U
  }.elsewhen(io.DlyCrcEn & (StateSFD | StateData(1) & (DlyCrcCnt.orR))){
    DlyCrcCnt := DlyCrcCnt + 1.U
  }

}



trait MacTxCRC{ this: MacTxBase =>

  val Initialize_Crc = StateIdle | StatePreamble | (DlyCrcCnt.orR)
  val Enable_Crc = ~StateFCS

  val Data_Crc = 
    Mux1H(Seq(
      StateData(0) -> Cat( io.TxData(0), io.TxData(1), io.TxData(2), io.TxData(3) ),
      StateData(1) -> Cat( io.TxData(4), io.TxData(5), io.TxData(6), io.TxData(7) ),
    ))

  val Crc = RegInit("hFFFFFFFF".U(32.W))
  
  when( Initialize_Crc ){
    Crc := "hFFFFFFFF".U
  } .otherwise{
    Crc := Cat(
      Crc.extract(27),
      Crc.extract(26),
      (Enable_Crc & (Data_Crc.extract(3) ^ Crc.extract(31))) ^ Crc.extract(25),
      (Enable_Crc & (Data_Crc.extract(2) ^ Crc.extract(30))) ^ Crc.extract(24),
      (Enable_Crc & (Data_Crc.extract(1) ^ Crc.extract(29))) ^ Crc.extract(23),
      (Enable_Crc & (Data_Crc.extract(3) ^ Data_Crc.extract(0) ^ Crc.extract(31) ^ Crc.extract(28))) ^ Crc.extract(22),
      (Enable_Crc & (Data_Crc.extract(3) ^ Data_Crc.extract(2) ^ Crc.extract(31) ^ Crc.extract(30))) ^ Crc.extract(21),
      (Enable_Crc & (Data_Crc.extract(2) ^ Data_Crc.extract(1) ^ Crc.extract(30) ^ Crc.extract(29))) ^ Crc.extract(20),
      (Enable_Crc & (Data_Crc.extract(1) ^ Data_Crc.extract(0) ^ Crc.extract(29) ^ Crc.extract(28))) ^ Crc.extract(19),
      (Enable_Crc & (Data_Crc.extract(0) ^ Crc.extract(28))) ^ Crc.extract(18),
      Crc.extract(17),
      Crc.extract(16),
      (Enable_Crc & (Data_Crc.extract(3) ^ Crc.extract(31))) ^ Crc.extract(15),
      (Enable_Crc & (Data_Crc.extract(2) ^ Crc.extract(30))) ^ Crc.extract(14),
      (Enable_Crc & (Data_Crc.extract(1) ^ Crc.extract(29))) ^ Crc.extract(13),
      (Enable_Crc & (Data_Crc.extract(0) ^ Crc.extract(28))) ^ Crc.extract(12),
      (Enable_Crc & (Data_Crc.extract(3) ^ Crc.extract(31))) ^ Crc.extract(11),
      (Enable_Crc & (Data_Crc.extract(3) ^ Data_Crc.extract(2) ^ Crc.extract(30) ^ Crc.extract(31))) ^ Crc.extract(10),
      (Enable_Crc & (Data_Crc.extract(3) ^ Data_Crc.extract(2) ^ Data_Crc.extract(1) ^ Crc.extract(29) ^ Crc.extract(30) ^ Crc.extract(31))) ^ Crc.extract(9),
      (Enable_Crc & (Data_Crc.extract(2) ^ Data_Crc.extract(1) ^ Data_Crc.extract(0) ^ Crc.extract(28) ^ Crc.extract(29) ^ Crc.extract(30))) ^ Crc.extract(8),
      (Enable_Crc & (Data_Crc.extract(3) ^ Data_Crc.extract(1) ^ Data_Crc.extract(0) ^ Crc.extract(28) ^ Crc.extract(29) ^ Crc.extract(31))) ^ Crc.extract(7),
      (Enable_Crc & (Data_Crc.extract(3) ^ Data_Crc.extract(2) ^ Data_Crc.extract(0) ^ Crc.extract(28) ^ Crc.extract(30) ^ Crc.extract(31))) ^ Crc.extract(6),
      (Enable_Crc & (Data_Crc.extract(2) ^ Data_Crc.extract(1) ^ Crc.extract(29)     ^ Crc.extract(30))) ^ Crc.extract(5),
      (Enable_Crc & (Data_Crc.extract(3) ^ Data_Crc.extract(1) ^ Data_Crc.extract(0) ^ Crc.extract(28) ^ Crc.extract(29) ^ Crc.extract(31))) ^ Crc.extract(4),
      (Enable_Crc & (Data_Crc.extract(3) ^ Data_Crc.extract(2) ^ Data_Crc.extract(0) ^ Crc.extract(28) ^ Crc.extract(30) ^ Crc.extract(31))) ^ Crc.extract(3),
      (Enable_Crc & (Data_Crc.extract(2) ^ Data_Crc.extract(1) ^ Crc.extract(29) ^ Crc.extract(30))) ^ Crc.extract( 2),
      (Enable_Crc & (Data_Crc.extract(3) ^ Data_Crc.extract(1) ^ Data_Crc.extract(0) ^ Crc.extract(28) ^ Crc.extract(29) ^ Crc.extract(31))) ^ Crc.extract(1),
      (Enable_Crc & (Data_Crc.extract(3) ^ Data_Crc.extract(2) ^ Data_Crc.extract(0) ^ Crc.extract(28) ^ Crc.extract(30) ^ Crc.extract(31))) ^ Crc.extract(0),
       Enable_Crc & (Data_Crc.extract(3) ^ Data_Crc.extract(2) ^ Data_Crc.extract(1) ^ Crc.extract(29) ^ Crc.extract(30) ^ Crc.extract(31)),
       Enable_Crc & (Data_Crc.extract(2) ^ Data_Crc.extract(1) ^ Data_Crc.extract(0) ^ Crc.extract(28) ^ Crc.extract(29) ^ Crc.extract(30)),
       Enable_Crc & (Data_Crc.extract(1) ^ Data_Crc.extract(0) ^ Crc.extract(28) ^ Crc.extract(29)),
       Enable_Crc & (Data_Crc.extract(0) ^ Crc.extract(28))
    )
  }

  CrcError := Crc =/= "hc704dd7b".U  // CRC not equal to magic number


}

trait MacTxRandom{ this: MacTxBase =>

  val x = RegInit(0.U(10.W))
  when(true.B){
    x := Cat( x(8,0), ~(x.extract(2) ^ x.extract(9)) )
  }

  val RandomLatched = RegEnable(
    Cat(
      Mux( RetryCnt > 9.U, x.extract(9), 0.U(1.W)),
      Mux( RetryCnt > 8.U, x.extract(8), 0.U(1.W)),
      Mux( RetryCnt > 7.U, x.extract(7), 0.U(1.W)),
      Mux( RetryCnt > 6.U, x.extract(6), 0.U(1.W)),
      Mux( RetryCnt > 5.U, x.extract(5), 0.U(1.W)),
      Mux( RetryCnt > 4.U, x.extract(4), 0.U(1.W)),
      Mux( RetryCnt > 3.U, x.extract(3), 0.U(1.W)),
      Mux( RetryCnt > 2.U, x.extract(2), 0.U(1.W)),
      Mux( RetryCnt > 1.U, x.extract(1), 0.U(1.W)),
      x.extract(0)
    ),
    0.U(10.W),
    StateJam & StateJam_q
  )

  // Random Number == 0      IEEE 802.3 page 68. If 0 we go to defer and not to backoff.
  RandomEq0 := RandomLatched === 0.U
  RandomEqByteCnt := ByteCnt(9,0) === RandomLatched & (NibCnt(6,0).andR)

}

class MacTx extends MacTxBase with MacTxFSM with MacTxCounter with MacTxCRC with MacTxRandom{
  val MTxD = RegInit(0.U(4.W)); io.MTxD := MTxD
  val StopExcessiveDeferOccured = RegInit(false.B)

  val StatusLatch = RegInit(false.B)
  val TxUsedData  = RegNext(StartData(0) | StartData(1), false.B); io.TxUsedData := TxUsedData
  val TxDone = RegInit(false.B); io.TxDone := TxDone
  val TxRetry = RegInit(false.B); io.TxRetry := TxRetry
  val TxAbort = RegInit(false.B); io.TxAbort := TxAbort

  val MTxEn = RegNext(StatePreamble | (StateData(0) | StateData(1)) | StatePAD | StateFCS | StateJam, false.B); io.MTxEn := MTxEn
  val MTxErr = RegNext( TooBig, false.B); io.MTxErr := MTxErr// Transmit error

  val WillTransmit = RegNext(StartPreamble | StatePreamble | (StateData(0) | StateData(1)) | StatePAD | StateFCS | StateJam, false.B); io.WillTransmit := WillTransmit// WillTransmit



  io.ResetCollision := ~(StatePreamble | (StateData(0) | StateData(1)) | StatePAD | StateFCS)
  val ExcessiveDeferOccured = io.TxStartFrm & StateDefer & ExcessiveDefer & ~StopExcessiveDeferOccured
  io.StartTxDone := ~io.Collision & (StateFCS & NibCntEq7 | StateData(1) & io.TxEndFrm & (~io.Pad | io.Pad & NibbleMinFl) & ~io.CrcEn)
  TooBig := ~io.Collision & MaxFrame & (StateData(0) | StateFCS);
  val StartTxRetry = StartJam & (ColWindow & ~RetryMax)
  io.LateCollision := StartJam & ~ColWindow
  io.MaxCollisionOccured := StartJam & ColWindow & RetryMax;
  StateSFD := StatePreamble & NibCntEq15;
  io.StartTxAbort := TooBig | ExcessiveDeferOccured | io.LateCollision | io.MaxCollisionOccured

  when(~io.TxStartFrm){
    StopExcessiveDeferOccured := false.B
  }.elsewhen(ExcessiveDeferOccured){
    StopExcessiveDeferOccured := true.B    
  }


  // Collision Window
  when(~io.Collision & ByteCnt(5,0) === io.CollValid & (StateData(1) | StatePAD & NibCnt.extract(0) | StateFCS & NibCnt.extract(0))){
    ColWindow := false.B
  } .elsewhen(StateIdle | StateIPG){
    ColWindow := true.B
  }




  // Start Window
  when(~io.TxStartFrm){
    StatusLatch := false.B
  } .elsewhen(ExcessiveDeferOccured | StateIdle){
    StatusLatch := true.B
  }


  // Transmit packet done
  when(io.TxStartFrm & ~StatusLatch){
    TxDone := false.B
  }.elsewhen(io.StartTxDone){
    TxDone := true.B
  }

  // Transmit packet retry
  when(io.TxStartFrm & ~StatusLatch){
    TxRetry := false.B
  } .elsewhen(StartTxRetry){
    TxRetry := true.B
  }


  // Transmit packet abort
  when(io.TxStartFrm & ~StatusLatch & ~ExcessiveDeferOccured){
    TxAbort := false.B
  } .elsewhen(io.StartTxAbort){
    TxAbort := true.B
  }

  // Retry counter
  when(ExcessiveDeferOccured | TooBig | io.StartTxDone
      | StateJam & NibCntEq7 & (~ColWindow | RetryMax)){
    RetryCnt := 0.U
  }.elsewhen(StateJam & NibCntEq7 & ColWindow & (RandomEq0 | io.NoBckof) | StateBackOff & RandomEqByteCnt){
    RetryCnt := RetryCnt + 1.U
  }

  RetryMax := RetryCnt === io.MaxRet

  when( true.B ){
    MTxD := // Transmit nibble
      Mux(
        StateData(0), io.TxData(3,0),     // Lower nibble
        Mux(StateData(1), io.TxData(7,4), // Higher nibble
          Mux( StateFCS, Cat(~Crc.extract(28), ~Crc.extract(29), ~Crc.extract(30), ~Crc.extract(31)), // Crc
            Mux(StateJam, 9.U, // Jam pattern
              Mux(
                StatePreamble, Mux(NibCntEq15, "hd".U, 5.U),//  SFD,Preamble
                0.U
              )
            )
          )
        )
      )
  }

  val PacketFinished_d = io.StartTxDone | TooBig | io.LateCollision | io.MaxCollisionOccured | ExcessiveDeferOccured;
  val PacketFinished = RegNext(PacketFinished_d, false.B)
  when(true.B){
    PacketFinished_q := PacketFinished
  }

}






























