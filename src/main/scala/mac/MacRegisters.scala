package MAC

import chisel3._
import chisel3.util._

class MacRegIO extends Bundle{
  val DataIn              = Input(UInt(32.W))
  val Address             = Input(UInt(8.W))
  val Rw                  = Input(Bool())
  val Cs                  = Input(UInt(4.W))
  val WCtrlDataStart      = Input(Bool())
  val RStatStart          = Input(Bool())
  val UpdateMIIRX_DATAReg = Input(Bool())
  val Prsd                = Input(UInt(16.W))
  val NValid_stat         = Input(Bool())
  val Busy_stat           = Input(Bool())
  val LinkFail            = Input(Bool())
  val TxB_IRQ             = Input(Bool())
  val TxE_IRQ             = Input(Bool())
  val RxB_IRQ             = Input(Bool())
  val RxE_IRQ             = Input(Bool())
  val Busy_IRQ            = Input(Bool())
  val RstTxPauseRq        = Input(Bool())
  val TxCtrlEndFrm        = Input(Bool())
  val StartTxDone         = Input(Bool())
  val TxClk               = Input(Bool())
  val RxClk               = Input(Bool())
  val SetPauseTimer       = Input(Bool())
  val dbg_dat             = Input(UInt(32.W)) // debug data input

  val DataOut     = Output(UInt(32.W))
  val r_RecSmall  = Output(Bool())
  val r_Pad       = Output(Bool())
  val r_HugEn     = Output(Bool())
  val r_CrcEn     = Output(Bool())
  val r_DlyCrcEn  = Output(Bool())
  val r_FullD     = Output(Bool())
  val r_ExDfrEn   = Output(Bool())
  val r_NoBckof   = Output(Bool())
  val r_LoopBck   = Output(Bool())
  val r_IFG       = Output(Bool())
  val r_Pro       = Output(Bool())
  val r_Iam       = Output(Bool())
  val r_Bro       = Output(Bool())
  val r_NoPre     = Output(Bool())
  val r_TxEn      = Output(Bool())
  val r_RxEn      = Output(Bool())
  val r_HASH0     = Output(UInt(32.W))
  val r_HASH1     = Output(UInt(32.W))
  val r_IPGT      = Output(UInt(7.W))
  val r_IPGR1     = Output(UInt(7.W))
  val r_IPGR2     = Output(UInt(7.W))
  val r_MinFL     = Output(UInt(16.W))
  val r_MaxFL     = Output(UInt(16.W))
  val r_MaxRet    = Output(UInt(4.W))
  val r_CollValid = Output(UInt(6.W))
  val r_TxFlow    = Output(Bool())
  val r_RxFlow    = Output(Bool())
  val r_PassAll   = Output(Bool())
  val r_MiiNoPre  = Output(Bool())
  val r_ClkDiv    = Output(UInt(8.W))
  val r_WCtrlData = Output(Bool())
  val r_RStat     = Output(Bool())
  val r_ScanStat  = Output(Bool())
  val r_RGAD      = Output(UInt(5.W))
  val r_FIAD      = Output(UInt(5.W))
  val r_CtrlData  = Output(UInt(16.W))
  val r_MAC       = Output(UInt(48.W))
  val r_TxBDNum   = Output(UInt(8.W))
  val int_o       = Output(Bool())
  val r_TxPauseTV = Output(UInt(16.W))
  val r_TxPauseRq = Output(Bool())

}

class MacReg extends Module{
  val io: MacRegIO = IO(new MacRegIO)



  val Write = io.Cs & Fill(4, io.Rw)
  val Read  = io.Cs.orR & ~io.Rw

  val MODER_Sel      = ( io.Address === "h0".U )
  val INT_SOURCE_Sel = ( io.Address === "h1".U )
  val INT_MASK_Sel   = ( io.Address === "h2".U )
  val IPGT_Sel       = ( io.Address === "h3".U )
  val IPGR1_Sel      = ( io.Address === "h4".U )
  val IPGR2_Sel      = ( io.Address === "h5".U )
  val PACKETLEN_Sel  = ( io.Address === "h6".U )
  val COLLCONF_Sel   = ( io.Address === "h7".U )
  val CTRLMODER_Sel  = ( io.Address === "h9".U )
  val MIIMODER_Sel   = ( io.Address === "hA".U )
  val MIICOMMAND_Sel = ( io.Address === "hB".U )
  val MIIADDRESS_Sel = ( io.Address === "hC".U )
  val MIITX_DATA_Sel = ( io.Address === "hD".U )
  val MAC_ADDR0_Sel  = ( io.Address === "h10".U )
  val MAC_ADDR1_Sel  = ( io.Address === "h11".U )
  val HASH0_Sel      = ( io.Address === "h12".U )
  val HASH1_Sel      = ( io.Address === "h13".U )
  val TXCTRL_Sel     = ( io.Address === "h14".U )
  val RXCTRL_Sel     = ( io.Address === "h15".U )
  val DBG_REG_Sel    = ( io.Address === "h16".U )
  val TX_BD_NUM_Sel  = ( io.Address === "h8".U )


  val MODER_Wr      = Write(2,0)       & Fill(3, MODER_Sel)
  val INT_SOURCE_Wr = Write.extract(0) & INT_SOURCE_Sel
  val INT_MASK_Wr   = Write.extract(0) & INT_MASK_Sel
  val IPGT_Wr       = Write.extract(0) & IPGT_Sel
  val IPGR1_Wr      = Write.extract(0) & IPGR1_Sel
  val IPGR2_Wr      = Write.extract(0) & IPGR2_Sel
  val PACKETLEN_Wr  = Write            & Fill(4, PACKETLEN_Sel)
  val COLLCONF_Wr   = Cat( Write.extract(2) & COLLCONF_Sel, 0.U(1.W), Write.extract(0) & COLLCONF_Sel)
  val CTRLMODER_Wr  = Write.extract(0) & CTRLMODER_Sel
  val MIIMODER_Wr   = Write(1,0)       & Fill(2, MIIMODER_Sel)
  val MIICOMMAND_Wr = Write.extract(0) & MIICOMMAND_Sel; 
  val MIIADDRESS_Wr = Write(1,0)       & Fill(2, MIIADDRESS_Sel)
  val MIITX_DATA_Wr = Write(1,0)       & Fill(2, MIITX_DATA_Sel)
  val MIIRX_DATA_Wr = io.UpdateMIIRX_DATAReg
  val MAC_ADDR0_Wr  = Write            & Fill(4, MAC_ADDR0_Sel)
  val MAC_ADDR1_Wr  = Write(1,0)       & Fill(2, MAC_ADDR1_Sel)
  val HASH0_Wr      = Write            & Fill(4, HASH0_Sel)
  val HASH1_Wr      = Write            & Fill(4, HASH1_Sel)
  val TXCTRL_Wr     = Write(2,0)       & Fill(3, TXCTRL_Sel)
  val TX_BD_NUM_Wr  = Write.extract(0) & TX_BD_NUM_Sel & (io.DataIn <= "h80".U)


  val INT_SOURCEOut = Wire(UInt(32.W))
  val MIISTATUSOut = Cat( io.NValid_stat, io.Busy_stat, io.LinkFail )





  val MODEROut = Cat(                                               // MODER Register
    RegEnable( io.DataIn.extract(16), 0.U(1.W), MODER_Wr.extract(2) ),
    RegEnable( io.DataIn(15,8),   "hA0".U(8.W), MODER_Wr.extract(1) ),
    RegEnable( io.DataIn( 7,0),       0.U(8.W), MODER_Wr.extract(0) ),
  )


  val INT_MASKOut = RegEnable(io.DataIn(6,0),     0.U(7.W), INT_MASK_Wr) // INT_MASK Register
  val IPGTOut     = RegEnable(io.DataIn(6,0), "h12".U(7.W), IPGT_Wr )    // IPGT Register
  val IPGR1Out    = RegEnable(io.DataIn(6,0), "h0C".U(7.W), IPGR1_Wr)    // IPGR1 Register
  val IPGR2Out    = RegEnable(io.DataIn(6,0), "h12".U(7.W), IPGR2_Wr)


  val PACKETLENOut = Cat(                                           // PACKETLEN Register
    RegEnable(io.DataIn(31,24),     0.U(8.W), PACKETLEN_Wr.extract(3)),
    RegEnable(io.DataIn(23,16), "h40".U(8.W), PACKETLEN_Wr.extract(2)),
    RegEnable(io.DataIn(15,8 ),     6.U(8.W), PACKETLEN_Wr.extract(1)),
    RegEnable(io.DataIn( 7,0 ),     0.U(8.W), PACKETLEN_Wr.extract(0))
  )

  val COLLCONFOut = Cat(    // COLLCONF Register
    RegEnable(io.DataIn(19,16), "hF".U(4.W), COLLCONF_Wr.extract(2)),
    0.U(10.W),
    RegEnable(io.DataIn(5,0),  "h3f".U(6.W), COLLCONF_Wr.extract(0))
  )

  val TX_BD_NUMOut = RegEnable(io.DataIn(7,0), "h40".U(8.W), TX_BD_NUM_Wr)        // TX_BD_NUM Register
  val CTRLMODEROut = RegEnable(io.DataIn(2,0),     0.U(3.W), CTRLMODER_Wr.extract(0)) // CTRLMODER Register

  val MIIMODEROut = Cat(                                                     // MIIMODER Register
    RegEnable(io.DataIn.extract(8), 0.U(1.W), MIIMODER_Wr.extract(1)),
    RegEnable(io.DataIn(7,0),   "h64".U(8.W), MIIMODER_Wr.extract(0))
  )

  val MIICOMMANDOut0 = RegEnable(io.DataIn.extract(0), 0.U(1.W), MIICOMMAND_Wr)
  val MIICOMMANDOut1 = RegInit(false.B)
  val MIICOMMANDOut2 = RegInit(false.B)

  when(io.RStatStart){
    MIICOMMANDOut1 := false.B
  } .elsewhen(MIICOMMAND_Wr){
    MIICOMMANDOut1 := io.DataIn.extract(1)
  }

  when(io.WCtrlDataStart){
    MIICOMMANDOut2 := false.B
  } .elsewhen(MIICOMMAND_Wr){
    MIICOMMANDOut2 := io.DataIn.extract(2)
  }
  val MIICOMMANDOut = Cat(MIICOMMANDOut2, MIICOMMANDOut1, MIICOMMANDOut0) // MIICOMMAND Register


  val MIIADDRESSOut = Cat(                                          // MIIADDRESSRegister
    RegEnable(io.DataIn(12,8), 0.U(5.W), MIIADDRESS_Wr.extract(1)),
    0.U(3.W),
    RegEnable(io.DataIn( 4,0), 0.U(5.W), MIIADDRESS_Wr.extract(0))
  )


  val MIITX_DATAOut = Cat(                                          // MIITX_DATA Register
    RegEnable(io.DataIn(15,8), 0.U(8.W), MIITX_DATA_Wr.extract(1)),
    RegEnable(io.DataIn( 7,0), 0.U(8.W), MIITX_DATA_Wr.extract(0))
  )

  val MIIRX_DATAOut = RegEnable(io.Prsd, 0.U(16.W), MIIRX_DATA_Wr)        // MIIRX_DATA Register

  val MAC_ADDR0Out = Cat(                                   // MAC_ADDR0 Register
    RegEnable(io.DataIn(31,24), 0.U(8.W), MAC_ADDR0_Wr.extract(3)),
    RegEnable(io.DataIn(23,16), 0.U(8.W), MAC_ADDR0_Wr.extract(2)),
    RegEnable(io.DataIn(15, 8), 0.U(8.W), MAC_ADDR0_Wr.extract(1)),
    RegEnable(io.DataIn(7 , 0), 0.U(8.W), MAC_ADDR0_Wr.extract(0))
  )

  val MAC_ADDR1Out = Cat(                                   // MAC_ADDR1 Register
    RegEnable(io.DataIn(15,8), 0.U(8.W), MAC_ADDR1_Wr.extract(1)),
    RegEnable(io.DataIn(7 ,0), 0.U(8.W), MAC_ADDR1_Wr.extract(0))
  )



  val HASH0Out = Cat(                                       // RXHASH0 Register
    RegEnable(io.DataIn(31,24), 0.U(8.W), HASH0_Wr.extract(3)),
    RegEnable(io.DataIn(23,16), 0.U(8.W), HASH0_Wr.extract(2)),
    RegEnable(io.DataIn(15, 8), 0.U(8.W), HASH0_Wr.extract(1)),
    RegEnable(io.DataIn(7 , 0), 0.U(8.W), HASH0_Wr.extract(0))
  )

  val HASH1Out = Cat(                                       // RXHASH1 Register
    RegEnable(io.DataIn(31,24), 0.U(8.W), HASH1_Wr.extract(3)),
    RegEnable(io.DataIn(23,16), 0.U(8.W), HASH1_Wr.extract(2)),
    RegEnable(io.DataIn(15, 8), 0.U(8.W), HASH1_Wr.extract(1)),
    RegEnable(io.DataIn(7 , 0), 0.U(8.W), HASH1_Wr.extract(0))
  )


  val TXCTRLOut16 = RegInit(false.B)
  when( io.RstTxPauseRq ){
    TXCTRLOut16 := false.B
  } .elsewhen( TXCTRL_Wr.extract(2) ){
    TXCTRLOut16 := io.DataIn.extract(16)
  }

  val TXCTRLOut = Cat(                                     // TXCTRL Register
    TXCTRLOut16,
    RegEnable(io.DataIn(15,8), 0.U(8.W), TXCTRL_Wr.extract(1)),
    RegEnable(io.DataIn(7, 0), 0.U(8.W), TXCTRL_Wr.extract(0))
  )



  // Reading data from registers
  io.DataOut := 
    Mux(
      Read,
      Mux1H(Seq(
        (io.Address === 0.U  )  ->  MODEROut,
        (io.Address === 1.U  )  ->  INT_SOURCEOut,
        (io.Address === 2.U  )  ->  INT_MASKOut,
        (io.Address === 3.U  )  ->  IPGTOut,
        (io.Address === 4.U  )  ->  IPGR1Out,
        (io.Address === 5.U  )  ->  IPGR2Out,
        (io.Address === 6.U  )  ->  PACKETLENOut,
        (io.Address === 7.U  )  ->  COLLCONFOut,
        (io.Address === 9.U  )  ->  CTRLMODEROut,
        (io.Address === 10.U )  ->  MIIMODEROut,
        (io.Address === 11.U )  ->  MIICOMMANDOut,
        (io.Address === 12.U )  ->  MIIADDRESSOut,
        (io.Address === 13.U )  ->  MIITX_DATAOut,
        (io.Address === 14.U )  ->  MIIRX_DATAOut,
        (io.Address === 15.U )  ->  MIISTATUSOut,
        (io.Address === 16.U )  ->  MAC_ADDR0Out,
        (io.Address === 17.U )  ->  MAC_ADDR1Out,
        (io.Address === 8.U  )  ->  TX_BD_NUMOut,
        (io.Address === 18.U )  ->  HASH0Out,
        (io.Address === 19.U )  ->  HASH1Out,
        (io.Address === 20.U )  ->  TXCTRLOut,
        (io.Address === 22.U )  ->  io.dbg_dat,
      )),
      0.U
    )


  io.r_RecSmall  := MODEROut.extract(16)
  io.r_Pad       := MODEROut.extract(15)
  io.r_HugEn     := MODEROut.extract(14)
  io.r_CrcEn     := MODEROut.extract(13)
  io.r_DlyCrcEn  := MODEROut.extract(12)
  io.r_FullD     := MODEROut.extract(10)
  io.r_ExDfrEn   := MODEROut.extract( 9)
  io.r_NoBckof   := MODEROut.extract( 8)
  io.r_LoopBck   := MODEROut.extract( 7)
  io.r_IFG       := MODEROut.extract( 6)
  io.r_Pro       := MODEROut.extract( 5)
  io.r_Iam       := MODEROut.extract( 4)
  io.r_Bro       := MODEROut.extract( 3)
  io.r_NoPre     := MODEROut.extract( 2)
  io.r_TxEn      := MODEROut.extract( 1) & (TX_BD_NUMOut > 0.U)      // Transmission is enabled when there is at least one TxBD.
  io.r_RxEn      := MODEROut.extract( 0) & (TX_BD_NUMOut < "h80".U)  // Reception is enabled when there is  at least one RxBD.

  io.r_IPGT      := IPGTOut
  io.r_IPGR1     := IPGR1Out
  io.r_IPGR2     := IPGR2Out
  io.r_MinFL     := PACKETLENOut(31,16)
  io.r_MaxFL     := PACKETLENOut(15, 0)
  io.r_MaxRet    := COLLCONFOut(19,16)
  io.r_CollValid := COLLCONFOut( 5, 0)
  io.r_TxFlow    := CTRLMODEROut.extract(2)
  io.r_RxFlow    := CTRLMODEROut.extract(1)
  io.r_PassAll   := CTRLMODEROut.extract(0)
  io.r_MiiNoPre  := MIIMODEROut.extract(8)
  io.r_ClkDiv    := MIIMODEROut(7,0)
  io.r_WCtrlData := MIICOMMANDOut.extract(2)
  io.r_RStat     := MIICOMMANDOut.extract(1)
  io.r_ScanStat  := MIICOMMANDOut.extract(0)
  io.r_RGAD      := MIIADDRESSOut(12,8)
  io.r_FIAD      := MIIADDRESSOut( 4,0)
  io.r_CtrlData  := MIITX_DATAOut(15,0)

  io.r_MAC := Cat( MAC_ADDR1Out(15,0), MAC_ADDR0Out(31,0) )
  io.r_HASH1 := HASH1Out
  io.r_HASH0 := HASH0Out
  io.r_TxBDNum := TX_BD_NUMOut
  io.r_TxPauseTV := TXCTRLOut(15,0)
  io.r_TxPauseRq := TXCTRLOut.extract(16)


  val SetTxCIrq_txclk_wire = Wire(Bool())
  val SetTxCIrq_sync1 = RegNext(SetTxCIrq_txclk_wire, false.B)
  val SetTxCIrq_sync2 = RegNext(SetTxCIrq_sync1, false.B)
  val SetTxCIrq_sync3 = RegNext(SetTxCIrq_sync2, false.B)
  val SetTxCIrq       = RegNext(SetTxCIrq_sync2 & ~SetTxCIrq_sync3, false.B)

  val SetRxCIrq_rxclk_wire = Wire(Bool())
  val SetRxCIrq_sync1 = RegNext(SetRxCIrq_rxclk_wire, false.B)
  val SetRxCIrq_sync2 = RegNext(SetRxCIrq_sync1, false.B)
  val SetRxCIrq_sync3 = RegNext(SetRxCIrq_sync2, false.B)
  val SetRxCIrq       = RegNext(SetRxCIrq_sync2 & ~SetRxCIrq_sync3, false.B)



  // Interrupt generation
  val irq_txb  = RegInit(false.B)  
  val irq_txe  = RegInit(false.B)
  val irq_rxb  = RegInit(false.B)
  val irq_rxe  = RegInit(false.B)
  val irq_busy = RegInit(false.B)
  val irq_txc  = RegInit(false.B)
  val irq_rxc  = RegInit(false.B)


  when(io.TxB_IRQ){
    irq_txb := true.B
  }.elsewhen(INT_SOURCE_Wr & io.DataIn.extract(0)){
    irq_txb := false.B
  }

  when(io.TxE_IRQ){
    irq_txe := true.B
  } .elsewhen(INT_SOURCE_Wr & io.DataIn.extract(1)){
    irq_txe := false.B
  }


  when(io.RxB_IRQ){
    irq_rxb := true.B
  } .elsewhen(INT_SOURCE_Wr & io.DataIn.extract(2)){
    irq_rxb := false.B
  }


  when(io.RxE_IRQ){
    irq_rxe := true.B
  } .elsewhen(INT_SOURCE_Wr & io.DataIn.extract(3)){
    irq_rxe := false.B
  }

  when(io.Busy_IRQ){
    irq_busy := true.B
  } .elsewhen(INT_SOURCE_Wr & io.DataIn.extract(4)){
    irq_busy := false.B
  }


  when(SetTxCIrq){
    irq_txc := true.B
  } .elsewhen(INT_SOURCE_Wr & io.DataIn.extract(5)){
    irq_txc := false.B
  }

  when(SetRxCIrq){
    irq_rxc := true.B
  } .elsewhen(INT_SOURCE_Wr & io.DataIn.extract(6)){
    irq_rxc := false.B
  }


  withClockAndReset( io.TxClk.asClock, reset ) {
    // val ResetTxCIrq_sync1 = Reg(Bool())
    val ResetTxCIrq_sync2 = RegNext(SetTxCIrq_sync1, false.B)
    val SetTxCIrq_txclk   = RegInit(false.B); SetTxCIrq_txclk_wire := SetTxCIrq_txclk

    // Synchronizing TxC Interrupt
    when(io.TxCtrlEndFrm & io.StartTxDone & io.r_TxFlow){
      SetTxCIrq_txclk := true.B
    } .elsewhen(ResetTxCIrq_sync2){
      SetTxCIrq_txclk := false.B
    }

  }


  withClockAndReset( io.RxClk.asClock, reset ) {
    val ResetRxCIrq_sync1 = RegNext(SetRxCIrq_sync2, false.B)
    val ResetRxCIrq_sync2 = RegNext(ResetRxCIrq_sync1, false.B)
    val ResetRxCIrq_sync3 = RegNext(ResetRxCIrq_sync2, false.B)
    val SetRxCIrq_rxclk   = RegInit(false.B); SetRxCIrq_rxclk_wire := SetRxCIrq_rxclk

    // Synchronizing RxC Interrupt
    when(io.SetPauseTimer & io.r_RxFlow){
      SetRxCIrq_rxclk := true.B
    } .elsewhen(ResetRxCIrq_sync2 & (~ResetRxCIrq_sync3)){
      SetRxCIrq_rxclk := false.B
    }


  }


  // Generating interrupt signal
  io.int_o :=
    (irq_txb  & INT_MASKOut.extract(0) ) | 
    (irq_txe  & INT_MASKOut.extract(1) ) | 
    (irq_rxb  & INT_MASKOut.extract(2) ) | 
    (irq_rxe  & INT_MASKOut.extract(3) ) | 
    (irq_busy & INT_MASKOut.extract(4) ) | 
    (irq_txc  & INT_MASKOut.extract(5) ) | 
    (irq_rxc  & INT_MASKOut.extract(6) )

  // For reading interrupt status
  INT_SOURCEOut := Cat( irq_rxc, irq_txc, irq_busy, irq_rxe, irq_rxb, irq_txe, irq_txb )


}


