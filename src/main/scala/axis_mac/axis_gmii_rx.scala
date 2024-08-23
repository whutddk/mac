package MAC

import chisel3._
import chisel3.util._


class GMII_RX_Bundle extends Bundle{
  val rxd = UInt(8.W)
  val rx_dv = Bool()
  val rx_er = Bool()
}


class GmiiRx_AxisTx_IO extends Bundle{
  val gmii = Input(new GMII_RX_Bundle)
  val axis = Decoupled(new AXIS_Bundle)

  val clkEn = Input(Bool())
  val miiSel = Input(Bool())

  val error_bad_frame = Output(Bool())
  val error_bad_fcs = Output(Bool())

}



/*
 * AXI4-Stream GMII frame receiver (GMII in, AXI out)
 */
class GmiiRx_AxisTx extends Module{
  val io = IO(new GmiiRx_AxisTx_IO)

  val ETH_PRE = "h55".U
  val ETH_SFD = "hD5".U

  val STATE_IDLE = 0.U
  val STATE_PAYLOAD = 1.U
  val STATE_CRC = 2.U
  val STATE_WAIT = 3.U

  val mii_odd = RegInit(false.B)
  val mii_locked = RegInit(false.B)

  val stateNext = Wire(UInt(2.W))
  val stateCurr = RegEnable( stateNext, 0.U, io.clkEn & ~(io.miiSel & ~mii_odd) )

  val crcUnit = Module(new crc32_8)
  val crcOut = crcUnit.io.crc
  val crcCnt = RegInit(0.U(2.W))
  val crcCmp = Reg(UInt(32.W))
  val isCrcFail = ~crcCmp =/= crcOut

  val gmii_rxd = for( i <- 0 until 5 ) yield { Reg(UInt(8.W)) }
  val gmii_rx_er = for( i <- 0 until 5 ) yield { Reg(Bool()) }
  val gmii_rx_dv = for( i <- 0 until 5 ) yield{ RegInit(false.B) }


  stateNext := 
    Mux1H(Seq(
      (stateCurr === STATE_IDLE)    -> ( Mux( gmii_rx_dv(4) & ~gmii_rx_er(4) & gmii_rxd(4) === ETH_SFD, STATE_PAYLOAD, STATE_IDLE )), //IDLE
      (stateCurr === STATE_PAYLOAD) -> ( Mux( gmii_rx_dv(4) &  gmii_rx_er(4), STATE_WAIT, Mux( ~io.gmii.rx_dv, STATE_CRC, STATE_PAYLOAD ) )), // PAYLOAD
      (stateCurr === STATE_CRC)     -> ( Mux( crcCnt.andR, STATE_IDLE, STATE_CRC )), //CRC
      (stateCurr === STATE_WAIT)    -> ( Mux( ~io.gmii.rx_dv, STATE_IDLE, STATE_WAIT  )), //WAIT_LAST
    ))

    






  when( io.clkEn ){
    when( io.miiSel ){
      gmii_rxd(0) := Cat(io.gmii.rxd(3,0), gmii_rxd(0)(7,4))
    } .otherwise{
      gmii_rxd(0) := io.gmii.rxd
    }
  }

  for( i <- 1 until 5 ) {
    when( io.clkEn ){
      when( io.miiSel ){
        when( mii_odd ){
          gmii_rxd(i) := gmii_rxd(i-1)
        }
      } .otherwise{
        gmii_rxd(i) := gmii_rxd(i-1)
      }
    }
  }






  when( io.clkEn ){
    when( io.miiSel ){
      when( mii_odd ){
        gmii_rx_er(0) := io.gmii.rx_er | gmii_rx_er(0)
      } .otherwise{
        gmii_rx_er(0) := io.gmii.rx_er
      }
    } .otherwise{
      gmii_rx_er(0) := io.gmii.rx_er
    }
  }

  for( i <- 1 until 5 ) {
    when( io.clkEn ){
      when( io.miiSel ){
        when( mii_odd ){
          gmii_rx_er(i) := gmii_rx_er(i-1)
        }
      } .otherwise{
        gmii_rx_er(i) := gmii_rx_er(i-1)
      }
    }
  }


  when( io.clkEn ){
    when( io.miiSel ){
      when( mii_odd ){
        gmii_rx_dv(0) := io.gmii.rx_dv & gmii_rx_dv(0)
      } .otherwise{
        gmii_rx_dv(0) := io.gmii.rx_dv
      }
    } .otherwise{
      gmii_rx_dv(0) := io.gmii.rx_dv
    }
  }

  for( i <- 1 until 5 ){
    when( io.clkEn ){
      when( io.miiSel ){
        when( mii_odd ){
          gmii_rx_dv(i) := gmii_rx_dv(i-1) & io.gmii.rx_dv
        }
      } .otherwise{
        gmii_rx_dv(i) := gmii_rx_dv(i-1) & io.gmii.rx_dv
      }
    }
  }










  when( io.clkEn & io.miiSel ){
    when( mii_locked ){
      mii_locked <= io.gmii.rx_dv
      mii_odd := ~mii_odd
    } .elsewhen( io.gmii.rx_dv && Cat(io.gmii.rxd(3,0), gmii_rxd(0)(7,4)) === ETH_SFD ) {
      mii_locked := true.B
      mii_odd := true.B
    } .otherwise{
      mii_odd := ~mii_odd
    }
  }










  val m_axis_tdata = RegNext( gmii_rxd(4) ); io.axis.bits.tdata := m_axis_tdata
  val m_axis_tlast = RegNext( (stateCurr === STATE_PAYLOAD & gmii_rx_dv(4) & gmii_rx_er(4)) | ( stateCurr === STATE_CRC & crcCnt.andR ) ); io.axis.bits.tlast := m_axis_tlast
  val m_axis_tuser = RegNext(
    (stateCurr === STATE_PAYLOAD & (
      (gmii_rx_dv(4) & gmii_rx_er(4)) |
      ( ~io.gmii.rx_dv & (gmii_rx_er(0) | gmii_rx_er(1) | gmii_rx_er(2) | gmii_rx_er(3)) )
    ))
  );
  io.axis.bits.tuser :=  m_axis_tuser | (stateCurr === STATE_IDLE & isCrcFail)

  val m_axis_tvalid = RegNext(io.clkEn & ~(io.miiSel & ~mii_odd) & (stateCurr === STATE_PAYLOAD | stateCurr === STATE_CRC), false.B); io.axis.valid := m_axis_tvalid

  assert( ~(io.axis.valid & ~io.axis.ready) )












    
  val error_bad_frame = m_axis_tvalid & m_axis_tuser; io.error_bad_frame := error_bad_frame
  val error_bad_fcs = m_axis_tvalid &
    RegNext( stateCurr === STATE_PAYLOAD & (
      ~(gmii_rx_dv(4) & gmii_rx_er(4)) & io.gmii.rx_dv & ( ~(gmii_rx_er(0) & ~gmii_rx_er(1) & ~gmii_rx_er(2) & ~gmii_rx_er(3)) & Cat(gmii_rxd(0), gmii_rxd(1), gmii_rxd(2), gmii_rxd(3)) =/= ~crcOut ) 
    ), false.B); io.error_bad_fcs := error_bad_fcs


  val fcs = RegInit( "hFFFFFFFF".U(32.W) )

  when( io.clkEn & ~(io.miiSel & ~mii_odd) ){
    when( stateCurr === STATE_PAYLOAD & gmii_rx_dv(4) & gmii_rx_er(4) ){
      fcs := "hdeadbeef".U
    } .elsewhen( stateCurr === STATE_CRC ){
      fcs := Mux( crcCnt.andR, crcOut, 0.U )
    }
  }


  
  when( io.clkEn & ~(io.miiSel & ~mii_odd) ){
    when( stateCurr === STATE_IDLE ){
      crcCmp := 0.U
    } .elsewhen( stateCurr ===  STATE_CRC ) {
      crcCmp := Cat( gmii_rxd(4), crcCmp(31,8) )
    }
  }




  crcUnit.io.isEnable := io.clkEn & ~( io.miiSel & ~mii_odd) & (stateCurr === STATE_PAYLOAD ) //payload or crc
  crcUnit.io.dataIn := gmii_rxd(4)
  
  crcUnit.reset := reset.asBool | (io.clkEn & ~( io.miiSel & ~mii_odd) & stateCurr === STATE_IDLE) //idle






  when( io.clkEn & ~(io.miiSel & ~mii_odd) ){
    when( stateCurr === STATE_PAYLOAD & ~io.gmii.rx_dv){
      crcCnt := 0.U
    } .elsewhen( stateCurr === STATE_CRC ){
      crcCnt := crcCnt + 1.U
    }    

  }



}

























