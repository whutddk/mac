package MAC

import chisel3._
import chisel3.util._


class Transmit_Enq_Ctrl_Bundle extends Bundle{

}

class Transmit_Deq_Ctrl_Bundle extends Bundle{

}

class Transmit_Enq_Bundle extends Bundle{
  val data = Decoupled(UInt(32.W))
  val ctrl = Decoupled(new RevBuff_Enq_Ctrl_Bundle)
}


class Transmit_Deq_Bundle extends Bundle{
  val data = Decoupled(UInt(32.W))
  val ctrl = Decoupled(new RevBuff_Deq_Ctrl_Bundle)
}


class TxBuffIO extends Bundle{
  val enq = Flipped(new Transmit_Enq_Bundle)
  val deq = new Transmit_Deq_Bundle
}


class TxBuff extends Module{
  val io: TxBuffIO = IO(new TxBuffIO)




  
}
