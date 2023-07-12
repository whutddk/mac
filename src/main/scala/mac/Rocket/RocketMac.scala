// package Top

// import chisel3._
// import Mac._


// import freechips.rocketchip.system._
// import org.chipsalliance.cde.config.Parameters
// import freechips.rocketchip.subsystem._
// import freechips.rocketchip.devices.tilelink._
// import freechips.rocketchip.util.DontTouch



// /** Example Top with periphery devices and ports, and a Rocket subsystem */
// class ExampleRocketSystem(implicit p: Parameters) extends RocketSubsystem
//     with HasAsyncExtInterrupts
//     with CanHaveMasterAXI4MemPort
//     with CanHaveMasterAXI4MMIOPort
//     with CanHaveSlaveAXI4Port
//     with WithManyMacMix
// {
//   // optionally add ROM devices
//   // Note that setting BootROMLocated will override the reset_vector for all tiles
//   val bootROM  = p(BootROMLocated(location)).map { BootROM.attach(_, this, CBUS) }
//   val maskROMs = p(MaskROMLocated(location)).map { MaskROM.attach(_, this, CBUS) }

//   override lazy val module = new ExampleRocketSystemModuleImp(this)
// }

// class ExampleRocketSystemModuleImp[+L <: ExampleRocketSystem](_outer: L) extends RocketSubsystemModuleImp(_outer)
//     with HasRTCModuleImp
//     with HasExtInterruptsModuleImp
//     with DontTouch

