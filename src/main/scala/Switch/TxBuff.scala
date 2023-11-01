package Switch

import chisel3._
import chisel3.util._

import MAC._




class TxBuffIO extends Bundle{
  val enq = Flipped(Decoupled(new Mac_Stream_Bundle))
  val deq = Decoupled(new Mac_Stream_Bundle)
}


abstract class TxBuffBase(val threshold: Int = 8) extends Module{
  val io: TxBuffIO = IO(new TxBuffIO)

  val buff = Module(new Queue( new Mac_Stream_Bundle, 2048 ))
  val enqCnt = Reg(UInt(16.W))

}


trait TxBuffEnq{ this: TxBuffBase =>
    io.enq <> buff.io.enq


    when( io.enq.fire ){
      when( io.enq.bits.isStart ) {
        enqCnt := 0.U
      } .elsewhen( io.enq.bits.isLast ){

      } .elsewhen( enqCnt === threshold.U ){
        enqCnt := enqCnt
      } .otherwise{
        enqCnt := enqCnt + 1.U        
      }
    } 

}

trait TxBuffDeq{ this: TxBuffBase =>

  val isReach = RegInit(false.B)

  when( io.enq.fire ){
    when( io.enq.bits.isStart ) {
      isReach := false.B
    } .elsewhen(
        io.enq.bits.isLast ||
        (if( threshold != 0 ) {enqCnt === threshold.U} else {false.B})
      ){
      isReach := true.B
    }
  }

    
    enqCnt === threshold.U

  io.deq.bits := buff.io.deq.bits
  io.deq.valid := buff.io.deq.valid & isReach
  buff.io.deq.ready := io.deq.ready & isReach
}


class TxBuff(threshold: Int = 8) extends TxBuffBase(threshold) with TxBuffEnq with TxBuffDeq
