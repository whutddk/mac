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
  val rxEnq = Decoupled(new Mac_Stream_Bundle)
  val txDeq = Flipped(Decoupled(new Mac_Stream_Bundle))

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
val TxAbort    = Wire(Bool())
val TxDone     = Wire(Bool())

dontTouch(TxStartFrm)
dontTouch(TxEndFrm  )
dontTouch(TxUsedData)
dontTouch(TxData    )
dontTouch(TxAbort   )
dontTouch(TxDone    )





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

dontTouch(r_LoopBck                  )
dontTouch(r_TxEn                     )
dontTouch(r_RxEn                     )
dontTouch(MRxDV_Lb                   )
dontTouch(MRxErr_Lb                  )
dontTouch(MRxD_Lb                    )
dontTouch(Transmitting               )
dontTouch(DribbleNibble              )
dontTouch(LoadRxStatus               )
dontTouch(r_HASH0                    )
dontTouch(r_HASH1                    )
dontTouch(r_IPGT                     )
dontTouch(r_IPGR1                    )
dontTouch(r_IPGR2                    )
dontTouch(r_CollValid                )
dontTouch(r_ExDfrEn                  )
dontTouch(r_IFG                      )
dontTouch(TxB_IRQ                    )
dontTouch(TxE_IRQ                    )
dontTouch(RxB_IRQ                    )
dontTouch(RxE_IRQ                    )
dontTouch(Busy_IRQ                   )
dontTouch(r_FullD                    )
dontTouch(r_NoPre                    )
dontTouch(StartTxDone                )
dontTouch(PerPacketCrcEn             )
dontTouch(LateCollision              )
dontTouch(DeferIndication            )
dontTouch(LateCollLatched            )
dontTouch(DeferLatched               )
dontTouch(RstDeferLatched            )
dontTouch(CarrierSenseLost           )






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

dontTouch(RxData              )
dontTouch(RxValid             )
dontTouch(RxStartFrm          )
dontTouch(RxEndFrm            )
dontTouch(WillTransmit        )
dontTouch(ResetCollision      )
dontTouch(LatchedCrcError     )
dontTouch(RxLateCollision     )
dontTouch(StartTxAbort        )
dontTouch(MaxCollisionOccured )
dontTouch(RetryLimit          )
dontTouch(StatePreamble       )
dontTouch(StateData           )


























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
io.mii.mtxerr_pad_o := txethmac.io.MTxErr
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


dontTouch(RxByteCnt        )
dontTouch(RxByteCntEq0     )
dontTouch(RxByteCntGreat2  )
dontTouch(RxCrcError       )
dontTouch(RxStateIdle      )
dontTouch(RxStatePreamble  )
dontTouch(RxStateSFD       )
dontTouch(RxStateData      )


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




  val wishbone = Module(new MacTileLink)


  wishbone.io.RetryLimit       := RetryLimit
  wishbone.io.LateCollLatched  := LateCollLatched
  wishbone.io.DeferLatched     := DeferLatched
  wishbone.io.CarrierSenseLost := CarrierSenseLost

  PerPacketCrcEn := wishbone.io.PerPacketCrcEn

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




















  val macTileLinkTx = withClockAndReset( io.mii.mtx_clk_pad_i.asClock, io.asyncReset ) (Module(new MacTileLinkTx))


  val txReq_ToAsync = Wire(new AsyncBundle(new Mac_Stream_Bundle))
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

  macTileLinkTx.io.TxAbort    := TxAbort
  macTileLinkTx.io.TxDone     := TxDone

  wishbone.io.TxAbortSync := ShiftRegister( TxAbort, 2, false.B, true.B )
  wishbone.io.TxDoneSync  := ShiftRegister( TxDone,  2, false.B, true.B )






















  val macTileLinkRx = withClockAndReset( io.mii.mrx_clk_pad_i.asClock, io.asyncReset ) ( Module(new MacTileLinkRx) )
 

  val req_ToAsync = Wire(new AsyncBundle(new Mac_Stream_Bundle))
 
  wishbone.io.rxReq <> FromAsyncBundle( req_ToAsync )


  withClockAndReset(io.mii.mrx_clk_pad_i.asClock, io.asyncReset) {  
    req_ToAsync <> ToAsyncBundle( macTileLinkRx.io.rxReq )
  }

    
    




  // wishbone.io.LatchedRxLength_rxclk   := macTileLinkRx.io.LatchedRxLength
  wishbone.io.RxStatusInLatched_rxclk := macTileLinkRx.io.RxStatusInLatched
      

  
  // Busy Interrupt
  val Busy_IRQ_sync = ShiftRegister(macTileLinkRx.io.Busy_IRQ_rck, 2, false.B, true.B)
  Busy_IRQ := Busy_IRQ_sync & ~RegNext(Busy_IRQ_sync, false.B)

  withClockAndReset( io.mii.mrx_clk_pad_i.asClock, io.asyncReset ) {
    macTileLinkRx.io.Busy_IRQ_syncb  := ShiftRegister( Busy_IRQ_sync,  2, false.B, true.B )

    macTileLinkRx.io.RxReady  := ShiftRegister( wishbone.io.RxReady, 2, false.B, true.B )
  }

  macTileLinkRx.io.RxData   := RxData
  macTileLinkRx.io.RxValid  := RxValid

  macTileLinkRx.io.RxStartFrm := RxStartFrm
  macTileLinkRx.io.RxEndFrm := RxEndFrm


  // macTileLinkRx.io.RxLength     := RxByteCnt
  macTileLinkRx.io.LoadRxStatus := LoadRxStatus
  macTileLinkRx.io.RxStatusIn   := Cat(false.B, false.B, false.B, false.B, DribbleNibble, false.B, false.B, LatchedCrcError, RxLateCollision)

  wishbone.io.rxEnq <> io.rxEnq
  wishbone.io.txDeq <> io.txDeq


}





