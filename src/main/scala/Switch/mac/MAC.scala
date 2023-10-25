package MAC

import chisel3._
import chisel3.util._

import Switch._

import freechips.rocketchip.util._

class MII extends Bundle with MDIO{
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
}



class MacIO extends Bundle{
  val mii = new MII
  val cfg = Flipped(new Mac_Config_Bundle)
  val rxEnq = new Receive_Enq_Bundle
  val txDeq = Flipped(new Transmit_Bundle)

  // val int_o = Output(Bool())

  val isLoopBack = Output(Bool())
  val asyncReset = Input(AsyncReset())
}


class Mac extends Module{

  val io = IO(new MacIO)


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
val TxDone     = Wire(Bool())
val TPauseRq = Wire(Bool())

dontTouch(TxStartFrm)
dontTouch(TxEndFrm  )
dontTouch(TxUsedData)
dontTouch(TxData    )
dontTouch(TxRetry   )
dontTouch(TxAbort   )
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

  miim.io.mdi   := io.mii.mdi

  Busy_stat := miim.io.Busy
  LinkFail  := miim.io.LinkFail
  NValid_stat := miim.io.Nvalid
  Prsd := miim.io.Prsd
  WCtrlDataStart := miim.io.WCtrlDataStart
  RStatStart := miim.io.RStatStart
  UpdateMIIRX_DATAReg := miim.io.UpdateMIIRX_DATAReg

  io.mii.mdc   := miim.io.mdc
  io.mii.mdo   := miim.io.mdo
  io.mii.mdoEn := miim.io.mdoEn
  


 





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
val r_NoPre                    = Wire(Bool())
val r_RxFlow                   = Wire(Bool())
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
val RxStatusWriteLatchedSync   = Wire(Bool())
val LateCollision              = Wire(Bool())
val DeferIndication            = Wire(Bool())
val LateCollLatched            = Wire(Bool())
val DeferLatched               = Wire(Bool())
val RstDeferLatched            = Wire(Bool())
val CarrierSenseLost           = Wire(Bool())

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
dontTouch(r_NoPre                    )
dontTouch(r_RxFlow                   )
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
dontTouch(RxStatusWriteLatchedSync )
dontTouch(LateCollision              )
dontTouch(DeferIndication            )
dontTouch(LateCollLatched            )
dontTouch(DeferLatched               )
dontTouch(RstDeferLatched            )
dontTouch(CarrierSenseLost           )





// val ethReg = Module(new MacReg(outer.configNode))



io.cfg.WCtrlDataStart      := WCtrlDataStart
io.cfg.RStatStart          := RStatStart
io.cfg.UpdateMIIRX_DATAReg := UpdateMIIRX_DATAReg
io.cfg.Prsd                := Prsd
io.cfg.NValid_stat         := NValid_stat
io.cfg.Busy_stat           := Busy_stat
io.cfg.LinkFail            := LinkFail
io.cfg.TxB_IRQ             := TxB_IRQ
io.cfg.TxE_IRQ             := TxE_IRQ
io.cfg.RxB_IRQ             := RxB_IRQ
io.cfg.RxE_IRQ             := RxE_IRQ
io.cfg.Busy_IRQ            := Busy_IRQ
io.cfg.RstTxPauseRq        := RstTxPauseRq
io.cfg.TxCtrlEndFrm        := TxCtrlEndFrm
io.cfg.StartTxDone         := StartTxDone
io.cfg.TxClk               := io.mii.mtx_clk_pad_i
io.cfg.RxClk               := io.mii.mrx_clk_pad_i
io.cfg.SetPauseTimer       := SetPauseTimer

r_Pad       := io.cfg.r_Pad
r_HugEn     := io.cfg.r_HugEn
r_CrcEn     := io.cfg.r_CrcEn
r_DlyCrcEn  := io.cfg.r_DlyCrcEn
r_FullD     := io.cfg.r_FullD
r_ExDfrEn   := io.cfg.r_ExDfrEn
r_NoBckof   := io.cfg.r_NoBckof
r_LoopBck   := io.cfg.r_LoopBck
r_IFG       := io.cfg.r_IFG
r_NoPre     := io.cfg.r_NoPre
r_TxEn      := io.cfg.r_TxEn
r_RxEn      := io.cfg.r_RxEn
r_HASH0     := io.cfg.r_HASH0
r_HASH1     := io.cfg.r_HASH1
r_IPGT      := io.cfg.r_IPGT
r_IPGR1     := io.cfg.r_IPGR1
r_IPGR2     := io.cfg.r_IPGR2
r_MinFL     := io.cfg.r_MinFL
r_MaxFL     := io.cfg.r_MaxFL
r_MaxRet    := io.cfg.r_MaxRet
r_CollValid := io.cfg.r_CollValid
r_TxFlow    := io.cfg.r_TxFlow
r_RxFlow    := io.cfg.r_RxFlow
r_MiiNoPre  := io.cfg.r_MiiNoPre
r_ClkDiv    := io.cfg.r_ClkDiv
r_WCtrlData := io.cfg.r_WCtrlData
r_RStat     := io.cfg.r_RStat
r_ScanStat  := io.cfg.r_ScanStat
r_RGAD      := io.cfg.r_RGAD
r_FIAD      := io.cfg.r_FIAD
r_CtrlData  := io.cfg.r_CtrlData
r_MAC       := io.cfg.r_MAC
r_TxBDNum   := io.cfg.r_TxBDNum
r_TxPauseTV := io.cfg.r_TxPauseTV
r_TxPauseRq := io.cfg.r_TxPauseRq




val RxData               = Wire(UInt(8.W))
val RxValid              = Wire(Bool())
val RxStartFrm           = Wire(Bool())
val RxEndFrm             = Wire(Bool())
val WillTransmit         = Wire(Bool())
val ResetCollision       = Wire(Bool())
val TxDataOut            = Wire(UInt(8.W))
val WillSendControlFrame = Wire(Bool())
val ReceiveEnd           = Wire(Bool())
val ReceivedPacketGood   = Wire(Bool())
val ReceivedLengthOK     = Wire(Bool())
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
dontTouch(WillTransmit        )
dontTouch(ResetCollision      )
dontTouch(TxDataOut           )
dontTouch(WillSendControlFrame)
dontTouch(ReceiveEnd          )
dontTouch(ReceivedPacketGood  )
dontTouch(ReceivedLengthOK    )
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

maccontrol.io.MTxClk                     := io.mii.mtx_clk_pad_i
maccontrol.io.MRxClk                     := io.mii.mrx_clk_pad_i
maccontrol.io.asyncReset                 := io.asyncReset

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
maccontrol.io.RxStatusWriteLatched_sync2 := RxStatusWriteLatchedSync

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


io.isLoopBack := r_LoopBck

// Muxed MII receive data valid
MRxDV_Lb := Mux(r_LoopBck, io.mii.mtxen_pad_o, io.mii.mrxdv_pad_i & RxEnSync)

// Muxed MII Receive Error
MRxErr_Lb := Mux(r_LoopBck, io.mii.mtxerr_pad_o, io.mii.mrxerr_pad_i & RxEnSync)

// Muxed MII Receive Data
MRxD_Lb := Mux(r_LoopBck, io.mii.mtxd_pad_o, io.mii.mrxd_pad_i)


val txethmac = withClockAndReset(io.mii.mtx_clk_pad_i.asClock, io.asyncReset)( Module(new MacTx))

txethmac.io.TxStartFrm      := TxStartFrmOut
txethmac.io.TxEndFrm        := TxEndFrmOut
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

io.mii.mtxd_pad_o    := txethmac.io.MTxD
io.mii.mtxen_pad_o   := txethmac.io.MTxEn
io.mii.mtxerr_pad_o := txethmac.io.MTxErr
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

val rxethmac = withClockAndReset(io.mii.mrx_clk_pad_i.asClock, io.asyncReset)( Module(new MacRx))

  rxethmac.io.MRxDV               := MRxDV_Lb
  rxethmac.io.MRxD                := MRxD_Lb
  rxethmac.io.Transmitting        := Transmitting
  rxethmac.io.HugEn               := r_HugEn
  rxethmac.io.DlyCrcEn            := r_DlyCrcEn
  rxethmac.io.MaxFL               := r_MaxFL
  rxethmac.io.r_IFG               := r_IFG
  rxethmac.io.MAC                 := r_MAC
  rxethmac.io.r_HASH0             := r_HASH0
  rxethmac.io.r_HASH1             := r_HASH1
  rxethmac.io.PassAll             := true.B
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
  AddressMiss       := rxethmac.io.AddressMiss


  withClockAndReset( io.mii.mtx_clk_pad_i.asClock, reset.asAsyncReset ) {
    // MII Carrier Sense Synchronization
    CarrierSense_Tx2 := ShiftRegister(io.mii.mcrs_pad_i, 2, false.B, true.B)

    TxCarrierSense := ~r_FullD & CarrierSense_Tx2

    val Collision_Tx1 = RegNext(io.mii.mcoll_pad_i, false.B)
    val Collision_Tx2 = RegInit(false.B)

    when(ResetCollision){
      Collision_Tx2 := false.B
    } .elsewhen(Collision_Tx1){
      Collision_Tx2 := true.B
    }


    // Synchronized Collision
    Collision := ~r_FullD & Collision_Tx2

  }




  withClockAndReset( io.mii.mrx_clk_pad_i.asClock, reset.asAsyncReset ) {
    val WillTransmit_q = ShiftRegister(WillTransmit, 2, false.B, true.B)

    Transmitting := ~r_FullD & WillTransmit_q
    RxEnSync := RegEnable( ShiftRegister(r_RxEn, 2), false.B, ~io.mii.mrxdv_pad_i)
  }




  // Synchronizing WillSendControlFrame to WB_CLK;
  val WillSendControlFrame_sync = ShiftRegisters(WillSendControlFrame, 3, false.B, true.B)

  when(true.B){
    RstTxPauseRq := WillSendControlFrame_sync(1) & ~WillSendControlFrame_sync(2)    
  }


  withClockAndReset( io.mii.mtx_clk_pad_i.asClock, reset.asAsyncReset ) {
    val TxPauseRq_sync = ShiftRegisters((r_TxPauseRq & r_TxFlow), 3, false.B, true.B )

    TPauseRq := RegNext( TxPauseRq_sync(1) & (~TxPauseRq_sync(2)), false.B )
  }


  val wishbone = Module(new MacTileLink)


  wishbone.io.RetryCntLatched  := RetryCntLatched
  wishbone.io.RetryLimit       := RetryLimit
  wishbone.io.LateCollLatched  := LateCollLatched
  wishbone.io.DeferLatched     := DeferLatched
  wishbone.io.CarrierSenseLost := CarrierSenseLost

  PerPacketCrcEn := wishbone.io.PerPacketCrcEn
  PerPacketPad   := wishbone.io.PerPacketPad

  wishbone.io.r_TxEn     := r_TxEn
  wishbone.io.r_RxEn     := r_RxEn
  TxB_IRQ  := false.B
  TxE_IRQ  := false.B
  RxB_IRQ  := false.B
  RxE_IRQ  := false.B




  val macstatus = Module(new MacStatus)

  macstatus.io.asyncReset          := io.asyncReset
  macstatus.io.MRxClk              := io.mii.mrx_clk_pad_i
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
  macstatus.io.Collision           := io.mii.mcoll_pad_i
  macstatus.io.CollValid           := r_CollValid
  macstatus.io.r_MinFL             := r_MinFL
  macstatus.io.r_MaxFL             := r_MaxFL
  macstatus.io.r_HugEn             := r_HugEn
  macstatus.io.StartTxDone         := StartTxDone
  macstatus.io.StartTxAbort        := StartTxAbort
  macstatus.io.RetryCnt            := RetryCnt
  macstatus.io.MTxClk              := io.mii.mtx_clk_pad_i
  macstatus.io.MaxCollisionOccured := MaxCollisionOccured
  macstatus.io.LateCollision       := LateCollision
  macstatus.io.DeferIndication     := DeferIndication
  macstatus.io.TxStartFrm          := TxStartFrmOut
  macstatus.io.StatePreamble       := StatePreamble
  macstatus.io.StateData           := StateData
  macstatus.io.CarrierSense        := CarrierSense_Tx2
  macstatus.io.Loopback            := r_LoopBck
  macstatus.io.r_FullD             := r_FullD
  macstatus.io.RstDeferLatched     := RstDeferLatched

  ReceivedLengthOK     := macstatus.io.ReceivedLengthOK
  ReceiveEnd           := macstatus.io.ReceiveEnd
  ReceivedPacketGood   := macstatus.io.ReceivedPacketGood
  LatchedCrcError      := macstatus.io.LatchedCrcError
  RxLateCollision      := macstatus.io.RxLateCollision
  DribbleNibble        := macstatus.io.DribbleNibble
  ReceivedPacketTooBig := macstatus.io.ReceivedPacketTooBig
  LoadRxStatus         := macstatus.io.LoadRxStatus
  RetryCntLatched      := macstatus.io.RetryCntLatched
  RetryLimit           := macstatus.io.RetryLimit
  LateCollLatched      := macstatus.io.LateCollLatched
  DeferLatched         := macstatus.io.DeferLatched
  CarrierSenseLost     := macstatus.io.CarrierSenseLost




















  val macTileLinkTx = withClockAndReset( io.mii.mtx_clk_pad_i.asClock, io.asyncReset ) (Module(new MacTileLinkTx))


  val txReq_ToAsync = Wire(new AsyncBundle(new TxFifo_Stream_Bundle))
  txReq_ToAsync <> ToAsyncBundle( wishbone.io.txReq )
  withClockAndReset(io.mii.mtx_clk_pad_i.asClock, io.asyncReset) {  
    macTileLinkTx.io.txReq <> FromAsyncBundle( txReq_ToAsync )  
  }


  withClockAndReset( io.mii.mtx_clk_pad_i.asClock, io.asyncReset ){
    macTileLinkTx.io.BlockingTxStatusWrite_sync := ShiftRegister( wishbone.io.BlockingTxStatusWrite, 2, false.B, true.B)
  }


  RstDeferLatched := macTileLinkTx.io.RstDeferLatched
  TxStartFrm      := macTileLinkTx.io.TxStartFrm
  TxEndFrm        := macTileLinkTx.io.TxEndFrm
  TxData          := macTileLinkTx.io.TxData

  
  wishbone.io.TxUsedData      := TxUsedData
  macTileLinkTx.io.TxUsedData := TxUsedData

  macTileLinkTx.io.TxRetry    := TxRetry
  macTileLinkTx.io.TxAbort    := TxAbort
  macTileLinkTx.io.TxDone     := TxDone

  wishbone.io.TxRetrySync := ShiftRegister( TxRetry, 2, false.B, true.B )
  wishbone.io.TxAbortSync := ShiftRegister( TxAbort, 2, false.B, true.B )
  wishbone.io.TxDoneSync  := ShiftRegister( TxDone,  2, false.B, true.B )






















  val macTileLinkRx = withClockAndReset( io.mii.mrx_clk_pad_i.asClock, io.asyncReset ) ( Module(new MacTileLinkRx) )
 

  val req_ToAsync = Wire(new AsyncBundle(new RxFifo_Stream_Bundle))
  val resp_ToAsync = Wire(new AsyncBundle(Bool()))
  val fateRxRespPort = Wire( Flipped(Decoupled(Bool())) )
  fateRxRespPort.ready := true.B
 
  wishbone.io.rxReq <> FromAsyncBundle( req_ToAsync )
  resp_ToAsync <> ToAsyncBundle( wishbone.io.rxResp )

  withClockAndReset(io.mii.mrx_clk_pad_i.asClock, io.asyncReset) {  
    req_ToAsync <> ToAsyncBundle( macTileLinkRx.io.rxReq )
    fateRxRespPort <> FromAsyncBundle( resp_ToAsync )  
  }

    
    




  wishbone.io.LatchedRxLength_rxclk   := macTileLinkRx.io.LatchedRxLength
  wishbone.io.RxStatusInLatched_rxclk := macTileLinkRx.io.RxStatusInLatched
      

  
  // Busy Interrupt
  val Busy_IRQ_sync = ShiftRegister(macTileLinkRx.io.Busy_IRQ_rck, 2, false.B, true.B)
  Busy_IRQ := Busy_IRQ_sync & ~RegNext(Busy_IRQ_sync, false.B)

  withClockAndReset( io.mii.mrx_clk_pad_i.asClock, io.asyncReset ) {
    RxStatusWriteLatchedSync         := fateRxRespPort.fire
    macTileLinkRx.io.Busy_IRQ_syncb  := ShiftRegister( Busy_IRQ_sync,  2, false.B, true.B )

    macTileLinkRx.io.RxReady  := ShiftRegister( wishbone.io.RxReady, 2, false.B, true.B )
  }

  macTileLinkRx.io.RxData   := RxData
  macTileLinkRx.io.RxValid  := RxValid

  macTileLinkRx.io.RxStartFrm := RxStartFrm
  macTileLinkRx.io.RxEndFrm := RxEndFrm


  macTileLinkRx.io.RxLength     := RxByteCnt
  macTileLinkRx.io.LoadRxStatus := LoadRxStatus
  macTileLinkRx.io.RxStatusIn   := Cat(ReceivedPauseFrm, AddressMiss, false.B, false.B, DribbleNibble, ReceivedPacketTooBig, false.B, LatchedCrcError, RxLateCollision)


  wishbone.io.rxEnq <> io.rxEnq
  wishbone.io.txDeq <> io.txDeq
}





