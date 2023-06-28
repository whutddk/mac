package MAC

import chisel3._
import chisel3.util._

class MacFifoIO(dw: Int, cntw: Int) extends Bundle{
    val write   = Input(Bool())
    val read    = Input(Bool())
    val clear   = Input(Bool())
    val data_in = Input(UInt(dw.W))

    val data_out = Output(UInt(dw.W))
    val almost_full = Output(Bool())
    val full = Output(Bool())
    val almost_empty = Output(Bool())
    val empty = Output(Bool())
    val cnt = Output(UInt(cntw.W))
}

class MacFifo(dw: Int, dp: Int) extends Module{
  def cntw = log2Ceil(dp)+1
  val io: MacFifoIO = IO(new MacFifoIO(dw, cntw ))

  val fifo = SyncReadMem( dp, UInt(dw.W) )
  val data_out = Reg(UInt(dw.W)); io.data_out := data_out



  val cnt = RegInit( 0.U(cntw.W)); io.cnt := cnt
  val read_pointer  = RegInit(0.U(log2Ceil(dp).W))
  val write_pointer = RegInit(0.U(log2Ceil(dp).W))

  when(io.clear){
    cnt := io.read ^ io.write
  } .elsewhen(io.read ^ io.write){
    when(io.read){
      cnt := cnt - 1.U
    } .otherwise{
      cnt := cnt + 1.U
    }
  }


  when(io.clear){
    read_pointer := io.read
  } .elsewhen(io.read & ~io.empty){
    read_pointer := read_pointer + 1.U
  }


  when(io.clear){
    write_pointer := io.write
  } .elsewhen(io.write & ~io.full){
    write_pointer := write_pointer + 1.U
  }

  io.empty        := ~(cnt.orR)
  io.almost_empty := cnt === 1.U
  io.full         := cnt === dp.U;
  io.almost_full  := cnt(cntw-2,0).andR



  when(io.write & io.clear){
    fifo.write(0.U, io.data_in)
  } .elsewhen(io.write & ~io.full){
    fifo.write(write_pointer, io.data_in)
  }


  when(io.clear){
    data_out := fifo.read(0.U)
  } .otherwise{
    data_out := fifo.read(read_pointer)
  }

}
