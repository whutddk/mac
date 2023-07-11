package MAC

import chisel3._
import chisel3.util._

import org.chipsalliance.cde.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._




class MacTest(implicit p: Parameters) extends LazyModule with HasMacParameters{

	val mac = LazyModule( new Mac )


	val tlRAM = TLRAM(
    address    = AddressSet(0x00000000L, 0x001FFFFFL),
    cacheable  = false,
    executable = false,
    atomics    = false,
    beatBytes  = 4,
    sramReg = true,
    devName = None,
  )

	tlRAM := mac.tlClientNode



		val tlClientIONode = 
			TLClientNode(Seq(TLMasterPortParameters.v1(
				Seq(TLMasterParameters.v1(
					name = "tlSlvIO",
					sourceId = IdRange(0, 1),
				))
			)))

		mac.tlMasterNode := tlClientIONode

    val tlSlv = InModuleBody {
      tlClientIONode.makeIOs()
    }

	lazy val module = new LazyModuleImp(this) {
		val io = IO(new MacIO with MDIO)

		io <> mac.module.io
	}
}