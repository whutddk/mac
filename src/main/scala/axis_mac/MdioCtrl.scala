package MAC

import chisel3._
import chisel3.util._


class MDIOCtrl_IO_Bundle extends Bundle with MDIO{

  val req = Flipped(Decoupled(new Bundle{
    val fiad = UInt(5.W)
    val rgad = UInt(5.W)
    val data = UInt(16.W)
    val isWR = Bool()
  }))

  val resp  = Decoupled(new Bundle{
    val data = UInt(16.W)
  })

  val div = Input(UInt(8.W))
  val noPre = Input(Bool())
}


abstract class MDIOCtrlBase extends Module{
  val io = IO( new MDIOCtrl_IO_Bundle )

  val isBusy = RegInit(false.B)
  val mdc = Reg(Bool())
  val mdcPos: Bool

  io.req.ready := ~isBusy



}

trait MDIOMdcCtrl{ this: MDIOCtrlBase =>
  val divCnt = RegInit(0.U(8.W))

  when( io.req.fire ){
    divCnt := 0.U
    mdc := false.B
  } .elsewhen( divCnt === io.div ){
    divCnt := 0.U
    mdc := ~mdc
  } .otherwise{
    divCnt := divCnt + 1.U
  }

  val mdcPos = mdc & divCnt === 0.U
  io.mdc := mdc & isBusy

}

trait MDIOTransCtrl{ this: MDIOCtrlBase =>
  val shiftReg = Reg(UInt( (32).W ))
  val shiftCnt = Reg(UInt(8.W))

  val st = WireDefault("b01".U(2.W))
  val isWR = RegEnable( io.req.bits.isWR, io.req.fire )
  val ta = WireDefault("b10".U(2.W))

  when( io.req.fire ){
    shiftReg := Cat( st, Mux( io.req.bits.isWR, "b01".U(2.W), "b10".U(2.W) ), io.req.bits.fiad, io.req.bits.rgad, ta, io.req.bits.data )
    shiftCnt := Mux( io.noPre, 32.U, 0.U )
  } .elsewhen( mdcPos & shiftCnt < 64.U ){
    shiftCnt := shiftCnt + 1.U
    when( shiftCnt >= 32.U ){
      shiftReg := Cat( shiftReg(30,0), io.mdi )
    }
  }

  io.mdo   := Mux( shiftCnt < 32.U, true.B, shiftReg.extract(31).asBool )
  io.mdoEn := isBusy & ( shiftCnt < 46.U | isWR )
      



}


class MDIOCtrl extends MDIOCtrlBase
with MDIOMdcCtrl
with MDIOTransCtrl{
  when( io.resp.fire ){
    isBusy := false.B
  } .elsewhen( io.req.fire ){
    isBusy := true.B
  }

  val respValid = RegInit(false.B)

  when( io.resp.fire ){
    respValid := false.B
  } .elsewhen( shiftCnt === 64.U ){
    respValid := true.B
  }

  io.resp.valid := respValid
  io.resp.bits.data  := shiftReg(15,0)


}




