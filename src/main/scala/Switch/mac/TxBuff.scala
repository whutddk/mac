package Switch

import chisel3._
import chisel3.util._


class Transmit_Enq_Ctrl_Bundle extends Bundle{

}


class Transmit_Deq_Req_Bundle extends Bundle{
  val txLength       = UInt(16.W)
  val PerPacketCrcEn = Bool()
  val PerPacketPad   = Bool()
}

class Transmit_Deq_Resp_Bundle extends Bundle{
  val RetryCntLatched  = UInt(4.W)
  val RetryLimit       = Bool()
  val LateCollLatched  = Bool()
  val DeferLatched     = Bool()
  val CarrierSenseLost = Bool()

  val isClear = Bool()
}

class Transmit_Enq_Bundle extends Bundle{
  val data = Decoupled(UInt(32.W))
  val ctrl = Decoupled(new Transmit_Enq_Ctrl_Bundle)
}


class Transmit_Deq_Bundle extends Bundle{
  val data = Decoupled(UInt(32.W))
  val req  = Decoupled(new Transmit_Deq_Req_Bundle)
  val resp = Flipped(Decoupled(new Transmit_Deq_Resp_Bundle))
}


class TxBuffIO extends Bundle{
  val enq = Flipped(new Transmit_Enq_Bundle)
  val deq = new Transmit_Deq_Bundle
}


class TxBuff extends Module{
  val io: TxBuffIO = IO(new TxBuffIO)

  io.enq.data.ready := true.B
  io.enq.ctrl.ready := true.B

  io.deq.data.valid := false.B
  io.deq.data.bits  := 0.U

  io.deq.req.valid := false.B
  io.deq.req.bits  := 0.U.asTypeOf(new Transmit_Deq_Req_Bundle)

  io.deq.resp.ready := true.B
  
}
