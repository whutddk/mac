package Switch

import chisel3._
import chisel3.util._

import freechips.rocketchip.regmapper._
import freechips.rocketchip.tilelink._
import org.chipsalliance.cde.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.interrupts._


class DMA_Register_Bundle extends Bundle{
  val r_TxPtr = Output(UInt(32.W))
  val r_RxPtr = Output(UInt(32.W))
  val r_TxLen = Output(UInt(16.W))
  val r_RxLen = Input (UInt(16.W))
  val triTx   = Output( Bool())
  val triRx   = Input ( Bool())
}


class DmaRegIO(implicit p: Parameters) extends SwitchBundle{

  val MacAddr = Output(UInt(48.W))

  val cfg = Vec( chn, new DMA_Register_Bundle )
}

class DmaReg(implicit p: Parameters) extends LazyModule with HasSwitchParameters{





  // DTS
  val dtsdevice = new SimpleDevice("dma",Seq("dma_0"))


  val configNode = TLRegisterNode(
    address = Seq(AddressSet(0x30001000L, 0x00000fffL)),
    device = dtsdevice,
    concurrency = 1,
    beatBytes = 32/8,
    executable = true
  )
  lazy val module = new DmaRegImp(this)
}

class DmaRegImp(outer: DmaReg)(implicit p: Parameters) extends LazyModuleImp(outer) with HasSwitchParameters{

    val io: DmaRegIO = IO(new DmaRegIO)






    val txPtr = for( i <- 0 until chn ) yield { RegInit( "h80002000".U(32.W)) }
    val rxPtr = for( i <- 0 until chn ) yield { RegInit( "h80002000".U(32.W)) }
    val txLen = for( i <- 0 until chn ) yield { RegInit( 65535.U(16.W)) }
    // val rxLen = RegInit( 65535.U(16.W))
    val triRx = for( i <- 0 until chn ) yield { RegInit(false.B) }

    for( i <- 0 until chn ) {
      io.cfg(i).r_TxPtr := txPtr(i)
      io.cfg(i).r_RxPtr := rxPtr(i)
      io.cfg(i).r_TxLen := txLen(i)      
    }

    for( i <- 0 until chn ) { when( io.cfg(i).triRx ) { triRx(i) := true.B } }




    val comMap = Seq(
      ( 0 << 2 ) ->
        RegFieldGroup("MAC_ADDR0", Some("MAC Address Register 0"), Seq(
          RegField(32)
        )),

      ( 1 << 2 ) ->
        RegFieldGroup("MAC_ADDR1", Some("MAC Address Register 1"), Seq(
          RegField(32)
        ))
    ) 
    
    val trigMap = 
      ((0 until chn).map{ i =>
        ( (10*i + 10) << 2 ) ->
          RegFieldGroup("DMATrigger", Some("Tx Control DMA"), Seq(
            RegField.w(1, RegWriteFn((valid, data) => { io.cfg(i).triTx := (valid & (data === 1.U)) ; true.B} ), RegFieldDesc("bd", s"bd$i", reset=Some(0x0))),
            RegField(1, triRx(i), RegFieldDesc("bd", s"bd$i", reset=Some(0x0)))
          ))
      })

    val lenMap = 
      (0 until chn).map{ i =>
        ( (10*i + 11) << 2 ) ->
          RegFieldGroup("TxRxDMALength", Some("Tx  RxControl DMA"), Seq(
            RegField(16, txLen(i), RegFieldDesc("txLen", "length of tx", reset=Some(65535))),
            RegField.r(16, io.cfg(i).r_RxLen, RegFieldDesc("rxLen", "length of rx")),
          ))
      }

    val txAddrMap =
      (0 until chn).map{ i =>
        ( (10*i + 12) << 2 ) ->
          RegFieldGroup("TxDMAAddress", Some("Tx Control DMA"), Seq(
            RegField(32, txPtr(i), RegFieldDesc("txPtr", "pointer of tx", reset=Some(0x80002000))),
          ))
      }

    val rxAddrMap =
      (0 until chn).map{ i =>
        ( (10*i + 13) << 2 ) ->
          RegFieldGroup("RxDMAAddress", Some("Rx Control DMA"), Seq(
            RegField(32, rxPtr(i), RegFieldDesc("rxPtr", "pointer of rx", reset=Some(0x80002000))),
          ))
      }


    val regMap = comMap ++ trigMap ++ lenMap ++ txAddrMap ++ rxAddrMap
    outer.configNode.regmap(regMap: _*)






}


