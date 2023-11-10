
package Switch

import chisel3._
import chisel3.util._
import chisel3.util.random._


import org.chipsalliance.cde.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._

import MAC._

class Robin_Bundle(implicit p: Parameters) extends SwitchBundle{
  val rx = Decoupled(new Mac_Stream_Bundle)
  val mInfo = new Rx_MuxInfo_Bundle

}

class Robin(implicit p: Parameters) extends SwitchModule{
  class RobinIO extends Bundle{
    val enq = Vec( chn+1, Flipped(new Robin_Bundle) )
    val deq = new Robin_Bundle
    val sel = Output(UInt( (log2Ceil(chn+1)).W ))
  }

  val io: RobinIO = IO(new RobinIO)




  val isLock = RegInit(false.B)
  val lockChn = Reg(UInt((log2Ceil(chn+1)).W))
  io.sel := lockChn

  val lfsr = ~LFSR( (log2Ceil(chn+1)+1), ~isLock)

  for( i <- 0 until chn+1 ) {
    when( i.U === lfsr ){
      when( io.enq(i).mInfo.dest.valid ){
        assert( ~isLock )
        isLock  := true.B
        lockChn := lfsr
      }
    }
  }

  when( io.deq.rx.fire & io.deq.rx.bits.isLast ){
    assert( isLock )
    isLock := false.B
  }


  for( i <- 0 until chn+1 ) {
    io.enq(i).rx.ready := false.B
  }
  when( ~isLock ){
    io.deq.rx.valid := false.B
    io.deq.rx.bits  := 0.U.asTypeOf(new Mac_Stream_Bundle)
    io.deq.mInfo.dest.valid   := false.B
    io.deq.mInfo.dest.bits    := 0.U
    io.deq.mInfo.source.valid := false.B
    io.deq.mInfo.source.bits  := 0.U
  } .otherwise{
    io.deq <> io.enq(lockChn)
  }


}