package Switch

import chisel3._
import chisel3.util._

import MAC._




class RxBuffIO extends Bundle{
  val enq = Flipped(Decoupled(new Mac_Stream_Bundle))
  val deq = Decoupled(new Mac_Stream_Bundle)
  val header = Decoupled(Vec( 20, UInt(8.W) ) )
}




class RxBuffBase(val threshold: Int = 32) extends Module{
  val io: RxBuffIO = IO(new RxBuffIO)

  val isEnqPi = RegInit(true.B)
  val isEnqPo = ~isEnqPi

  val isDeqPi = RegInit(true.B)
  val isDeqPo = ~isDeqPi


  val buff = for( i <- 0 until 2 ) yield { Module(new Queue( new Mac_Stream_Bundle, 2048 )) }


  val header = for( i <- 0 until 2 ) yield { Reg(Vec( 20, UInt(8.W) )) }
  val hValid = for( i <- 0 until 2 ) yield { RegInit(false.B) }
  val isReach = for( i <- 0 until 2 ) yield { RegInit(false.B) }
  
  val recCnt = RegInit(0.U(12.W))



}


trait RxBuffEnq{ this: RxBuffBase =>



  buff(0).io.enq.valid := isEnqPi & io.enq.valid
  buff(0).io.enq.bits  := io.enq.bits

  buff(1).io.enq.valid := isEnqPo & io.enq.valid
  buff(1).io.enq.bits  := io.enq.bits

  io.enq.ready := (isEnqPi & buff(0).io.enq.ready) | (isEnqPo & buff(1).io.enq.ready)

  when( io.enq.fire & io.enq.bits.isLast ){
    isEnqPi := ~isEnqPi
  }



  when( io.enq.fire ){
    when( io.enq.bits.isStart ){
      recCnt := 0.U      
    } .otherwise{
      recCnt := recCnt + 1.U
    }
  }

  when( io.enq.fire ){
    when( recCnt < 20.U ){
      when( isEnqPi ){ header(0)(recCnt) := io.enq.bits.data } 
      .elsewhen( isEnqPo ){ header(1)(recCnt) := io.enq.bits.data
      } .otherwise{
        assert(false.B, "Assert Failed, Rx Under Run")
      }
    }
  }

  when( io.enq.fire ){
    when( io.enq.bits.isStart ){
      when( isEnqPi ){ isReach(0) := false.B }
      when( isEnqPo ){ isReach(1) := false.B }
    } .elsewhen(
      io.enq.bits.isLast ||
      (if( threshold != 0 ) {recCnt === threshold.U } else {false.B})
      ){
      when( isEnqPi ){ isReach(0) := true.B }
      when( isEnqPo ){ isReach(1) := true.B }
    }
  }



}

trait RxBuffDeq{ this: RxBuffBase =>

  when( io.header.fire ){
    when( isDeqPi ) { hValid(0) := false.B }
    when( isDeqPo ) { hValid(1) := false.B }
  } .elsewhen( io.enq.fire & recCnt === 20.U ){
    when( isEnqPi ) { hValid(0) := true.B }
    when( isEnqPo ) { hValid(1) := true.B }
  }


  when( io.deq.fire & io.deq.bits.isLast ){
    isDeqPi := ~isDeqPi
  }


  when( isDeqPi ){
    io.deq.valid := buff(0).io.deq.valid & isReach(0)
    io.deq.bits  := buff(0).io.deq.bits
    buff(0).io.deq.ready := io.deq.ready & isReach(0)

    buff(1).io.deq.ready := false.B

    io.header.valid := hValid(0)
    io.header.bits  := header(0)
  } .otherwise{
    assert( isDeqPo )
    io.deq.valid := buff(1).io.deq.valid & isReach(1)
    io.deq.bits  := buff(1).io.deq.bits
    buff(1).io.deq.ready := io.deq.ready & isReach(1)

    buff(0).io.deq.ready := false.B

    io.header.valid := hValid(1)
    io.header.bits  := header(1)
  }



}


class RxBuff(threshold: Int = 8) extends RxBuffBase(threshold) with RxBuffEnq with RxBuffDeq{

}

