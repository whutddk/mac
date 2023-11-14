package Wrapeer

import chisel3._
import chisel3.util._
import chisel3.experimental.dataview._

import org.chipsalliance.cde.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.interrupts._

import MAC._
import Switch._



class EfablessTopIO(implicit p: Parameters) extends SwitchIO{

  val wbs_stb_i = Input(Bool())
  val wbs_cyc_i = Input(Bool())
  val wbs_we_i = Input(Bool())
  val wbs_sel_i = Input(UInt(4.W))
  val wbs_dat_i = Input(UInt(32.W))
  val wbs_adr_i = Input(UInt(32.W))
  val wbs_ack_o = Output(Bool())
  val wbs_dat_o = Output(UInt(32.W))

}

class EfablessTop(implicit p: Parameters) extends LazyModule with HasSwitchParameters{



  val eSwitch = LazyModule(new Switch)


  val wbCnvMasterNode = TLClientNode(Seq(TLMasterPortParameters.v1(
    Seq(TLMasterParameters.v1(
      name = "wishbone",
      sourceId = IdRange(0, 1),
    ))
  )))

  val sram = TLRAM(
    address = AddressSet(0x30002000L, 0x00001fffL),
    cacheable = false,
    executable = false,
    atomics = false,
    beatBytes = 4
  )
  
  val wbXbar = TLXbar()
  val sramXbar = TLXbar()

    eSwitch.macReg(0).configNode := TLBuffer() := wbXbar
    eSwitch.macReg(1).configNode := TLBuffer() := wbXbar
    eSwitch.dmaReg.configNode    := TLBuffer() := wbXbar
  sram := TLBuffer() := sramXbar := TLBuffer() := wbXbar := TLBuffer() := wbCnvMasterNode
                        sramXbar := TLBuffer() := eSwitch.tlClientNode


	val intSinkNode = for( i <- 0 until chn ) yield { IntSinkNode(IntSinkPortSimple()) }
	intSinkNode(0) := eSwitch.macReg(0).int_node
	intSinkNode(1) := eSwitch.macReg(1).int_node

	val int0 = InModuleBody {
	  intSinkNode(0).makeIOs()
	}

	val int1 = InModuleBody {
	  intSinkNode(1).makeIOs()
	}

  lazy val module = new EfablessTopImp(this)
}


class EfablessTopImp(outer: EfablessTop)(implicit p: Parameters) extends LazyModuleImp(outer) with HasSwitchParameters{
  val io: EfablessTopIO = IO(new EfablessTopIO)

  io.viewAsSupertype(new SwitchIO) <> outer.eSwitch.module.io

  val ( wb_bus, wb_edge ) = outer.wbCnvMasterNode.out.head


  val wbAck = RegInit(false.B)
  val wbDato = Reg(UInt(32.W))

  val isBusy = RegInit(false.B)
  val wbAValid = RegInit(false.B)
  val wbABits  = Reg(new TLBundleA(wb_edge.bundle))

  when( io.wbs_cyc_i & io.wbs_stb_i & ~isBusy ){
    assert( ~wbAValid )
    wbAValid := true.B
    isBusy   := true.B
    when( io.wbs_we_i ){
      wbABits :=
        wb_edge.Put(
          fromSource = 0.U,
          toAddress = io.wbs_adr_i,
          lgSize = log2Ceil(32/8).U,
          data = io.wbs_dat_i,
          mask = io.wbs_sel_i,
        )._2
    } .otherwise{
      wbABits :=
        wb_edge.Get(
          fromSource = 0.U,
          toAddress = io.wbs_adr_i,
          lgSize = log2Ceil(32/8).U,
        )._2
    }
  } .elsewhen( wb_bus.a.fire ){
    wbAValid := false.B
  } .elsewhen( wb_bus.d.fire ){
    wbAck := true.B
    wbDato := wb_bus.d.bits.data
  } .elsewhen( (~io.wbs_cyc_i | ~io.wbs_stb_i) & isBusy ){
    isBusy := false.B
    wbAck := false.B
  }


  wb_bus.d.ready := true.B
  wb_bus.a.valid := wbAValid
  wb_bus.a.bits := wbABits

  io.wbs_ack_o := wbAck & io.wbs_cyc_i & io.wbs_stb_i
  io.wbs_dat_o := wbDato
}





