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

    val triTx = Input(Bool())
    val triRx = Output(Bool())
    val r_TxPtr = Input(UInt(32.W))
    val r_RxPtr = Input(UInt(32.W))
    val r_TxLen = Input(UInt(16.W))
    val r_RxLen = Output(UInt(16.W))



  }

  val io: SwitchMuxIO = IO(new SwitchMuxIO)
  val (_, _, isLastD, transDCnt) = edgeOut.count(io.dmaMst.D)

  val rxBuff = Module(new RxBuff(8))
  val txBuff = Module(new TxBuff(0))
  rxBuff.headerIO.ready := true.B


  def stateIdle = 0.U
  def stateRx   = 1.U
  def stateTx   = 2.U

  val stateNxt = Wire(UInt(2.W))
  val stateCur = RegNext(stateNxt, stateIdle)
  val stateDMA = RegInit(0.U(2.W))

  val dmaAValid = RegInit(false.B)
  val dmaABits  = Reg(new TLBundleA(edgeOut.bundle))
  val dmaAddress = Reg( UInt(32.W) )
}

trait DMAMstFSM{ this: DMAMstBase =>

  stateNxt := 
    Mux1H(Seq(
      ( stateCur === stateIdle ) -> Mux( (io.triTx & io.r_TxLen > 32.U), stateTx, Mux( rxBuff.io.deq.valid, stateRx, stateIdle ) ),
      ( stateCur === stateRx   ) -> Mux( stateDMA === 0.U, stateIdle, stateRx ),
      ( stateCur === stateTx   ) -> Mux( stateDMA === 0.U, stateIdle, stateTx ),
    ))


  when( stateNxt =/= stateIdle && stateCur === stateIdle ){
    stateDMA := 1.U
  } .elsewhen( io.dmaMst.A.fire ){
    stateDMA := 2.U
  } .elsewhen( io.dmaMst.D.fire & isLastD ){
    when( stateCur === stateTx & dmaAddress === (io.r_TxPtr + io.r_TxLen) ){
      stateDMA := 0.U
    } .elsewhen( stateCur === stateRx & ~rxBuff.io.deq.valid  ){
      stateDMA := 0.U
    } .otherwise{
      stateDMA := 1.U
    }
  }

}


trait DMAMstTileLink{ this: DMAMstBase =>





  when( stateCur === stateIdle & stateNxt === stateTx ){
    dmaAddress := io.r_TxPtr
  } .elsewhen( stateCur === stateIdle & stateNxt === stateRx ){
    dmaAddress := io.r_RxPtr
  } .elsewhen( io.dmaMst.A.fire ){
    dmaAddress := dmaAddress + 1.U
  }

  rxBuff.io.deq.ready := io.dmaMst.A.fire & (stateCur === stateRx)
  assert( ~(rxBuff.io.deq.ready & ~rxBuff.io.deq.valid) )

  // rxBuff.io.header.ready := ~(io.triTx & io.r_TxLen > 32.U) & stateCur === stateIdle

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
        data = rxBuff.io.deq.bits.data,
        mask = "b1".U,
      )._2
  }


  io.dmaMst.A.valid := dmaAValid
  io.dmaMst.A.bits := dmaABits


  io.dmaMst.D.ready := 
    Mux1H(Seq(
      ( stateCur === stateRx ) -> true.B,
      ( stateCur === stateTx ) -> txBuff.io.enq.ready,
    ))
}

trait DMAMstBuff{ this: DMAMstBase =>

  val rxLength = RegInit(0.U)

  when( rxBuff.io.deq.fire ){
    when( rxBuff.io.deq.bits.isStart ){
      rxLength := 0.U
    } .otherwise{
      rxLength := rxLength + 1.U
    }
  }

  io.triRx := rxBuff.io.deq.fire & rxBuff.io.deq.bits.isLast
  io.r_RxLen := RegEnable( rxLength, io.triRx )


  txBuff.io.enq.valid := io.dmaMst.D.valid & stateCur === stateTx
  txBuff.io.enq.bits.data  := io.dmaMst.D.bits.data
  txBuff.io.enq.bits.isStart := RegEnable( dmaAddress === io.r_TxPtr, io.dmaMst.A.fire)
  txBuff.io.enq.bits.isLast  := dmaAddress === (io.r_TxPtr + io.r_TxLen)
}


abstract class DMAMst(edgeOut: TLEdgeOut)(implicit p: Parameters) extends DMAMstBase(edgeOut)
with DMAMstFSM
with DMAMstTileLink
with DMAMstBuff
