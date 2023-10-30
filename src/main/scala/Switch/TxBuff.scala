package Switch

import chisel3._
import chisel3.util._

import MAC._




class TxBuffIO extends Bundle{
  val enq = Flipped(Decoupled(new Mac_Stream_Bundle))
  val deq = Decoupled(new Mac_Stream_Bundle)
}


abstract class TxBuffBase extends Module{
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

      } .otherwise{
        enqCnt := enqCnt + 1.U        
      }
    } 

}

trait TxBuffDeq{ this: TxBuffBase =>

  io.deq.bits := buff.io.deq.bits
  io.deq.valid := buff.io.deq.valid
  buff.io.deq.ready := io.deq.ready
}


class TxBuff extends TxBuffBase with TxBuffEnq with TxBuffDeq
