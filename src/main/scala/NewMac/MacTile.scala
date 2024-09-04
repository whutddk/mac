package NewMac

import chisel3._
import chisel3.util._
import chisel3.util.experimental._

import org.chipsalliance.cde.config._
import freechips.rocketchip.tilelink._



import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.interrupts._




class MacTileIO(implicit p: Parameters) extends MacBundle{
    val srcAddress = Input( UInt(32.W) )
    val txLen = Input(UInt(8.W))
    val destAddress = Input( UInt(32.W) )
    val code = Output(UInt(8.W))
    val trigger = Input(Bool())

    val gmii = new Bundle{
      val tx = Output(new GMII_TX_Bundle)
      val rx = Input(new GMII_RX_Bundle)
    }

    val clkEn = Input(Bool())
    val miiSel = Input(Bool())
    val ifg_delay = Input(UInt(8.W))
    val interrupt = Output(Bool())
}


class MacTile(implicit p: Parameters) extends LazyModule with HasMacParameters{

  val tlClientNode = TLClientNode(Seq(TLMasterPortParameters.v1(
    Seq(TLMasterParameters.v1(
      name = "DMA",
      sourceId = IdRange(0, 1),
    ))
  )))


  // val memSlaveNode = TLManagerNode(Seq(
  //   TLSlavePortParameters.v1(
  //     Seq(TLSlaveParameters.v1(
  //       address = AddressSet(0x00000000L, 0xffffffffL).subtract(AddressSet(0x0L, 0x7fffffffL)),
  //       regionType = RegionType.GET_EFFECTS,
  //       executable = false,
  //       supportsGet = TransferSizes(1, 256/8),
  //       supportsPutFull = TransferSizes(1, 256/8),
  //       supportsPutPartial = TransferSizes(1, 256/8),
  //     )),
  //     beatBytes = 64/8,
  //   )
  // ))
  //  memSlaveNode := tlClientNode


  val memRange = AddressSet(0x00000000L, 0xffffffffL).subtract(AddressSet(0x0L, 0x7fffffffL))
  val memAXI4SlaveNode = AXI4SlaveNode(Seq(
    AXI4SlavePortParameters(
      slaves = Seq(
        AXI4SlaveParameters(
          address = memRange,
          regionType = RegionType.UNCACHED,
          executable = true,
          supportsRead = TransferSizes(1, 64/8),
          supportsWrite = TransferSizes(1, 64/8)
        )
      ),
      beatBytes = 64 / 8
    )
  ))


    memAXI4SlaveNode := 
    AXI4UserYanker() := 
    AXI4IdIndexer(4) :=
    AXI4Deinterleaver(64/8) :=
    TLToAXI4() :=
    // TLWidthWidget(64 / 8) := 
    tlClientNode

 


//   val macReg = for( i <- 0 until chn ) yield { LazyModule(new MacReg(i)) }
//   val dmaReg = LazyModule(new DmaReg)

  lazy val module = new MacTileImp(this)
  
  val axiSlv = InModuleBody {
    // memSlaveNode.makeIOs()
    memAXI4SlaveNode.makeIOs()
  }
}

class MacTileImp(outer: MacTile)(implicit p: Parameters) extends LazyModuleImp(outer) with HasMacParameters{
  val io = IO(new MacTileIO)
  val ( dma_bus, dma_edge ) = outer.tlClientNode.out.head

  val mac = Module(new DMA2Mac(dma_edge))

  mac.io.srcAddress  := io.srcAddress
  mac.io.txLen       := io.txLen
  mac.io.destAddress := io.destAddress
  io.code := mac.io.code
  mac.io.trigger := io.trigger

  mac.io.gmii <> io.gmii

  mac.io.clkEn  := io.clkEn
  mac.io.miiSel := io.miiSel
  mac.io.ifg_delay := io.ifg_delay
  io.interrupt  := mac.io.interrupt


  mac.io.tile.D.bits  := dma_bus.d.bits
  mac.io.tile.D.valid := dma_bus.d.valid
  dma_bus.d.ready := mac.io.tile.D.ready
  dma_bus.a.valid := mac.io.tile.A.valid
  dma_bus.a.bits  := mac.io.tile.A.bits
  mac.io.tile.A.ready := dma_bus.a.ready
}

