package NewMac

import chisel3._
import chisel3.util._

class CoreIO_Bundle extends Bundle{

  val gmii = new Bundle{
    val tx = Output(new GMII_TX_Bundle)
    val rx = Input(new GMII_RX_Bundle)
  }

  val axis = new Bundle{
    val rx = Flipped(Decoupled(new AXIS_Bundle))
    val tx = Decoupled(new AXIS_Bundle)
  }

  val clkEn = Input(Bool())
  val miiSel = Input(Bool())

  val ifg_delay = Input(UInt(8.W))

  val error_bad_frame = Output(Bool())
  val error_bad_fcs = Output(Bool())

  val isPaddingEnable = Input(Bool())
  val minFrameLength  = Input( UInt(8.W) )
}




class Core extends Module{
  val io = IO(new CoreIO_Bundle)


  val gmiiRx_AxisTx = Module(new GmiiRx_AxisTx )
  val gmiiTx_AxisRx = Module(new GmiiTx_AxisRx )

  io.gmii.tx := gmiiTx_AxisRx.io.gmii
  io.axis.rx <> gmiiTx_AxisRx.io.axis

  gmiiTx_AxisRx.io.clkEn := io.clkEn
  gmiiTx_AxisRx.io.miiSel := io.miiSel
  
  gmiiTx_AxisRx.io.ifg_delay := io.ifg_delay

  gmiiRx_AxisTx.io.gmii := io.gmii.rx
  gmiiRx_AxisTx.io.axis <> io.axis.tx

  gmiiRx_AxisTx.io.clkEn := io.clkEn
  gmiiRx_AxisTx.io.miiSel := io.miiSel

  io.error_bad_frame := gmiiRx_AxisTx.io.error_bad_frame
  io.error_bad_fcs   := gmiiRx_AxisTx.io.error_bad_fcs


  gmiiTx_AxisRx.io.isPaddingEnable := io.isPaddingEnable
  gmiiTx_AxisRx.io.minFrameLength  := io.minFrameLength
}

