package test

import MAC._
import BACK._
import Switch._

import chisel3._
import chisel3.stage._
import freechips.rocketchip.diplomacy._

import org.chipsalliance.cde.config._



object testModule extends App {


  val cfg = new MacCfg

  (new chisel3.stage.ChiselStage).execute( Array("--show-registrations", "--full-stacktrace", "--target-dir", "generated/Main", "-e", "verilog") ++ args, Seq(
      ChiselGeneratorAnnotation(() => {
    val soc = LazyModule(new MacTest()(cfg))
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

