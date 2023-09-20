package MAC

import chisel3._

import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import org.chipsalliance.cde.config._


trait WithManyMacMix { this: BaseSubsystem =>

    val mac0 = LazyModule(new Mac)

    sbus.coupleFrom("mac_mst") { _ := TLBuffer() := mac0.tlClientNode }
    pbus.coupleTo("mac_cfg")   { mac0.tlMasterNode := TLFragmenter(pbus) := _ }
    pbus.coupleTo("mac_cfg")   { mac0.ethReg.configNode   := TLFragmenter(pbus) := _ }

    ibus.fromSync := mac0.ethReg.int_node

}


trait WithManyMacMixModuleImp extends LazyModuleImp {
  val outer: WithManyMacMix

  val macIO = IO(new MacIO)

  macIO <> outer.mac0.module.io

  println("Warning, did you delete the rocket-chip.jar on top to reflash?")
  println("Warning, did you delete the rocket-chip.jar on top to reflash?")
  println("Warning, did you delete the rocket-chip.jar on top to reflash?")
  println("Warning, did you delete the rocket-chip.jar on top to reflash?")
  println("Warning, did you delete the rocket-chip.jar on top to reflash?")

}

class MacConfig() extends Config((site, here, up) => {
  case MacParamsKey => MacSetting()
})


