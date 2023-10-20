package Switch

import chisel3._
import chisel3.util._





class Transmit_Req_Bundle extends Bundle{
  val txLength       = UInt(16.W)
  val PerPacketCrcEn = Bool()
  val PerPacketPad   = Bool()
}

class Transmit_Resp_Bundle extends Bundle{
  val RetryCntLatched  = UInt(4.W)
  val RetryLimit       = Bool()
  val LateCollLatched  = Bool()
  val DeferLatched     = Bool()
  val CarrierSenseLost = Bool()

  val isClear = Bool()
}

class Transmit_Bundle extends Bundle{
  val data = Decoupled(UInt(32.W))
  val req  = Decoupled(new Transmit_Req_Bundle)
  val resp = Flipped(Decoupled(new Transmit_Resp_Bundle))
}

class TxBuffIO extends Bundle{
  val enq = Flipped(new Transmit_Bundle)
  val deq = new Transmit_Bundle
}


class TxBuff extends Module{
  val io: TxBuffIO = IO(new TxBuffIO)

  io.enq.data.ready := true.B
  io.enq.req.ready := true.B

  io.enq.resp.valid := false.B
  io.enq.resp.bits := DontCare

  io.deq.data.valid := false.B
  io.deq.data.bits  := 0.U

  io.deq.req.valid := false.B
  io.deq.req.bits  := DontCare

  io.deq.resp.ready := true.B
  
}
