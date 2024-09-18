package NewMac

import chisel3._
import chisel3.util._

import freechips.rocketchip.diplomacy._
import org.chipsalliance.cde.config._
import freechips.rocketchip.util._

class CoreIO_Bundle extends Bundle{

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

  val clkEn = Input(Bool())
  val miiSel = Input(Bool())

  val ifg_delay = Input(UInt(8.W))

  val error_bad_frame = Output(Bool())
  val error_bad_fcs = Output(Bool())

  val isPaddingEnable = Input(Bool())
  val minFrameLength  = Input( UInt(8.W) )
}




class Core(implicit p: Parameters) extends NewMacModule{
  val io = IO(new CoreIO_Bundle)


  val gmiiRx_AxisTx = withClockAndReset( io.gmii.rclk.asClock, reset ) { Module(new GmiiRx_AxisTx ) }
  val gmiiTx_AxisRx = withClockAndReset( io.gmii.tclk.asClock, reset ) { Module(new GmiiTx_AxisRx ) }

  io.gmii.tx := gmiiTx_AxisRx.io.gmii

  gmiiTx_AxisRx.io.clkEn := io.clkEn
  gmiiTx_AxisRx.io.miiSel := io.miiSel
  
  gmiiTx_AxisRx.io.ifg_delay := io.ifg_delay

  gmiiRx_AxisTx.io.gmii := io.gmii.rx

  gmiiRx_AxisTx.io.clkEn := io.clkEn
  gmiiRx_AxisTx.io.miiSel := io.miiSel

  io.error_bad_frame := gmiiRx_AxisTx.io.error_bad_frame
  io.error_bad_fcs   := gmiiRx_AxisTx.io.error_bad_fcs


  gmiiTx_AxisRx.io.isPaddingEnable := io.isPaddingEnable
  gmiiTx_AxisRx.io.minFrameLength  := io.minFrameLength






  val req_ToAsync = Wire(new AsyncBundle(new AXIS_Bundle, params = AsyncQueueParams(depth=4, sync=2)))
  val resp_ToAsync = Wire(new AsyncBundle(new AXIS_Bundle, params = AsyncQueueParams(depth=4, sync=2)))

  io.axis.tx <> FromAsyncBundle( req_ToAsync, 2 )
  resp_ToAsync <> ToAsyncBundle( io.axis.rx, AsyncQueueParams(depth=4, sync=2) )

  withClockAndReset( io.gmii.tclk.asClock, reset ) {
    gmiiTx_AxisRx.io.axis <> FromAsyncBundle( resp_ToAsync, 2 )
  }  

  withClockAndReset( io.gmii.rclk.asClock, reset ) {
    req_ToAsync <> ToAsyncBundle( gmiiRx_AxisTx.io.axis, AsyncQueueParams(depth=4, sync=2) )

  }  










}

