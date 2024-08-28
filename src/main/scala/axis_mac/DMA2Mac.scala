package MAC

import chisel3._
import chisel3.util._
import chisel3.util.experimental._

import org.chipsalliance.cde.config._
import freechips.rocketchip.tilelink._




abstract class DMA2MacBase(edge: TLEdgeOut)(implicit p: Parameters) extends MacModule{
  def dw = 64

  class DMA2MacIO_Bundle extends Bundle{

    val srcAddress = Input( UInt(32.W) )
    val txLen = Input(UInt(8.W))
    val destAddress = Input( UInt(32.W) )
    val code = Output(UInt(8.W))
    val trigger = Input(Bool())

    val tile = new Bundle{
      val A = new DecoupledIO(new TLBundleA(edge.bundle))
      val D = Flipped(new DecoupledIO(new TLBundleD(edge.bundle)))
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

  val io = IO(new DMA2AxisIO_Bundle)

  val txFifo = Module(new Queue(UInt(dw.W), 256, useSyncReadMem = true) with InlineInstance )
  val rxFifo = Module(new Queue(UInt(dw.W), 256, useSyncReadMem = true) with InlineInstance )

  val mac = Module( new Core )


  val isTxBusy = RegInit(false.B)
  val isRxBusy = RegInit(false.B)



}

trait DMA2MacRxBuff{ this: DMA2MacBase =>


  val dataLatch = for( i <- 0 until (dw/8-1) ) yield { Reg(UInt(8.W)) }
  val latchCnt  = RegInit( 0.U(log2Ceil(dw/8).W) )

  val isRxErr = RegInit(false.B)
  val isRxEnd = RegInit(false.B)
  val isRxFifoEmpty = ~rxFifo.io.deq.valid

  mac.io.axis.tx.ready := ~(isRxBusy & isRxEnd)

  when( mac.io.axis.tx.fire ){
    when( ~isRxBusy ){
      isRxBusy := true.B
    } .elsewhen( isRxEnd & isRxFifoEmpty ){
      isRxBusy := false.B
    }
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
          Cat( dataLatch.reverse ) &
          Cat( Fill( 8*(dw/8-i), 0.U(1.W) ), Fill( 8*i, 1.U(1.W) ) )
        ) |
        mac.io.axis.tx.bits.tdata << ( i << 3 )
      )

    })

    // Mux1H(Seq(
    //   ( latchCnt === 0.U ) -> Cat( 0.U(24.W), rxMac.io.axis.bits.tdata ),
    //   ( latchCnt === 1.U ) -> Cat( 0.U(16.W), rxMac.io.axis.bits.tdata, dataLatch(0) ),
    //   ( latchCnt === 2.U ) -> Cat( 0.U(8.W),  rxMac.io.axis.bits.tdata, dataLatch(1), dataLatch(0) ),
    //   ( latchCnt === 3.U ) -> Cat( rxMac.io.axis.bits.tdata, dataLatch(2), dataLatch(1), dataLatch(0) ),
    // ))







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

  val dataEmit = Reg(UInt(dw.W))
  val emitCnt  = RegInit(0.U(log2Ceil(dw/8).W))

  val isTxErr = RegInit(false.B)
  val isTxEnd = RegInit(false.B)

  val isTxFifoEmpty = ~txFifo.io.deq.valid

  txFifo.io.deq.ready := ~isTxBusy | emitCnt.andR
  mac.io.axis.rx.valid := isTxBusy
  mac.io.axis.rx.bits.tdata := dataEmit >> (emitCnt >> 3)
  mac.io.axis.rx.bits.tlast := isTxBusy & isTxEnd & emitCnt.andR
  mac.io.axis.rx.bits.tuser := isTxErr

  when( ~isTxBusy & txFifo.io.deq.fire ){
    isTxBusy := true.B
  } .elsewhen( mac.io.axis.rx.fire & isTxEnd & emitCnt.andR ){
    isTxBusy := false.B
  }

  when( ~isTxBusy & txFifo.io.deq.fire ){
    isTxEnd := false.B
  } .elsewhen( isTxBusy & isTxFifoEmpty ){
    isTxEnd := true.B
  }

  when( ~isTxBusy & txFifo.io.deq.fire ){
    isTxErr := false.B
  } .elsewhen( (txFifo.io.enq.valid & ~txFifo.io.enq.ready) | (mac.io.axis.rx.valid & ~mac.io.axis.rx.ready) ){
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
  // val txPtr = Reg(UInt(32.W))
  val rxPtr = Reg(UInt(32.W))

  val (_, _, is_trans_done, transCnt) = edge.count(io.tile.D)

  val txPending = RegInit(false.B)

  def STATE_IDLE = 0.U
  def STATE_TXREQ = 1.U
  def STATE_TXRSP = 2.U
  def STATE_RXREQ = 3.U
  def STATE_RXRSP = 4.U


  val stateNext = Wire(UInt(3.W))
  val stateCurr = RegNext(stateNext, 0.U(3.W))


  when( txPtr === io.txLen ){
    txPending := false.B
  }.elsewhen( io.trigger ){
    txPending := true.B
  }

  stateNext := 
    Mux1H(Seq(
      (stateCurr === STATE_IDLE)    -> Mux( rxFifo.io.deq.valid, STATE_RXREQ, Mux( txPending, STATE_TXREQ, STATE_IDLE ) ),
      (stateCurr === STATE_TXREQ  ) -> Mux( io.tile.A.fire, STATE_TXRSP, STATE_TXREQ ),
      (stateCurr === STATE_TXRSP  ) -> Mux( io.tile.D.fire & is_trans_done, STATE_IDLE, STATE_TXRSP ),
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
        edge.Get(
            fromSource = 0.U,
            toAddress = io.srcAddress,
            lgSize = txLen
        )._2,
      ( stateCurr === STATE_RX ) -> 
        edge.Put(
            fromSource = 0.U,
            toAddress = io.destAddress + rxPtr,
            lgSize = log2Ceil(dw/8).U,
            data = rxFifo.io.deq.bits,
            mask = Fill( dw/8, 1.U(1.W) )
        )._2,
    ))

  when( stateCurr === STATE_IDLE & stateNext === STATE_RXREQ ){
    rxPtr := 0.U
  } .elsewhen( stateCurr === STATE_RXREQ & io.tile.A.fire ){
    rxPtr := rxPtr + (log2Ceil(dw/8)).U
  }

  val code = Reg(UInt(8.W)); io.code := code

  when( stateCurr === STATE_RXRSP & stateNext === StateIdle ){
    when( isRxErr ){
      io.interrupt := true.B
      code := "hFF".U
    } .otherwise{
      code := rxPtr
    }
  }

  when( stateCurr === STATE_TXRSP & stateNext === StateIdle ){
    when( isTxErr ){

      code := "hFE".U
    } .otherwise{

    }
  }

  io.interrupt := 
    stateNext === StateIdle & ( (stateCurr === STATE_RXRSP & isRxErr) | (stateCurr === STATE_TXRSP & isTxErr) )

}


class DMA2Mac(edge: TLEdgeOut)(implicit p: Parameters) extends DMA2MacBase
with DMA2MacRx
with DMA2MacTx
with DMA2MacTile{

  mac.io.gmii <> io.gmii

  mac.io.clkEn  := io.clkEn
  mac.io.miiSel := io.miiSel

  mac.io.ifg_delay := io.ifg_delay

  io.error_bad_frame := mac.io.error_bad_frame
  io.error_bad_fcs := mac.io.error_bad_fcs
}

