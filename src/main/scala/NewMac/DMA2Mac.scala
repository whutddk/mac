package NewMac

import chisel3._
import chisel3.util._
import chisel3.util.experimental._

import org.chipsalliance.cde.config._
import freechips.rocketchip.tilelink._




abstract class DMA2MacBase(edge: TLEdgeOut)(implicit p: Parameters) extends NewMacModule{
  def dw = 64
  val dmaEdge: TLEdgeOut = edge

  class DMA2MacIO_Bundle extends Bundle{

    val srcAddress = Input( UInt(32.W) )
    val txLen = Input(UInt(8.W))
    val destAddress = Input( UInt(32.W) )
    val code = Output(UInt(8.W))
    val trigger = Input(Bool())

    val tile = new Bundle{
      val A = new DecoupledIO(new TLBundleA(dmaEdge.bundle))
      val D = Flipped(new DecoupledIO(new TLBundleD(dmaEdge.bundle)))
    }



    val gmii = new Bundle{
      val tx = Output(new GMII_TX_Bundle)
      val rx = Input(new GMII_RX_Bundle)
    }

    val clkEn = Input(Bool())
    val miiSel = Input(Bool())

    val ifg_delay = Input(UInt(8.W))

    val error_bad_frame = Output(Bool())
    val error_bad_fcs = Output(Bool())
    
    val interrupt = Output(Bool())


  }

  val io = IO(new DMA2MacIO_Bundle)

  val txFifo = Module(new Queue(UInt(dw.W), 256, useSyncReadMem = true) with InlineInstance )
  val rxFifo = Module(new Queue(UInt(dw.W), 256, useSyncReadMem = true) with InlineInstance )

  val mac = Module( new Core )


  val isTxBusy = RegInit(false.B)
  val isRxBusy = RegInit(false.B)

  val dataLatch = for( i <- 0 until (dw/8-1) ) yield { Reg(UInt(8.W)) }
  val latchCnt  = RegInit( 0.U(log2Ceil(dw/8).W) )

  val isRxErr = RegInit(false.B)
  val isRxEnd = RegInit(false.B)
  val isRxFifoEmpty = ~rxFifo.io.deq.valid

  val dataEmit = Reg(UInt(dw.W))
  val emitCnt  = RegInit(0.U(log2Ceil(dw/8).W))

  val isTxErr = RegInit(false.B)
  val isTxEnd = RegInit(false.B)

  val isTxFifoEmpty = ~txFifo.io.deq.valid


  def STATE_IDLE = 0.U
  def STATE_TXREQ = 1.U
  def STATE_TXRSP = 2.U
  def STATE_RXREQ = 3.U
  def STATE_RXRSP = 4.U


  val stateNext = Wire(UInt(3.W))
  val stateCurr = RegNext(stateNext, 0.U(3.W))


}

trait DMA2MacRxBuff{ this: DMA2MacBase =>




  mac.io.axis.tx.ready := ~(isRxBusy & isRxEnd)

  when( mac.io.axis.tx.fire & ~isRxBusy ){
    isRxBusy := true.B
  } .elsewhen( isRxEnd & isRxFifoEmpty ){
    isRxBusy := false.B
  }


  when( mac.io.axis.tx.fire ){
    when( ~isRxBusy ){
      isRxErr := false.B
    } .elsewhen( ~isRxErr ){
      isRxErr := mac.io.axis.tx.bits.tuser
    }
  }

  when( mac.io.axis.tx.fire ){
    when( ~isRxBusy ){
      isRxEnd  := false.B
    } .elsewhen( ~isRxEnd ){
      isRxEnd  := mac.io.axis.tx.bits.tlast
    }
  }

  rxFifo.io.enq.valid :=
    mac.io.axis.tx.fire & ( mac.io.axis.tx.bits.tlast | latchCnt.andR)

  rxFifo.io.enq.bits  := 
    Mux1H( ( 0 until dw/8 ).map{i =>


      ( latchCnt === i.U ) -> (
        (
          Cat(mac.io.axis.tx.bits.tdata, Cat( (0 until i).map{ j => dataLatch(j) }.reverse ))
        )
      )


    })







  when( mac.io.axis.tx.fire ){
    when( ~isRxBusy ){
      latchCnt := 1.U
      dataLatch(0) := mac.io.axis.tx.bits.tdata
    } .otherwise{
      when( ~mac.io.axis.tx.bits.tlast ){
        latchCnt := latchCnt + 1.U

        for( i <- 0 until dw/8-1 ){
          when( latchCnt === i.U ){
            dataLatch(i) := mac.io.axis.tx.bits.tdata
          }
        }

        // when( latchCnt === 0.U ){
        //   dataLatch(0) := rxMac.io.axis.bits.tdata
        // } .elsewhen( latchCnt === 1.U ){
        //   dataLatch(1) := rxMac.io.axis.bits.tdata
        // } .elsewhen( latchCnt === 2.U ){
        //   dataLatch(2) := rxMac.io.axis.bits.tdata
        // } .elsewhen( latchCnt === 3.U ){
        // }

      } .otherwise{
        latchCnt := 0.U

        for( i <- 0 until dw/8-1 ){
          dataLatch(i) := 0.U
        }
        // dataLatch(0) := 0.U
        // dataLatch(1) := 0.U
        // dataLatch(2) := 0.U
      }

    }

  }







}


trait DMA2MacTxBuff{ this: DMA2MacBase =>


  txFifo.io.deq.ready := ~isTxBusy | emitCnt.andR
  mac.io.axis.rx.valid := isTxBusy
  mac.io.axis.rx.bits.tdata := dataEmit >> (emitCnt << 3)
  mac.io.axis.rx.bits.tlast := isTxBusy & isTxEnd & emitCnt.andR & isTxFifoEmpty
  mac.io.axis.rx.bits.tuser := isTxErr

  when( ~isTxBusy & txFifo.io.deq.fire ){
    isTxBusy := true.B
  } .elsewhen( mac.io.axis.rx.fire & isTxEnd & emitCnt.andR & isTxFifoEmpty ){
    isTxBusy := false.B
  }

  when( ~isTxBusy & txFifo.io.deq.fire ){
    isTxEnd := false.B
  } .elsewhen( isTxBusy & stateCurr === STATE_IDLE ){
    isTxEnd := true.B
  }

  when( ~isTxBusy & txFifo.io.deq.fire ){
    isTxErr := false.B
  } .elsewhen( (txFifo.io.enq.valid & ~txFifo.io.enq.ready) | (~mac.io.axis.rx.valid & mac.io.axis.rx.ready) ){
    isTxErr := true.B
  }

  when( txFifo.io.deq.fire ){
    emitCnt := 0.U
    dataEmit := txFifo.io.deq.bits
  } .elsewhen( mac.io.axis.rx.fire ){
    emitCnt := emitCnt + 1.U
  }


}

trait DMA2MacTile{ this: DMA2MacBase =>
  val txPtr = Reg(UInt(32.W))
  val rxPtr = Reg(UInt(32.W))

  // val (_, _, is_trans_done, transCnt) = dmaEdge.count(io.tile.D)

  val txPending = RegInit(false.B)




  when( stateCurr === STATE_TXRSP & stateNext === STATE_IDLE ){
    txPending := false.B
  }.elsewhen( io.trigger ){
    txPending := true.B
  }

  stateNext := 
    Mux1H(Seq(
      (stateCurr === STATE_IDLE)    -> Mux( rxFifo.io.deq.valid, STATE_RXREQ, Mux( txPending & ~isTxBusy, STATE_TXREQ, STATE_IDLE ) ),
      (stateCurr === STATE_TXREQ  ) -> Mux( io.tile.A.fire, STATE_TXRSP, STATE_TXREQ ),
      (stateCurr === STATE_TXRSP  ) -> Mux( io.tile.D.fire , Mux( txPtr === io.txLen , STATE_IDLE, STATE_TXREQ), STATE_TXRSP ),
      (stateCurr === STATE_RXREQ  ) -> Mux( io.tile.A.fire, STATE_RXRSP, STATE_RXREQ ),
      (stateCurr === STATE_RXRSP  ) -> Mux( io.tile.D.fire, Mux( isRxEnd & isRxFifoEmpty, STATE_IDLE, STATE_RXREQ), STATE_RXRSP ),

    ))

  txFifo.io.enq.valid := 
    stateCurr === STATE_TXRSP & io.tile.D.valid

  txFifo.io.enq.bits := io.tile.D.bits.data

  io.tile.D.ready := 
    (stateCurr === STATE_TXRSP & txFifo.io.enq.ready) |
    (stateCurr === STATE_RXRSP)



  io.tile.A.valid :=
    (stateCurr === STATE_TXREQ) |
    (stateCurr === STATE_RXREQ & rxFifo.io.deq.valid)

  rxFifo.io.deq.ready := stateCurr === STATE_RXREQ & io.tile.A.ready

  io.tile.A.bits :=
    Mux1H(Seq(
      ( stateCurr === STATE_TXREQ ) -> 
        dmaEdge.Get(
            fromSource = 0.U,
            toAddress = io.srcAddress + (txPtr << log2Ceil(dw/8)),
            lgSize = log2Ceil(dw/8).U
        )._2,
      ( stateCurr === STATE_RXREQ ) -> 
        dmaEdge.Put(
            fromSource = 0.U,
            toAddress = io.destAddress + (rxPtr << log2Ceil(dw/8)),
            lgSize = log2Ceil(dw/8).U,
            data = rxFifo.io.deq.bits,
            mask = Fill( dw/8, 1.U(1.W) )
        )._2,
    ))

  when( stateCurr === STATE_IDLE & stateNext === STATE_TXREQ ){
    txPtr := 0.U
  } .elsewhen( stateCurr === STATE_TXREQ & io.tile.A.fire ){
    txPtr := txPtr + 1.U
  }


  when( stateCurr === STATE_IDLE & stateNext === STATE_RXREQ ){
    rxPtr := 0.U
  } .elsewhen( stateCurr === STATE_RXREQ & io.tile.A.fire ){
    rxPtr := rxPtr + 1.U
  }

  val code = Reg(UInt(8.W)); io.code := code

  when( stateCurr === STATE_RXRSP & stateNext === STATE_IDLE ){
    when( isRxErr ){
      io.interrupt := true.B
      code := "hFF".U
    } .otherwise{
      code := rxPtr
    }
  }

  when( stateCurr === STATE_TXRSP & stateNext === STATE_IDLE ){
    when( isTxErr ){

      code := "hFE".U
    } .otherwise{

    }
  }

  io.interrupt := 
    stateNext === STATE_IDLE & ( (stateCurr === STATE_RXRSP & isRxErr) | (stateCurr === STATE_TXRSP & isTxErr) )

}


class DMA2Mac(edge: TLEdgeOut)(implicit p: Parameters) extends DMA2MacBase(edge)
with DMA2MacRxBuff
with DMA2MacTxBuff
with DMA2MacTile{

  mac.io.gmii <> io.gmii

  mac.io.clkEn  := io.clkEn
  mac.io.miiSel := io.miiSel

  mac.io.ifg_delay := io.ifg_delay

  io.error_bad_frame := mac.io.error_bad_frame
  io.error_bad_fcs := mac.io.error_bad_fcs
}

