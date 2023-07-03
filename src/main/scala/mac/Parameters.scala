package MAC

import chisel3._
import chisel3.stage._
import freechips.rocketchip.diplomacy._

import org.chipsalliance.cde.config._

case object MacParamsKey extends Field[MacSetting]


case class MacSetting(){

}

trait HasMacParameters {
  implicit val p: Parameters

  val riftSetting = p(MacParamsKey)
}

class MacCfg extends Config((_, _, _) => {
  case MacParamsKey => MacSetting()
})