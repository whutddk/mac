package MAC

import chisel3._
import chisel3.util

class MDIO extends Bundle{
  val mdi   = Input( Bool()) // MII Management Data In
  val mdc   = Output(Bool()) // MII Management Data Clock
  val mdo   = Output(Bool()) // MII Management Data Output
  val mdoEn = Output(Bool()) // MII Management Data Output Enable
}

class MIIMIO extends MDIO{
  val CtrlData = Input( UInt(16.W) )       // Control Data (to be written to the PHY reg.)
  val Rgad = Input(UInt(5.W))              // Register Address (within the PHY)
  val Fiad = Input( UInt(5.W) )            // PHY Address
  val NoPre = Input(Bool())                // No Preamble (no 32-bit preamble)
  val WCtrlData = Input(Bool())            // Write Control Data operation
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
}

/** Connecting the Clock Generator Module */
trait MIIMClockGen{ this: MIIMBase =>
  val Divider = Wire( UInt(8.W) )            // Divider for the host clock // Divider (input clock will be divided by the Divider[7:0])


  val TempDivider   = Mux( Divider < 2.U, 2.U, Divider ) // If smaller than 2
  val CounterPreset = ( TempDivider >> 1 ) - 1.U        // We are counting half of period


  // Counter counts half period
  val Counter  = RegInit( 1.U(8.W) )
  val Mdc = RegInit(false.B)      // Output clock
  val CountEq0 = Counter === 0.U
  val MdcEn    = CountEq0 & ~Mdc; // Enable signal is asserted for one Clk period before Mdc rises.
  val MdcEn_n  = CountEq0 & Mdc;  // Enable signal is asserted for one Clk period before Mdc falls.

  when( CountEq0 ) {
    Counter := CounterPreset
  } .otherwise{
    Counter := Counter - 1.U
  }

  // Mdc is asserted every other half period
  when(CountEq0) {
    Mdc := ~Mdc
  }


}


trait MIIMShiftReg{ this: MIIMBase =>
  
  val ShiftReg = RegInit(0.U(8.W)) // Shift register for shifting the data in and out
  val Prsd     = RegInit(0.U(16.W))
  val LinkFail = RegInit(false.B)

  when(MdcEn_n){
    when(|ByteSelect) {
      /* verilator lint_off CASEINCOMPLETE */
      ShiftReg := Mux1H(Seq(
        ByteSelect === "h1".U -> Cat("b01".U(2.W), ~WriteOp, WriteOp, Fiad(4,1)),
        ByteSelect === "h2".U -> Cat(Fiad.extract(0), Rgad(4,0), "b01".U(2.W)),
        ByteSelect === "h4".U -> CtrlData(15,8),
        ByteSelect === "h8".U -> CtrlData( 7,0),
      ))    
    } .otherwise{
      ShiftReg := Cat(ShiftReg(6,0), Mdi)
      when(LatchByte.extract(0)){
        Prsd := Cat(Prsd(15,8), ShiftReg(6,0), Mdi)
        when(Rgad === 1.U){
          LinkFail := ~ShiftReg.extract(1)  // this is bit [2], because it is not shifted yet                
        }
      } .elsewhen(LatchByte.extract(1)){
          Prsd := Cat(ShiftReg(6:0), Mdi, Prsd(7,0))
      }
    }
  }

  val ShiftedBit = ShiftReg.extract(7) // This bit is output of the shift register and is connected to the Mdo signal

}

trait MIIMOutputCtl{ this: MIIMBase =>

// Generation of the Serial Enable signal (enables the serialization of the data)
val SerialEn =  WriteOp & InProgress &     ( BitCounter > 31.U | ( ( BitCounter === 0.U ) & NoPre ) )
                | ~WriteOp & InProgress & (( BitCounter > 31.U & BitCounter < 46.U ) | ( ( BitCounter === 0.U ) & NoPre ))

val MdoEn  = ShiftRegisters( SerialEn | InProgress & BitCounter<32.U, 3, false.B, en = MdcEn_n)
val Mdo_2d = RegEnable( ~SerialEn & BitCounter<32.U, false.B, MdcEn_n)
val Mdo_d  = RegEnable( ShiftedBit | Mdo_2d,       false.B, MdcEn_n)
val Mdo    = RegEnable( Mdo_d,                     false.B, MdcEn_n)






}


trait MIIM { this: MIIMBase =>


  // Generation of the EndBusy signal. It is used for ending the MII Management operation.
  val EndBusy_d = RegNext(false.B, ~InProgress_q2 & InProgress_q3)
  val EndBusy   = RegInit(false.B, EndBusy_d)


  // Update MII RX_DATA register
  val UpdateMIIRX_DATAReg = RegInit(false.B, EndBusy & ~WCtrlDataStart_q) // Updates MII RX_DATA register with read data




  // Generation of the delayed signals used for positive edge triggering.
  val WCtrlData_q = ShiftRegisters(WCtrlData, 3, false.B, en = true.B)
  val RStat_q     = ShiftRegisters(RStat,     3, false.B, en = true.B)
  val ScanStat_q  = ShiftRegisters(ScanStat,  2, false.B, en = true.B)
  val SyncStatMdcEn = RegEnable(ScanStat_q(1), false.B, enable = MdcEn) // Scan Status operation delayed at least cycles and synchronized to MdcEn

 



  // Generation of the Start Commands (Write Control Data or Read Status)
  val WCtrlDataStart = RegInit(false.B) // Start Write Control Data Command (positive edge detected)
  val WCtrlDataStart_q = RegEnable(WCtrlDataStart, false.B, enable= ~EndBusy)
  val RStatStart = RegInit(false.B) // Start Read Status Command (positive edge detected)

  when( EndBusy ){
    WCtrlDataStart := false.B
    RStatStart := false.B
  } .otherwise{
    when( WCtrlData_q(1) & ~WCtrlData_q(2) ){
      WCtrlDataStart := true.B
    }
    when(RStat_q(1) & ~RStat_q(2)){
      RStatStart := true.B
    }
  }



  // Generation of the Nvalid signal (indicates when the status is invalid)
  val Nvalid = RegInit(false.B)
  when( ~InProgress_q2 & InProgress_q3 ) {
    Nvalid := false.B
  } .elsewhen(ScanStat_q2  & ~SyncStatMdcEn) {
    Nvalid := true.B
  }


  // Signals used for the generation of the Operation signals (positive edge)

  val WCtrlDataStart_q = ShiftRegisters(WCtrlDataStart, 2, false.B, en: MdcEn) // Start Write Control Data Command delayed 2 Mdc cycle
  val RStatStart_q     = ShiftRegisters(RStatStart,     2, false.B, en: MdcEn) // Start Read Status Command delayed 2 Mdc cycles
  val InProgress_q     = ShiftRegisters(InProgress,     3, false.B, en: MdcEn) // Operation in progress delayed 3 Mdc cycles
  val LatchByte0       = ShiftRegisters(InProgress & ~WriteOp & BitCounter == "h3F".U, 2, false.B, MdcEn)   // Latch Byte selects which part of Read Status Data is updated from the shift register
  val LatchByte1       = ShiftRegisters(InProgress & ~WriteOp & BitCounter == "h37".U, 2, false.B, MdcEn)   // Latch Byte selects which part of Read Status Data is updated from the shift register
  val LatchByte        = Cat( LatchByte1, LatchByte0 ) // Latch Byte selects which part of Read Status Data is updated from the shift register

    

  // Generation of the Operation signals
  val WriteDataOp  = WCtrlDataStart_q(0) & ~WCtrlDataStart_q(1) // Write Data Operation (positive edge detected)
  val ReadStatusOp = RStatStart_q(0)     & ~RStatStart_q(1)     // Read Status Operation (positive edge detected)
  val ScanStatusOp = SyncStatMdcEn       & ~InProgress & ~InProgress_q(0) & ~InProgress_q(1) // Scan Status Operation (positive edge detected)
  val StartOp      = WriteDataOp | ReadStatusOp | ScanStatusOp  // Start Operation (start of any of the preceding operations)

  // Busy
  val Busy = WCtrlData | WCtrlDataStart | RStat | RStatStart | SyncStatMdcEn | EndBusy | InProgress | InProgress_q3 | Nvalid;


  // Generation of the InProgress signal (indicates when an operation is in progress)
  // Generation of the WriteOp signal (indicates when a write is in progress)
  val InProgress = RegInit(false.B) // Operation in progress
  val WriteOp    = RegInit(false.B) // Write Operation Latch (When asserted, write operation is in progress)

  when(MdcEn){
    when(StartOp) {
      InProgress := true.B
      when( ~InProgress ){
        WriteOp := WriteDataOp
      }
    } .elsewhen(EndOp) {
      InProgress := false.B
      WriteOp := false.B
    }
  }

  // Bit Counter counts from 0 to 63 (from 32 to 63 when NoPre is asserted)
  val BitCounter = RegInit( 0.U(7.W) )         // Bit Counter

  when( MdcEn ){
    when( InProgress ) {
      when( NoPre &  BitCounter === 0.U ) {
        BitCounter := "h21".U
      } .otherwise {
        BitCounter := BitCounter + 1.U
      }
    } .otherwise {
      BitCounter := 0.U
    }
  }

  // Operation ends when the Bit Counter reaches 63        
  val EndOp = BitCounter === 63.U // End of Operation
  val ByteSelect = Wire( Vec( 4, Bool() ) )         // Byte Select defines which byte (preamble, data, operation, etc.) is loaded and shifted through the shift register.

  ByteSelect(0) := InProgress & ((NoPre & (BitCounter === 0.U)) | (~NoPre & (BitCounter === "h20".U)));
  ByteSelect(1) := InProgress & (BitCounter === "h28".U);
  ByteSelect(2) := InProgress & WriteOp & (BitCounter === "h30".U);
  ByteSelect(3) := InProgress & WriteOp & (BitCounter === "h38".U);

}