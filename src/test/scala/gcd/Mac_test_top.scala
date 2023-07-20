package MAC

import chisel3._
import chisel3.util._

import org.chipsalliance.cde.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.interrupts._



class MacTest(implicit p: Parameters) extends Mac{




	val tlRAM = TLRAM(
    address    = AddressSet(0x00000000L, 0x001FFFFFL),
    cacheable  = false,
    executable = false,
    atomics    = false,
    beatBytes  = 4,
    sramReg = true,
    devName = None,
  )

	tlRAM := tlClientNode



	val tlClientIONode = 
		TLClientNode(Seq(TLMasterPortParameters.v1(
			Seq(TLMasterParameters.v1(
				name = "tlSlvIO",
				sourceId = IdRange(0, 1),
			))
		)))

		val tlBar = TLXbar()

	tlMasterNode := tlBar := tlClientIONode
	ethReg.configNode  := tlBar

	val intSinkNode = IntSinkNode(IntSinkPortSimple())
	intSinkNode := ethReg.int_node

    val tlSlv = InModuleBody {
      tlClientIONode.makeIOs()
    }

	val int = InModuleBody {
	  intSinkNode.makeIOs()
	}



}