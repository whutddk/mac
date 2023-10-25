package Switch

import chisel3._
import chisel3.util._

class Receive_Enq_Ctrl_Bundle extends Bundle{
  val LatchedRxLength   = UInt(16.W)
  val RxStatusInLatched = UInt(9.W)
}

class Receive_Deq_Ctrl_Bundle extends Receive_Enq_Ctrl_Bundle{

}

class Receive_Enq_Bundle extends Bundle{
  val data = Decoupled(UInt(8.W))
  val ctrl = Decoupled(new Receive_Enq_Ctrl_Bundle)
}


class Receive_Deq_Bundle extends Bundle{
  val data = Decoupled(UInt(8.W))
  val ctrl = Decoupled(new Receive_Deq_Ctrl_Bundle)
  val header = Decoupled(Vec( 20, UInt(8.W) ) )
}


class RxBuffIO extends Bundle{
  val enq = Flipped(new Receive_Enq_Bundle)
  val deq = new Receive_Deq_Bundle
}




class RxBuffBase extends Module{
  val io: RxBuffIO = IO(new RxBuffIO)

  val isEnqPi = RegInit(true.B)
  val isEnqPo = ~isEnqPi

  val isDeqPi = RegInit(true.B)
  val isDeqPo = ~isEnqPi


  val buff = for( i <- 0 until 2 ) yield { Module(new Queue( UInt(8.W), 2048 )) }
  val info = for( i <- 0 until 2 ) yield { Reg(new Receive_Enq_Ctrl_Bundle) }
  // dontTouch(buff(0).io.enq)
  // dontTouch(info(0))
  // dontTouch(buff(1).io.enq)
  // dontTouch(info(1))

  val header = for( i <- 0 until 2 ) yield { Reg(Vec( 20, UInt(8.W) )) }
  val hValid = for( i <- 0 until 2 ) yield { RegInit(false.B) }

  val cValid = for( i <- 0 until 2 ) yield { RegInit(false.B) }


  val recCnt = RegInit(0.U(5.W))


}


trait RxBuffEnq{ this: RxBuffBase =>



  buff(0).io.enq.valid := isEnqPi & io.enq.data.valid
  buff(0).io.enq.bits  := io.enq.data.bits

  buff(1).io.enq.valid := isEnqPo & io.enq.data.valid
  buff(1).io.enq.bits  := io.enq.data.bits

  io.enq.data.ready := (isEnqPi & buff(0).io.enq.ready) | (isEnqPo & buff(1).io.enq.ready)
  io.enq.ctrl.ready := true.B

  when( io.enq.ctrl.fire ){
    isEnqPi := ~isEnqPi
    when( isEnqPi ){
      info(0)   := io.enq.ctrl.bits
    } .elsewhen( isEnqPo ){
      info(1)   := io.enq.ctrl.bits
    }
  }



  when( io.enq.ctrl.fire ){
    recCnt := 0.U
  } .elsewhen( io.enq.data.fire ){
    when( recCnt < 20.U ){
      recCnt := recCnt + 1.U
      when( isEnqPi ){
        header(0)(recCnt) := io.enq.data.bits
      } .elsewhen( isEnqPo ){
        header(1)(recCnt) := io.enq.data.bits
      } .otherwise{
        assert(false.B, "Assert Failed, Rx Under Run")
      }
    }.elsewhen( recCnt === 20.U ){
      recCnt := 21.U
    }
  }

}

trait RxBuffDeq{ this: RxBuffBase =>

  when( io.deq.header.fire ){
    when( isDeqPi ) { hValid(0) := false.B }
    when( isDeqPo ) { hValid(1) := false.B }
  } .elsewhen( io.enq.data.fire & recCnt === 20.U ){
    when( isEnqPi ) { hValid(0) := true.B }
    when( isEnqPo ) { hValid(1) := true.B }
  }


  when( io.deq.ctrl.fire ){
    isDeqPi := ~isDeqPi
    when( isDeqPi ) { cValid(0) := false.B }
    when( isDeqPo ) { cValid(1) := false.B }
  } .elsewhen( io.enq.ctrl.fire ){
    when( isEnqPi ) { cValid(0) := true.B }
    when( isEnqPo ) { cValid(1) := true.B }
  }


  when( isDeqPi ){
    io.deq.data <> buff(0).io.deq
    buff(1).io.deq.ready := false.B

    io.deq.header.valid := hValid(0)
    io.deq.header.bits  := header(0)

    io.deq.ctrl.valid := cValid(0) & ~buff(0).io.deq.valid
    io.deq.ctrl.bits  := info(0)
  } .otherwise{
    assert( isDeqPo )
    io.deq.data <> buff(1).io.deq
    buff(0).io.deq.ready := false.B

    io.deq.header.valid := hValid(1)
    io.deq.header.bits  := header(1)

    io.deq.ctrl.valid := cValid(1) & ~buff(1).io.deq.valid
    io.deq.ctrl.bits  := info(1)
  }



}


class RxBuff extends RxBuffBase with RxBuffEnq with RxBuffDeq{

}

