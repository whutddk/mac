package NewMac

import chisel3._
import chisel3.stage._
import freechips.rocketchip.diplomacy._

import org.chipsalliance.cde.config._

abstract class NewMacModule(implicit val p: Parameters) extends Module with HasNewMacParameters { def io: Record }
abstract class NewMacBundle(implicit val p: Parameters) extends Bundle with HasNewMacParameters


case object NewMacParamsKey extends Field[NewMacSetting]


case class NewMacSetting(
  // isTileLink: Boolean = true
){
  // require( chn > 0 , "Error, at least one mac chn!\n" )
  // require( chn <= 16, "Error, register address between only 0x000~0x1000, 0x100 for each!\n" )
}

trait HasNewMacParameters {
  implicit val p: Parameters

  val macSetting = p(MacParamsKey)

  // def chn = switchSetting.chn

}

class NewMacCfg extends Config((site, here, up) => {
  case NewMacParamsKey => MacSetting()
})

