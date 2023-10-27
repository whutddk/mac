package Switch

import chisel3._
import chisel3.util._

import MAC._



// class Transmit_Req_Bundle extends Bundle{
//   // val txLength       = UInt(16.W)
//   val PerPacketCrcEn = Bool()
// }

class Transmit_Resp_Bundle extends Bundle{
  val RetryLimit       = Bool()
  val LateCollLatched  = Bool()
  val DeferLatched     = Bool()
  val CarrierSenseLost = Bool()

  val isClear = Bool()
}

class Transmit_Bundle extends Bundle{
  val req = Decoupled(new Mac_Stream_Bundle)
  val resp = Flipped(Decoupled(new Transmit_Resp_Bundle))
}

class TxBuffIO extends Bundle{
  val enq = Flipped(new Transmit_Bundle)
  val deq = new Transmit_Bundle
}


abstract class TxBuffBase extends Module{
  val io: TxBuffIO = IO(new TxBuffIO)

  val buff = Module(new Queue( new Mac_Stream_Bundle, 2048 ))
  val enqCnt = Reg(UInt(16.W))

}


trait TxBuffEnq{ this: TxBuffBase =>
    io.enq.req <> buff.io.enq


    when( io.enq.req.fire ){
      when( io.enq.req.bits.isStart ) {
        enqCnt := 0.U
      } .elsewhen( io.enq.req.bits.isLast ){

      } .otherwise{
        enqCnt := enqCnt + 1.U        
      }
    } 

}

trait TxBuffDeq{ this: TxBuffBase =>

  io.deq.resp <> io.enq.resp


  io.deq.req.bits := buff.io.deq.bits
  io.deq.req.valid := buff.io.deq.valid
  buff.io.deq.ready := io.deq.req.ready
}


class TxBuff extends TxBuffBase with TxBuffEnq with TxBuffDeq
