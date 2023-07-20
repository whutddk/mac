package MAC

import chisel3._
import chisel3.util._


trait MacTileLinkRegister { this: MacTileLinkBase =>





  val RecSmall  = RegInit(false.B)
  val Pad       = RegInit(true.B)
  val HugEn     = RegInit(false.B)
  val CrcEn     = RegInit(true.B)
  val DlyCrcEn  = RegInit(false.B)

  val FullD     = RegInit(false.B)
  val ExDfrEn   = RegInit(false.B)
  val NoBckof   = RegInit(false.B)
  val LoopBck   = RegInit(false.B)
  val IFG       = RegInit(false.B)
  val Pro       = RegInit(false.B)
  val Iam       = RegInit(false.B)
  val Bro       = RegInit(false.B)
  val NoPre     = RegInit(false.B)
  val TxEn      = RegInit(false.B)
  val RxEn      = RegInit(false.B)

  // Interrupt generation
  val irq_txb  = RegInit(false.B)  
  val irq_txe  = RegInit(false.B)
  val irq_rxb  = RegInit(false.B)
  val irq_rxe  = RegInit(false.B)
  val irq_busy = RegInit(false.B)
  val irq_txc  = RegInit(false.B)
  val irq_rxc  = RegInit(false.B)

  

  when(TxB_IRQ){ irq_txb := true.B }
  when(TxE_IRQ){ irq_txe := true.B }
  when(RxB_IRQ){ irq_rxb := true.B }
  when(RxE_IRQ){ irq_rxe := true.B }
  when(Busy_IRQ){ irq_busy := true.B }
  when(SetTxCIrq){ irq_txc := true.B }
  when(SetRxCIrq){ irq_rxc := true.B }

  val INT_MASK = RegInit(0.U(7.W))        // INT_MASK Register
  val IPGT     = RegInit("h12".U(7.W))    // IPGT Register
  val IPGR1    = RegInit("h0C".U(7.W))    // IPGR1 Register
  val IPGR2    = RegInit("h12".U(7.W))

  val maxFL = RegInit("h0600".U(16.W))
  val minFL = RegInit("h0040".U(16.W))

  val collValid = RegInit("h3f".U(6.W))
  val maxRet = RegInit("hF".U(4.U))

  val TX_BD_NUM = RegInit("h40".U(8.W))        // TX_BD_NUM Register

  val txFlow  = RegInit(false.B)
  val rxFlow  = RegInit(false.B)
  val passAll = RegInit(false.B)

  val clkDiv = RegInit("h64".U(8.W))
  val miiNoPre = RegInit(false.B)


  val scanStat = RegInit(false.B)
  val readStat = RegInit(false.B)
  val wCtrlData = RegInit(false.B)

  when(io.RStatStart){ readStat := false.B }
  when(io.WCtrlDataStart){ wCtrlData := false.B }

  val FIAD = RegInit(0.U(5.W))
  val RGAD = RegInit(0.U(5.W))

  val MIITX_DATA = RegInit(0.U(16.W)) // MIITX_DATA Register

  val MIIRX_DATA = RegEnable(io.Prsd, 0.U(16.W), io.UpdateMIIRX_DATAReg)        // MIIRX_DATA Register

  val Mac_ADDR0 = RegInit(0.U(32.W))
  val Mac_ADDR1 = RegInit(0.U(16.W))

  val HASH0 = RegInit(0.U(32.W))
  val HASH1 = RegInit(0.U(32.W))

  val txPauseRq = RegInit(false.B)
  val txPauseTV = RegInit(0.U(16.W))

  when( io.RstTxPauseRq ){ txPauseRq := false.B }





  configNode.regmap(
    ( 0.U  << 2 ) -> 
      RegFieldGroup("MODER", Some("Mode Register"), Seq(
        RegField(1, RxEn   , RegFieldDesc( "RxEn", "RxEn", reset = Some(0))) ,
        RegField(1, TxEn   , RegFieldDesc( "TxEn", "TxEn", reset = Some(0) ) ),
        RegField(1, NoPre  , RegFieldDesc( "NoPre", "NoPre", reset = Some(0) ) ),
        RegField(1, Bro    , RegFieldDesc( "Bro", "Bro", reset = Some(0) ) ),
        RegField(1, Iam    , RegFieldDesc( "Iam", "Iam", reset = Some(0) ) ),
        RegField(1, Pro    , RegFieldDesc( "Pro", "Pro", reset = Some(0) ) ),
        RegField(1, IFG    , RegFieldDesc( "IFG", "IFG", reset = Some(0) ) ),
        RegField(1, LoopBck, RegFieldDesc( "LoopBck", "LoopBck", reset = Some(0) ) ),
        RegField(1, NoBckof, RegFieldDesc( "NoBckof", "NoBckof", reset = Some(0) ) ),
        RegField(1, ExDfrEn, RegFieldDesc( "ExDfrEn", "ExDfrEn", reset = Some(0) ) ),
        RegField(1, FullD  , RegFieldDesc( "FullD", "FullD", reset = Some(0) ) ),
        RegField.r(1),
        RegField(1, DlyCrcEn, RegFieldDesc( "DlyCrcEn", "DlyCrcEn", reset=Some(0)) ),
        RegField(1, CrcEn   , RegFieldDesc( "CrcEn", "CrcEn",       reset=Some(1)) ),
        RegField(1, HugEn   , RegFieldDesc( "HugEn", "HugEn",       reset=Some(0)) ),
        RegField(1, Pad     , RegFieldDesc( "Pad", "Pad",           reset=Some(1)) ),
        RegField(1, RecSmall, RegFieldDesc( "RecSmall", "RecSmall", reset=Some(0)) )
      )),

    ( 1.U << 2 ) ->
      RegFieldGroup("INT_SOURCE", Some("Interrupt Source Register"), Seq(
        RegField(1, irq_txb,  RegWriteFn((valid, data) => { when (valid & data) { irq_txb := 0.U }; true.B }),  RegFieldDesc("txb", "txb", reset=Some(0))), 
        RegField(1, irq_txe,  RegWriteFn((valid, data) => { when (valid & data) { irq_txe := 0.U }; true.B }),  RegFieldDesc("txe", "txe", reset=Some(0))),
        RegField(1, irq_rxb,  RegWriteFn((valid, data) => { when (valid & data) { irq_rxb := 0.U }; true.B }),  RegFieldDesc("rxb", "rxb", reset=Some(0))),
        RegField(1, irq_rxe,  RegWriteFn((valid, data) => { when (valid & data) { irq_rxe := 0.U }; true.B }),  RegFieldDesc("rxe", "rxe", reset=Some(0))),
        RegField(1, irq_busy, RegWriteFn((valid, data) => { when (valid & data) { irq_busy := 0.U }; true.B }), RegFieldDesc("busy", "busy", reset=Some(0))),
        RegField(1, irq_txc,  RegWriteFn((valid, data) => { when (valid & data) { irq_txc := 0.U }; true.B }),  RegFieldDesc("txc", "txc", reset=Some(0))),
        RegField(1, irq_rxc,  RegWriteFn((valid, data) => { when (valid & data) { irq_rxc := 0.U }; true.B }),  RegFieldDesc("rxc", "rxc", reset=Some(0))),
      )),

    ( 2.U << 2 ) -> 
      RegFieldGroup("INT_MASK", Some("Interrupt Mask Register"), Seq(
        RegField(7, INT_MASK)
      )),

    ( 3.U << 2 ) -> 
      RegFieldGroup("IPGT", Some("Back to Back Inter Packet Gap Register"), Seq(
        RegField(7, IPGT, RegFieldDesc("IPGT", "IPGT", reset=Some(0x12)))
      )),

    ( 4.U << 2 ) ->
      RegFieldGroup("IPGR1", Some("Non Back to Back Inter Packet Gap Register 1"), Seq(
        RegField(7, IPGR1, RegFieldDesc("IPGR1", "IPGR1", reset=Some(0xc)))
      )),

    ( 5.U  << 2 ) ->
      RegFieldGroup("IPGR2", Some("Non Back to Back Inter Packet Gap Register 2"), Seq(
        RegField(7, IPGR2, RegFieldDesc("IPGR2", "IPGR2", reset=Some(0x12)))
      )),

    ( 6.U << 2 ) ->
      RegFieldGroup("PACKETLEN", Some("Packet Length Register"), Seq(
        RegField(16, maxFL, RegFieldDesc("maxFL", "Maximum Frame Length", reset=Some(0x0600))),
        RegField(16, minFL, RegFieldDesc("minFL", "Minimum Frame Length", reset=Some(0x0040))),
      )),
    ( 7.U << 2 ) ->
      RegFieldGroup("COLLCONF", Some("Collision and Retry Configuration Register"), Seq(
        RegField(6, collValid, RegFieldDesc("collValid", "Collision Valid", reset=Some(0x3f))),
        RegField.r(10),
        RegField(4, maxRet, RegFieldDesc("maxRet", "Maximum Retry", reset=Some(0xf))),
      )),

    ( 8.U << 2 ) ->
      RegFieldGroup("TX_BD_NUM", Some("Transmit BD Number Register"), Seq(
        RegField(8, TX_BD_NUM, RegFieldDesc("TX_BD_NUM", "TX_BD_NUM", reset=Some(0x40))),
      )),

    ( 9.U << 2 ) ->
      RegFieldGroup("CTRLMODER", Some("Control Module Mode Register"), Seq(
        RegField(1, passAll, RegFieldDesc("PassAll", "Pass All Receive Frames", reset=Some(0))),
        RegField(1, rxFlow , RegFieldDesc("RxFlow", "Receive Flow Control", reset=Some(0))),
        RegField(1, txFlow , RegFieldDesc("TxFlow", "Transmit Flow Control", reset=Some(0))),
      )),

    ( 10.U << 2 ) ->
      RegFieldGroup("MIIMODER", Some("MII Mode Register"), Seq(
        RegField(8, clkDiv,   RegFieldDesc("clkDiv", "Clock Divider", reset=Some(0x64))),
        RegField(1, miiNoPre, RegFieldDesc("MIINoPre", "NO Preamble", reset=Some(0x0))),
      )),

    ( 11.U << 2 ) ->
      RegFieldGroup("MIICOMMAND", Some("MII Command Register"), Seq(
        RegField(1, scanStat,  RegFieldDesc("scanStat", "Scan Status", reset=Some(0x0))),
        RegField(1, readStat,  RegFieldDesc("readStat", "Read Status", reset=Some(0x0))),
        RegField(1, wCtrlStat, RegFieldDesc("wCtrlStat", "Write Control Status", reset=Some(0x0))),
      )),

    ( 12.U << 2 ) ->
      RegFieldGroup("MIIADDRESS", Some("MII Address Register"), Seq(
        RegField(5, FIAD,  RegFieldDesc("FIAD", "PHY Address", reset=Some(0x0))),
        RegField.r(3),
        RegField(5, RGAD,  RegFieldDesc("RGAD", "Register Address", reset=Some(0x0))),
      )),

    ( 13.U << 2 ) ->
      RegFieldGroup("MIITX_DATA", Some("MII Transmit Data"), Seq(
        RegField(16, MIITX_DATA)
      )),

    ( 14.U << 2 )  ->  MIIRX_DATAOut
      RegFieldGroup("MIIRX_DATA", Some("MII Receive Data"), Seq(
        RegField.r(16, MIIRX_DATA)
      )),

    ( 15.U << 2 ) ->
      RegFieldGroup("MIISTATUS", Some("MII Status Register"), Seq(
        RegField.r(1, io.LinkFail),
        RegField.r(1, io.Busy_stat),
        RegField.r(1, io.NValid_stat),
      )),

    ( 16.U << 2 ) ->
      RegFieldGroup("MAC_ADDR0", Some("MAC Address Register 0"), Seq(
        RegField(32, MAC_ADDR0)
      )),

    ( 17.U << 2 ) ->
      RegFieldGroup("MAC_ADDR1", Some("MAC Address Register 1"), Seq(
        RegField(16, MAC_ADDR1)
      )),

    ( 18.U << 2 ) ->
      RegFieldGroup("HASH0", Some("HASH Register 0"), Seq(
        RegField(32, HASH0)
      )),

    ( 19.U << 2 ) ->
      RegFieldGroup("HASH1", Some("HASH Register 1"), Seq(
        RegField(32, HASH1)
      )),
      
    ( 20.U << 2 ) ->,
      RegFieldGroup("TXCTRL", Some("Tx Control Register"), Seq(
        RegField(16, txPauseTV, RegFieldDesc("TxPauseTV", "Tx Pause Timer Value", reset=Some(0x0))),
        RegField(1,  txPauseRq, RegFieldDesc("TxPauseRQ", "Tx Pause Request", reset=Some(0x0))),
      )),
  )





  io.r_RecSmall  := RecSmall
  io.r_Pad       := Pad
  io.r_HugEn     := HugEn
  io.r_CrcEn     := CrcEn
  io.r_DlyCrcEn  := DlyCrcEn
  io.r_FullD     := FullD
  io.r_ExDfrEn   := ExDfrEn
  io.r_NoBckof   := NoBckof
  io.r_LoopBck   := LoopBck
  io.r_IFG       := IFG
  io.r_Pro       := Pro
  io.r_Iam       := Iam
  io.r_Bro       := Bro
  io.r_NoPre     := NoPre
  val r_TxEn     = TxEn & (TX_BD_NUM > 0.U)      // Transmission is enabled when there is at least one TxBD.
  io.r_RxEn     := RxEn & (TX_BD_NUM < "h80".U)  // Reception is enabled when there is  at least one RxBD.

  io.r_IPGT      := IPGT
  io.r_IPGR1     := IPGR1
  io.r_IPGR2     := IPGR2
  io.r_MinFL     := minFL
  io.r_MaxFL     := maxFL
  io.r_MaxRet    := maxRet
  io.r_CollValid := collValid
  io.r_TxFlow    := txFlow
  io.r_RxFlow    := rxFlow
  io.r_PassAll   := passAll
  io.r_MiiNoPre  := miiNoPre
  io.r_ClkDiv    := clkDiv
  io.r_WCtrlData := wCtrlData
  io.r_RStat     := readStat
  io.r_ScanStat  := scanStat
  io.r_RGAD      := RGAD
  io.r_FIAD      := FIAD
  io.r_CtrlData  := MIITX_DATA

  io.r_MAC := Cat( MAC_ADDR1(15,0), MAC_ADDR0(31,0) )
  io.r_HASH1 := HASH1
  io.r_HASH0 := HASH0
  val r_TxBDNum = TX_BD_NUM
  io.r_TxPauseTV := txPauseTV
  io.r_TxPauseRq := txPauseRq





























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
    (irq_txb  & INT_MASK.extract(0) ) | 
    (irq_txe  & INT_MASK.extract(1) ) | 
    (irq_rxb  & INT_MASK.extract(2) ) | 
    (irq_rxe  & INT_MASK.extract(3) ) | 
    (irq_busy & INT_MASK.extract(4) ) | 
    (irq_txc  & INT_MASK.extract(5) ) | 
    (irq_rxc  & INT_MASK.extract(6) )



}


