package MAC

import chisel3._
import chisel3.util._

class RevBuff_Enq_Ctrl_Bundle extends Bundle{
  val LatchedRxLength   = UInt(16.W)
  val RxStatusInLatched = UInt(9.W)
  val isRxAbort         = Bool()
}

class RevBuff_Deq_Ctrl_Bundle extends Bundle{

}

class RevBuff_Enq_Bundle extends Bundle{
  val data = Decoupled(UInt(32.W))
  val ctrl = Decoupled(new RevBuff_Enq_Ctrl_Bundle)
}


class RevBuff_Deq_Bundle extends Bundle{
  val data = Decoupled(UInt(32.W))
  val ctrl = Decoupled(new RevBuff_Deq_Ctrl_Bundle)
}


class RevBuffIO extends Bundle{
  val enq = Flipped(new RevBuff_Enq_Bundle)
  val deq = new RevBuff_Deq_Bundle
}


class Packet_Info_Bundle extends Bundle{
  val LatchedRxLength   = UInt(16.W)
  val RxStatusInLatched = UInt(9.W)
  val header = Vec( 5, UInt(32.W) )
}


class RevBuff extends Module{
  val io: RevBuffIO = IO(new RevBuffIO)

  val isPing = RegInit(true.B)
  val isPong = ~isPing

  val pipoBuff = for( i <- 0 until 2 ) yiled { Module(new Queue( UInt(32.W), 2048/4 )) }
  val pipoInfo = for( i <- 0 until 2 ) yiled { Reg(new Packet_Info_Bundle) }

  val recCnt = RegInit(0.U(3.W))

  pipoBuff(0).io.enq.valid := isPing & io.enq.data.valid
  pipoBuff(0).io.enq.bits  := io.enq.data.bits

  pipoBuff(1).io.enq.valid := isPong & io.enq.data.valid
  pipoBuff(1).io.enq.btis  := io.enq.data.bits

  io.enq.data.ready := (isPing & io.enq.data.ready) | (isPong & io.enq.data.ready)
  io.enq.ctrl.ready := true.B

  pipoBuff(0).reset := reset.asBool | (isPing & io.enq.ctrl.fire & io.enq.ctrl.bits.isRxAbort)
  pipoBuff(1).reset := reset.asBool | (isPong & io.enq.ctrl.fire & io.enq.ctrl.bits.isRxAbort)

  when( io.enq.ctrl.fire & ~io.enq.ctrl.bits.isRxAbort){
    isPing := ~isPing
    when( isPing ){
      pipoInfo(0).LatchedRxLength   := io.enq.ctrl.bits.LatchedRxLength
      pipoInfo(0).RxStatusInLatched := io.enq.ctrl.bits.RxStatusInLatched
    } .elsewhen( isPong ){
      pipoInfo(1).LatchedRxLength   := io.enq.ctrl.bits.LatchedRxLength
      pipoInfo(1).RxStatusInLatched := io.enq.ctrl.bits.RxStatusInLatched
    }
  }



  when( io.enq.ctrl.fire ){
    recCnt := 0.U
  } .elsewhen( io.enq.data.fire ){
    when( recCnt < 5 ){
      recCnt := recCnt + 1.U
      when( isPing ){
        pipoInfo(0).header(recCnt) := io.enq.data.bits
      } .elsewhen( isPong ){
        pipoInfo(1).header(recCnt) := io.enq.data.bits
      } .otherwise{
        assert(false.B, "Assert Failed, Rx Under Run")
      }
    }
  }





}
