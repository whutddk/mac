package MAC

import chisel3._
import chisel3.stage._
import freechips.rocketchip.diplomacy._

import org.chipsalliance.cde.config._

abstract class MacModule(implicit val p: Parameters) extends Module with HasMacParameters { def io: Record }
abstract class MacBundle(implicit val p: Parameters) extends Bundle with HasMacParameters


case object MacParamsKey extends Field[MacSetting]


case class MacSetting(
  // isTileLink: Boolean = true
){
  // require( chn > 0 , "Error, at least one mac chn!\n" )
  // require( chn <= 16, "Error, register address between only 0x000~0x1000, 0x100 for each!\n" )
}

trait HasMacParameters {
  implicit val p: Parameters

  val macSetting = p(MacParamsKey)

  // def chn = switchSetting.chn

}

class MacCfg extends Config((_, _, _) => {
  case MacParamsKey => MacSetting()
})

class EfConfig extends Config((site, here, up) => {
  case MacParamsKey => MacSetting(
    // chn = 2,
  )
})