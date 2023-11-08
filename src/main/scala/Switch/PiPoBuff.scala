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
    when( io.enq.bits.isLast ){
      cnt := 0.U      
    } .elsewhen( io.enq.bits.isStart ){
      cnt := 1.U      
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



class TxBuff(threshold: Int = 8) extends PiPoBuffBase(2048, threshold)




class Rx_MuxInfo_Bundle extends Bundle{
  val dest   = Valid(UInt((48+1).W))
  val source = Valid(UInt((48+1).W))

  def isBroadcast = dest.bits.extract(40)
}

class RxBuff(threshold: Int = 8) extends PiPoBuffBase(2048, threshold){
  val mInfo = IO(new Rx_MuxInfo_Bundle)

  val destReg   = for( i <- 0 until 2 ) yield { Reg(Vec(6, UInt(8.W))) }
  val destValid = for( i <- 0 until 2 ) yield { RegInit(false.B) }

  val sourceReg = for( i <- 0 until 2 ) yield { Reg(Vec(6, UInt(8.W))) }
  val sourceValid = for( i <- 0 until 2 ) yield { RegInit(false.B) }

  when( io.enq.fire ){
    when( cnt < 6.U ){
      when( isEnqPi ){ destReg(0)(cnt) := io.enq.bits.data } 
      when( isEnqPo ){ destReg(1)(cnt) := io.enq.bits.data }
    } .elsewhen( cnt < 12.U ){
      when( isEnqPi ){ sourceReg(0)(cnt-6.U) := io.enq.bits.data } 
      when( isEnqPo ){ sourceReg(1)(cnt-6.U) := io.enq.bits.data }
    } 
  }


  when( io.deq.fire & io.deq.bits.isLast ){
    when( isDeqPi ) { destValid(0) := false.B; sourceValid(0) := false.B }
    when( isDeqPo ) { destValid(1) := false.B; sourceValid(1) := false.B }
  } .elsewhen( io.enq.fire & cnt === 5.U ){
    when( isEnqPi ) { destValid(0) := true.B }
    when( isEnqPo ) { destValid(1) := true.B }
  } .elsewhen( io.enq.fire & cnt === 11.U ){
    when( isEnqPi ) { sourceValid(0) := true.B }
    when( isEnqPo ) { sourceValid(1) := true.B }
  }

  mInfo.dest.valid :=
    Mux1H(Seq(
      isDeqPi -> destValid(0),
      isDeqPo -> destValid(1),
    ))

  mInfo.dest.bits :=
    Mux1H(Seq(
      isDeqPi -> Cat(destReg(0)),
      isDeqPo -> Cat(destReg(1)),
    ))

  mInfo.source.valid :=
    Mux1H(Seq(
      isDeqPi -> sourceValid(0),
      isDeqPo -> sourceValid(1),
    ))

  mInfo.source.bits :=
    Mux1H(Seq(
      isDeqPi -> Cat(sourceReg(0)),
      isDeqPo -> Cat(sourceReg(1)),
    ))


}



