package NewMac

import chisel3._
import chisel3.util._

import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import org.chipsalliance.cde.config._
import freechips.rocketchip.amba.apb._
import freechips.rocketchip.amba.axi4._




trait WithAccMacMix { this: BaseSubsystem =>

    val axiDataNode = AXI4SlaveNode(Seq(
        AXI4SlavePortParameters(
        slaves = Seq(
            AXI4SlaveParameters(
            address = Seq(AddressSet(0x50000000L, 0x07ffffffL)),
            regionType = RegionType.UNCACHED,
            executable = false,
            supportsRead = TransferSizes(1, 4),
            supportsWrite = TransferSizes(1, 4)
            )
        ),
        beatBytes = 32 / 8
        )
    ))


  val apbcfgNode = APBSlaveNode(portParams = Seq(APBSlavePortParameters(
    slaves = Seq(APBSlaveParameters(
      address = Seq(AddressSet(0x58000000L, 0x07ffffffL)),
      executable = false, // processor can execute from this memory
    )),
    beatBytes = 32 / 8
  )))
// AXI4Fragmenter() :=
  pbus.coupleTo("accMac_slv") { axiDataNode := AXI4UserYanker() := AXI4IdIndexer(4) :=  AXI4Deinterleaver(32/8) := TLToAXI4() := _ }
  pbus.coupleTo("accMac_cfg")  { apbcfgNode := TLToAPB() := TLFragmenter(pbus) := _ }


  val axiData = InModuleBody {
    axiDataNode.makeIOs()
  }

  val apbCfg = InModuleBody {
    apbcfgNode.makeIOs()
  }


}


trait WithAccMacMixModuleImp extends LazyModuleImp {

  val inner = IO(new Bundle{
    val gmii = new Bundle{
      val tx = Output(new GMII_TX_Bundle)
      val tclk = Input(Bool())
      val rx = Input(new GMII_RX_Bundle)
      val rclk = Input(Bool())
    }

    val axis = new Bundle{
      val rx = Flipped(Decoupled(new AXIS_Bundle))
      val tx = Decoupled(new AXIS_Bundle)
    }


  })


  val core = Module(new Core())


  inner.gmii <> core.io.gmii
  inner.axis <> core.io.axis

  core.io.clkEn  := true.B
  core.io.miiSel := true.B

  core.io.ifg_delay := 12.U

  core.io.isPaddingEnable := true.B
  core.io.minFrameLength  := 64.U

}

// class AccMacConfig() extends Config((site, here, up) => {
//   case AccMacParamsKey => NewMacSetting()
// })


