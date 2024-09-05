package test

import MAC._
import Switch._
import NewMac._

import chisel3._
import chisel3.stage._
import freechips.rocketchip.diplomacy._

import org.chipsalliance.cde.config._



object testModule extends App {


  // val cfg = new MacCfg

  // (new chisel3.stage.ChiselStage).execute( Array("--show-registrations", "--full-stacktrace", "--target-dir", "generated/Main", "-e", "verilog") ++ args, Seq(
  //     ChiselGeneratorAnnotation(() => {
  //   val soc = LazyModule(new MacTest()(cfg))
  //   soc.module
  // })
  // ))



  (new chisel3.stage.ChiselStage).execute( Array("--target-dir", "generated/ethernet/", "-E", "verilog" ) ++ args, Seq(
      ChiselGeneratorAnnotation(() => { new GmiiTx_AxisRx })
    ))

  (new chisel3.stage.ChiselStage).execute( Array("--target-dir", "generated/ethernet/", "-E", "verilog" ) ++ args, Seq(
      ChiselGeneratorAnnotation(() => { new GmiiRx_AxisTx })
    ))

  (new chisel3.stage.ChiselStage).execute( Array("--target-dir", "generated/ethernet/", "-E", "verilog" ) ++ args, Seq(
    ChiselGeneratorAnnotation(() => { new Core })
  ))  

  (new chisel3.stage.ChiselStage).execute( Array("--target-dir", "generated/ethernet/", "-E", "verilog" ) ++ args, Seq(
    ChiselGeneratorAnnotation(() => { new MDIOCtrl })
  ))  

  val cfg = new NewMacCfg

  (new chisel3.stage.ChiselStage).execute( Array("--show-registrations", "--full-stacktrace", "--target-dir", "generated/ethernet/", "-E", "verilog") ++ args, Seq(
      ChiselGeneratorAnnotation(() => {
    val soc = LazyModule(new MacTile()(cfg))
    soc.module
  })
  ))

// import Wrapeer._

//   val cfg = new EfConfig

//   (new chisel3.stage.ChiselStage).execute( Array("--show-registrations", "--full-stacktrace", "--target-dir", "generated/Main", "-E", "verilog") ++ args, Seq(
//       ChiselGeneratorAnnotation(() => {
//     val soc = LazyModule(new EfablessTop()(cfg))
//     soc.module
//   })
//   ))

}

