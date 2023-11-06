package Switch

import chisel3._

import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import org.chipsalliance.cde.config._


trait WithSwitchMix { this: BaseSubsystem =>

    val switch = LazyModule(new Switch)

    sbus.coupleFrom("switch_mst") { _ := TLBuffer() := TLWidthWidget(8 / 8) := switch.tlClientNode }
    // pbus.coupleTo("switch_cfg")   { switch0.tlMasterNode := TLFragmenter(pbus) := _ }

    for( i <- 0 until 3 ){
      pbus.coupleTo("switch_cfg")   { switch.macReg(i).configNode   := TLFragmenter(pbus) := _ }   
      ibus.fromSync := switch.macReg(i).int_node   
    }

    pbus.coupleTo("switch_cfg")   { switch.dmaReg.configNode   := TLFragmenter(pbus) := _ }



}


trait WithSwitchMixModuleImp extends LazyModuleImp {
  val outer: WithSwitchMix

  val switchIO = IO(new SwitchIO)

  switchIO <> outer.switch.module.io

  // println("Warning, did you delete the rocket-chip.jar on top to reflash?")
  // println("Warning, did you delete the rocket-chip.jar on top to reflash?")
  // println("Warning, did you delete the rocket-chip.jar on top to reflash?")
  // println("Warning, did you delete the rocket-chip.jar on top to reflash?")
  // println("Warning, did you delete the rocket-chip.jar on top to reflash?")

}

class SwitchConfig() extends Config((site, here, up) => {
  case SwitchParamsKey => SwitchSetting()
})


