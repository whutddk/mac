package Switch

import chisel3._
import chisel3.util._

import MAC._

import org.chipsalliance.cde.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.interrupts._

import chisel3.experimental.dataview._


class SwitchIO(implicit p: Parameters) extends SwitchBundle{
  val mii = Vec( chn, new MII )

  val isLoopBack = Output(Vec( chn, Bool()))
  val asyncReset = Input(AsyncReset())
}


class Switch(implicit p: Parameters) extends LazyModule with HasSwitchParameters{

  val tlClientNode = TLClientNode(Seq(TLMasterPortParameters.v1(
    Seq(TLMasterParameters.v1(
      name = "DMA",
      sourceId = IdRange(0, chn),
    ))
  )))




  val macReg = for( i <- 0 until chn ) yield { LazyModule(new MacReg(i)) }
  val dmaReg = LazyModule(new DmaReg)

  lazy val module = new SwitchImp(this)

}

class SwitchImp(outer: Switch)(implicit p: Parameters) extends LazyModuleImp(outer) with HasSwitchParameters{
  val io = IO(new SwitchIO)
  val ( dma_bus, dma_edge ) = outer.tlClientNode.out.head

  val mac = ( 0 until chn ).map{ i =>
    Module(new MacNode)
  }

  ( 0 until chn ).map{ i =>
    mac(i).io.mii <> io.mii(i)
    mac(i).io.asyncReset := io.asyncReset
    io.isLoopBack(i) := mac(i).io.isLoopBack

    outer.macReg(i).module.io.asyncReset := io.asyncReset
    outer.macReg(i).module.io.viewAsSupertype(new Mac_Config_Bundle) <> mac(i).io.cfg
  }


  val dmaMst = Module(new DmaNode(dma_edge))

  dmaMst.io.triTx             := outer.dmaReg.module.io.triTx
  outer.dmaReg.module.io.triRx := dmaMst.io.triRx


  dmaMst.io.r_TxPtr           := outer.dmaReg.module.io.r_TxPtr
  dmaMst.io.r_RxPtr           := outer.dmaReg.module.io.r_RxPtr
  dmaMst.io.r_TxLen           := outer.dmaReg.module.io.r_TxLen
  outer.dmaReg.module.io.r_RxLen := dmaMst.io.r_RxLen

  dmaMst.io.dmaMst.D.bits  := dma_bus.d.bits
  dmaMst.io.dmaMst.D.valid := dma_bus.d.valid
  dma_bus.d.ready := dmaMst.io.dmaMst.D.ready
  dma_bus.a.valid := dmaMst.io.dmaMst.A.valid
  dma_bus.a.bits  := dmaMst.io.dmaMst.A.bits
  dmaMst.io.dmaMst.A.ready := dma_bus.a.ready




  val robin = Module(new Robin)

  // dmaMst.ex.tx <> 
  //  <> mac(0).ex.tx

  for( i <- 0 until chn ){
    mac(i).ex.rx <> robin.io.enq(i).rx
    mac(i).ex.mInfo <> robin.io.enq(i).mInfo
  }

  dmaMst.ex.rx    <> robin.io.enq(chn+1-1).rx
  dmaMst.ex.mInfo <> robin.io.enq(chn+1-1).mInfo


  val destTable = Wire( Vec( chn+1, Vec( 4, UInt(48.W) ) ) )
  for( i <- 0 until chn+1 ){
    destTable(i)(0) := "h102030405A00".U + i.U
    destTable(i)(1) := "h102030405B00".U + i.U
    destTable(i)(2) := "h102030405C00".U + i.U
    destTable(i)(3) := "h102030405D00".U + i.U
  }



  val isHit = Reg(Bool())
  val destChn = Reg(UInt((log2Ceil(chn+1)).W))
  val isMuxBusy = RegInit(false.B)


  when( robin.io.deq.rx.fire & robin.io.deq.rx.bits.isLast ){
    assert( isMuxBusy )
    isMuxBusy := false.B
  } .elsewhen( robin.io.deq.mInfo.dest.valid ){
    assert( ~isMuxBusy )
    isMuxBusy := true.B


    isHit   := false.B
    for( i <- 0 until chn+1 ){
      when( 
        robin.io.deq.mInfo.dest.bits === destTable(i)(0) ||
        robin.io.deq.mInfo.dest.bits === destTable(i)(1) ||
        robin.io.deq.mInfo.dest.bits === destTable(i)(2) ||
        robin.io.deq.mInfo.dest.bits === destTable(i)(3)
      ){
        isHit   := true.B
        destChn := i.U
      }
    }

  }

  val tempTxPort = Wire( Vec( chn+1, Decoupled(new Mac_Stream_Bundle) ) )
  for( i <- 0 until chn ){
    mac(i).ex.tx.valid := tempTxPort(i).valid
    mac(i).ex.tx.bits  := tempTxPort(i).bits
    tempTxPort(i).ready := mac(i).ex.tx.ready
  }
    dmaMst.ex.tx.valid := tempTxPort(chn+1-1).valid
    dmaMst.ex.tx.bits  := tempTxPort(chn+1-1).bits
    tempTxPort(chn+1-1).ready := dmaMst.ex.tx.ready
  

  val allTxReady = tempTxPort.map{ x => x.ready }.foldLeft(true.B)(_&_)


  for( i <- 0 until chn+1 ){
    tempTxPort(chn+1-1).valid := false.B
    tempTxPort(chn+1-1).bits  := 0.U.asTypeOf(new Mac_Stream_Bundle)
  }
  robin.io.deq.rx.ready := false.B

  when( isMuxBusy ){
    when( isHit ){
      tempTxPort(destChn).valid := robin.io.deq.rx.valid
      tempTxPort(destChn).bits  := robin.io.deq.rx.bits
      robin.io.deq.rx.ready := tempTxPort(destChn).ready
    } .otherwise{
      for( i <- 0 until chn+1 ){
        tempTxPort(i).valid := robin.io.deq.rx.valid & allTxReady
        tempTxPort(i).bits  := robin.io.deq.rx.bits
      }
      robin.io.deq.rx.ready := allTxReady
    }
  }

}

