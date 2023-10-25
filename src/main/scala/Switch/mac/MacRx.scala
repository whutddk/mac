package MAC

import chisel3._
import chisel3.util._



class MacRxIO extends Bundle{

  val MRxDV               = Input(Bool())
  val MRxD                = Input(UInt(4.W))
  val Transmitting        = Input(Bool())
  val DlyCrcEn            = Input(Bool())
  val MaxFL               = Input(UInt(16.W))
  val r_IFG               = Input(Bool())
  val MAC                 = Input(UInt(48.W))     //  Station Address  
  val r_HASH0             = Input(UInt(32.W)) //  lower 4 bytes Hash Table
  val r_HASH1             = Input(UInt(32.W)) //  upper 4 bytes Hash Table
  val PassAll             = Input(Bool())
  val ControlFrmAddressOK = Input(Bool())

  val RxData          = Output(UInt(8.W))
  val RxValid         = Output(Bool())
  val RxStartFrm      = Output(Bool())
  val RxEndFrm        = Output(Bool())
  val ByteCnt         = Output(UInt(16.W))
  val ByteCntEq0      = Output(Bool())
  val ByteCntGreat2   = Output(Bool())
  val CrcError        = Output(Bool())
  val StateIdle       = Output(Bool())
  val StatePreamble   = Output(Bool())
  val StateSFD        = Output(Bool())
  val StateData       = Output(UInt(2.W))
  val AddressMiss     = Output(Bool())
}


abstract class MacRxBase extends Module with RequireAsyncReset{
  val io: MacRxIO = IO(new MacRxIO)

  val MRxDEqD = io.MRxD === "hd".U
  val MRxDEq5 = io.MRxD === "h5".U

  val IFGCounterEq24 = Wire(Bool())

  val ByteCntEq0 = Wire(Bool())
  io.ByteCntEq0 := ByteCntEq0

  val ByteCntEq1 = Wire(Bool())
  val ByteCntEq2 = Wire(Bool())
  val ByteCntEq3 = Wire(Bool())
  val ByteCntEq4 = Wire(Bool())
  val ByteCntEq5 = Wire(Bool())
  val ByteCntEq6 = Wire(Bool())
  val ByteCntEq7 = Wire(Bool())

  val ByteCntSmall7 = Wire(Bool())

  val DlyCrcCnt  = RegInit(0.U(4.W))

  val StateData0    = RegInit(false.B)
  val StateData1    = RegInit(false.B)
  val StateIdle     = RegInit(false.B); io.StateIdle := StateIdle
  val StateDrop     = RegInit(true.B)
  val StatePreamble = RegInit(false.B); io.StatePreamble := StatePreamble
  val StateSFD      = RegInit(false.B); io.StateSFD := StateSFD

  val RxData_d = RegInit(0.U(8.W))
  val RxData = RegNext(RxData_d, 0.U); io.RxData := RxData

  val Crc = RegInit("hFFFFFFFF".U(32.W))



}

trait MacRxFSM { this: MacRxBase =>

  val StateData = Cat( StateData1, StateData0 )
  io.StateData := StateData

  // Defining the next state
  val StartIdle     = ~io.MRxDV & (StateDrop | StatePreamble | StateSFD |  StateData0 | StateData1 )
  val StartPreamble =  io.MRxDV & ~MRxDEq5 & (StateIdle & ~io.Transmitting)
  val StartSFD      =  io.MRxDV &  MRxDEq5 & (StateIdle & ~io.Transmitting | StatePreamble)
  val StartData0    =  io.MRxDV & (StateSFD & MRxDEqD & IFGCounterEq24 | StateData1)
  val StartData1    =  io.MRxDV & StateData0
  val StartDrop     =  io.MRxDV & (
    (StateIdle & io.Transmitting) | (StateSFD & ~IFGCounterEq24 & MRxDEqD )
  )


  when(StartPreamble | StartSFD | StartDrop){
    StateIdle := false.B
  } .elsewhen( StartIdle ){
    StateIdle := true.B
  }

  when( StartIdle ){
    StateDrop := false.B
  } .elsewhen(StartDrop){
    StateDrop := true.B
  }

  when(StartSFD | StartIdle | StartDrop ){
    StatePreamble := false.B
  } .elsewhen(StartPreamble){
    StatePreamble := true.B
  }

  when(StartPreamble | StartIdle | StartData0 | StartDrop){
    StateSFD := false.B
  } .elsewhen (StartSFD){
    StateSFD := true.B
  }


  when(StartIdle | StartData1 | StartDrop){
    StateData0 := false.B
  } .elsewhen(StartData0){
    StateData0 := true.B
  }


  when(StartIdle | StartData0 | StartDrop){
    StateData1 := false.B
  } .elsewhen(StartData1){
    StateData1 := true.B
  }

}

trait MacRxCounter { this: MacRxBase =>

  val ByteCnt    = RegInit(0.U(16.W))
  val IFGCounter = RegInit(0.U(5.W))

  val ByteCntMax = ByteCnt === "hffff".U

  val ResetByteCounter = io.MRxDV & (io.StateSFD & MRxDEqD)

  val IncrementByteCounter =
    ~ResetByteCounter & io.MRxDV & (
      io.StatePreamble | io.StateSFD | io.StateIdle & ~io.Transmitting |
      (io.StateData.extract(1) & ~ByteCntMax & ~(io.DlyCrcEn & DlyCrcCnt.orR))
    )

  val ByteCntDelayed = ByteCnt + 4.U
  io.ByteCnt := Mux(io.DlyCrcEn, ByteCntDelayed, ByteCnt)

  when( DlyCrcCnt === 9.U ){
    DlyCrcCnt := 0.U
  } .elsewhen(io.DlyCrcEn & io.StateSFD){
    DlyCrcCnt := 1.U
  } .elsewhen(io.DlyCrcEn & (DlyCrcCnt.orR)){
    DlyCrcCnt :=  DlyCrcCnt + 1.U
  }

  when( ResetByteCounter ){
    ByteCnt := 0.U
  } .elsewhen(IncrementByteCounter){
    ByteCnt := ByteCnt + 1.U
  }

  ByteCntEq0 := ByteCnt === 0.U
  ByteCntEq1 := ByteCnt === 1.U
  ByteCntEq2 := ByteCnt === 2.U 
  ByteCntEq3 := ByteCnt === 3.U 
  ByteCntEq4 := ByteCnt === 4.U 
  ByteCntEq5 := ByteCnt === 5.U 
  ByteCntEq6 := ByteCnt === 6.U
  ByteCntEq7 := ByteCnt === 7.U

  io.ByteCntGreat2   := ByteCnt > 2.U
  ByteCntSmall7      := ByteCnt < 7.U

  val ResetIFGCounter = io.StateSFD & io.MRxDV & MRxDEqD | StateDrop;
  val IncrementIFGCounter = ~ResetIFGCounter & (StateDrop | io.StateIdle | io.StatePreamble | io.StateSFD) & ~IFGCounterEq24;

  when( ResetIFGCounter ){
    IFGCounter := 0.U
  } .elsewhen(IncrementIFGCounter){
    IFGCounter := IFGCounter + 1.U
  }

  IFGCounterEq24 := (IFGCounter === "h18".U) | io.r_IFG; // 24*400 = 9600 ns or r_IFG is set to 1

}

trait MacRxFAddrCheck { this: MacRxBase =>

  val HashBit = Wire(UInt(1.W))

  val RxCheckEn   = io.StateData.orR

  val AddressMiss = RegInit(false.B) // This ff holds the "Address Miss" information that is written to the RX BD status. 

  io.AddressMiss := AddressMiss




  val CrcHash = RegInit(0.U(6.W))
  val CrcHashGood = RegNext(StateData0 & ByteCntEq6) // Latching CRC for use in the hash table

  when(ByteCntEq0){
    AddressMiss := false.B
  } .elsewhen(ByteCntEq7 & RxCheckEn){
    AddressMiss := (~((io.PassAll & io.ControlFrmAddressOK)));    
  }
   
  val IntHash = Mux(CrcHash.extract(5), io.r_HASH1, io.r_HASH0)
  val ByteHash = 
    Mux1H(Seq(
      (CrcHash(4,3) === "b00".U) -> IntHash(7 ,0),
      (CrcHash(4,3) === "b01".U) -> IntHash(15,8),
      (CrcHash(4,3) === "b10".U) -> IntHash(23,16),
      (CrcHash(4,3) === "b11".U) -> IntHash(31,24),
    ))

  HashBit := ByteHash >> CrcHash(2,0)

  when(StateIdle){
    CrcHash := 0.U
  } .elsewhen(StateData0 & ByteCntEq6){
    CrcHash := Crc(31,26)
  }

}

trait MacRxCRC { this: MacRxBase =>

  val Data_Crc   = Cat(io.MRxD.extract(0),io.MRxD.extract(1),io.MRxD.extract(2),io.MRxD.extract(3))
  val Enable_Crc = io.MRxDV & (io.StateData.orR)
  val Initialize_Crc = io.StateSFD | (io.DlyCrcEn & DlyCrcCnt > 0.U & DlyCrcCnt < 9.U)

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

  io.CrcError := Crc =/= "hc704dd7b".U  // CRC not equal to magic number


}

class MacRx extends MacRxBase with MacRxFSM with MacRxCounter with MacRxFAddrCheck with MacRxCRC{



  val GenerateRxValid = StateData0 & (~ByteCntEq0 | DlyCrcCnt >= 3.U);
  val DelayData   = RegNext(StateData0, false.B)
  val LatchedByte = RegInit(0.U(8.W))
  when(true.B) {
    LatchedByte := Cat(io.MRxD, LatchedByte(7,4))// Latched byte    
  }



  // Output byte stream
  when( GenerateRxValid ){
    RxData_d := LatchedByte & Fill(8, StateData0 | StateData1)        // Data goes through only in data state 
  } .elsewhen(~DelayData){
    RxData_d := 0.U        // Delaying data to be valid for two cycles.        // Zero when not active.
  }


  io.RxValid := ShiftRegister(GenerateRxValid, 2, false.B, true.B)


  val GenerateRxStartFrm = StateData0 & ( (ByteCntEq1 & ~io.DlyCrcEn) | ((DlyCrcCnt === 3.U) & io.DlyCrcEn) )

  io.RxStartFrm := ShiftRegister(GenerateRxStartFrm, 2, false.B, true.B)


  val GenerateRxEndFrm = StateData0 & (~io.MRxDV & io.ByteCntGreat2)
  val DribbleRxEndFrm  = StateData1 &  ~io.MRxDV & io.ByteCntGreat2

  val RxEndFrm_d = RegNext(GenerateRxEndFrm, false.B)
  io.RxEndFrm  := RegNext(RxEndFrm_d | DribbleRxEndFrm, false.B)

}
