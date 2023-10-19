package Switch

import chisel3._
import chisel3.util._







class SwitchMux(edgeOut: TLEdgeOut)(implicit p: Parameters) extends SwitchModule{

  class MacTileLinkMasterIO extends Bundle{
    val A = Decoupled(new TLBundleA(edgeOut.bundle))
    val D = Flipped(Decoupled(new TLBundleD(edgeOut.bundle)))
  }

  class SwitchMuxIO(implicit p: Parameters) extends SwitchBundle{
    val dmaMst = new MacTileLinkMasterIO

    val triTx = Input(Bool())

    val rxEnq = Vec(chn, new Receive_Enq_Bundle)
    val txDeq = Vec(chn, Flipped(new Transmit_Deq_Bundle))

  }

  val io: SwitchMuxIO = IO(new SwitchMuxIO)


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


    def ptr = 0x8002000
    def txLength       = 8
    def PerPacketCrcEn = true.B
    def PerPacketPad   = true.B



  val rxBuff = Module(new RxBuff)
  rxBuff.io.enq <> io.rxEnq(0)




  val txBuff = Module(new TxBuff)
  txBuff.io.deq <> mac.io.txDeq(0)


  rxBuff.io.deq.ctrl.ready := stateCur === stateRx


  val txReqValid = RegInit(false.B)

  when( txBuff.io.enq.req.fire ){
    txReqValid := false.B
  } .elsewhen( io.triTx ){
    txReqValid := true.B
  }

  txBuff.io.enq.req.valid := txReqValid && stateCur === stateIdle
  txBuff.io.enq.bits.txLength := txLength
  txBuff.io.enq.bits.PerPacketCrcEn := PerPacketCrcEn
  txBuff.io.enq.bits.PerPacketPad := PerPacketPad




  txBuff.io.enq.resp.ready := true.B





  val dmaTxAValid = RegInit(false.B)
  val dmaTxABits  = Reg(new TLBundleA(edgeOut.bundle))


  val dmaAddress = Reg( UInt(32.W) )

  when( stateCur === stateIdle && stateNxt =/= stateIdle ){
    dmaAddress := ptr.U
  } .elsewhen( io.A.fire ){
    dmaAddress := dmaAddress + 4.U
  }

  rxBuff.io.deq.data.ready := io.A.ready & stateCur === stateRx
  rxBuff.io.deq.header.ready := ~io.io.triTx && ~txReqValid

  when( io.A.fire ){
    dmaTxAValid := false.B
  } .elsewhen( txBuff.io.enq.req.fire ){
    dmaTxAValid := true.B
    dmaTxABits :=
      edgeOut.Get(
        fromSource = 0.U,
        toAddress = dmaAddress >> 2 << 2,
        lgSize = log2Ceil(txLength).U,
      )._2
  }


  io.A.valid :=
    Mux1H(Seq(
      stateCur === stateRx -> rxBuff.io.deq.data.valid,
      stateCur === stateTx -> dmaTxAValid,
    ))

  when( stateCur === stateRx ){
    io.A.bits := 
      edgeOut.Put(
        fromSource = 0.U,
        toAddress = dmaAddress >> 2 << 2,
        lgSize = log2Ceil(32/8).U,
        data = rxBuff.io.deq.data.bits,
        mask = "b1111".U,
      )._2    
  } .elsewhen( stateCur === stateTx ){
    io.A.bits := dmaTxABits
  } .otherwise{
    io.A.bits := DontCare
  }


  txBuff.io.enq.data.valid := io.D.valid & stateCur === stateTx
  txBuff.io.enq.data.bits  := io.D.bits.data


  io.D.ready := 
    Mux1H(Seq(
      stateCur === stateRx -> true.B,
      stateCur === stateTx -> txBuff.io.enq.data.ready,
    ))



}


