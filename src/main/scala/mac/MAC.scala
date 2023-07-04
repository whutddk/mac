package MAC

import chisel3._
import chisel3.util._

import org.chipsalliance.cde.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._

class Mac(implicit p: Parameters) extends LazyModule with HasMacParameters{
  def isTileLink = false

  // val chipLinkMasterNode = TLManagerNode(Seq(TLSlavePortParameters.v1(
  //     managers = Seq(TLSlaveParameters.v1(
  //       address = Seq(AddressSet(0x00000000L, 0x0FFFL)),
  //       regionType = RegionType.UNCACHED,
  //       executable = false,
  //       supportsGet         = TransferSizes(32/8, 32/8),
  //       supportsPutFull     = TransferSizes(32/8, 32/8),
  //       supportsPutPartial  = TransferSizes(32/8, 32/8)

  //     )),
  //     beatBytes = 32/8)))

  lazy val module = new MacImp(this)



}



class MacIO extends Bundle{
  // Tx
  val mtx_clk_pad_i = Input(Bool())
  val mtxd_pad_o    = Output(UInt(4.W))
  val mtxen_pad_o   = Output(Bool())
  val mtxerr_pad_o  = Output(Bool())

  // Rx
  val mrx_clk_pad_i = Input(Bool())
  val mrxd_pad_i    = Input(UInt(4.W))
  val mrxdv_pad_i   = Input(Bool())
  val mrxerr_pad_i  = Input(Bool())

  // Common Tx and Rx
  val mcoll_pad_i = Input(Bool())
  val mcrs_pad_i  = Input(Bool())

  val int_o = Output(Bool())
}

class MacImp(outer: Mac) extends LazyModuleImp(outer){
  def isTileLink = false
  val io = IO(new MacIO with MDIO with MacWishboneMasterIO with MacWishboneSlaveIO)
    //( if(!isTileLink) { IO(new MacIO with MDIO with MacWishboneMasterIO with MacWishboneSlaveIO) } //else {IO(new MacIO with MDIO with MacWishboneMasterIO)} )


val r_ClkDiv            = Wire(UInt(8.W))
val r_MiiNoPre          = Wire(Bool())
val r_CtrlData          = Wire(UInt(16.W))
val r_FIAD              = Wire(UInt(5.W))
val r_RGAD              = Wire(UInt(5.W))
val r_WCtrlData         = Wire(Bool())
val r_RStat             = Wire(Bool())
val r_ScanStat          = Wire(Bool())
val NValid_stat         = Wire(Bool())
val Busy_stat           = Wire(Bool())
val LinkFail            = Wire(Bool())
val Prsd                = Wire(UInt(16.W))
val WCtrlDataStart      = Wire(Bool())
val RStatStart          = Wire(Bool())
val UpdateMIIRX_DATAReg = Wire(Bool())


dontTouch( r_ClkDiv            )
dontTouch( r_MiiNoPre          )
dontTouch( r_CtrlData          )
dontTouch( r_FIAD              )
dontTouch( r_RGAD              )
dontTouch( r_WCtrlData         )
dontTouch( r_RStat             )
dontTouch( r_ScanStat          )
dontTouch( NValid_stat         )
dontTouch( Busy_stat           )
dontTouch( LinkFail            )
dontTouch( Prsd                )
dontTouch( WCtrlDataStart      )
dontTouch( RStatStart          )
dontTouch( UpdateMIIRX_DATAReg )




val TxStartFrm = Wire(Bool())
val TxEndFrm   = Wire(Bool())
val TxUsedData = Wire(Bool())
val TxData     = Wire(UInt(8.W))
val TxRetry    = Wire(Bool())
val TxAbort    = Wire(Bool())
val TxUnderRun = Wire(Bool())
val TxDone     = Wire(Bool())
val TPauseRq = Wire(Bool())

dontTouch(TxStartFrm)
dontTouch(TxEndFrm  )
dontTouch(TxUsedData)
dontTouch(TxData    )
dontTouch(TxRetry   )
dontTouch(TxAbort   )
dontTouch(TxUnderRun)
dontTouch(TxDone    )
dontTouch(TPauseRq  )

val RstTxPauseRq = RegInit(false.B)





// Connecting Miim module
val miim = Module(new MIIM)

  miim.io.Divider   := r_ClkDiv
  miim.io.NoPre     := r_MiiNoPre
  miim.io.WCtrlData := r_WCtrlData
  miim.io.CtrlData  := r_CtrlData
  miim.io.Fiad      := r_FIAD
  miim.io.Rgad      := r_RGAD
  miim.io.RStat     := r_RStat
  miim.io.ScanStat  := r_ScanStat

  miim.io.mdi   := io.mdi

  Busy_stat := miim.io.Busy
  LinkFail  := miim.io.LinkFail
  NValid_stat := miim.io.Nvalid
  Prsd := miim.io.Prsd
  WCtrlDataStart := miim.io.WCtrlDataStart
  RStatStart := miim.io.RStatStart
  UpdateMIIRX_DATAReg := miim.io.UpdateMIIRX_DATAReg

  io.mdc   := miim.io.mdc
  io.mdo   := miim.io.mdo
  io.mdoEn := miim.io.mdoEn
  


 







val RegDataOut                 = Wire(UInt(32.W))
val r_RecSmall                 = Wire(Bool())
val r_LoopBck                  = Wire(Bool())
val r_TxEn                     = Wire(Bool())
val r_RxEn                     = Wire(Bool())
val MRxDV_Lb                   = Wire(Bool())
val MRxErr_Lb                  = Wire(Bool())
val MRxD_Lb                    = Wire(UInt(4.W))
val Transmitting               = Wire(Bool())
val r_HugEn                    = Wire(Bool())
val r_DlyCrcEn                 = Wire(Bool())
val r_MaxFL                    = Wire(UInt(16.W))
val r_MinFL                    = Wire(UInt(16.W))
val ShortFrame                 = Wire(Bool())
val DribbleNibble              = Wire(Bool())
val ReceivedPacketTooBig       = Wire(Bool())
val r_MAC                      = Wire(UInt(48.W))
val LoadRxStatus               = Wire(Bool())
val r_HASH0                    = Wire(UInt(32.W))
val r_HASH1                    = Wire(UInt(32.W))
val r_TxBDNum                  = Wire(UInt(8.W))
val r_IPGT                     = Wire(UInt(7.W))
val r_IPGR1                    = Wire(UInt(7.W))
val r_IPGR2                    = Wire(UInt(7.W))
val r_CollValid                = Wire(UInt(6.W))
val r_TxPauseTV                = Wire(UInt(16.W))
val r_TxPauseRq                = Wire(Bool())
val r_MaxRet                   = Wire(UInt(4.W))
val r_NoBckof                  = Wire(Bool())
val r_ExDfrEn                  = Wire(Bool())
val r_TxFlow                   = Wire(Bool())
val r_IFG                      = Wire(Bool())
val TxB_IRQ                    = Wire(Bool())
val TxE_IRQ                    = Wire(Bool())
val RxB_IRQ                    = Wire(Bool())
val RxE_IRQ                    = Wire(Bool())
val Busy_IRQ                   = Wire(Bool())
val r_Pad                      = Wire(Bool())
val r_CrcEn                    = Wire(Bool())
val r_FullD                    = Wire(Bool())
val r_Pro                      = Wire(Bool())
val r_Bro                      = Wire(Bool())
val r_NoPre                    = Wire(Bool())
val r_RxFlow                   = Wire(Bool())
val r_PassAll                  = Wire(Bool())
val TxCtrlEndFrm               = Wire(Bool())
val StartTxDone                = Wire(Bool())
val SetPauseTimer              = Wire(Bool())
val TxUsedDataIn               = Wire(Bool())
val TxDoneIn                   = Wire(Bool())
val TxAbortIn                  = Wire(Bool())
val PerPacketPad               = Wire(Bool())
val PadOut                     = Wire(Bool())
val PerPacketCrcEn             = Wire(Bool())
val CrcEnOut                   = Wire(Bool())
val TxStartFrmOut              = Wire(Bool())
val TxEndFrmOut                = Wire(Bool())
val ReceivedPauseFrm           = Wire(Bool())
val ControlFrmAddressOK        = Wire(Bool())
val RxStatusWriteLatched_sync2 = Wire(Bool())
val LateCollision              = Wire(Bool())
val DeferIndication            = Wire(Bool())
val LateCollLatched            = Wire(Bool())
val DeferLatched               = Wire(Bool())
val RstDeferLatched            = Wire(Bool())
val CarrierSenseLost           = Wire(Bool())
val RegCs                      = Wire(UInt(4.W))

dontTouch(RegDataOut                 )
dontTouch(r_RecSmall                 )
dontTouch(r_LoopBck                  )
dontTouch(r_TxEn                     )
dontTouch(r_RxEn                     )
dontTouch(MRxDV_Lb                   )
dontTouch(MRxErr_Lb                  )
dontTouch(MRxD_Lb                    )
dontTouch(Transmitting               )
dontTouch(r_HugEn                    )
dontTouch(r_DlyCrcEn                 )
dontTouch(r_MaxFL                    )
dontTouch(r_MinFL                    )
dontTouch(ShortFrame                 )
dontTouch(DribbleNibble              )
dontTouch(ReceivedPacketTooBig       )
dontTouch(r_MAC                      )
dontTouch(LoadRxStatus               )
dontTouch(r_HASH0                    )
dontTouch(r_HASH1                    )
dontTouch(r_TxBDNum                  )
dontTouch(r_IPGT                     )
dontTouch(r_IPGR1                    )
dontTouch(r_IPGR2                    )
dontTouch(r_CollValid                )
dontTouch(r_TxPauseTV                )
dontTouch(r_TxPauseRq                )
dontTouch(r_MaxRet                   )
dontTouch(r_NoBckof                  )
dontTouch(r_ExDfrEn                  )
dontTouch(r_TxFlow                   )
dontTouch(r_IFG                      )
dontTouch(TxB_IRQ                    )
dontTouch(TxE_IRQ                    )
dontTouch(RxB_IRQ                    )
dontTouch(RxE_IRQ                    )
dontTouch(Busy_IRQ                   )
dontTouch(r_Pad                      )
dontTouch(r_CrcEn                    )
dontTouch(r_FullD                    )
dontTouch(r_Pro                      )
dontTouch(r_Bro                      )
dontTouch(r_NoPre                    )
dontTouch(r_RxFlow                   )
dontTouch(r_PassAll                  )
dontTouch(TxCtrlEndFrm               )
dontTouch(StartTxDone                )
dontTouch(SetPauseTimer              )
dontTouch(TxUsedDataIn               )
dontTouch(TxDoneIn                   )
dontTouch(TxAbortIn                  )
dontTouch(PerPacketPad               )
dontTouch(PadOut                     )
dontTouch(PerPacketCrcEn             )
dontTouch(CrcEnOut                   )
dontTouch(TxStartFrmOut              )
dontTouch(TxEndFrmOut                )
dontTouch(ReceivedPauseFrm           )
dontTouch(ControlFrmAddressOK        )
dontTouch(RxStatusWriteLatched_sync2 )
dontTouch(LateCollision              )
dontTouch(DeferIndication            )
dontTouch(LateCollLatched            )
dontTouch(DeferLatched               )
dontTouch(RstDeferLatched            )
dontTouch(CarrierSenseLost           )
dontTouch(RegCs                      )







val ethReg = Module(new MacReg)

ethReg.io.DataIn              := io.WB_DAT_I
ethReg.io.Address             := io.WB_ADR_I(9,2)
ethReg.io.Rw                  := io.WB_WE_I
ethReg.io.Cs                  := RegCs
ethReg.io.WCtrlDataStart      := WCtrlDataStart
ethReg.io.RStatStart          := RStatStart
ethReg.io.UpdateMIIRX_DATAReg := UpdateMIIRX_DATAReg
ethReg.io.Prsd                := Prsd
ethReg.io.NValid_stat         := NValid_stat
ethReg.io.Busy_stat           := Busy_stat
ethReg.io.LinkFail            := LinkFail
ethReg.io.TxB_IRQ             := TxB_IRQ
ethReg.io.TxE_IRQ             := TxE_IRQ
ethReg.io.RxB_IRQ             := RxB_IRQ
ethReg.io.RxE_IRQ             := RxE_IRQ
ethReg.io.Busy_IRQ            := Busy_IRQ
ethReg.io.RstTxPauseRq        := RstTxPauseRq
ethReg.io.TxCtrlEndFrm        := TxCtrlEndFrm
ethReg.io.StartTxDone         := StartTxDone
ethReg.io.TxClk               := io.mtx_clk_pad_i
ethReg.io.RxClk               := io.mrx_clk_pad_i
ethReg.io.SetPauseTimer       := SetPauseTimer
ethReg.io.dbg_dat             := 0.U

RegDataOut  := ethReg.io.DataOut
r_RecSmall  := ethReg.io.r_RecSmall
r_Pad       := ethReg.io.r_Pad
r_HugEn     := ethReg.io.r_HugEn
r_CrcEn     := ethReg.io.r_CrcEn
r_DlyCrcEn  := ethReg.io.r_DlyCrcEn
r_FullD     := ethReg.io.r_FullD
r_ExDfrEn   := ethReg.io.r_ExDfrEn
r_NoBckof   := ethReg.io.r_NoBckof
r_LoopBck   := ethReg.io.r_LoopBck
r_IFG       := ethReg.io.r_IFG
r_Pro       := ethReg.io.r_Pro
r_Bro       := ethReg.io.r_Bro
r_NoPre     := ethReg.io.r_NoPre
r_TxEn      := ethReg.io.r_TxEn
r_RxEn      := ethReg.io.r_RxEn
r_HASH0     := ethReg.io.r_HASH0
r_HASH1     := ethReg.io.r_HASH1
r_IPGT      := ethReg.io.r_IPGT
r_IPGR1     := ethReg.io.r_IPGR1
r_IPGR2     := ethReg.io.r_IPGR2
r_MinFL     := ethReg.io.r_MinFL
r_MaxFL     := ethReg.io.r_MaxFL
r_MaxRet    := ethReg.io.r_MaxRet
r_CollValid := ethReg.io.r_CollValid
r_TxFlow    := ethReg.io.r_TxFlow
r_RxFlow    := ethReg.io.r_RxFlow
r_PassAll   := ethReg.io.r_PassAll
r_MiiNoPre  := ethReg.io.r_MiiNoPre
r_ClkDiv    := ethReg.io.r_ClkDiv
r_WCtrlData := ethReg.io.r_WCtrlData
r_RStat     := ethReg.io.r_RStat
r_ScanStat  := ethReg.io.r_ScanStat
r_RGAD      := ethReg.io.r_RGAD
r_FIAD      := ethReg.io.r_FIAD
r_CtrlData  := ethReg.io.r_CtrlData
r_MAC       := ethReg.io.r_MAC
r_TxBDNum   := ethReg.io.r_TxBDNum
io.int_o    := ethReg.io.int_o
r_TxPauseTV := ethReg.io.r_TxPauseTV
r_TxPauseRq := ethReg.io.r_TxPauseRq




val RxData               = Wire(UInt(8.W))
val RxValid              = Wire(Bool())
val RxStartFrm           = Wire(Bool())
val RxEndFrm             = Wire(Bool())
val RxAbort              = Wire(Bool())
val WillTransmit         = Wire(Bool())
val ResetCollision       = Wire(Bool())
val TxDataOut            = Wire(UInt(8.W))
val WillSendControlFrame = Wire(Bool())
val ReceiveEnd           = Wire(Bool())
val ReceivedPacketGood   = Wire(Bool())
val ReceivedLengthOK     = Wire(Bool())
val InvalidSymbol        = Wire(Bool())
val LatchedCrcError      = Wire(Bool())
val RxLateCollision      = Wire(Bool())
val RetryCntLatched      = Wire(UInt(4.W))  
val RetryCnt             = Wire(UInt(4.W))
val StartTxAbort         = Wire(Bool())
val MaxCollisionOccured  = Wire(Bool())
val RetryLimit           = Wire(Bool())
val StatePreamble        = Wire(Bool())
val StateData            = Wire(UInt(2.W))

dontTouch(RxData              )
dontTouch(RxValid             )
dontTouch(RxStartFrm          )
dontTouch(RxEndFrm            )
dontTouch(RxAbort             )
dontTouch(WillTransmit        )
dontTouch(ResetCollision      )
dontTouch(TxDataOut           )
dontTouch(WillSendControlFrame)
dontTouch(ReceiveEnd          )
dontTouch(ReceivedPacketGood  )
dontTouch(ReceivedLengthOK    )
dontTouch(InvalidSymbol       )
dontTouch(LatchedCrcError     )
dontTouch(RxLateCollision     )
dontTouch(RetryCntLatched     )
dontTouch(RetryCnt            )
dontTouch(StartTxAbort        )
dontTouch(MaxCollisionOccured )
dontTouch(RetryLimit          )
dontTouch(StatePreamble       )
dontTouch(StateData           )


// Connecting MACControl
val maccontrol = Module(new MacControl)

maccontrol.io.MTxClk                     := io.mtx_clk_pad_i
maccontrol.io.MRxClk                     := io.mrx_clk_pad_i
maccontrol.io.TxReset                    := reset.asBool
maccontrol.io.RxReset                    := reset.asBool
maccontrol.io.TPauseRq                   := TPauseRq
maccontrol.io.TxDataIn                   := TxData
maccontrol.io.TxStartFrmIn               := TxStartFrm
maccontrol.io.TxUsedDataIn               := TxUsedDataIn
maccontrol.io.TxEndFrmIn                 := TxEndFrm
maccontrol.io.TxDoneIn                   := TxDoneIn
maccontrol.io.TxAbortIn                  := TxAbortIn
maccontrol.io.PadIn                      := r_Pad | PerPacketPad
maccontrol.io.CrcEnIn                    := r_CrcEn | PerPacketCrcEn
maccontrol.io.RxData                     := RxData
maccontrol.io.RxValid                    := RxValid
maccontrol.io.RxStartFrm                 := RxStartFrm
maccontrol.io.RxEndFrm                   := RxEndFrm
maccontrol.io.ReceiveEnd                 := ReceiveEnd
maccontrol.io.ReceivedPacketGood         := ReceivedPacketGood
maccontrol.io.ReceivedLengthOK           := ReceivedLengthOK
maccontrol.io.TxFlow                     := r_TxFlow
maccontrol.io.RxFlow                     := r_RxFlow
maccontrol.io.DlyCrcEn                   := r_DlyCrcEn
maccontrol.io.TxPauseTV                  := r_TxPauseTV
maccontrol.io.MAC                        := r_MAC
maccontrol.io.RxStatusWriteLatched_sync2 := RxStatusWriteLatched_sync2
maccontrol.io.r_PassAll                  := r_PassAll

TxDataOut := maccontrol.io.TxDataOut
TxStartFrmOut := maccontrol.io.TxStartFrmOut
TxEndFrmOut := maccontrol.io.TxEndFrmOut
TxDone := maccontrol.io.TxDoneOut
TxAbort := maccontrol.io.TxAbortOut
TxUsedData := maccontrol.io.TxUsedDataOut
PadOut := maccontrol.io.PadOut
CrcEnOut := maccontrol.io.CrcEnOut
WillSendControlFrame := maccontrol.io.WillSendControlFrame
TxCtrlEndFrm := maccontrol.io.TxCtrlEndFrm
ReceivedPauseFrm := maccontrol.io.ReceivedPauseFrm
ControlFrmAddressOK := maccontrol.io.ControlFrmAddressOK
SetPauseTimer := maccontrol.io.SetPauseTimer






val TxCarrierSense   = Wire(Bool())
val Collision        = Wire(Bool())
val CarrierSense_Tx2 = Wire(Bool())
val RxEnSync         = Wire(Bool())

dontTouch(TxCarrierSense  )
dontTouch(Collision       )
dontTouch(CarrierSense_Tx2)
dontTouch(RxEnSync        )


// Muxed MII receive data valid
MRxDV_Lb := Mux(r_LoopBck, io.mtxen_pad_o, io.mrxdv_pad_i & RxEnSync)

// Muxed MII Receive Error
MRxErr_Lb := Mux(r_LoopBck, io.mtxerr_pad_o, io.mrxerr_pad_i & RxEnSync)

// Muxed MII Receive Data
MRxD_Lb := Mux(r_LoopBck, io.mtxd_pad_o, io.mrxd_pad_i)


val txethmac = Module(new MacTx)
txethmac.clock := io.mtx_clk_pad_i.asClock
txethmac.reset := reset.asBool
txethmac.io.TxStartFrm      := TxStartFrmOut
txethmac.io.TxEndFrm        := TxEndFrmOut
txethmac.io.TxUnderRun      := TxUnderRun
txethmac.io.TxData          := TxDataOut
txethmac.io.CarrierSense    := TxCarrierSense
txethmac.io.Collision       := Collision
txethmac.io.Pad             := PadOut
txethmac.io.CrcEn           := CrcEnOut
txethmac.io.FullD           := r_FullD
txethmac.io.HugEn           := r_HugEn
txethmac.io.DlyCrcEn        := r_DlyCrcEn
txethmac.io.MinFL           := r_MinFL
txethmac.io.MaxFL           := r_MaxFL
txethmac.io.IPGT            := r_IPGT
txethmac.io.IPGR1           := r_IPGR1
txethmac.io.IPGR2           := r_IPGR2
txethmac.io.CollValid       := r_CollValid
txethmac.io.MaxRet          := r_MaxRet
txethmac.io.NoBckof         := r_NoBckof
txethmac.io.ExDfrEn         := r_ExDfrEn

io.mtxd_pad_o    := txethmac.io.MTxD
io.mtxen_pad_o   := txethmac.io.MTxEn
io. mtxerr_pad_o := txethmac.io.MTxErr
TxDoneIn := txethmac.io.TxDone
TxRetry := txethmac.io.TxRetry
TxAbortIn := txethmac.io.TxAbort
TxUsedDataIn := txethmac.io.TxUsedData
WillTransmit := txethmac.io.WillTransmit
ResetCollision := txethmac.io.ResetCollision
RetryCnt := txethmac.io.RetryCnt
StartTxDone := txethmac.io.StartTxDone
StartTxAbort := txethmac.io.StartTxAbort
MaxCollisionOccured := txethmac.io.MaxCollisionOccured
LateCollision := txethmac.io.LateCollision
DeferIndication := txethmac.io.DeferIndication
StatePreamble := txethmac.io.StatePreamble
StateData := txethmac.io.StateData





val RxByteCnt         = Wire(UInt(16.W))
val RxByteCntEq0      = Wire(Bool())
val RxByteCntGreat2   = Wire(Bool())
val RxByteCntMaxFrame = Wire(Bool())
val RxCrcError        = Wire(Bool())
val RxStateIdle       = Wire(Bool())
val RxStatePreamble   = Wire(Bool())
val RxStateSFD        = Wire(Bool())
val RxStateData       = Wire(UInt(2.W))
val AddressMiss       = Wire(Bool())


dontTouch(RxByteCnt        )
dontTouch(RxByteCntEq0     )
dontTouch(RxByteCntGreat2  )
dontTouch(RxByteCntMaxFrame)
dontTouch(RxCrcError       )
dontTouch(RxStateIdle      )
dontTouch(RxStatePreamble  )
dontTouch(RxStateSFD       )
dontTouch(RxStateData      )
dontTouch(AddressMiss      )

val rxethmac = Module(new MacRx)
  rxethmac.clock := io.mrx_clk_pad_i.asClock
  rxethmac.reset := reset.asBool
  rxethmac.io.MRxDV               := MRxDV_Lb
  rxethmac.io.MRxD                := MRxD_Lb
  rxethmac.io.Transmitting        := Transmitting
  rxethmac.io.HugEn               := r_HugEn
  rxethmac.io.DlyCrcEn            := r_DlyCrcEn
  rxethmac.io.MaxFL               := r_MaxFL
  rxethmac.io.r_IFG               := r_IFG
  rxethmac.io.MAC                 := r_MAC
  rxethmac.io.r_Bro               := r_Bro
  rxethmac.io.r_Pro               := r_Pro
  rxethmac.io.r_HASH0             := r_HASH0
  rxethmac.io.r_HASH1             := r_HASH1
  rxethmac.io.PassAll             := r_PassAll
  rxethmac.io.ControlFrmAddressOK := ControlFrmAddressOK

  RxData            := rxethmac.io.RxData
  RxValid           := rxethmac.io.RxValid
  RxStartFrm        := rxethmac.io.RxStartFrm
  RxEndFrm          := rxethmac.io.RxEndFrm
  RxByteCnt         := rxethmac.io.ByteCnt
  RxByteCntEq0      := rxethmac.io.ByteCntEq0
  RxByteCntGreat2   := rxethmac.io.ByteCntGreat2
  RxByteCntMaxFrame := rxethmac.io.ByteCntMaxFrame
  RxCrcError        := rxethmac.io.CrcError
  RxStateIdle       := rxethmac.io.StateIdle
  RxStatePreamble   := rxethmac.io.StatePreamble
  RxStateSFD        := rxethmac.io.StateSFD
  RxStateData       := rxethmac.io.StateData
  RxAbort           := rxethmac.io.RxAbort
  AddressMiss       := rxethmac.io.AddressMiss


  withClockAndReset( io.mtx_clk_pad_i.asClock, reset ) {
    // MII Carrier Sense Synchronization
    CarrierSense_Tx2 := ShiftRegister(io.mcrs_pad_i, 2, false.B, true.B)

    TxCarrierSense := ~r_FullD & CarrierSense_Tx2

    val Collision_Tx1 = RegNext(io.mcoll_pad_i, false.B)
    val Collision_Tx2 = RegInit(false.B)

    when(ResetCollision){
      Collision_Tx2 := false.B
    } .elsewhen(Collision_Tx1){
      Collision_Tx2 := true.B
    }


    // Synchronized Collision
    Collision := ~r_FullD & Collision_Tx2

  }




  withClockAndReset( io.mrx_clk_pad_i.asClock, reset ) {
    val WillTransmit_q = ShiftRegister(WillTransmit, 2, false.B, true.B)

    Transmitting := ~r_FullD & WillTransmit_q
    RxEnSync := RegEnable(r_RxEn, false.B, ~io.mrxdv_pad_i)
  }




  // Synchronizing WillSendControlFrame to WB_CLK;
  val WillSendControlFrame_sync = ShiftRegisters(WillSendControlFrame, 3, false.B, true.B)

  when(true.B){
    RstTxPauseRq := WillSendControlFrame_sync(1) & ~WillSendControlFrame_sync(2)    
  }


  withClockAndReset( io.mtx_clk_pad_i.asClock, reset ) {
    val TxPauseRq_sync = ShiftRegisters((r_TxPauseRq & r_TxFlow), 3, false.B, true.B )

    TPauseRq := RegNext( TxPauseRq_sync(1) & (~TxPauseRq_sync(2)), false.B )
  }

  val LatchedMRxErr = Wire(Bool())




  val RxAbort_latch_wire = Wire(Bool())
  val RxAbort_wb = ShiftRegister( RxAbort_latch_wire, 2, false.B, true.B )

  withClockAndReset( io.mrx_clk_pad_i.asClock, reset ) {
    val RxAbort_latch = RegInit(false.B); RxAbort_latch_wire := RxAbort_latch
    val RxAbortRst = ShiftRegister( RxAbort_wb, 2, false.B, true.B )
    
    // Synchronizing RxAbort to the WISHBONE clock
    when(RxAbort | (ShortFrame & ~r_RecSmall) | LatchedMRxErr & ~InvalidSymbol | (ReceivedPauseFrm & (~r_PassAll))){
      RxAbort_latch := true.B
    } .elsewhen(RxAbortRst){
      RxAbort_latch := false.B
    }

  }




  val wishbone = Module(new MacTileLink)

  wishbone.io.WB_DAT_I := io.WB_DAT_I
  io.WB_DAT_O := wishbone.io.WB_DAT_O
  wishbone.io.WB_ADR_I := io.WB_ADR_I
  wishbone.io.WB_WE_I  := io.WB_WE_I
  wishbone.io.WB_SEL_I := io.WB_SEL_I
  wishbone.io.WB_CYC_I := io.WB_CYC_I
  wishbone.io.WB_STB_I := io.WB_STB_I
  io.WB_ACK_O := wishbone.io.WB_ACK_O
  io.WB_ERR_O := wishbone.io.WB_ERR_O


  io.m_wb_adr_o := wishbone.io.m_wb_adr_o
  io.m_wb_sel_o := wishbone.io.m_wb_sel_o
  io.m_wb_we_o  := wishbone.io.m_wb_we_o
  io.m_wb_dat_o := wishbone.io.m_wb_dat_o
  io.m_wb_cyc_o := wishbone.io.m_wb_cyc_o
  io.m_wb_stb_o := wishbone.io.m_wb_stb_o
  wishbone.io.m_wb_dat_i := io.m_wb_dat_i
  wishbone.io.m_wb_ack_i := io.m_wb_ack_i
  wishbone.io.m_wb_err_i := io.m_wb_err_i
  io.m_wb_cti_o := wishbone.io.m_wb_cti_o
  io.m_wb_bte_o := wishbone.io.m_wb_bte_o

  wishbone.io.InvalidSymbol        := InvalidSymbol
  wishbone.io.LatchedCrcError      := LatchedCrcError
  wishbone.io.RxLateCollision      := RxLateCollision
  wishbone.io.ShortFrame           := ShortFrame
  wishbone.io.DribbleNibble        := DribbleNibble
  wishbone.io.ReceivedPacketTooBig := ReceivedPacketTooBig
  wishbone.io.RxLength             := RxByteCnt
  wishbone.io.LoadRxStatus         := LoadRxStatus
  wishbone.io.ReceivedPacketGood   := ReceivedPacketGood
  wishbone.io.AddressMiss          := AddressMiss
  wishbone.io.r_RxFlow             := r_RxFlow
  wishbone.io.r_PassAll            := r_PassAll
  wishbone.io.ReceivedPauseFrm     := ReceivedPauseFrm

  wishbone.io.RetryCntLatched  := RetryCntLatched
  wishbone.io.RetryLimit       := RetryLimit
  wishbone.io.LateCollLatched  := LateCollLatched
  wishbone.io.DeferLatched     := DeferLatched
  RstDeferLatched := wishbone.io.RstDeferLatched
  wishbone.io.CarrierSenseLost := CarrierSenseLost

  wishbone.io.MTxClk         := io.mtx_clk_pad_i
  wishbone.io.TxUsedData     := TxUsedData
  wishbone.io.TxRetry        := TxRetry
  wishbone.io.TxAbort        := TxAbort
  wishbone.io.TxDone         := TxDone
  TxStartFrm     := wishbone.io.TxStartFrm
  TxEndFrm       := wishbone.io.TxEndFrm
  TxData         := wishbone.io.TxData
  TxUnderRun     := wishbone.io.TxUnderRun
  PerPacketCrcEn := wishbone.io.PerPacketCrcEn
  PerPacketPad   := wishbone.io.PerPacketPad

  wishbone.io.MRxClk         := io.mrx_clk_pad_i
  wishbone.io.RxData         := RxData
  wishbone.io.RxValid        := RxValid
  wishbone.io.RxStartFrm     := RxStartFrm
  wishbone.io.RxEndFrm       := RxEndFrm
  wishbone.io.RxAbort        := RxAbort_wb
  RxStatusWriteLatched_sync2 := wishbone.io.RxStatusWriteLatched_sync2

  wishbone.io.r_TxEn     := r_TxEn
  wishbone.io.r_RxEn     := r_RxEn
  wishbone.io.r_TxBDNum  := r_TxBDNum
  wishbone.io.RegDataOut := RegDataOut
  RegCs := wishbone.io.RegCs
  TxB_IRQ  := wishbone.io.TxB_IRQ
  TxE_IRQ  := wishbone.io.TxE_IRQ
  RxB_IRQ  := wishbone.io.RxB_IRQ
  RxE_IRQ  := wishbone.io.RxE_IRQ
  Busy_IRQ := wishbone.io.Busy_IRQ



  val macstatus = Module(new MacStatus)

  macstatus.io.reset               := reset.asBool
  macstatus.io.MRxClk              := io.mrx_clk_pad_i
  macstatus.io.RxCrcError          := RxCrcError
  macstatus.io.MRxErr              := MRxErr_Lb
  macstatus.io.MRxDV               := MRxDV_Lb
  macstatus.io.RxStateSFD          := RxStateSFD
  macstatus.io.RxStateData         := RxStateData
  macstatus.io.RxStatePreamble     := RxStatePreamble
  macstatus.io.RxStateIdle         := RxStateIdle
  macstatus.io.Transmitting        := Transmitting
  macstatus.io.RxByteCnt           := RxByteCnt
  macstatus.io.RxByteCntEq0        := RxByteCntEq0
  macstatus.io.RxByteCntGreat2     := RxByteCntGreat2
  macstatus.io.RxByteCntMaxFrame   := RxByteCntMaxFrame
  macstatus.io.MRxD                := MRxD_Lb
  macstatus.io.Collision           := io.mcoll_pad_i
  macstatus.io.CollValid           := r_CollValid
  macstatus.io.r_RecSmall          := r_RecSmall
  macstatus.io.r_MinFL             := r_MinFL
  macstatus.io.r_MaxFL             := r_MaxFL
  macstatus.io.r_HugEn             := r_HugEn
  macstatus.io.StartTxDone         := StartTxDone
  macstatus.io.StartTxAbort        := StartTxAbort
  macstatus.io.RetryCnt            := RetryCnt
  macstatus.io.MTxClk              := io.mtx_clk_pad_i
  macstatus.io.MaxCollisionOccured := MaxCollisionOccured
  macstatus.io.LateCollision       := LateCollision
  macstatus.io.DeferIndication     := DeferIndication
  macstatus.io.TxStartFrm          := TxStartFrmOut
  macstatus.io.StatePreamble       := StatePreamble
  macstatus.io.StateData           := StateData
  macstatus.io.CarrierSense        := CarrierSense_Tx2
  macstatus.io.TxUsedData          := TxUsedDataIn
  macstatus.io.Loopback            := r_LoopBck
  macstatus.io.r_FullD             := r_FullD
  macstatus.io.RstDeferLatched     := RstDeferLatched

  ReceivedLengthOK     := macstatus.io.ReceivedLengthOK
  ReceiveEnd           := macstatus.io.ReceiveEnd
  ReceivedPacketGood   := macstatus.io.ReceivedPacketGood
  InvalidSymbol        := macstatus.io.InvalidSymbol
  LatchedCrcError      := macstatus.io.LatchedCrcError
  RxLateCollision      := macstatus.io.RxLateCollision
  ShortFrame           := macstatus.io.ShortFrame
  DribbleNibble        := macstatus.io.DribbleNibble
  ReceivedPacketTooBig := macstatus.io.ReceivedPacketTooBig
  LoadRxStatus         := macstatus.io.LoadRxStatus
  RetryCntLatched      := macstatus.io.RetryCntLatched
  RetryLimit           := macstatus.io.RetryLimit
  LateCollLatched      := macstatus.io.LateCollLatched
  DeferLatched         := macstatus.io.DeferLatched
  CarrierSenseLost     := macstatus.io.CarrierSenseLost
  LatchedMRxErr        := macstatus.io.LatchedMRxErr

}
