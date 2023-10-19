package Switch

import chisel3._
import chisel3.util._

import MAC._

import org.chipsalliance.cde.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.interrupts._

import chisel3.experimental.dataview._


class SwitchIO(implicit p: Parameters) extends SwitchBundle{
  val mii = Vec( chn, new MII )

  val isLoopBack = Output(Vec( chn, Bool()))
  val asyncReset = Input(AsyncReset())
}


class Switch(implicit p: Parameters) extends LazyModule with HasSwitchParameters{

  val tlClientNode = TLClientNode(Seq(TLMasterPortParameters.v1(
    Seq(TLMasterParameters.v1(
      name = "DMA",
      sourceId = IdRange(0, chn),
    ))
  )))




  val ethReg = LazyModule(new MacReg)

  lazy val module = new SwitchImp(this)

}

class SwitchImp(outer: Switch)(implicit p: Parameters) extends LazyModuleImp(outer) with HasSwitchParameters{
  val io = IO(new SwitchIO)
  val ( dma_bus, dma_edge ) = outer.tlClientNode.out.head

  val mac = ( 0 until chn ).map{ i =>
    Module(new Mac)
  }

  ( 0 until chn ).map{ i =>
    mac(i).io.mii <> io.mii(i)
    mac(i).io.asyncReset := io.asyncReset
    io.isLoopBack(i) := mac(i).io.isLoopBack
  }

  outer.ethReg.module.io.asyncReset := io.asyncReset
  outer.ethReg.module.io.viewAsSupertype(new Mac_Config_Bundle) <> mac(0).io.cfg

  val switchMux = Module(new SwitchMux)

  switchMux.io.rxEnq(0) <> mac(0).io.rxEnq
  switchMux.io.txDeq(0) <> mac(0).io.txDeq



















}

