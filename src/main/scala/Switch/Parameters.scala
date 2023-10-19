package Switch

import chisel3._
import chisel3.stage._
import freechips.rocketchip.diplomacy._

import org.chipsalliance.cde.config._

abstract class SwitchModule(implicit val p: Parameters) extends Module with HasSwitchParameters { def io: Record }
abstract class SwitchBundle(implicit val p: Parameters) extends Bundle with HasSwitchParameters


case object SwitchParamsKey extends Field[SwitchSetting]


case class SwitchSetting(
  // isTileLink: Boolean = true
  chn: Int = 1
){

}

trait HasSwitchParameters {
  implicit val p: Parameters

  val switchSetting = p(SwitchParamsKey)

  def chn = switchSetting.chn

}

class SwitchCfg extends Config((_, _, _) => {
  case SwitchParamsKey => SwitchSetting()
})
