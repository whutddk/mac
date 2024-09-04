package NewMac

import chisel3._
import chisel3.util._

import freechips.rocketchip.regmapper._
import freechips.rocketchip.tilelink._
import org.chipsalliance.cde.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.interrupts._



class RegistersIO extends Bundle{
  val isPaddingEnable = Output(Bool())
  val minFrameLength  = Output( UInt(8.W) )
  val srcAddress = Output( UInt(32.W) )
  val txLen = Output(UInt(8.W))
  val destAddress = Output( UInt(32.W) )
  val code = Input(UInt(8.W))
  val trigger = Output(Bool())
  val ifg_delay = Output(UInt(8.W))

  val interrupt = Input(Bool())
}



class Registers()(implicit p: Parameters) extends LazyModule{





  // DTS
  val dtsdevice = new SimpleDevice(s"Registers",Seq(s"Registers"))
  val int_node = IntSourceNode(IntSourcePortSimple(num = 1, resources = dtsdevice.int))


  val configNode = TLRegisterNode(
    address = Seq(AddressSet(0x30000000L, 0x000000ffL)),
    device = dtsdevice,
    concurrency = 1,
    beatBytes = 64/8,
    executable = true
  )
  lazy val module = new RegistersImp(this)
}

class RegistersImp(outer: Registers)(implicit p: Parameters) extends LazyModuleImp(outer) with HasSwitchParameters{

    val io: RegistersIO = IO(new RegistersIO)
    val (int, _) = outer.int_node.out(0)

    val isPaddingEnable = RegInit(true.B); io.isPaddingEnable := isPaddingEnable
    val minFrameLength  = RegInit( 64.U(8.W) ); io.minFrameLength := minFrameLength
    val ifg_delay       = RegInit( 12.U(8.W) ); io.ifg_delay := ifg_delay
    val srcAddress  = RegInit( "h81000000".U(32.W) ); io.srcAddress := srcAddress
    val txLen       = RegInit( 32.U(8.W)); io.txLen := txLen
    val destAddress = RegInit( "h82000000".U(32.W) ); io.destAddress := destAddress
    // val code        = RegInit( 0.U(8.W))
    val trigger     = Wire(Bool()) 

    

    outer.configNode.regmap(
      ( 0 << 3 ) -> 
        RegFieldGroup("PaddingEnable", Some("PaddingEnable"), Seq(
          RegField(1, isPaddingEnable, RegFieldDesc( "PaddingEnable", "PaddingEnable", reset = Some(0)))
        )),

      ( 1 << 3 ) ->
        RegFieldGroup("minFrameLength", Some("minFrameLength"), Seq(
          RegField(32, minFrameLength, RegFieldDesc("minFrameLength", "minFrameLength", reset=Some(64))), 
        )),

      ( 2 << 3 ) -> 
        RegFieldGroup("ifg_delay", Some("ifg_delay"), Seq(
          RegField(8, ifg_delay)
        )),

      ( 3 << 3 ) -> 
        RegFieldGroup("srcAddress", Some("srcAddress"), Seq(
          RegField(32, srcAddress, RegFieldDesc("srcAddress", "srcAddress", reset=Some(0x81000000)))
        )),

      ( 4 << 3 ) ->
        RegFieldGroup("txLen", Some("txLen"), Seq(
          RegField(8, txLen, RegFieldDesc("txLen", "txLen", reset=Some(32)))
        )),

      ( 5 << 3 ) ->
        RegFieldGroup("destAddress", Some("destAddress"), Seq(
          RegField(32, destAddress, RegFieldDesc("destAddress", "destAddress", reset=Some(0x82000000)))
        )),

      ( 6 << 3 ) ->
        RegFieldGroup("code", Some("code"), Seq(
          RegField.r(8, io.code, RegFieldDesc("code", "code", reset=Some(0x0))),
        )),


      ( 7 << 3 ) ->
        RegFieldGroup("trigger", Some("trigger"), Seq(
          RegField(1, 0.U,  RegWriteFn((valid, data) => { when ((valid & data) === 1.U) { io.trigger := true.B }; true.B }),  RegFieldDesc("trigger", "trigger", reset=Some(0))),
        )),
        
    )























    // Generating interrupt signal
    // io.int_o :=
    int(0)    := io.interrupt




}


