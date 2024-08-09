package MAC

import chisel3._
import chisel3.util._


class GMII_RX_Bundle extends Bundle{
  val rxd = UInt(8.W)
  val rx_dv = Bool()
  val rx_er = Bool()
}

class AXIS_TX_Bundle extends Bundle{
  val tdata = UInt(8.W)
  val tvalid = Bool()
  val tlast = Bool()
  val tuser = Bool()
}

class GmiiRx_AxisTx_IO extends Bundle{
  val gmii = Input(new GMII_RX_Bundle)
  val axis = Output(new AXIS_TX_Bundle)

  val clkEn = Input(Bool()),
  val miiSel = Input(Bool()),

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



  val stateNext = Wire(UInt(2.W))
  val stateCurr = RegEnable( stateNext, 0.U, io.clkEn & ~(io.miiSel & ~mii_odd) )

  val crcUnit = Module(new crc32_8)
  val crcOut = crcUnit.io.crc
  val crcCnt = RegInit(0.U(2.W))

  val mii_odd = RegInit(false.B)
  val mii_locked = RegInit(false.B)

  stateNext := 
    Mux1H(Seq(
      (stateCurr === 0.U) -> ( Mux( gmii_rx_dv_d4 && !gmii_rx_er(4) && gmii_rxd(4) == ETH_SFD, 1.U, 0.U )), //IDLE
      (stateCurr === 1.U) -> ( Mux( gmii_rx_dv_d4 && gmii_rx_er(4), 3.U, Mux( ~gmii_rx_dv, 2.U, 1.U ) )), // PAYLOAD
      (stateCurr === 2.U) -> ( Mux( &crcCnt, 0.U, 2.U )), //CRC
      (stateCurr === 3.U) -> ( Mux( ~gmii_rx_dv, 0.U, 3.U  )), //WAIT_LAST
    ))

    











  val gmii_rxd = for( i <- 0 until 5 ) yield { Wire(UInt(8.W)) }
  gmii_rxd(0) := RegEnable( Mux( io.miiSel, Cat(io.gmii.rxd(3,0), gmii_rxd(0)(7,4)), io.gmii.rxd ), io.clkEn )
  for( i <- 1 until 5 ) {
    gmii_rxd(i) := RegEnable( gmii_rxd(i-1), io.clkEn & ( (io.miiSel & mii_odd) | ~io.miiSel ) )
  }

  val gmii_rx_er = for( i <- 0 until 5 ) yield { Wire(Bool()) }
  gmii_rx_er(0) := RegEnable( io.gmii.rx_er | (io.miiSel & mii_odd & gmii_rx_er(0)), io.clkEn )
  for( i <- 1 until 5 ) {
    gmii_rx_er(i) := RegEnable( gmii_rx_er(i-1), io.clkEn & ( (io.miiSel & mii_odd) | ~io.miiSel ) )
  }


  val gmii_rx_dv = for( i <- 0 until 5 ) yield{ Wire(Bool()) }
  gmii_rx_dv(0) := RegEnable( io.gmii.rx_dv & ( (io.miiSel & mii_odd & gmii_rx_dv(0)) | ~(io.miiSel & mii_odd) ) , false.B, io.clkEn )
  for( i <- 1 until 5 ){
    gmii_rx_dv(i) := RegEnable( gmii_rx_dv(i-1) & io.gmii.rx_dv, false.B, io.clkEn & ((io.miiSel & mii_odd) | ~io.miiSel) )
  }



  when( clk_enable & mii_select ){
    when( io.gmii.rx_dv && Cat(io.gmii.rxd(3,0), gmii_rxd(0)(7,4)) === ETH_SFD ) {
      mii_odd := true.B
    } .otherwise{
      mii_odd := ~mii_odd
    }

    when( mii_locked ){
      mii_locked <= io.gmii.rx_dv;
    } .elsewhen( io.gmii.rx_dv && {io.gmii.rxd[3:0], gmii_rxd(0)[7:4]} == ETH_SFD ){
      mii_locked := true.B
    }
  }










  val m_axis_tdata = RegNext( gmii_rxd(4) ); io.axis.tdata := m_axis_tdata
  val m_axis_tlast = RegNext( (stateCurr === 1.U & gmii_rx_dv(4) & gmii_rx_er(4)) | ( stateCurr === 2.U & &crcCnt ) ); io.axis.tlast := m_axis_tlast
  val m_axis_tuser = RegNext(
    stateCurr === 1.U & (
      (gmii_rx_dv(4) && gmii_rx_er(4)) |
      ( ~io.gmii.rx_dv & (
        (gmii_rx_er(0) | gmii_rx_er(1) | gmii_rx_er(2) | gmii_rx_er(3)) |
        Cat(gmii_rxd(0), gmii_rxd(1), gmii_rxd(2), gmii_rxd(3)) =/= ~crcOut
        ) 
      )
    )
  ); io.axis.tuser := m_axis_tuser

  val m_axis_tvalid = RegNext(io.clkEn & ~(io.miiSel & ~mii_odd) & (stateCurr === 1.U | stateCurr === 2.U), false.B); io.axis.tvalid := m_axis_tvalid

    
  val error_bad_frame = m_axis_tvalid & m_axis_tuser; io.error_bad_frame := error_bad_frame
  val error_bad_fcs = m_axis_tvalid &
    RegNext( stateCurr === 1.U & (
      ~(gmii_rx_dv(4) & gmii_rx_er(4)) & io.gmii.rx_dv & ( ~(gmii_rx_er(0) & ~gmii_rx_er(1) & ~gmii_rx_er(2) & ~gmii_rx_er(3)) & Cat(gmii_rxd(0), gmii_rxd(1), gmii_rxd(2), gmii_rxd(3)) =/= ~crcOut ) 
    ), false.B); io.error_bad_fcs := error_bad_fcs


  val fcs = RegInit( "hFFFFFFFF".U(32.W) )

  when( io.clkEn & ~(io.miiSel & ~mii_odd) ){
    when( stateCurr === 1.U & gmii_rx_dv(4) & gmii_rx_er(4) ){
      fcs := "hdeadbeef".U
    } .elsewhen( stateCurr === 2.U ){
      fcs := Mux( &crcCnt, crcOut, 0.U )
    }
  }







  crcUnit.io.isEnable := io.clkEn & ~( io.miiSel & ~mii_odd) & (stateCurr === 1.U | stateCurr === 2.U) //payload or crc
  crcUnit.io.dataIn := gmii_rxd(4)
  
  crcUnit.reset := reset.asBool | (io.clkEn & ~( io.miiSel & ~mii_odd) & stateCurr === 0.U) //idle






  when( io.clkEn & ~(io.miiSel & ~mii_odd) ){
    when( stateCurr === 1.U & ~io.gmii.rx_dv){
      crcCnt := 0.U
    } .elsewhen( stateCurr === 2.U ){
      crcCnt := crcCnt + 1.U
    }    

  }



}

























