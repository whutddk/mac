package MAC

import chisel3._
import chisel3.util._



class MIIMIO extends Bundle with MDIO{
  val Divider  = Input( UInt(8.W) )        // Divider for the host clock // Divider (input clock will be divided by the Divider[7:0])
  val NoPre = Input(Bool())                // No Preamble (no 32-bit preamble)

  val WCtrlData = Input(Bool())            // Write Control Data operation
  val CtrlData = Input( UInt(16.W) )       // Control Data (to be written to the PHY reg.)

  val Fiad = Input( UInt(5.W) )            // PHY Address
  val Rgad = Input(UInt(5.W))              // Register Address (within the PHY)

  val RStat = Input( Bool() )              // Read Status operation

  val ScanStat = Input( Bool() )           // Scan Status operation

  val Busy = Output(Bool())                // Busy Signal
  val LinkFail = Output(Bool())            // Link Integrity Signal
  val Nvalid = Output(Bool())              // Invalid Status (qualifier for the valid scan result)
  val Prsd = Output(UInt(16.W))            // Read Status Data (data read from the PHY)
  
  val WCtrlDataStart = Output(Bool())      // This signals resets the WCTRLDATA bit in the MIIM Command register
  val RStatStart = Output(Bool())          // This signal resets the RSTAT BIT in the MIIM Command register
  val UpdateMIIRX_DATAReg = Output(Bool()) // Updates MII RX_DATA register with read data

}

class MIIMBase extends Module{
  val io: MIIMIO = IO(new MIIMIO)


  val ByteSelect = Wire( Vec( 4, Bool() ) )         // Byte Select defines which byte (preamble, data, operation, etc.) is loaded and shifted through the shift register.


  // Counter counts half period
  val Counter  = RegInit( 1.U(8.W) )
  val mdc = RegInit(false.B)      // Output clock
  val mdcEn    = (Counter === 0.U) & ~mdc  // Enable signal is asserted for one Clk period before mdc rises.
  val mdcEn_n  = (Counter === 0.U) &  mdc  // Enable signal is asserted for one Clk period before mdc falls.

  val ShiftReg = RegInit(0.U(8.W)) // Shift register for shifting the data in and out
  val Prsd     = RegInit(0.U(16.W))
  val LinkFail = RegInit(false.B)

  
  val BitCounter = RegInit( 0.U(7.W) ) // Bit Counter counts from 0 to 63 (from 32 to 63 when NoPre is asserted)
  val EndOp = BitCounter === 63.U     // Operation ends when the Bit Counter reaches 63    


  val InProgress = RegInit(false.B)                                        // Operation in progress
  val InProgress_q     = ShiftRegisters(InProgress,     3, false.B, mdcEn) // Operation in progress delayed 3 mdc cycles
  val EndBusy =  ShiftRegister(~InProgress_q(1) & InProgress_q(2), 2, false.B, true.B)    // Generation of the EndBusy signal. It is used for ending the MII Management operation. 

  val WriteOp    = RegInit(false.B) // Write Operation Latch (When asserted, write operation is in progress)




  io.mdc := mdc
  io.LinkFail := LinkFail
  io.Prsd := Prsd


 


}

/** Connecting the Clock Generator Module */
trait MIIMClockGen{ this: MIIMBase =>

  val TempDivider   = Mux( io.Divider < 2.U, 2.U, io.Divider ) // If smaller than 2
  val CounterPreset = ( TempDivider >> 1 ) - 1.U               // We are counting half of period

  when( Counter === 0.U ) {
    mdc := ~mdc              // mdc is asserted every other half period
    Counter := CounterPreset
  } .otherwise{
    Counter := Counter - 1.U
  }

}


trait MIIMShiftReg{ this: MIIMBase =>

  val LatchByte0 = ShiftRegister(InProgress & ~WriteOp & BitCounter === "h3F".U, 2, false.B, mdcEn)   // Latch Byte selects which part of Read Status Data is updated from the shift register
  val LatchByte1 = ShiftRegister(InProgress & ~WriteOp & BitCounter === "h37".U, 2, false.B, mdcEn)   // Latch Byte selects which part of Read Status Data is updated from the shift register

  ByteSelect(0) := InProgress & ((io.NoPre & (BitCounter === 0.U)) | (~io.NoPre & (BitCounter === "h20".U)));
  ByteSelect(1) := InProgress & (BitCounter === "h28".U);
  ByteSelect(2) := InProgress & WriteOp & (BitCounter === "h30".U);
  ByteSelect(3) := InProgress & WriteOp & (BitCounter === "h38".U);

  when(mdcEn_n){
    when(ByteSelect.reduce(_|_)) {
      ShiftReg := Mux1H(Seq(
        ByteSelect(0) -> Cat("b01".U(2.W), ~WriteOp, WriteOp, io.Fiad(4,1)),
        ByteSelect(1) -> Cat(io.Fiad.extract(0), io.Rgad(4,0), "b10".U(2.W)),
        ByteSelect(2) -> io.CtrlData(15,8),
        ByteSelect(3) -> io.CtrlData( 7,0),
      ))    
    } .otherwise{
      ShiftReg := Cat(ShiftReg(6,0), io.mdi)
      when(LatchByte0){
        Prsd := Cat(Prsd(15,8), ShiftReg(6,0), io.mdi)
        when(io.Rgad === 1.U){
          LinkFail := ~ShiftReg.extract(1)  // this is bit [2], because it is not shifted yet                
        }
      } .elsewhen(LatchByte1){
        Prsd := Cat(ShiftReg(6,0), io.mdi, Prsd(7,0))
      }
    }
  }

}

trait MIIMOutputCtl{ this: MIIMBase =>

  // Generation of the Serial Enable signal (enables the serialization of the data)
  val SerialEn =  ( WriteOp & InProgress & ( BitCounter > 31.U | ( ( BitCounter === 0.U ) & io.NoPre ) )) |
                  (~WriteOp & InProgress & (( BitCounter > 31.U & BitCounter < 46.U ) | ( ( BitCounter === 0.U ) & io.NoPre )))

  val mdoEn  = ShiftRegister( SerialEn | (InProgress & BitCounter<32.U), 3, false.B, mdcEn_n)
  val mdo_2d = RegEnable( ~SerialEn & BitCounter<32.U, false.B, mdcEn_n)
  val mdo_d  = RegEnable( ShiftReg.extract(7) | mdo_2d,       false.B, mdcEn_n)
  val mdo    = RegEnable( mdo_d,                     false.B, mdcEn_n)
  io.mdo := mdo
  io.mdoEn := mdoEn

}


class MIIM extends MIIMBase with MIIMClockGen with MIIMShiftReg with MIIMOutputCtl{

  val WCtrlData_q = ShiftRegisters(io.WCtrlData, 3, false.B, true.B)
  val WCtrlDataStart = RegInit(false.B)                                    // Start Write Control Data Command (positive edge detected)
  val WCtrlDataStart_q = ShiftRegisters(WCtrlDataStart, 2, false.B, mdcEn) // Start Write Control Data Command delayed 2 mdc cycle
  val WriteDataOp  = WCtrlDataStart_q(0) & ~WCtrlDataStart_q(1)            // Write Data Operation (positive edge detected) 
  io.WCtrlDataStart := WCtrlDataStart

  // Generation of the Operation signals


  
  val StartOp = Wire(Bool()) // Start Operation (start of any of the preceding operations)
                        


  when( EndBusy ){
    WCtrlDataStart := false.B
  } .elsewhen( WCtrlData_q(1) & ~WCtrlData_q(2) ){
    WCtrlDataStart := true.B
  }

  // Update MII RX_DATA register
  val WCtrlDataStart_q0 = RegEnable(WCtrlDataStart, false.B, ~EndBusy)
  val UpdateMIIRX_DATAReg = RegNext(EndBusy & ~WCtrlDataStart_q0, false.B) // Updates MII RX_DATA register with read data
  io.UpdateMIIRX_DATAReg := UpdateMIIRX_DATAReg



  val RStat_q    = ShiftRegisters(io.RStat,     3, false.B, true.B)
  val RStatStart = RegInit(false.B)                                    // Start Read Status Command (positive edge detected)
  val RStatStart_q = ShiftRegisters(RStatStart,     2, false.B, mdcEn) // Start Read Status Command delayed 2 mdc cycles
  val ReadStatusOp = RStatStart_q(0)     & ~RStatStart_q(1)            // Read Status Operation (positive edge detected)
  io.RStatStart := RStatStart

  when( EndBusy ){
    RStatStart := false.B
  } .elsewhen(RStat_q(1) & ~RStat_q(2)){
    RStatStart := true.B
  }




  when(mdcEn){
    when(StartOp) {
      InProgress := true.B
    } .elsewhen(EndOp) {
      InProgress := false.B
    }
  }



  val ScanStat_q  = ShiftRegisters(io.ScanStat,  2, false.B, true.B)
  val SyncStatmdcEn = RegEnable(ScanStat_q(1), false.B, mdcEn) // Scan Status operation delayed at least cycles and synchronized to mdcEn
  val ScanStatusOp = SyncStatmdcEn       & ~InProgress & ~InProgress_q(0) & ~InProgress_q(1) // Scan Status Operation (positive edge detected)


  val Nvalid = RegInit(false.B) // Generation of the Nvalid signal (indicates when the status is invalid)
  io.Nvalid := Nvalid

  when( ~InProgress_q(1) & InProgress_q(2) ) {
    Nvalid := false.B
  } .elsewhen(ScanStat_q(1)  & ~SyncStatmdcEn) {
    Nvalid := true.B
  }




  when(mdcEn){
    when(StartOp) {
      when( ~InProgress ){
        WriteOp := Mux( WriteDataOp, true.B, false.B )
      }
    } .elsewhen(EndOp) {
      WriteOp := false.B
    }
  }


  when( mdcEn ){
    when( InProgress ) {
      when( io.NoPre & BitCounter === 0.U ) {
        BitCounter := "h21".U
      } .otherwise {
        BitCounter := BitCounter + 1.U
      }
    } .otherwise {
      BitCounter := 0.U
    }
  }


  StartOp := WriteDataOp | ReadStatusOp | ScanStatusOp
  io.Busy := io.WCtrlData | WCtrlDataStart | io.RStat | RStatStart | SyncStatmdcEn | EndBusy | InProgress | InProgress_q(2) | Nvalid

}


