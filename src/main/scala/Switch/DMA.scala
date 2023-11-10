package Switch

import chisel3._
import chisel3.util._


import org.chipsalliance.cde.config._
import freechips.rocketchip.tilelink._


abstract class DMAMstBase(val edgeOut: TLEdgeOut)(implicit p: Parameters) extends SwitchNode{
  class MacTileLinkMasterIO extends Bundle{
    val A = Decoupled(new TLBundleA(edgeOut.bundle))
    val D = Flipped(Decoupled(new TLBundleD(edgeOut.bundle)))
  }

  class SwitchMuxIO(implicit p: Parameters) extends SwitchBundle{
    val dmaMst = new MacTileLinkMasterIO

    val cfg = Vec( chn, Flipped(new DMA_Register_Bundle) )

    val selIn  = Input(UInt( (log2Ceil(chn)).W))
    val selOut = Output(UInt((log2Ceil(chn)).W))

  }

  val io: SwitchMuxIO = IO(new SwitchMuxIO)
  val (_, _, isLastD, transDCnt) = edgeOut.count(io.dmaMst.D)

  val rxBuff = Module(new RxBuff(8))
  val txBuff = Module(new TxBuff(0))



  def stateIdle = 0.U
  def stateRx   = 1.U
  def stateTx   = 2.U

  val stateNxt = Wire(UInt(2.W))
  val stateCur = RegNext(stateNxt, stateIdle)
  val stateDMA = RegInit(0.U(2.W))

  val dmaAValid = RegInit(false.B)
  val dmaABits  = Reg(new TLBundleA(edgeOut.bundle))
  val dmaAddress = Reg( UInt(32.W) )

  val trigRxNum = Reg(UInt((log2Ceil(chn)).W))
  val trigTxNum = Reg(UInt((log2Ceil(chn)).W))

  val rxLength = RegInit(0.U)
}

trait DMAMstFSM{ this: DMAMstBase =>

  stateNxt := 
    Mux1H(Seq(
      ( stateCur === stateIdle ) -> Mux( (0 until chn).map{i => (io.cfg(i).triTx & io.cfg(i).r_TxLen > 32.U)}.foldLeft(false.B)(_|_) , stateTx, Mux( txBuff.io.deq.valid, stateRx, stateIdle ) ),
      ( stateCur === stateRx   ) -> Mux( stateDMA === 0.U, stateIdle, stateRx ),
      ( stateCur === stateTx   ) -> Mux( stateDMA === 0.U, stateIdle, stateTx ),
    ))


  when( stateNxt =/= stateIdle && stateCur === stateIdle ){
    stateDMA := 1.U
  } .elsewhen( io.dmaMst.A.fire ){
    stateDMA := 2.U
  } .elsewhen( io.dmaMst.D.fire & isLastD ){
    when( stateCur === stateTx & dmaAddress === (io.cfg(trigTxNum).r_TxPtr + io.cfg(trigTxNum).r_TxLen) ){
      stateDMA := 0.U
    } .elsewhen( stateCur === stateRx & ~txBuff.io.deq.valid  ){
      stateDMA := 0.U
    } .otherwise{
      stateDMA := 1.U
    }
  }

}


trait DMAMstTileLink{ this: DMAMstBase =>



  val mInfoValid = stateCur === stateTx

  when( txBuff.io.deq.valid & stateCur === stateIdle & stateNxt === stateRx  ){

    trigRxNum := io.selIn

    dmaAddress := io.cfg(io.selIn).r_RxPtr
  }

  for( i <- 0 until chn ){
    io.cfg(i).triRx   := txBuff.io.deq.fire & txBuff.io.deq.bits.isLast & trigRxNum === i.U
    io.cfg(i).r_RxLen := RegEnable( rxLength + 1.U, io.cfg(i).triRx )
  }

  for( i <- 0 until chn ){
    when( io.cfg(i).triTx & io.cfg(i).r_TxLen > 32.U & stateCur === stateIdle ){
      trigTxNum := i.U
      dmaAddress := io.cfg(i).r_TxPtr
      assert( stateNxt === stateTx )
    }
  }

  when( io.dmaMst.A.fire ){
    dmaAddress := dmaAddress + 1.U
  }

  txBuff.io.deq.ready := io.dmaMst.A.fire & (stateCur === stateRx)
  assert( ~(txBuff.io.deq.ready & ~txBuff.io.deq.valid) )

  when( io.dmaMst.A.fire ){
    dmaAValid := false.B
  }.elsewhen( stateDMA === 1.U & ~io.dmaMst.A.valid & stateCur === stateTx ){
    dmaAValid := true.B
    dmaABits :=
      edgeOut.Get(
        fromSource = 0.U,
        toAddress = dmaAddress,
        lgSize = log2Ceil(8/8).U,
      )._2
  } .elsewhen( stateDMA === 1.U & ~io.dmaMst.A.valid & stateCur === stateRx ) {
    dmaAValid := true.B
    dmaABits :=
      edgeOut.Put(
        fromSource = 0.U,
        toAddress = dmaAddress,
        lgSize = log2Ceil(8/8).U,
        data = txBuff.io.deq.bits.data,
        mask = "b1".U,
      )._2
  }


  io.dmaMst.A.valid := dmaAValid
  io.dmaMst.A.bits := dmaABits


  io.dmaMst.D.ready := 
    Mux1H(Seq(
      ( stateCur === stateRx ) -> true.B,
      ( stateCur === stateTx ) -> rxBuff.io.enq.ready,
    ))
}

trait DMAMstBuff{ this: DMAMstBase =>



  when( txBuff.io.deq.fire ){
    when( txBuff.io.deq.bits.isStart ){
      rxLength := 0.U
    } .otherwise{
      rxLength := rxLength + 1.U
    }
  }




  rxBuff.io.enq.valid := io.dmaMst.D.valid & stateCur === stateTx
  rxBuff.io.enq.bits.data  := io.dmaMst.D.bits.data
  rxBuff.io.enq.bits.isStart := RegEnable( dmaAddress === io.cfg(trigTxNum).r_TxPtr, io.dmaMst.A.fire)
  rxBuff.io.enq.bits.isLast  := dmaAddress === (io.cfg(trigTxNum).r_TxPtr + io.cfg(trigTxNum).r_TxLen)

  io.selOut := trigTxNum

}


abstract class DMAMst(edgeOut: TLEdgeOut)(implicit p: Parameters) extends DMAMstBase(edgeOut)
with DMAMstFSM
with DMAMstTileLink
with DMAMstBuff
