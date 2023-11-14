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
  chn: Int = 3
){
  require( chn > 0 , "Error, at least one mac chn!\n" )
  require( chn <= 16, "Error, register address between only 0x000~0x1000, 0x100 for each!\n" )
}

trait HasSwitchParameters {
  implicit val p: Parameters

  val switchSetting = p(SwitchParamsKey)

  def chn = switchSetting.chn

}

class SwitchCfg extends Config((_, _, _) => {
  case SwitchParamsKey => SwitchSetting()
})

class EfConfig extends Config((site, here, up) => {
  case SwitchParamsKey => SwitchSetting(
    chn = 2,
  )
})