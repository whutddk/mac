// package MAC

// import chisel3._
// import chisel3.util._

// import org.chipsalliance.cde.config._
// import freechips.rocketchip.diplomacy._
// import freechips.rocketchip.tilelink._
// import freechips.rocketchip.interrupts._



// class MacTest(implicit p: Parameters) extends Mac{



// 	val tlClientIONode = 
// 		TLClientNode(Seq(TLMasterPortParameters.v1(
// 			Seq(TLMasterParameters.v1(
// 				name = "tlSlvIO",
// 				sourceId = IdRange(0, 1),
// 			))
// 		)))


// 	ethReg.configNode  := tlClientIONode

// 	val intSinkNode = IntSinkNode(IntSinkPortSimple())
// 	intSinkNode := ethReg.int_node

//     val tlSlv = InModuleBody {
//       tlClientIONode.makeIOs()
//     }

// 	val int = InModuleBody {
// 	  intSinkNode.makeIOs()
// 	}



// }