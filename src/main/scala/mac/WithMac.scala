package MAC

import chisel3._
import org.chipsalliance.cde.config.Field
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.diplomacy.{LazyModule,BufferParams}
import freechips.rocketchip.tilelink.{TLBuffer, TLIdentityNode, TLWidthWidget, TLFragmenter}


trait WithManyMacMix { this: BaseSubsystem =>

    val mac = LazyModule(new Mac)

    fbus.coupleFrom("mac_mst") { _ := TLBuffer() := mac.tlClientNode }
    pbus.coupleTo("mac_cfg")   { mac.tlMasterNode := _ } //TLFragmenter(4, pbus.blockBytes) := TLWidthWidget(pbus.beatBytes) := 

    ibus.fromSync := mac.int_node

}