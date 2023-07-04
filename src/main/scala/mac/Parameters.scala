package MAC

import chisel3._
import chisel3.stage._
import freechips.rocketchip.diplomacy._

import org.chipsalliance.cde.config._

abstract class MacModule(implicit val p: Parameters) extends Module with HasMacParameters { def io: Record }
abstract class MacBundle(implicit val p: Parameters) extends Bundle with HasMacParameters


case object MacParamsKey extends Field[MacSetting]


case class MacSetting(
  isTileLink: Boolean = false
){

}

trait HasMacParameters {
  implicit val p: Parameters

  val macSetting = p(MacParamsKey)

  def isTileLink = macSetting.isTileLink
}

class MacCfg extends Config((_, _, _) => {
  case MacParamsKey => MacSetting()
})
