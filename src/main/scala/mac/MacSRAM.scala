package MAC

import chisel3._
import chisel3.util._


class MacSRAMIO extends Bundle{
  val ce   = Input(Bool())           // Chip enable input, active high
  val we   = Input(Vec(4, Bool()))   // Write enable input, active high
  val oe   = Input(Bool())           // Output enable input, active high
  val addr = Input(UInt(8.W))        // address bus inputs
  val di   = Input(UInt(32.W))       // input data bus
  val dato = Output(UInt(32.W))      // output data bus
}

class MacSRAM extends Module{
  val io: MacSRAMIO = IO(new MacSRAMIO)


   // Generic RAM's registers and wires

  val mem0 = Mem( 256, UInt(8.W) )
  val mem1 = Mem( 256, UInt(8.W) )
  val mem2 = Mem( 256, UInt(8.W) )
  val mem3 = Mem( 256, UInt(8.W) )
  val q    = Wire(UInt(32.W))
  val raddr = Reg( UInt(8.W) )


  // Data output drivers
  io.dato := Mux((io.oe & io.ce), q, DontCare)

  // read operation
  when( io.ce ){
    raddr := io.addr  // read address needs to be registered to read clock
  }

  q := Mux(reset.asBool, 0.U, Cat(mem3.read(raddr), mem2.read(raddr), mem1.read(raddr), mem0.read(raddr)))

  // write operation
  when(io.ce & io.we(3)){
    mem3.write(io.addr, io.di(31,24))
  }

  when (io.ce & io.we(2)){
    mem2.write(io.addr, io.di(23,16))
  }

  when(io.ce & io.we(1)){
    mem1.write(io.addr, io.di(15, 8))
  }

  when(io.ce & io.we(0)){
    mem0.write(io.addr, io.di( 7, 0))
  }


}

