package NewMac

import chisel3._

import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import org.chipsalliance.cde.config._




trait WithNewMacMix { this: BaseSubsystem =>


  val macReg = LazyModule(new Registers)

  val tlClientNode = TLClientNode(Seq(TLMasterPortParameters.v1(
    Seq(TLMasterParameters.v1(
      name = "DMA",
      sourceId = IdRange(0, 1),
    ))
  )))

  sbus.coupleFrom("newMac_mst") { _ := TLBuffer() := TLWidthWidget(64 / 8) := tlClientNode }
  pbus.coupleTo(s"newMac_cfg")   { macReg.configNode   := TLFragmenter(pbus) := _ }
  ibus.fromSync := macReg.int_node   


}


trait WithNewMacMixModuleImp extends LazyModuleImp {
  val outer: WithNewMacMix

  val io = IO(new new Bundle{
    val tx = Output(new GMII_TX_Bundle)
    val rx = Input(new GMII_RX_Bundle)
  })



  // println("Warning, did you delete the rocket-chip.jar on top to reflash?")
  // println("Warning, did you delete the rocket-chip.jar on top to reflash?")
  // println("Warning, did you delete the rocket-chip.jar on top to reflash?")
  // println("Warning, did you delete the rocket-chip.jar on top to reflash?")
  // println("Warning, did you delete the rocket-chip.jar on top to reflash?")





  val ( dma_bus, dma_edge ) = outer.tlClientNode.out.head

  val mac = Module(new DMA2Mac(dma_edge))



  mac.io.srcAddress  := outer.macReg.module.io.srcAddress
  mac.io.txLen       := outer.macReg.module.io.txLen
  mac.io.destAddress := outer.macReg.module.io.destAddress
  outermacReg.module.io.code := mac.io.code
  mac.io.trigger     := outermacReg.module.io.trigger
  mac.io.ifg_delay := outermacReg.module.io.ifg_delay
  outermacReg.module.io.interrupt  := mac.io.interrupt

  mac.io.clkEn  := true.B
  mac.io.miiSel := false.B

  mac.io.tile.D.bits  := dma_bus.d.bits
  mac.io.tile.D.valid := dma_bus.d.valid
  dma_bus.d.ready := mac.io.tile.D.ready
  dma_bus.a.valid := mac.io.tile.A.valid
  dma_bus.a.bits  := mac.io.tile.A.bits
  mac.io.tile.A.ready := dma_bus.a.ready


  io.tx <> mac.io.gmii.tx
  io.rx <> mac.io.gmii.rx


}

class NewMacConfig() extends Config((site, here, up) => {
  case NewMacParamsKey => NewMacSetting()
})


