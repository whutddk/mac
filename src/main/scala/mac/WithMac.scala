package MAC

import chisel3._

import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import org.chipsalliance.cde.config._


trait WithManyMacMix { this: BaseSubsystem =>

    val mac0 = LazyModule(new Mac)

    fbus.coupleFrom("mac_mst") { _ := TLBuffer() := mac0.tlClientNode }
    mbus.coupleTo("mac_cfg")   { mac0.tlMasterNode := TLFragmenter(pbus) := _ }

    ibus.fromSync := mac0.int_node

}


trait WithManyMacMixModuleImp extends LazyModuleImp {
  val outer: WithManyMacMix

  val macIO = IO(new MacIO)

  macIO <> outer.mac0.module.io

}

class MacConfig() extends Config((site, here, up) => {
  case MacParamsKey => MacSetting()
})


