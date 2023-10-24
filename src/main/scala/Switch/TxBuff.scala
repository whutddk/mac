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
  val data = Decoupled(UInt(8.W))
  val req  = Decoupled(new Transmit_Req_Bundle)
  val resp = Flipped(Decoupled(new Transmit_Resp_Bundle))
}

class TxBuffIO extends Bundle{
  val enq = Flipped(new Transmit_Bundle)
  val deq = new Transmit_Bundle
}


abstract class TxBuffBase extends Module{
  val io: TxBuffIO = IO(new TxBuffIO)



  val buff = Module(new Queue( UInt(8.W), 2048 ))
  val reqInfo = Reg(new Transmit_Req_Bundle)
  val respInfo = Reg(new Transmit_Resp_Bundle)
  val enqCnt = Reg(UInt(16.W))
  val deqReqValid = RegInit(false.B)



  
}


trait TxBuffEnq{ this: TxBuffBase =>
    io.enq.data <> buff.io.enq


    when( io.enq.req.fire ){
      enqCnt := 0.U
      reqInfo := io.enq.req.bits
    } .elsewhen( io.enq.data.fire ){
      enqCnt := enqCnt + 1.U
      // when( enqCnt + 4.U >= reqInfo.txLength ){
      // }
    }


  io.enq.req.ready := ~buff.io.deq.valid


  when( io.deq.req.fire ){
    deqReqValid := false.B
  } .elsewhen( io.enq.data.fire && enqCnt === 32.U ) {
    deqReqValid := true.B
  }

}

trait TxBuffDeq{ this: TxBuffBase =>
  val isBusy = RegInit(false.B)

  when( io.deq.req.fire ){
    assert( ~isBusy )
    isBusy := true.B
  } .elsewhen(io.deq.resp.fire){
    assert( isBusy )
    isBusy := false.B
  }

  io.deq.resp <> io.enq.resp

  io.deq.req.valid := deqReqValid
  io.deq.req.bits  := reqInfo

  io.deq.data.bits := buff.io.deq.bits
  io.deq.data.valid := buff.io.deq.valid & isBusy
  buff.io.deq.ready := io.deq.data.ready & isBusy

}


class TxBuff extends TxBuffBase with TxBuffEnq with TxBuffDeq
