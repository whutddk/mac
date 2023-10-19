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


  val ethReg = LazyModule(new MacReg)

  lazy val module = new SwitchImp(this)

}

class SwitchImp(outer: Switch)(implicit p: Parameters) extends LazyModuleImp(outer) with HasSwitchParameters{
  val io = IO(new SwitchIO)

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

















  val rxBuff = Module(new RxBuff)

  rxBuff.io.enq <> mac(0).io.rxEnq
  rxBuff.io.deq.data.ready := false.B
  rxBuff.io.deq.ctrl.ready := false.B

  val txBuff = Module(new TxBuff)
  txBuff.io.deq <> mac(0).io.txDeq
  txBuff.io.enq.ctrl.valid := false.B
  txBuff.io.enq.data.valid := false.B
  txBuff.io.enq.data.bits  := 0.U




}

