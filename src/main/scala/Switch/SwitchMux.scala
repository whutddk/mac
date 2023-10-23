package Switch

import chisel3._
import chisel3.util._


import org.chipsalliance.cde.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._




class SwitchMux(edgeOut: TLEdgeOut)(implicit p: Parameters) extends SwitchModule{

  class MacTileLinkMasterIO extends Bundle{
    val A = Decoupled(new TLBundleA(edgeOut.bundle))
    val D = Flipped(Decoupled(new TLBundleD(edgeOut.bundle)))
  }

  class SwitchMuxIO(implicit p: Parameters) extends SwitchBundle{
    val dmaMst = new MacTileLinkMasterIO

    val triTx = Input(Bool())

    val rxEnq = Vec(chn, Flipped(new Receive_Enq_Bundle))
    val txDeq = Vec(chn, new Transmit_Bundle)

  }

  val io: SwitchMuxIO = IO(new SwitchMuxIO)
  val (_, _, isLastD, transDCnt) = edgeOut.count(io.dmaMst.D)

  val rxBuff = Module(new RxBuff)
  rxBuff.io.enq <> io.rxEnq(0)

  val txBuff = Module(new TxBuff)
  txBuff.io.deq <> io.txDeq(0)

  def stateIdle = 0.U
  def stateRx   = 1.U
  def stateTx   = 2.U

  val stateNxt = Wire(UInt(2.W))
  val stateCur = RegNext(stateNxt, stateIdle)

  stateNxt := 
    Mux1H(Seq(
      ( stateCur === stateIdle ) -> Mux( rxBuff.io.deq.header.fire, stateRx, Mux( txBuff.io.enq.req.fire, stateTx, stateIdle ) ),
      ( stateCur === stateRx   ) -> Mux( rxBuff.io.deq.ctrl.fire, stateIdle, stateRx ),
      ( stateCur === stateTx   ) -> Mux( txBuff.io.enq.resp.fire, stateIdle, stateTx ),
    ))


    def ptr = "h80002000".U
    def txLength       = 64.U
    def PerPacketCrcEn = true.B
    def PerPacketPad   = true.B






  rxBuff.io.deq.ctrl.ready := stateCur === stateRx & io.dmaMst.D.fire & isLastD


  val txReqValid = RegInit(false.B)

  when( txBuff.io.enq.req.fire ){
    txReqValid := false.B
  } .elsewhen( io.triTx ){
    txReqValid := true.B
  }

  txBuff.io.enq.req.valid := txReqValid && stateCur === stateIdle
  txBuff.io.enq.req.bits.txLength := txLength
  txBuff.io.enq.req.bits.PerPacketCrcEn := PerPacketCrcEn
  txBuff.io.enq.req.bits.PerPacketPad := PerPacketPad




  txBuff.io.enq.resp.ready := true.B





  val dmaTxAValid = RegInit(false.B)
  val dmaTxABits  = Reg(new TLBundleA(edgeOut.bundle))


  val dmaAddress = Reg( UInt(32.W) )

  when( stateCur === stateIdle ){
    dmaAddress := ptr
  } .elsewhen( io.dmaMst.A.fire ){
    dmaAddress := dmaAddress + 1.U
  }

  rxBuff.io.deq.data.ready := io.dmaMst.A.fire & (stateCur === stateRx)
  assert( ~(rxBuff.io.deq.data.ready & ~rxBuff.io.deq.data.valid) )

  rxBuff.io.deq.header.ready := ~io.triTx && ~txReqValid

  when( io.dmaMst.A.fire ){
    dmaTxAValid := false.B
  } .elsewhen( txBuff.io.enq.req.fire | (stateCur === stateTx & io.dmaMst.D.fire & isLastD & dmaAddress < (ptr + txLength)) ){
    dmaTxAValid := true.B
    dmaTxABits :=
      edgeOut.Get(
        fromSource = 0.U,
        toAddress = dmaAddress,
        lgSize = log2Ceil(8/8).U,
      )._2
  } .elsewhen( rxBuff.io.deq.header.fire | (stateCur === stateRx & io.dmaMst.D.fire & isLastD & rxBuff.io.deq.data.valid ) ) {
    dmaTxAValid := true.B
    dmaTxABits :=
      edgeOut.Put(
        fromSource = 0.U,
        toAddress = dmaAddress,
        lgSize = log2Ceil(8/8).U,
        data = rxBuff.io.deq.data.bits,
        mask = "b1".U,
      )._2
  }


  io.dmaMst.A.valid := dmaTxAValid
  io.dmaMst.A.bits := dmaTxABits



  txBuff.io.enq.data.valid := io.dmaMst.D.valid & stateCur === stateTx
  txBuff.io.enq.data.bits  := io.dmaMst.D.bits.data


  io.dmaMst.D.ready := 
    Mux1H(Seq(
      ( stateCur === stateRx ) -> true.B,
      ( stateCur === stateTx ) -> txBuff.io.enq.data.ready,
    ))



}

