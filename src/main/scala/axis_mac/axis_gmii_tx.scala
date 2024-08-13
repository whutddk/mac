package MAC

import chisel3._
import chisel3.util._

class GMII_TX_Bundle extends Bundle{
  val txd = UInt(8.W)
  val tx_en = Bool()
  val tx_er = Bool()
}

class AXIS_Bundle extends Bundle{
  val tdata = UInt(8.W)
  val tlast = Bool()
  val tuser = Bool()
}



class GmiiTx_AxisRx_IO extends Bundle{
  val gmii = Output(new GMII_TX_Bundle)
  val axis = Flipped(Decoupled(new AXIS_Bundle))

  val clkEn = Input(Bool())
  val miiSel = Input(Bool())

  val ifg_delay = Input(UInt(8.W))

}


/*
 * AXI4-Stream GMII frame transmitter (AXI in, GMII out)
 */
class GmiiTx_AxisRx extends Module{
  val io = IO(new GmiiTx_AxisRx_IO)


  val crcUnit = Module(new crc32_8)

  def STATE_IDLE  = 0.U
  def STATE_PREAMBLE = 1.U
  def STATE_PAYLOAD = 2.U
  def STATE_LAST = 3.U
  def STATE_PAD = 4.U
  def STATE_FCS = 5.U
  def STATE_WAIT = 6.U
  def STATE_IFG = 7.U

  val ETH_PRE = "h55".U
  val ETH_SFD = "hD5".U

  def ENABLE_PADDING = true.B
  def MIN_FRAME_LENGTH = 64.U

  val mii_odd = Reg(Bool())
  val mii_msn = Reg(UInt(4.W))

  val stateNext = Wire(UInt(3.W))
  val stateCurr = RegEnable( stateNext, 0.U, io.clkEn & ~(io.miiSel && mii_odd) )

  val s_tdata = Reg(UInt(8.W))
  val frame_ptr = RegInit(0.U(16.W))
  val s_axis_tready = RegInit(false.B); io.axis.ready := s_axis_tready

  val crcOut = crcUnit.io.crc
  val fcs = RegEnable( crcOut, "hFFFFFFFF".U(32.W), io.clkEn & ~(io.miiSel & mii_odd) & stateCurr === STATE_FCS & frame_ptr === 3.U )
  val ifg = Reg(UInt(8.W))


  stateNext := Mux1H(Seq(
    (stateCurr === STATE_IDLE)     -> ( Mux( io.axis.valid, STATE_PREAMBLE, STATE_IDLE )),
    (stateCurr === STATE_PREAMBLE) -> ( Mux( frame_ptr === 7.U, STATE_PAYLOAD, STATE_PREAMBLE )),
    (stateCurr === STATE_PAYLOAD)  -> ( Mux( io.axis.valid, Mux( io.axis.bits.tlast, Mux( io.axis.bits.tuser, STATE_IFG, STATE_LAST ), STATE_PAYLOAD ),STATE_WAIT ) ),
    (stateCurr === STATE_LAST)     -> ( Mux( ENABLE_PADDING && frame_ptr < MIN_FRAME_LENGTH-5.U, STATE_PAD, STATE_FCS )),
    (stateCurr === STATE_PAD)      -> ( Mux( (frame_ptr < MIN_FRAME_LENGTH-5.U), STATE_PAD, STATE_FCS )),
    (stateCurr === STATE_FCS)      -> ( Mux( frame_ptr < 3.U, STATE_FCS, STATE_IFG )),
    (stateCurr === STATE_WAIT)     -> ( Mux( io.axis.valid & io.axis.bits.tlast, STATE_IFG, STATE_WAIT ) ),
    (stateCurr === STATE_IFG)      -> ( Mux( ifg < io.ifg_delay-1.U, STATE_IFG, STATE_IDLE )),
  ))












  when( io.clkEn & ~(io.miiSel & mii_odd) ){
    when( stateCurr === STATE_IDLE ){
      assert( frame_ptr === 0.U )
      when( io.axis.valid ){
        frame_ptr := 1.U
        s_axis_tready := false.B
      }
    } .elsewhen( stateCurr ===  STATE_PREAMBLE ){
      frame_ptr := frame_ptr + 1.U
      when( frame_ptr === 6.U ){
        assert( io.axis.valid === true.B )
        s_axis_tready := true.B
        s_tdata := io.axis.bits.tdata
      } .elsewhen( frame_ptr === 7.U ){
        frame_ptr := 0.U
        s_axis_tready := true.B
        s_tdata := io.axis.bits.tdata
      }
    } .elsewhen( stateCurr === STATE_PAYLOAD ){
      
      s_tdata := io.axis.bits.tdata

      when( io.axis.valid ){
        s_axis_tready := true.B
        frame_ptr := frame_ptr + 1.U
        when( io.axis.bits.tlast ){
          s_axis_tready := false.B    
          when( io.axis.bits.tuser ){
            frame_ptr := 0.U
          }                    
        }
      } .otherwise{
        s_axis_tready := true.B
        frame_ptr := 0.U
      }
    } .elsewhen( stateCurr === STATE_LAST ){
      frame_ptr := frame_ptr + 1.U
      when(ENABLE_PADDING && frame_ptr < MIN_FRAME_LENGTH-5.U){
        s_tdata := 0.U
      } .otherwise{
        frame_ptr := 0.U
      }
    } .elsewhen( stateCurr === STATE_PAD ){
      assert( frame_ptr <= MIN_FRAME_LENGTH-5.U )

      frame_ptr := frame_ptr + 1.U
      s_tdata := 0.U

      when( frame_ptr === MIN_FRAME_LENGTH-5.U ){
        frame_ptr := 0.U
      }
    } .elsewhen( stateCurr === STATE_FCS ){
      assert( frame_ptr <= 3.U )
      frame_ptr := frame_ptr + 1.U
      when( frame_ptr === 3.U ){
        frame_ptr := 0.U
      }
    } .elsewhen( stateCurr === STATE_WAIT ){
      frame_ptr := frame_ptr + 1.U
      s_axis_tready := true.B
      when( io.axis.valid & io.axis.bits.tlast ){
        s_axis_tready := false.B
      }
    } .elsewhen( stateCurr === STATE_IFG ){
      frame_ptr := frame_ptr + 1.U
    }
  }





val reset_crc  = io.clkEn & ~(io.miiSel & mii_odd) & ( stateCurr === STATE_IDLE | stateCurr === STATE_PREAMBLE | stateCurr === STATE_WAIT | stateCurr === STATE_IFG )
val update_crc = io.clkEn & ~(io.miiSel & mii_odd) & ( stateCurr === STATE_PAYLOAD | stateCurr === STATE_LAST | stateCurr === STATE_PAD )






  crcUnit.io.isEnable := update_crc
  crcUnit.io.dataIn := s_tdata
  crcUnit.reset := reset.asBool | reset_crc




when( io.clkEn & ~(io.miiSel & mii_odd) ){
  when( stateCurr === STATE_WAIT ){
    ifg := 0.U
  } .elsewhen( stateCurr === STATE_IFG ){
    ifg := ifg + 1.U
  }
}














val gmii_txd = Reg(UInt(8.W));     io.gmii.txd := gmii_txd
val gmii_tx_en = RegInit(false.B); io.gmii.tx_en := gmii_tx_en
val gmii_tx_er = RegInit(false.B); io.gmii.tx_er := gmii_tx_er


when( io.clkEn ){
  when( io.miiSel ){
    mii_msn := gmii_txd(7,4)
    gmii_txd := gmii_txd & "h0F".U(8.W)
    when( mii_odd ){
      gmii_txd := mii_msn & "h0F".U(8.W)
    } 
  }.otherwise{
    when( stateCurr === STATE_IDLE ){
      when( io.axis.valid ){
        gmii_txd := ETH_PRE
      }
    } .elsewhen( stateCurr === STATE_PREAMBLE ){
      gmii_txd := Mux( frame_ptr === 7.U, ETH_SFD, ETH_PRE)
    } .elsewhen( stateCurr === STATE_PAYLOAD | stateCurr === STATE_LAST ){
      gmii_txd := s_tdata;
    } .elsewhen( stateCurr === STATE_PAD ){
      gmii_txd := 0.U
    } .elsewhen( stateCurr === STATE_FCS ){
      gmii_txd := Mux1H(Seq(
        (frame_ptr === 0.U) -> ~crcOut( 7,0),
        (frame_ptr === 1.U) -> ~crcOut(15,8),
        (frame_ptr === 2.U) -> ~crcOut(23,16),
        (frame_ptr === 3.U) -> ~crcOut(31,24),
      ))
    }
  }
}


when( io.clkEn){
  when(io.miiSel && mii_odd){
    mii_odd := false.B
  } .otherwise{
    when( stateCurr === STATE_IDLE ){
      mii_odd := io.axis.valid
    } .otherwise{
      mii_odd := true.B
    }
  }
}

when( io.clkEn & ~(io.miiSel & mii_odd) ){
  when( stateCurr === STATE_IDLE ){
    gmii_tx_en := io.axis.valid
  } .otherwise{
    gmii_tx_en := stateCurr === STATE_PREAMBLE | stateCurr === STATE_PAYLOAD | stateCurr === STATE_LAST | stateCurr === STATE_PAD | stateCurr === STATE_FCS
  }
} .otherwise{
  gmii_tx_en := false.B
}

when( io.clkEn & ~(io.miiSel & mii_odd) & stateCurr === STATE_PAYLOAD ){
  when( io.axis.valid ) {
    when( io.axis.bits.tlast & io.axis.bits.tuser ){
      gmii_tx_er := true.B
    } .otherwise{
      gmii_tx_er := false.B
    }
  } .otherwise{
    gmii_tx_er := true.B
  }
} .otherwise{
    gmii_tx_er := false.B
}

















}





