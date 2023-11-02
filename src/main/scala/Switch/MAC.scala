package Switch

import chisel3._
import chisel3.util._

import org.chipsalliance.cde.config._

import MAC._




class MII extends Bundle with MDIO{
  // Tx
  val mtx_clk_pad_i = Input(Bool())
  val mtxd_pad_o    = Output(UInt(4.W))
  val mtxen_pad_o   = Output(Bool())

  // Rx
  val mrx_clk_pad_i = Input(Bool())
  val mrxd_pad_i    = Input(UInt(4.W))
  val mrxdv_pad_i   = Input(Bool())

  // Common Tx and Rx
  val mcoll_pad_i = Input(Bool())
  val mcrs_pad_i  = Input(Bool())  
}



class MacIO extends Bundle{
  val mii = new MII
  val cfg = Flipped(new Mac_Config_Bundle)

  // val int_o = Output(Bool())

  val isLoopBack = Output(Bool())
  val asyncReset = Input(AsyncReset())
}


abstract class Mac(implicit p: Parameters) extends SwitchNode{

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



  val TxStartFrm = Wire(Bool())
  val TxEndFrm   = Wire(Bool())
  val TxUsedData = Wire(Bool())
  val TxData     = Wire(UInt(8.W))
  val TxAbort    = Wire(Bool())
  val TxDone     = Wire(Bool())




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
  val MRxD_Lb                    = Wire(UInt(4.W))
  val Transmitting               = Wire(Bool())
  val DribbleNibble              = Wire(Bool())
  val LoadRxStatus               = Wire(Bool())
  val r_HASH0                    = Wire(UInt(32.W))
  val r_HASH1                    = Wire(UInt(32.W))
  val r_IPGT                     = Wire(UInt(7.W))
  val r_IPGR1                    = Wire(UInt(7.W))
  val r_IPGR2                    = Wire(UInt(7.W))
  val r_CollValid                = Wire(UInt(6.W))
  val r_ExDfrEn                  = Wire(Bool())
  val r_IFG                      = Wire(Bool())
  val TxB_IRQ                    = Wire(Bool())
  val TxE_IRQ                    = Wire(Bool())
  val RxB_IRQ                    = Wire(Bool())
  val RxE_IRQ                    = Wire(Bool())
  val Busy_IRQ                   = Wire(Bool())
  val r_FullD                    = Wire(Bool())
  val r_NoPre                    = Wire(Bool())
  val StartTxDone                = Wire(Bool())
  val PerPacketCrcEn             = Wire(Bool())
  val LateCollision              = Wire(Bool())
  val DeferIndication            = Wire(Bool())
  val LateCollLatched            = Wire(Bool())
  val DeferLatched               = Wire(Bool())
  val RstDeferLatched            = Wire(Bool())
  val CarrierSenseLost           = Wire(Bool())



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
  io.cfg.StartTxDone         := StartTxDone
  io.cfg.TxClk               := io.mii.mtx_clk_pad_i
  io.cfg.RxClk               := io.mii.mrx_clk_pad_i

  r_FullD     := io.cfg.r_FullD
  r_ExDfrEn   := io.cfg.r_ExDfrEn
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
  r_CollValid := io.cfg.r_CollValid
  r_MiiNoPre  := io.cfg.r_MiiNoPre
  r_ClkDiv    := io.cfg.r_ClkDiv
  r_WCtrlData := io.cfg.r_WCtrlData
  r_RStat     := io.cfg.r_RStat
  r_ScanStat  := io.cfg.r_ScanStat
  r_RGAD      := io.cfg.r_RGAD
  r_FIAD      := io.cfg.r_FIAD
  r_CtrlData  := io.cfg.r_CtrlData



  val RxData               = Wire(UInt(8.W))
  val RxValid              = Wire(Bool())
  val RxStartFrm           = Wire(Bool())
  val RxEndFrm             = Wire(Bool())
  val WillTransmit         = Wire(Bool())
  val ResetCollision       = Wire(Bool())
  val LatchedCrcError      = Wire(Bool())
  val RxLateCollision      = Wire(Bool())
  val StartTxAbort         = Wire(Bool())
  val MaxCollisionOccured  = Wire(Bool())
  val RetryLimit           = Wire(Bool())
  val StatePreamble        = Wire(Bool())
  val StateData            = Wire(UInt(2.W))

























  val TxCarrierSense   = Wire(Bool())
  val Collision        = Wire(Bool())
  val CarrierSense_Tx2 = Wire(Bool())
  val RxEnSync         = Wire(Bool())


  io.isLoopBack := r_LoopBck

  // Muxed MII receive data valid
  MRxDV_Lb := Mux(r_LoopBck, io.mii.mtxen_pad_o, io.mii.mrxdv_pad_i & RxEnSync)

  // Muxed MII Receive Data
  MRxD_Lb := Mux(r_LoopBck, io.mii.mtxd_pad_o, io.mii.mrxd_pad_i)


  val txethmac = withClockAndReset(io.mii.mtx_clk_pad_i.asClock, io.asyncReset)( Module(new MacTx))

  txethmac.io.TxStartFrm      := TxStartFrm
  txethmac.io.TxEndFrm        := TxEndFrm
  txethmac.io.TxData          := TxData
  txethmac.io.CarrierSense    := TxCarrierSense
  txethmac.io.Collision       := Collision
  txethmac.io.CrcEn           := PerPacketCrcEn
  txethmac.io.FullD           := r_FullD
  txethmac.io.IPGT            := r_IPGT
  txethmac.io.IPGR1           := r_IPGR1
  txethmac.io.IPGR2           := r_IPGR2
  txethmac.io.CollValid       := r_CollValid
  txethmac.io.ExDfrEn         := r_ExDfrEn

  io.mii.mtxd_pad_o    := txethmac.io.MTxD
  io.mii.mtxen_pad_o   := txethmac.io.MTxEn
  TxDone := txethmac.io.TxDone
  TxAbort := txethmac.io.TxAbort
  TxUsedData := txethmac.io.TxUsedData
  WillTransmit := txethmac.io.WillTransmit
  ResetCollision := txethmac.io.ResetCollision
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
  val RxCrcError        = Wire(Bool())
  val RxStateIdle       = Wire(Bool())
  val RxStatePreamble   = Wire(Bool())
  val RxStateSFD        = Wire(Bool())
  val RxStateData       = Wire(UInt(2.W))



  val rxethmac = withClockAndReset(io.mii.mrx_clk_pad_i.asClock, io.asyncReset)( Module(new MacRx))

  rxethmac.io.MRxDV               := MRxDV_Lb
  rxethmac.io.MRxD                := MRxD_Lb
  rxethmac.io.Transmitting        := Transmitting
  rxethmac.io.r_IFG               := r_IFG
  rxethmac.io.r_HASH0             := r_HASH0
  rxethmac.io.r_HASH1             := r_HASH1

  RxData            := rxethmac.io.RxData
  RxValid           := rxethmac.io.RxValid
  RxStartFrm        := rxethmac.io.RxStartFrm
  RxEndFrm          := rxethmac.io.RxEndFrm
  RxByteCnt         := rxethmac.io.ByteCnt
  RxByteCntEq0      := rxethmac.io.ByteCntEq0
  RxByteCntGreat2   := rxethmac.io.ByteCntGreat2
  RxCrcError        := rxethmac.io.CrcError
  RxStateIdle       := rxethmac.io.StateIdle
  RxStatePreamble   := rxethmac.io.StatePreamble
  RxStateSFD        := rxethmac.io.StateSFD
  RxStateData       := rxethmac.io.StateData



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










  val macstatus = Module(new MacStatus)

  macstatus.io.asyncReset          := io.asyncReset
  macstatus.io.MRxClk              := io.mii.mrx_clk_pad_i
  macstatus.io.RxCrcError          := RxCrcError
  macstatus.io.MRxDV               := MRxDV_Lb
  macstatus.io.RxStateSFD          := RxStateSFD
  macstatus.io.RxStateData         := RxStateData
  macstatus.io.RxStatePreamble     := RxStatePreamble
  macstatus.io.RxStateIdle         := RxStateIdle
  macstatus.io.Transmitting        := Transmitting
  macstatus.io.RxByteCnt           := RxByteCnt
  macstatus.io.RxByteCntEq0        := RxByteCntEq0
  macstatus.io.RxByteCntGreat2     := RxByteCntGreat2
  macstatus.io.MRxD                := MRxD_Lb
  macstatus.io.Collision           := io.mii.mcoll_pad_i
  macstatus.io.CollValid           := r_CollValid
  macstatus.io.StartTxDone         := StartTxDone
  macstatus.io.StartTxAbort        := StartTxAbort
  macstatus.io.MTxClk              := io.mii.mtx_clk_pad_i
  macstatus.io.MaxCollisionOccured := MaxCollisionOccured
  macstatus.io.LateCollision       := LateCollision
  macstatus.io.DeferIndication     := DeferIndication
  macstatus.io.TxStartFrm          := TxStartFrm
  macstatus.io.StatePreamble       := StatePreamble
  macstatus.io.StateData           := StateData
  macstatus.io.CarrierSense        := CarrierSense_Tx2
  macstatus.io.Loopback            := r_LoopBck
  macstatus.io.r_FullD             := r_FullD
  macstatus.io.RstDeferLatched     := RstDeferLatched

  LatchedCrcError      := macstatus.io.LatchedCrcError
  RxLateCollision      := macstatus.io.RxLateCollision
  DribbleNibble        := macstatus.io.DribbleNibble
  LoadRxStatus         := macstatus.io.LoadRxStatus
  RetryLimit           := macstatus.io.RetryLimit
  LateCollLatched      := macstatus.io.LateCollLatched
  DeferLatched         := macstatus.io.DeferLatched
  CarrierSenseLost     := macstatus.io.CarrierSenseLost




















  val macTileLinkTx = Module(new MacTileLinkTx)

  RstDeferLatched := macTileLinkTx.io.RstDeferLatched
  TxStartFrm      := macTileLinkTx.io.TxStartFrm
  TxEndFrm        := macTileLinkTx.io.TxEndFrm
  TxData          := macTileLinkTx.io.TxData
  macTileLinkTx.io.TxUsedData := TxUsedData
  macTileLinkTx.io.TxAbort    := TxAbort
  macTileLinkTx.io.TxDone     := TxDone

  macTileLinkTx.io.RetryLimit       := RetryLimit
  macTileLinkTx.io.LateCollLatched  := LateCollLatched
  macTileLinkTx.io.DeferLatched     := DeferLatched
  macTileLinkTx.io.CarrierSenseLost := CarrierSenseLost
  PerPacketCrcEn := macTileLinkTx.io.PerPacketCrcEn

  macTileLinkTx.io.r_TxEn    := r_TxEn



  macTileLinkTx.io.MTxClk     := io.mii.mtx_clk_pad_i
  macTileLinkTx.io.asyncReset := io.asyncReset












  val macTileLinkRx = Module(new MacTileLinkRx)
  macTileLinkRx.io.DribbleNibble   := DribbleNibble
  macTileLinkRx.io.LatchedCrcError := LatchedCrcError
  macTileLinkRx.io.RxLateCollision := RxLateCollision
  macTileLinkRx.io.LoadRxStatus    := LoadRxStatus

  macTileLinkRx.io.RxData     := RxData
  macTileLinkRx.io.RxValid    := RxValid
  macTileLinkRx.io.RxStartFrm := RxStartFrm
  macTileLinkRx.io.RxEndFrm   := RxEndFrm

  Busy_IRQ := macTileLinkRx.io.Busy_IRQ

  macTileLinkRx.io.r_RxEn := r_RxEn



  macTileLinkRx.io.MRxClk := io.mii.mrx_clk_pad_i
  macTileLinkRx.io.asyncReset := io.asyncReset

  TxB_IRQ  := false.B
  TxE_IRQ  := false.B
  RxB_IRQ  := false.B
  RxE_IRQ  := false.B

}





