package Switch

import chisel3._
import chisel3.util._

import MAC._









class PiPoBuffBase[T<:Data]( dp: Int, val threshold: Int = 32) extends Module{

  class PiPoBuffIO extends Bundle{
    val enq = Flipped(Decoupled(new Mac_Stream_Bundle))
    val deq = Decoupled(new Mac_Stream_Bundle)
  }

  val io: PiPoBuffIO = IO(new PiPoBuffIO)

  val isEnqPi = RegInit(true.B)
  val isEnqPo = ~isEnqPi

  val isDeqPi = RegInit(true.B)
  val isDeqPo = ~isDeqPi


  val buff = for( i <- 0 until 2 ) yield { Module(new Queue( new Mac_Stream_Bundle, dp )) }
  val isReach = for( i <- 0 until 2 ) yield { RegInit(false.B) }
  val cnt = RegInit(0.U((log2Ceil(dp)).W))



  buff(0).io.enq.valid := isEnqPi & io.enq.valid
  buff(0).io.enq.bits  := io.enq.bits

  buff(1).io.enq.valid := isEnqPo & io.enq.valid
  buff(1).io.enq.bits  := io.enq.bits

  io.enq.ready :=
    Mux1H(Seq(
      isEnqPi -> buff(0).io.enq.ready,
      isEnqPo -> buff(1).io.enq.ready
    ))

  when( io.enq.fire & io.enq.bits.isLast ){
    isEnqPi := ~isEnqPi
  }



  when( io.enq.fire ){
    when( io.enq.bits.isStart ){
      cnt := 0.U      
    } .otherwise{
      cnt := cnt + 1.U
    }
  }



  when( io.enq.fire ){
    when( io.enq.bits.isStart ){
      when( isEnqPi ){ isReach(0) := false.B }
      when( isEnqPo ){ isReach(1) := false.B }
    } .elsewhen(
      io.enq.bits.isLast ||
      (if( threshold != 0 ) { cnt === threshold.U } else {false.B})
      ){
      when( isEnqPi ){ isReach(0) := true.B }
      when( isEnqPo ){ isReach(1) := true.B }
    }
  }


  when( io.deq.fire & io.deq.bits.isLast ){
    isDeqPi := ~isDeqPi
  }

  io.deq.valid :=
    Mux1H(Seq(
      isDeqPi -> ( buff(0).io.deq.valid & isReach(0) ),
      isDeqPo -> ( buff(1).io.deq.valid & isReach(1) ),
    ))

  io.deq.bits :=
    Mux1H(Seq(
      isDeqPi -> buff(0).io.deq.bits,
      isDeqPo -> buff(1).io.deq.bits,
    ))

  buff(0).io.deq.ready := io.deq.ready & isReach(0) & isDeqPi
  buff(1).io.deq.ready := io.deq.ready & isReach(1) & isDeqPo
}




class RxBuff(threshold: Int = 8) extends PiPoBuffBase(2048, threshold){
  val headerIO = IO(Decoupled(Vec( 20, UInt(8.W) ) ))



  val header = for( i <- 0 until 2 ) yield { Reg(Vec( 20, UInt(8.W) )) }
  val hValid = for( i <- 0 until 2 ) yield { RegInit(false.B) }


  when( io.enq.fire ){
    when( cnt < 20.U ){
      when( isEnqPi ){ header(0)(cnt) := io.enq.bits.data } 
      .elsewhen( isEnqPo ){ header(1)(cnt) := io.enq.bits.data
      } .otherwise{
        assert(false.B, "Assert Failed, Rx Under Run")
      }
    }
  }


  when( headerIO.fire ){
    when( isDeqPi ) { hValid(0) := false.B }
    when( isDeqPo ) { hValid(1) := false.B }
  } .elsewhen( io.enq.fire & cnt === 20.U ){
    when( isEnqPi ) { hValid(0) := true.B }
    when( isEnqPo ) { hValid(1) := true.B }
  }


    headerIO.valid := 
      Mux1H(Seq(
        isDeqPi -> hValid(0),
        isDeqPo -> hValid(1),
      ))
      
    headerIO.bits  := 
      Mux1H(Seq(
        isDeqPi -> header(0),
        isDeqPo -> header(1),
      ))

}

class TxBuff(threshold: Int = 8) extends PiPoBuffBase(2048, threshold)

