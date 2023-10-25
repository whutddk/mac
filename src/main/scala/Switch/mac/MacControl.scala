package MAC

import chisel3._
import chisel3.util._

class MacControlIO extends Bundle{
val MTxClk                     = Input(Bool())             // Transmit clock (from PHY)
val MRxClk                     = Input(Bool())             // Receive clock (from PHY)

val asyncReset = Input(AsyncReset())

val TxDataIn                   = Input(UInt(8.W))          // Transmit packet data byte (from host)
val TxStartFrmIn               = Input(Bool())             // Transmit packet start frame input (from host)
val TxUsedDataIn               = Input(Bool())             // Transmit packet used data (from TxEthMAC)
val TxEndFrmIn                 = Input(Bool())             // Transmit packet end frame input (from host)
val TxDoneIn                   = Input(Bool())             // Transmit packet done (from TxEthMAC)
val TxAbortIn                  = Input(Bool())             // Transmit packet abort (input from TxEthMAC)
val PadIn                      = Input(Bool())             // Padding (input from registers)
val CrcEnIn                    = Input(Bool())             // Crc append (input from registers)


val TxDataOut                  = Output(UInt(8.W))         // Transmit Packet Data (to TxEthMAC)
val TxStartFrmOut              = Output(Bool())            // Transmit packet start frame (output to TxEthMAC)
val TxEndFrmOut                = Output(Bool())            // Transmit packet end frame (output to TxEthMAC)
val TxDoneOut                  = Output(Bool())            // Transmit packet done (to host)
val TxAbortOut                 = Output(Bool())            // Transmit packet aborted (to host)
val TxUsedDataOut              = Output(Bool())            // Transmit packet used data (to host)
val PadOut                     = Output(Bool())            // Padding (output to TxEthMAC)
val CrcEnOut                   = Output(Bool())            // Crc append (output to TxEthMAC)

}


class MacControl extends RawModule{
  val io: MacControlIO = IO(new MacControlIO)

   

    io.TxDoneOut := ((~io.TxStartFrmIn)  & io.TxDoneIn)     // TxDoneOut
    io.TxAbortOut := ((~io.TxStartFrmIn) & io.TxAbortIn)  // TxAbortOut
    io.TxUsedDataOut := io.TxUsedDataIn  // TxUsedDataOut
    io.TxStartFrmOut := io.TxStartFrmIn  // TxStartFrmOut

    io.TxEndFrmOut := io.TxEndFrmIn  // TxEndFrmOut
    io.TxDataOut := io.TxDataIn // TxDataOut[7:0]
    io.PadOut := io.PadIn
    io.CrcEnOut := io.CrcEnIn





}



