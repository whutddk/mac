package Switch

import chisel3._
import chisel3.util._


import org.chipsalliance.cde.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._

import MAC._



class SwitchNodeIO extends Bundle{
  val rx = Decoupled(new Mac_Stream_Bundle)
  val mInfo = new Rx_MuxInfo_Bundle

  val tx = Flipped(Decoupled(new Mac_Stream_Bundle))

}



abstract class SwitchNode(implicit p: Parameters) extends SwitchModule{
  val ex: SwitchNodeIO = IO(new SwitchNodeIO)

}



class MacNode(implicit p: Parameters) extends Mac{
  val rxBuff = Module(new RxBuff)
  val txBuff = Module(new TxBuff)



  macTileLinkRx.io.rxEnq <> rxBuff.io.enq
  txBuff.io.deq <> macTileLinkTx.io.txDeq

  rxBuff.io.deq <> ex.rx
  ex.tx <> txBuff.io.enq


  ex.mInfo <> rxBuff.mInfo
}



class DmaNode(edgeOut: TLEdgeOut)(implicit p: Parameters) extends DMAMst(edgeOut){
  def DmaMac = "habcdef".U(48.W)

  ex.tx <> rxBuff.io.enq
  txBuff.io.deq <> ex.rx

  ex.mInfo.dest.valid := mInfoValid
  ex.mInfo.dest.bits  := Cat( 1.U, trigTxNum )
  ex.mInfo.source.valid := mInfoValid
  ex.mInfo.source.bits  := DmaMac

}

