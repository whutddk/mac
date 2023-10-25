package MAC

import chisel3._
import chisel3.util._

class MacControlIO extends Bundle{
val MTxClk                     = Input(Bool())             // Transmit clock (from PHY)
val MRxClk                     = Input(Bool())             // Receive clock (from PHY)

val asyncReset = Input(AsyncReset())

val TPauseRq                   = Input(Bool())             // Transmit control frame (from host)
val TxDataIn                   = Input(UInt(8.W))          // Transmit packet data byte (from host)
val TxStartFrmIn               = Input(Bool())             // Transmit packet start frame input (from host)
val TxUsedDataIn               = Input(Bool())             // Transmit packet used data (from TxEthMAC)
val TxEndFrmIn                 = Input(Bool())             // Transmit packet end frame input (from host)
val TxDoneIn                   = Input(Bool())             // Transmit packet done (from TxEthMAC)
val TxAbortIn                  = Input(Bool())             // Transmit packet abort (input from TxEthMAC)
val PadIn                      = Input(Bool())             // Padding (input from registers)
val CrcEnIn                    = Input(Bool())             // Crc append (input from registers)
val RxData                     = Input(UInt(8.W))          // Receive Packet Data (from RxEthMAC)
val RxValid                    = Input(Bool())             // Received a valid packet
val RxStartFrm                 = Input(Bool())             // Receive packet start frame (input from RxEthMAC)
val RxEndFrm                   = Input(Bool())             // Receive packet end frame (input from RxEthMAC)
val ReceiveEnd                 = Input(Bool())             // End of receiving of the current packet (input from RxEthMAC)
val ReceivedPacketGood         = Input(Bool())             // Received packet is good
val ReceivedLengthOK           = Input(Bool())             // Length of the received packet is OK
val TxFlow                     = Input(Bool())             // Tx flow control (from registers)
val RxFlow                     = Input(Bool())             // Rx flow control (from registers)
val DlyCrcEn                   = Input(Bool())             // Delayed CRC enabled (from registers)
val TxPauseTV                  = Input(UInt(16.W))         // Transmit Pause Timer Value (from registers)
val MAC                        = Input(UInt(48.W))         // MAC address (from registers)
val RxStatusWriteLatched_sync2 = Input(Bool())

val TxDataOut                  = Output(UInt(8.W))         // Transmit Packet Data (to TxEthMAC)
val TxStartFrmOut              = Output(Bool())            // Transmit packet start frame (output to TxEthMAC)
val TxEndFrmOut                = Output(Bool())            // Transmit packet end frame (output to TxEthMAC)
val TxDoneOut                  = Output(Bool())            // Transmit packet done (to host)
val TxAbortOut                 = Output(Bool())            // Transmit packet aborted (to host)
val TxUsedDataOut              = Output(Bool())            // Transmit packet used data (to host)
val PadOut                     = Output(Bool())            // Padding (output to TxEthMAC)
val CrcEnOut                   = Output(Bool())            // Crc append (output to TxEthMAC)
val WillSendControlFrame       = Output(Bool())
val TxCtrlEndFrm               = Output(Bool())
val ReceivedPauseFrm           = Output(Bool())
val ControlFrmAddressOK        = Output(Bool())
val SetPauseTimer              = Output(Bool())


}


class MacControl extends RawModule{
  val io: MacControlIO = IO(new MacControlIO)

  val Pause_wire = Wire(Bool())

  val SlotFinished = Wire(Bool())

  // Reserved multicast address and Type/Length for PAUSE control
  val ReservedMulticast = "h0180C2000001".U(48.W)
  val TypeLength        = "h8808".U(16.W)
         









  val DecrementPauseTimer = Wire(Bool())
  val PauseTimerEq0 = Wire(Bool())




  val ControlEnd   = Wire(Bool())

  val MuxedCtrlData = Wire(UInt(8.W))
  val EnableCnt = Wire(Bool())
















  withClockAndReset( io.MTxClk.asClock, io.asyncReset ) {
    val BlockTxDone = RegInit(false.B)
    val SendingCtrlFrm = RegInit(false.B)         // Sending Control Frame (enables padding and CRC)  
    val CtrlMux = RegInit(false.B)
    val ControlData = RegInit(0.U(8.W))
    val TxCtrlStartFrm = RegInit(false.B)

    val DlyCrcCnt = RegInit(0.U(4.W))
    val ByteCnt = RegInit(0.U(6.W))
    val ControlEnd_q = RegNext(ControlEnd)

    val TxCtrlStartFrm_q = RegNext(TxCtrlStartFrm)
    val TxCtrlEndFrm = RegNext(ControlEnd | ControlEnd_q, false.B); io.TxCtrlEndFrm := TxCtrlEndFrm     // Generation of the transmit control packet end frame
    val TxUsedDataIn_q = RegNext(io.TxUsedDataIn, false.B)

    val WillSendControlFrame = RegInit(false.B); io.WillSendControlFrame := WillSendControlFrame // A command for Sending the control frame is active (latched)
  
    val TxUsedDataOutDetected = RegInit(false.B)

    // Synchronization of the pause timer
    val PauseTimerEq0_sync1 = RegNext(PauseTimerEq0, true.B)
    val PauseTimerEq0_sync2 = RegNext(PauseTimerEq0_sync1, true.B)

    val Pause = RegInit(false.B); Pause_wire := Pause // Pause signal generation


    when((io.TxDoneIn | io.TxAbortIn | ~TxUsedDataOutDetected) & ~io.TxStartFrmOut){
      Pause := io.RxFlow & ~PauseTimerEq0_sync2
    }

    // Signal TxUsedDataOut was detected (a transfer is already in progress)
    when(io.TxDoneIn | io.TxAbortIn){
      TxUsedDataOutDetected := false.B
    } .elsewhen(io.TxUsedDataOut){
      TxUsedDataOutDetected := true.B
    }

    // Latching variables
    val TxAbortInLatched = RegNext(io.TxAbortIn, false.B)
    val TxDoneInLatched  = RegNext(io.TxDoneIn,  false.B)
    

    val MuxedAbort = RegInit(false.B) // Generating muxed abort signal

    when(io.TxStartFrmIn){
      MuxedAbort := false.B
    } .elsewhen(io.TxAbortIn & ~TxAbortInLatched & TxUsedDataOutDetected){
      MuxedAbort := true.B
    }

    val MuxedDone = RegInit(false.B) // Generating muxed done signal

    when(io.TxStartFrmIn){
      MuxedDone := false.B
    } .elsewhen(io.TxDoneIn & (~TxDoneInLatched) & TxUsedDataOutDetected){
      MuxedDone := true.B
    }


    when(TxCtrlEndFrm & CtrlMux){
      WillSendControlFrame := false.B
    } .elsewhen(io.TPauseRq & io.TxFlow){
      WillSendControlFrame := true.B
    }


    // Generation of the transmit control packet start frame
    when(TxUsedDataIn_q & CtrlMux){
      TxCtrlStartFrm := false.B
    } .elsewhen(WillSendControlFrame & ~io.TxUsedDataOut & (io.TxDoneIn | io.TxAbortIn | io.TxStartFrmIn | (~TxUsedDataOutDetected))){
      TxCtrlStartFrm := true.B
    }

    // Generation of the multiplexer signal (controls muxes for switching between
    // normal and control packets)
    when(WillSendControlFrame & ~io.TxUsedDataOut){
      CtrlMux := true.B
    } .elsewhen(io.TxDoneIn){
      CtrlMux := false.B
    }

    // Generation of the Sending Control Frame signal (enables padding and CRC)
    when(WillSendControlFrame & TxCtrlStartFrm){
      SendingCtrlFrm := true.B
    } .elsewhen(io.TxDoneIn){
      SendingCtrlFrm := false.B
    }

  // Generation of the signal that will block sending the Done signal to the eth_wishbone module
  // While sending the control frame
    when(TxCtrlStartFrm){
      BlockTxDone := true.B
    } .elsewhen(io.TxStartFrmIn){
      BlockTxDone := false.B
    }

    val IncrementDlyCrcCnt = CtrlMux & io.TxUsedDataIn & ~DlyCrcCnt.extract(2)
    val ResetByteCnt = io.asyncReset.asBool | (~TxCtrlStartFrm & (io.TxDoneIn | io.TxAbortIn))

    // Delayed CRC counter
    when(ResetByteCnt){
      DlyCrcCnt := 0.U
    } .elsewhen(IncrementDlyCrcCnt){
      DlyCrcCnt := DlyCrcCnt + 1.U
    }


    val IncrementByteCnt    = CtrlMux & (TxCtrlStartFrm & ~TxCtrlStartFrm_q & ~io.TxUsedDataIn | io.TxUsedDataIn & ~ControlEnd)
    val IncrementByteCntBy2 = CtrlMux & TxCtrlStartFrm & (~TxCtrlStartFrm_q) & io.TxUsedDataIn     // When TxUsedDataIn and CtrlMux are set at the same time

    EnableCnt := (~io.DlyCrcEn | io.DlyCrcEn & (DlyCrcCnt(1,0).andR))


    // Byte counter
    when(ResetByteCnt){
      ByteCnt := 0.U
    } .elsewhen(IncrementByteCntBy2 & EnableCnt){
      ByteCnt := ByteCnt + 2.U
    } .elsewhen(IncrementByteCnt & EnableCnt){
      ByteCnt := ByteCnt + 1.U
    }

    ControlEnd := ByteCnt === "h22".U

    MuxedCtrlData := // Control data generation (goes to the TxEthMAC module)
      Mux1H(Seq(
        (ByteCnt === 0.U)  -> Mux(~io.DlyCrcEn | io.DlyCrcEn & (DlyCrcCnt(1,0).andR), 1.U, 0.U),
        (ByteCnt === 2.U)  -> "h80".U,
        (ByteCnt === 4.U)  -> "hC2".U,
        (ByteCnt === 6.U)  -> "h00".U,
        (ByteCnt === 8.U)  -> "h00".U,
        (ByteCnt === 10.U) -> "h01".U,
        (ByteCnt === 12.U) -> io.MAC(47,40),
        (ByteCnt === 14.U) -> io.MAC(39,32),
        (ByteCnt === 16.U) -> io.MAC(31,24),
        (ByteCnt === 18.U) -> io.MAC(23,16),
        (ByteCnt === 20.U) -> io.MAC(15, 8),
        (ByteCnt === 22.U) -> io.MAC( 7, 0),
        (ByteCnt === 24.U) -> "h88".U,            // Type/Length
        (ByteCnt === 26.U) -> "h08".U,
        (ByteCnt === 28.U) -> "h00".U,            // Opcode
        (ByteCnt === 30.U) -> "h01".U,
        (ByteCnt === 32.U) -> io.TxPauseTV(15,8), // Pause timer value
        (ByteCnt === 34.U) -> io.TxPauseTV( 7,0),
      ))
    
    // Latched Control data
    when(~ByteCnt.extract(0)){
      ControlData := MuxedCtrlData
    }

    io.TxDoneOut := Mux(CtrlMux, ((~io.TxStartFrmIn) & (~BlockTxDone) & MuxedDone), ((~io.TxStartFrmIn) & (~BlockTxDone) & io.TxDoneIn))     // TxDoneOut
    io.TxAbortOut := Mux(CtrlMux, ((~io.TxStartFrmIn) & (~BlockTxDone) & MuxedAbort), ((~io.TxStartFrmIn) & (~BlockTxDone) & io.TxAbortIn))  // TxAbortOut
    io.TxUsedDataOut := ~CtrlMux & io.TxUsedDataIn  // TxUsedDataOut
    io.TxStartFrmOut := Mux(CtrlMux, TxCtrlStartFrm, (io.TxStartFrmIn & ~Pause)) // TxStartFrmOut
    io.TxEndFrmOut := Mux(CtrlMux, TxCtrlEndFrm, io.TxEndFrmIn)  // TxEndFrmOut
    io.TxDataOut := Mux(CtrlMux, ControlData, io.TxDataIn ) // TxDataOut[7:0]
    io.PadOut := io.PadIn | SendingCtrlFrm  // PadOut
    io.CrcEnOut := io.CrcEnIn | SendingCtrlFrm  // CrcEnOut
  }

  



















  withClockAndReset( io.MRxClk.asClock, io.asyncReset.asAsyncReset ) {
    val AddressOK = RegInit(false.B); io.ControlFrmAddressOK := AddressOK                // Multicast or unicast address detected 
    val TypeLengthOK = RegInit(false.B)             // Type/Length field contains 0x8808
    val DetectionWindow = RegInit(true.B)           // Detection of the PAUSE frame is possible within this window
    val OpCodeOK = RegInit(false.B)                 // PAUSE opcode detected (0x0001)
    val DlyCrcCnt = RegInit(0.U(3.W))
    val ByteCnt = RegInit(0.U(5.W))
    val AssembledTimerValue = RegInit(0.U(16.W))
    val LatchedTimerValue = RegInit(0.U(16.W))
    val ReceivedPauseFrmWAddr = RegInit(false.B)
    val PauseTimer = RegInit(0.U(16.W))

    val ByteCntEq0  = io.RxValid & ByteCnt ===  "h0".U
    val ByteCntEq1  = io.RxValid & ByteCnt ===  "h1".U
    val ByteCntEq2  = io.RxValid & ByteCnt ===  "h2".U
    val ByteCntEq3  = io.RxValid & ByteCnt ===  "h3".U
    val ByteCntEq4  = io.RxValid & ByteCnt ===  "h4".U
    val ByteCntEq5  = io.RxValid & ByteCnt ===  "h5".U
    val ByteCntEq12 = io.RxValid & ByteCnt === "h0C".U
    val ByteCntEq13 = io.RxValid & ByteCnt === "h0D".U
    val ByteCntEq14 = io.RxValid & ByteCnt === "h0E".U
    val ByteCntEq15 = io.RxValid & ByteCnt === "h0F".U
    val ByteCntEq16 = io.RxValid & ByteCnt === "h10".U
    val ByteCntEq17 = io.RxValid & ByteCnt === "h11".U
    val ByteCntEq18 = io.RxValid & ByteCnt === "h12".U & DetectionWindow

    // Address Detection (Multicast or unicast)
    when(DetectionWindow & ByteCntEq0){
      AddressOK :=  io.RxData === ReservedMulticast(47,40) | io.RxData === io.MAC(47,40)        
    } .elsewhen(DetectionWindow & ByteCntEq1){
      AddressOK := (io.RxData === ReservedMulticast(39,32) | io.RxData === io.MAC(39,32)) & AddressOK;        
    } .elsewhen(DetectionWindow & ByteCntEq2){
      AddressOK := (io.RxData === ReservedMulticast(31,24) | io.RxData === io.MAC(31,24)) & AddressOK;        
    } .elsewhen(DetectionWindow & ByteCntEq3){
      AddressOK := (io.RxData === ReservedMulticast(23,16) | io.RxData === io.MAC(23,16)) & AddressOK;        
    } .elsewhen(DetectionWindow & ByteCntEq4){
      AddressOK := (io.RxData === ReservedMulticast(15,8)  | io.RxData === io.MAC(15,8))  & AddressOK;        
    } .elsewhen(DetectionWindow & ByteCntEq5){
      AddressOK := (io.RxData === ReservedMulticast(7,0)   | io.RxData === io.MAC(7,0))   & AddressOK;        
    } .elsewhen(io.ReceiveEnd){
      AddressOK := false.B
    }

    // TypeLengthOK (Type/Length Control frame detected)
    when(DetectionWindow & ByteCntEq12){
      TypeLengthOK := ByteCntEq12 & (io.RxData === TypeLength(15,8));        
    } .elsewhen(DetectionWindow & ByteCntEq13){
      TypeLengthOK := ByteCntEq13 & (io.RxData === TypeLength(7,0)) & TypeLengthOK;        
    } .elsewhen(io.ReceiveEnd){
      TypeLengthOK := false.B
    }

    // Latch Control Frame Opcode

    when(ByteCntEq16){
      OpCodeOK := false.B
    } .otherwise{
      when(DetectionWindow & ByteCntEq14){
        OpCodeOK := ByteCntEq14 & io.RxData === 0.U        
      }
      when(DetectionWindow & ByteCntEq15){
        OpCodeOK := ByteCntEq15 & io.RxData === 1.U & OpCodeOK;        
      }
    }

    // ReceivedPauseFrmWAddr (+Address Check)
    when(io.ReceiveEnd){
      ReceivedPauseFrmWAddr := false.B
    } .elsewhen(ByteCntEq16 & TypeLengthOK & OpCodeOK & AddressOK){
      ReceivedPauseFrmWAddr := true.B      
    }

    // Assembling 16-bit timer value from two 8-bit data
    when(io.RxStartFrm){
      AssembledTimerValue := 0.U
    } .otherwise{
      when(DetectionWindow & ByteCntEq16){
        AssembledTimerValue := Cat(io.RxData, AssembledTimerValue(7,0)) 
      }
      when(DetectionWindow & ByteCntEq17){
        AssembledTimerValue := Cat(AssembledTimerValue(15,8), io.RxData )
      }
    }

    // Detection window (while PAUSE detection is possible)
    when(ByteCntEq18){
      DetectionWindow := false.B
    } .elsewhen(io.ReceiveEnd){
      DetectionWindow := true.B
    }

    // Latching Timer Value
    when(DetectionWindow & ReceivedPauseFrmWAddr & ByteCntEq18){
      LatchedTimerValue := AssembledTimerValue
    } .elsewhen(io.ReceiveEnd){
      LatchedTimerValue := 0.U    
    }

    // Delayed CEC counter
    when(io.RxValid & io.RxEndFrm){
      DlyCrcCnt := 0.U
    }.elsewhen(io.RxValid & ~io.RxEndFrm & ~DlyCrcCnt.extract(2)){
      DlyCrcCnt := DlyCrcCnt + 1.U
    }

    val IncrementByteCnt =
      io.RxValid & DetectionWindow & ~ByteCntEq18 & 
      (~io.DlyCrcEn | io.DlyCrcEn & DlyCrcCnt.extract(2))

    // Byte counter
    when(io.RxEndFrm){
      ByteCnt := 0.U    
    } .elsewhen(IncrementByteCnt){
      ByteCnt :=  ByteCnt + 1.U
    }


    when(io.SetPauseTimer){
      PauseTimer := LatchedTimerValue
    } .elsewhen(DecrementPauseTimer){
      PauseTimer := PauseTimer - 1.U
    }


    val Divider2 = RegInit(false.B) // Divider2 is used for incrementing the Slot timer every other clock

    when(PauseTimer.orR & io.RxFlow){
      Divider2 := ~Divider2
    } .otherwise{
      Divider2 := false.B
    }

    val SlotTimer = RegInit(0.U(6.W))  // SlotTimer

    when(io.asyncReset.asBool()){
      SlotTimer := 0.U
    } .elsewhen(Pause_wire & io.RxFlow & Divider2){
      SlotTimer :=  SlotTimer + 1.U
    }

    val ReceivedPauseFrm = RegInit(false.B); io.ReceivedPauseFrm := ReceivedPauseFrm // Pause Frame received

    when(io.RxStatusWriteLatched_sync2 ){
      ReceivedPauseFrm := false.B
    } .elsewhen(ByteCntEq16 & TypeLengthOK & OpCodeOK){
      ReceivedPauseFrm := true.B
    }

  io.SetPauseTimer :=
    io.ReceiveEnd & ReceivedPauseFrmWAddr & io.ReceivedPacketGood & io.ReceivedLengthOK & io.RxFlow

    DecrementPauseTimer := SlotFinished & PauseTimer.orR
    PauseTimerEq0       := ~PauseTimer.orR

    SlotFinished := SlotTimer.andR & Pause_wire & io.RxFlow & Divider2  // Slot is 512 bits (64 bytes)
  }         








}



