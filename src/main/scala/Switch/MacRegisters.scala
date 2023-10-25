package MAC

import chisel3._
import chisel3.util._

import freechips.rocketchip.regmapper._
import freechips.rocketchip.tilelink._
import org.chipsalliance.cde.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.interrupts._

class Mac_Config_Bundle extends Bundle{
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
  val StartTxDone         = Input(Bool())
  val TxClk               = Input(Bool())
  val RxClk               = Input(Bool())

  val r_Pad       = Output(Bool())
  val r_HugEn     = Output(Bool())
  val r_CrcEn     = Output(Bool())
  val r_DlyCrcEn  = Output(Bool())
  val r_FullD     = Output(Bool())
  val r_ExDfrEn   = Output(Bool())
  val r_LoopBck   = Output(Bool())
  val r_IFG       = Output(Bool())
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
  val r_CollValid = Output(UInt(6.W))
  val r_MiiNoPre  = Output(Bool())
  val r_ClkDiv    = Output(UInt(8.W))
  val r_WCtrlData = Output(Bool())
  val r_RStat     = Output(Bool())
  val r_ScanStat  = Output(Bool())
  val r_RGAD      = Output(UInt(5.W))
  val r_FIAD      = Output(UInt(5.W))
  val r_CtrlData  = Output(UInt(16.W))


}




class MacRegIO extends Mac_Config_Bundle{
  val asyncReset = Input(AsyncReset())


  val r_TxPtr = Output(UInt(32.W))
  val r_RxPtr = Output(UInt(32.W))
  val r_TxLen = Output(UInt(16.W))
  val r_RxLen = Input(UInt(16.W))
  val triTx = Output(Bool())
  val triRx = Input(Bool())
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


    val Pad       = RegInit(true.B)
    val HugEn     = RegInit(false.B)
    val CrcEn     = RegInit(true.B)
    val DlyCrcEn  = RegInit(false.B)

    val FullD     = RegInit(false.B)
    val ExDfrEn   = RegInit(false.B)
    val LoopBck   = RegInit(false.B)
    val IFG       = RegInit(false.B)
    val NoPre     = RegInit(false.B)
    val TxEn      = RegInit(false.B)
    val RxEn      = RegInit(false.B)

    // Interrupt generation
    val irq_txb  = RegInit(false.B)  
    val irq_txe  = RegInit(false.B)
    val irq_rxb  = RegInit(false.B)
    val irq_rxe  = RegInit(false.B)
    val irq_busy = RegInit(false.B)
    



    val INT_MASK = RegInit(0.U(7.W))        // INT_MASK Register
    val IPGT     = RegInit("h12".U(7.W))    // IPGT Register
    val IPGR1    = RegInit("h0C".U(7.W))    // IPGR1 Register
    val IPGR2    = RegInit("h12".U(7.W))

    val maxFL = RegInit("h0600".U(16.W))
    val minFL = RegInit("h0040".U(16.W))

    val collValid = RegInit("h3f".U(6.W))

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



    val HASH0 = RegInit(0.U(32.W))
    val HASH1 = RegInit(0.U(32.W))




    val txPtr = RegInit( "h80002000".U(32.W))
    val rxPtr = RegInit( "h80002000".U(32.W))
    val txLen = RegInit( 65535.U(16.W))
    // val rxLen = RegInit( 65535.U(16.W))
    val triRx = RegInit(false.B)

    io.r_TxPtr := txPtr
    io.r_RxPtr := rxPtr
    io.r_TxLen := txLen
    when( io.triRx ) { triRx := true.B }



    outer.configNode.regmap(
      ( 0 << 2 ) -> 
        RegFieldGroup("MODER", Some("Mode Register"), Seq(
          RegField(1, RxEn   , RegFieldDesc( "RxEn", "RxEn", reset = Some(0))) ,
          RegField(1, TxEn   , RegFieldDesc( "TxEn", "TxEn", reset = Some(0) ) ),
          RegField(1, NoPre  , RegFieldDesc( "NoPre", "NoPre", reset = Some(0) ) ),
          RegField.r(1, 0.U  , RegFieldDesc( "Bro", "Bro", reset = Some(0) ) ),
          RegField.r(1, 0.U  , RegFieldDesc( "Iam", "Iam Unused", reset = Some(0) ) ),
          RegField.r(1, 0.U  , RegFieldDesc( "Pro", "Pro", reset = Some(0) ) ),
          RegField(1, IFG    , RegFieldDesc( "IFG", "IFG", reset = Some(0) ) ),
          RegField(1, LoopBck, RegFieldDesc( "LoopBck", "LoopBck", reset = Some(0) ) ),
          RegField.r(1, 0.U, RegFieldDesc( "NoBckof", "NoBckof", reset = Some(0) ) ),
          RegField(1, ExDfrEn, RegFieldDesc( "ExDfrEn", "ExDfrEn", reset = Some(0) ) ),
          RegField(1, FullD  , RegFieldDesc( "FullD", "FullD", reset = Some(0) ) ),
          RegField.r(1, 0.U),
          RegField(1, DlyCrcEn, RegFieldDesc( "DlyCrcEn", "DlyCrcEn", reset=Some(0)) ),
          RegField(1, CrcEn   , RegFieldDesc( "CrcEn", "CrcEn",       reset=Some(1)) ),
          RegField.r(1, 1.U   , RegFieldDesc( "HugEn", "HugEn",       reset=Some(0)) ),
          RegField(1, Pad     , RegFieldDesc( "Pad", "Pad",           reset=Some(1)) ),
          RegField.r(1, 1.U,    RegFieldDesc( "RecSmall", "RecSmall", reset=Some(0)) )
        )),

      ( 1 << 2 ) ->
        RegFieldGroup("INT_SOURCE", Some("Interrupt Source Register"), Seq(
          RegField(1, irq_txb,  RegWriteFn((valid, data) => { when ((valid & data) === 1.U) { irq_txb := 0.U }; true.B }),  RegFieldDesc("txb", "txb", reset=Some(0))), 
          RegField(1, irq_txe,  RegWriteFn((valid, data) => { when ((valid & data) === 1.U) { irq_txe := 0.U }; true.B }),  RegFieldDesc("txe", "txe", reset=Some(0))),
          RegField(1, irq_rxb,  RegWriteFn((valid, data) => { when ((valid & data) === 1.U) { irq_rxb := 0.U }; true.B }),  RegFieldDesc("rxb", "rxb", reset=Some(0))),
          RegField(1, irq_rxe,  RegWriteFn((valid, data) => { when ((valid & data) === 1.U) { irq_rxe := 0.U }; true.B }),  RegFieldDesc("rxe", "rxe", reset=Some(0))),
          RegField(1, irq_busy, RegWriteFn((valid, data) => { when ((valid & data) === 1.U) { irq_busy := 0.U }; true.B }), RegFieldDesc("busy", "busy", reset=Some(0))),
          RegField.r(1, 0.U,  RegFieldDesc("txc", "txc", reset=Some(0))),
          RegField.r(1, 0.U,  RegFieldDesc("rxc", "rxc", reset=Some(0))),
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
          RegField.r(4, 0.U, RegFieldDesc("maxRet", "Maximum Retry", reset=Some(0xf))),
        )),
      ( 8 << 2 ) ->
        RegFieldGroup("TX_BD_NUM", Some("Transmit BD Number Register"), Seq(
          RegField.r(8, 0.U, RegFieldDesc("TX_BD_NUM", "TX_BD_NUM Un-used", reset=Some(0x40))),
        )),

      ( 9 << 2 ) ->
        RegFieldGroup("CTRLMODER", Some("Control Module Mode Register"), Seq(
          RegField.r(1, 1.U, RegFieldDesc("PassAll", "Pass All Receive Frames", reset=Some(0))),
          RegField.r(1, 0.U , RegFieldDesc("RxFlow", "Receive Flow Control", reset=Some(0))),
          RegField.r(1, 0.U , RegFieldDesc("TxFlow", "Transmit Flow Control", reset=Some(0))),
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
        RegFieldGroup("MAC_ADDR0", Some("MAC Address Register 0"), Seq(
          RegField.r(1)
        )),

      ( 17 << 2 ) ->
        RegFieldGroup("MAC_ADDR1", Some("MAC Address Register 1"), Seq(
          RegField.r(1)
        )),

      ( 18 << 2 ) ->
        RegFieldGroup("HASH0", Some("HASH Register 0"), 
          RegField.bytes(HASH0)
        ),

      ( 19 << 2 ) ->
        RegFieldGroup("HASH1", Some("HASH Register 1"), 
          RegField.bytes(HASH1)
        ),
        
      ( 20 << 2 ) ->
        RegFieldGroup("TXCTRL", Some("Tx Control Register"), Seq(
          RegField.r(8, 0,U, RegFieldDesc("TxPauseTV", "Tx Pause Timer Value", reset=Some(0x0))), 
          RegField.r(1, 0.U, RegFieldDesc("TxPauseRQ", "Tx Pause Request", reset=Some(0x0))),
        )),

      ( 30 << 2 ) ->
        RegFieldGroup("DMATrigger", Some("Tx Control DMA"), Seq(
          RegField.w(1, RegWriteFn((valid, data) => { io.triTx := (valid & (data === 1.U)) ; true.B} ), RegFieldDesc("bd", "bd", reset=Some(0x0))),
          RegField(1, triRx, RegFieldDesc("bd", "bd", reset=Some(0x0))),
        )),

      ( 31 << 2 ) ->
        RegFieldGroup("TxRxDMALength", Some("Tx  RxControl DMA"), Seq(
          RegField(16, txLen, RegFieldDesc("txLen", "length of tx", reset=Some(65535))),
          RegField.r(16, io.r_RxLen, RegFieldDesc("rxLen", "length of rx")),
        )),

      ( 32 << 2 ) ->
        RegFieldGroup("TxDMAAddress", Some("Tx Control DMA"), Seq(
          RegField(32, txPtr, RegFieldDesc("txPtr", "pointer of tx", reset=Some(0x80002000))),
        )),

      ( 33 << 2 ) ->
        RegFieldGroup("RxDMAAddress", Some("Rx Control DMA"), Seq(
          RegField(32, rxPtr, RegFieldDesc("rxPtr", "pointer of rx", reset=Some(0x80002000))),
        )),
    )




    io.r_Pad       := Pad
    io.r_HugEn     := HugEn
    io.r_CrcEn     := CrcEn
    io.r_DlyCrcEn  := DlyCrcEn
    io.r_FullD     := FullD
    io.r_ExDfrEn   := ExDfrEn
    io.r_LoopBck   := LoopBck
    io.r_IFG       := IFG
    io.r_NoPre     := NoPre
    io.r_TxEn     := TxEn
    io.r_RxEn     := RxEn

    io.r_IPGT      := IPGT
    io.r_IPGR1     := IPGR1
    io.r_IPGR2     := IPGR2
    io.r_MinFL     := minFL
    io.r_MaxFL     := maxFL
    io.r_CollValid := collValid
    io.r_MiiNoPre  := miiNoPre
    io.r_ClkDiv    := clkDiv
    io.r_WCtrlData := wCtrlData
    io.r_RStat     := readStat
    io.r_ScanStat  := scanStat
    io.r_RGAD      := RGAD
    io.r_FIAD      := FIAD
    io.r_CtrlData  := MIITX_DATA

    io.r_HASH1 := HASH1
    io.r_HASH0 := HASH0

































    when(io.TxB_IRQ){ irq_txb := true.B }
    when(io.TxE_IRQ){ irq_txe := true.B }
    when(io.RxB_IRQ){ irq_rxb := true.B }
    when(io.RxE_IRQ){ irq_rxe := true.B }
    when(io.Busy_IRQ){ irq_busy := true.B }








    // Generating interrupt signal
    // io.int_o :=
    int(0)    :=
      (irq_txb  & INT_MASK.extract(0) ) | 
      (irq_txe  & INT_MASK.extract(1) ) | 
      (irq_rxb  & INT_MASK.extract(2) ) | 
      (irq_rxe  & INT_MASK.extract(3) ) | 
      (irq_busy & INT_MASK.extract(4) )




}


