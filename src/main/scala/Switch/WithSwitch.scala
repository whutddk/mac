package Switch

import chisel3._

import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import org.chipsalliance.cde.config._


trait WithSwitchMix { this: BaseSubsystem =>

    val switch = LazyModule(new Switch)

    sbus.coupleFrom("switch_mst") { _ := TLBuffer() := switch.tlClientNode }
    // pbus.coupleTo("switch_cfg")   { switch0.tlMasterNode := TLFragmenter(pbus) := _ }
    pbus.coupleTo("switch_cfg")   { switch.ethReg.configNode   := TLFragmenter(pbus) := _ }

    ibus.fromSync := switch.ethReg.int_node

}


trait WithSwitchMixModuleImp extends LazyModuleImp {
  val outer: WithSwitchMix

  val switchIO = IO(new SwitchIO)

  switchIO <> outer.switch.module.io

  println("Warning, did you delete the rocket-chip.jar on top to reflash?")
  println("Warning, did you delete the rocket-chip.jar on top to reflash?")
  println("Warning, did you delete the rocket-chip.jar on top to reflash?")
  println("Warning, did you delete the rocket-chip.jar on top to reflash?")
  println("Warning, did you delete the rocket-chip.jar on top to reflash?")

}

class SwitchConfig() extends Config((site, here, up) => {
  case SwitchParamsKey => SwitchSetting()
})


