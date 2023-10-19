

package MAC

import chisel3._
import chisel3.util._


    val write   = Input(Bool())
    val read    = Input(Bool())
    val clear   = Input(Bool())
    val data_in = Input(UInt(dw.W))

    val data_out = Output(UInt(dw.W))
    val almost_full = Output(Bool())
    val full = Output(Bool())
    val almost_empty = Output(Bool())
    val empty = Output(Bool())


    val cnt = Output(UInt(cntw.W))


class AsyncFifo(dw: Int, aw: Int) extends RawModule{

  def dp: Int = { var res = 1; for ( i <- 0 until aw ) { res = res * 2 }; return res }

  class AsyncFifoIO extends Bundle{
    val clockEnq = Input(Bool())
    val clockDeq = Input(Bool())
    val resetEnq = Input(Bool())
    val resetDeq = Input(Bool())

    val enq = Flipped(Decoupled(UInt(dw.W)))
    val deq = Decoupled( UInt(dw.W) )

    val almost_full = Output(Bool())
    val almost_empty = Output(Bool())

    def full  = ~enq.ready
    def empty = ~deq.valid

  }

  val io: AsyncFifoIO = IO(new AsyncFifoIO)

  val fifo = withClockAndReset( io.clockEnq.asClock, io.reset ) (Mem( dp, UInt(dw.W) ))


  val wrPtr = withClockAndReset( io.clockEnq.asClock, io.resetEnq ) (RegInit(0.U((aw+1).W)))
  val wrPrtGray = BinaryToGray(wrPtr)

  val rdPtr = withClockAndReset( io.clockDeq.asClock, io.resetDeq ) (RegInit(0.U((aw+1).W)))
  val rdPrtGray = BinaryToGray(rdPtr)

  val wrPrtGraySync = withClockAndReset( io.clockDeq.asClock, io.resetDeq ) ( ShiftRegister( wrPrtGray, 2, 0.U, true.B ) )
  val rdPrtGraySync = withClockAndReset( io.clockEnq.asClock, io.resetEnq ) ( ShiftRegister( rdPrtGray, 2, 0.U, true.B ) )

  val isEmpty = wrPrtGraySync === rdPrtGray
  val isFull  = (wrPrtGray(aw-2, 0) === rdPrtGraySync(aw-2, 0)) & (wrPrtGray.extract(aw-1) =/= rdPrtGraySync.extract(aw-1))

  io.enq.ready := ~isFull
  io.deq.valid := ~isEmpty

  withClockAndReset( io.clockEnq.asClock, io.resetEnq ){
    when( io.enq.fire ){
      fifo(wrPtr) := io.enq.bits
    }
  }

  io.deq.bits := fifo(rdPtr)


}
