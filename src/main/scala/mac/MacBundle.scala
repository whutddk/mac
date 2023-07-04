package MAC

import chisel3._
import chisel3.util._




class TxBuffDesc extends Bundle{
  val len = UInt(16.W) //[31.16]
  val rd = Bool() //[15]
  val irq  = Bool() //14
  val wr = Bool()  //13
  val pad = Bool() //12
  val crc = Bool() //11
  val reserved1 = UInt(2.W) //10,9
  val ur = Bool()  //8
  val rtry = UInt(4.W)  //7 6 5 4
  val rl = Bool()  //3
  val lc = Bool()  //2
  val df = Bool()  //1
  val cs = Bool()  //0
}

class RxBuffDesc extends Bundle{
  val len = UInt(16.W) //[31.16]
  val e = Bool() //15
  val irq = Bool() //14
  val wrap = Bool() //13
  val reserved1 = UInt(4.W) //12 11 10 9
  val cf = Bool() //8
  val m = Bool() //7
  val or = Bool() //6
  val is = Bool() //5
  val dn = Bool() //4
  val tl = Bool() //3
  val sf = Bool() //2
  val crc = Bool() //1
  val lc = Bool() //0
}

trait MacWishboneMasterIO{ this: Bundle =>
  val m_wb_adr_o = Output(UInt(32.W))
  val m_wb_sel_o = Output(UInt(4.W))
  val m_wb_we_o  = Output(Bool())
  val m_wb_dat_i = Input(UInt(32.W))
  val m_wb_dat_o = Output(UInt(32.W))
  val m_wb_cyc_o = Output(Bool())
  val m_wb_stb_o = Output(Bool())
  val m_wb_ack_i = Input(Bool())
  val m_wb_err_i = Input(Bool())
  val m_wb_cti_o = Output(UInt(3.W))
  val m_wb_bte_o = Output(UInt(2.W))
}


trait MacWishboneSlaveIO{ this: Bundle =>
  val WB_DAT_I = Input(UInt(32.W))       // WISHBONE data input
  val WB_DAT_O = Output(UInt(32.W))       // WISHBONE data output

  val WB_ADR_I = Input(UInt(12.W))       // WISHBONE address input
  val WB_WE_I  = Input(Bool())        // WISHBONE write enable input

  val WB_SEL_I = Input(UInt(4.W))     // WISHBONE byte select input
  val WB_CYC_I = Input(Bool())     // WISHBONE cycle input
  val WB_STB_I = Input(Bool())     // WISHBONE strobe input

  val WB_ACK_O = Output(Bool())       // WISHBONE acknowledge output
  val WB_ERR_O = Output(Bool())       // WISHBONE error output
}

trait MDIO { this: Bundle =>
  val mdi   = Input( Bool()) // MII Management Data In
  val mdc   = Output(Bool()) // MII Management Data Clock
  val mdo   = Output(Bool()) // MII Management Data Output
  val mdoEn = Output(Bool()) // MII Management Data Output Enable
}

