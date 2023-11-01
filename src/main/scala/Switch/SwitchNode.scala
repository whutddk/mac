package Switch

import chisel3._
import chisel3.util._


import org.chipsalliance.cde.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._

import MAC._

class Rx_MuxInfo_Bundle extends Bundle{

}

class SwitchNodeIO extends Bundle{
  val rx = Decoupled(new Mac_Stream_Bundle)
  val dest = Output(new Rx_MuxInfo_Bundle)

  val tx = Flipped(Decoupled(new Mac_Stream_Bundle))

}



abstract class SwitchNode(implicit p: Parameters) extends SwitchModule{
  val ex: SwitchNodeIO = IO(new SwitchNodeIO)

}



class MacNode(implicit p: Parameters) extends Mac{
  val rxBuff = Module(new RxBuff)
  val txBuff = Module(new TxBuff)

  rxBuff.io.header.ready := true.B

  macTileLinkRx.io.rxEnq <> rxBuff.io.enq
  txBuff.io.deq <> macTileLinkTx.io.txDeq

  rxBuff.io.deq <> ex.rx
  ex.tx <> txBuff.io.enq

  ex.dest := DontCare
}



class DmaNode(edgeOut: TLEdgeOut)(implicit p: Parameters) extends DMAMst(edgeOut){
  ex.tx <> rxBuff.io.enq
  txBuff.io.deq <> ex.rx
}

