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





class RevBuff extends Module{
  val io: RevBuffIO = IO(new RevBuffIO)

  val isPing = RegInit(true.B)
  val isPong = ~isPing

  val pipoBuff = for( i <- 0 until 2 ) yiled { Module(new Queue( UInt(32.W), 2048/4 )) }
  val pipoInfo = for( i <- 0 until 2 ) yiled { Reg(Vec( 18, UInt(8.W))) }

  val recCnt = RegInit(0.U(5.W))







}

