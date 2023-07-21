package MAC

import chisel3._
import chisel3.util._

import freechips.rocketchip.regmapper._
import freechips.rocketchip.tilelink._
import org.chipsalliance.cde.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.interrupts._

class MacRegIO extends Bundle{

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
  // val int_o       = Output(Bool())
  val r_TxPauseTV = Output(UInt(16.W))
  val r_TxPauseRq = Output(Bool())

}

class MacReg(implicit p: Parameters) extends LazyModule{

  // DTS
  val dtsdevice = new SimpleDevice("mac",Seq("mac_0"))
  val int_node = IntSourceNode(IntSourcePortSimple(num = 1, resources = dtsdevice.int))


  val configNode = TLRegisterNode(
    address = Seq(AddressSet(0x30000000L, 0x000003ffL)),
    device = dtsdevice,
    concurrency = 1,
    beatBytes = 32/8,
    executable = true
  )
  lazy val module = new MacRegImp(this)
}

class MacRegImp(outer: MacReg)(implicit p: Parameters) extends LazyModuleImp(outer){

    val io: MacRegIO = IO(new MacRegIO)
    val (int, _) = outer.int_node.out(0)


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

    



    val INT_MASK = RegInit(0.U(7.W))        // INT_MASK Register
    val IPGT     = RegInit("h12".U(7.W))    // IPGT Register
    val IPGR1    = RegInit("h0C".U(7.W))    // IPGR1 Register
    val IPGR2    = RegInit("h12".U(7.W))

    val maxFL = RegInit("h0600".U(16.W))
    val minFL = RegInit("h0040".U(16.W))

    val collValid = RegInit("h3f".U(6.W))
    val maxRet = RegInit("hF".U(4.W))

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





    outer.configNode.regmap(
      ( 0 << 2 ) -> 
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
          RegField.r(1, 0.U),
          RegField(1, DlyCrcEn, RegFieldDesc( "DlyCrcEn", "DlyCrcEn", reset=Some(0)) ),
          RegField(1, CrcEn   , RegFieldDesc( "CrcEn", "CrcEn",       reset=Some(1)) ),
          RegField(1, HugEn   , RegFieldDesc( "HugEn", "HugEn",       reset=Some(0)) ),
          RegField(1, Pad     , RegFieldDesc( "Pad", "Pad",           reset=Some(1)) ),
          RegField(1, RecSmall, RegFieldDesc( "RecSmall", "RecSmall", reset=Some(0)) )
        )),

      ( 1 << 2 ) ->
        RegFieldGroup("INT_SOURCE", Some("Interrupt Source Register"), Seq(
          RegField(1, irq_txb,  RegWriteFn((valid, data) => { when ((valid & data) === 1.U) { irq_txb := 0.U }; true.B }),  RegFieldDesc("txb", "txb", reset=Some(0))), 
          RegField(1, irq_txe,  RegWriteFn((valid, data) => { when ((valid & data) === 1.U) { irq_txe := 0.U }; true.B }),  RegFieldDesc("txe", "txe", reset=Some(0))),
          RegField(1, irq_rxb,  RegWriteFn((valid, data) => { when ((valid & data) === 1.U) { irq_rxb := 0.U }; true.B }),  RegFieldDesc("rxb", "rxb", reset=Some(0))),
          RegField(1, irq_rxe,  RegWriteFn((valid, data) => { when ((valid & data) === 1.U) { irq_rxe := 0.U }; true.B }),  RegFieldDesc("rxe", "rxe", reset=Some(0))),
          RegField(1, irq_busy, RegWriteFn((valid, data) => { when ((valid & data) === 1.U) { irq_busy := 0.U }; true.B }), RegFieldDesc("busy", "busy", reset=Some(0))),
          RegField(1, irq_txc,  RegWriteFn((valid, data) => { when ((valid & data) === 1.U) { irq_txc := 0.U }; true.B }),  RegFieldDesc("txc", "txc", reset=Some(0))),
          RegField(1, irq_rxc,  RegWriteFn((valid, data) => { when ((valid & data) === 1.U) { irq_rxc := 0.U }; true.B }),  RegFieldDesc("rxc", "rxc", reset=Some(0))),
        )),

      ( 2 << 2 ) -> 
        RegFieldGroup("INT_MASK", Some("Interrupt Mask Register"), Seq(
          RegField(7, INT_MASK)
        )),

      ( 3 << 2 ) -> 
        RegFieldGroup("IPGT", Some("Back to Back Inter Packet Gap Register"), Seq(
          RegField(7, IPGT, RegFieldDesc("IPGT", "IPGT", reset=Some(0x12)))
        )),

      ( 4 << 2 ) ->
        RegFieldGroup("IPGR1", Some("Non Back to Back Inter Packet Gap Register 1"), Seq(
          RegField(7, IPGR1, RegFieldDesc("IPGR1", "IPGR1", reset=Some(0xc)))
        )),

      ( 5 << 2 ) ->
        RegFieldGroup("IPGR2", Some("Non Back to Back Inter Packet Gap Register 2"), Seq(
          RegField(7, IPGR2, RegFieldDesc("IPGR2", "IPGR2", reset=Some(0x12)))
        )),

      ( 6 << 2 ) ->
        RegFieldGroup("PACKETLEN", Some("Packet Length Register"), 
          RegField.bytes(maxFL, Some(RegFieldDesc("maxFL", "Maximum Frame Length", reset=Some(0x0600)))) ++
          RegField.bytes(minFL, Some(RegFieldDesc("minFL", "Minimum Frame Length", reset=Some(0x0040))))
        ),
      ( 7 << 2 ) ->
        RegFieldGroup("COLLCONF", Some("Collision and Retry Configuration Register"), Seq(
          RegField(6, collValid, RegFieldDesc("collValid", "Collision Valid", reset=Some(0x3f))),
          RegField.r(10,0.U),
          RegField(4, maxRet, RegFieldDesc("maxRet", "Maximum Retry", reset=Some(0xf))),
        )),
      ( 8 << 2 ) ->
        RegFieldGroup("TX_BD_NUM", Some("Transmit BD Number Register"), Seq(
          RegField(8, TX_BD_NUM, RegWriteFn((valid, data) => { when (valid & data <= "h80".U) { TX_BD_NUM := data }; true.B }), RegFieldDesc("TX_BD_NUM", "TX_BD_NUM", reset=Some(0x40))),
        )),

      ( 9 << 2 ) ->
        RegFieldGroup("CTRLMODER", Some("Control Module Mode Register"), Seq(
          RegField(1, passAll, RegFieldDesc("PassAll", "Pass All Receive Frames", reset=Some(0))),
          RegField(1, rxFlow , RegFieldDesc("RxFlow", "Receive Flow Control", reset=Some(0))),
          RegField(1, txFlow , RegFieldDesc("TxFlow", "Transmit Flow Control", reset=Some(0))),
        )),

      ( 10 << 2 ) ->
        RegFieldGroup("MIIMODER", Some("MII Mode Register"), Seq(
          RegField(8, clkDiv,   RegFieldDesc("clkDiv", "Clock Divider", reset=Some(0x64))),
          RegField(1, miiNoPre, RegFieldDesc("MIINoPre", "NO Preamble", reset=Some(0x0))),
        )),

      ( 11 << 2 ) ->
        RegFieldGroup("MIICOMMAND", Some("MII Command Register"), Seq(
          RegField(1, scanStat,  RegFieldDesc("scanStat", "Scan Status", reset=Some(0x0))),
          RegField(1, readStat,  RegFieldDesc("readStat", "Read Status", reset=Some(0x0))),
          RegField(1, wCtrlData, RegFieldDesc("wCtrlData", "Write Control Data", reset=Some(0x0))),
        )),

      ( 12 << 2 ) ->
        RegFieldGroup("MIIADDRESS", Some("MII Address Register"), Seq(
          RegField(5, FIAD,  RegFieldDesc("FIAD", "PHY Address", reset=Some(0x0))),
          RegField.r(3 ,0.U),
          RegField(5, RGAD,  RegFieldDesc("RGAD", "Register Address", reset=Some(0x0))),
        )),

      ( 13 << 2 ) ->
        RegFieldGroup("MIITX_DATA", Some("MII Transmit Data"), 
          RegField.bytes(MIITX_DATA)
        ),

      ( 14 << 2 ) -> 
        RegFieldGroup("MIIRX_DATA", Some("MII Receive Data"), Seq(
          RegField.r(16, MIIRX_DATA)
        )),

      ( 15 << 2 ) ->
        RegFieldGroup("MIISTATUS", Some("MII Status Register"), Seq(
          RegField.r(1, io.LinkFail),
          RegField.r(1, io.Busy_stat),
          RegField.r(1, io.NValid_stat),
        )),

      ( 16 << 2 ) ->
        RegFieldGroup("MAC_ADDR0", Some("MAC Address Register 0"), 
          RegField.bytes(Mac_ADDR0)
        ),

      ( 17 << 2 ) ->
        RegFieldGroup("MAC_ADDR1", Some("MAC Address Register 1"), 
          RegField.bytes(Mac_ADDR1)
        ),

      ( 18 << 2 ) ->
        RegFieldGroup("HASH0", Some("HASH Register 0"), 
          RegField.bytes(HASH0)
        ),

      ( 19 << 2 ) ->
        RegFieldGroup("HASH1", Some("HASH Register 1"), 
          RegField.bytes(HASH1)
        ),
        
      ( 20 << 2 ) ->
        RegFieldGroup("TXCTRL", Some("Tx Control Register"), 
          RegField.bytes(txPauseTV, Some(RegFieldDesc("TxPauseTV", "Tx Pause Timer Value", reset=Some(0x0)))) ++ Seq(
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
    io.r_TxEn     := TxEn & (TX_BD_NUM > 0.U)      // Transmission is enabled when there is at least one TxBD.
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

    io.r_MAC := Cat( Mac_ADDR1(15,0), Mac_ADDR0(31,0) )
    io.r_HASH1 := HASH1
    io.r_HASH0 := HASH0
    io.r_TxBDNum := TX_BD_NUM
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

    when(io.TxB_IRQ){ irq_txb := true.B }
    when(io.TxE_IRQ){ irq_txe := true.B }
    when(io.RxB_IRQ){ irq_rxb := true.B }
    when(io.RxE_IRQ){ irq_rxe := true.B }
    when(io.Busy_IRQ){ irq_busy := true.B }
    when(SetTxCIrq){ irq_txc := true.B }
    when(SetRxCIrq){ irq_rxc := true.B }





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
    // io.int_o :=
    int(0)    :=
      (irq_txb  & INT_MASK.extract(0) ) | 
      (irq_txe  & INT_MASK.extract(1) ) | 
      (irq_rxb  & INT_MASK.extract(2) ) | 
      (irq_rxe  & INT_MASK.extract(3) ) | 
      (irq_busy & INT_MASK.extract(4) ) | 
      (irq_txc  & INT_MASK.extract(5) ) | 
      (irq_rxc  & INT_MASK.extract(6) )



}


