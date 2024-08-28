package MAC

import chisel3._
import chisel3.util._


// module eth_crc (Clk, Reset, Data, Enable, Initialize, Crc, CrcError);


// input Clk;
// input Reset;
  // input [3:0] Data;
  // input Enable;
  // input Initialize;

  // output [31:0] Crc;
// output CrcError;

// reg  [31:0] Crc;

// wire [31:0] CrcNext;


// assign CrcNext[0] = Enable & (Data[0] ^ Crc[28]); 
// assign CrcNext[1] = Enable & (Data[1] ^ Data[0] ^ Crc[28] ^ Crc[29]); 
// assign CrcNext[2] = Enable & (Data[2] ^ Data[1] ^ Data[0] ^ Crc[28] ^ Crc[29] ^ Crc[30]); 
// assign CrcNext[3] = Enable & (Data[3] ^ Data[2] ^ Data[1] ^ Crc[29] ^ Crc[30] ^ Crc[31]); 
// assign CrcNext[4] = (Enable & (Data[3] ^ Data[2] ^ Data[0] ^ Crc[28] ^ Crc[30] ^ Crc[31])) ^ Crc[0]; 
// assign CrcNext[5] = (Enable & (Data[3] ^ Data[1] ^ Data[0] ^ Crc[28] ^ Crc[29] ^ Crc[31])) ^ Crc[1]; 
// assign CrcNext[6] = (Enable & (Data[2] ^ Data[1] ^ Crc[29] ^ Crc[30])) ^ Crc[ 2]; 
// assign CrcNext[7] = (Enable & (Data[3] ^ Data[2] ^ Data[0] ^ Crc[28] ^ Crc[30] ^ Crc[31])) ^ Crc[3]; 
// assign CrcNext[8] = (Enable & (Data[3] ^ Data[1] ^ Data[0] ^ Crc[28] ^ Crc[29] ^ Crc[31])) ^ Crc[4]; 
// assign CrcNext[9] = (Enable & (Data[2] ^ Data[1] ^ Crc[29] ^ Crc[30])) ^ Crc[5]; 
// assign CrcNext[10] = (Enable & (Data[3] ^ Data[2] ^ Data[0] ^ Crc[28] ^ Crc[30] ^ Crc[31])) ^ Crc[6]; 
// assign CrcNext[11] = (Enable & (Data[3] ^ Data[1] ^ Data[0] ^ Crc[28] ^ Crc[29] ^ Crc[31])) ^ Crc[7]; 
// assign CrcNext[12] = (Enable & (Data[2] ^ Data[1] ^ Data[0] ^ Crc[28] ^ Crc[29] ^ Crc[30])) ^ Crc[8]; 
// assign CrcNext[13] = (Enable & (Data[3] ^ Data[2] ^ Data[1] ^ Crc[29] ^ Crc[30] ^ Crc[31])) ^ Crc[9]; 
// assign CrcNext[14] = (Enable & (Data[3] ^ Data[2] ^ Crc[30] ^ Crc[31])) ^ Crc[10]; 
// assign CrcNext[15] = (Enable & (Data[3] ^ Crc[31])) ^ Crc[11]; 
// assign CrcNext[16] = (Enable & (Data[0] ^ Crc[28])) ^ Crc[12]; 
// assign CrcNext[17] = (Enable & (Data[1] ^ Crc[29])) ^ Crc[13]; 
// assign CrcNext[18] = (Enable & (Data[2] ^ Crc[30])) ^ Crc[14]; 
// assign CrcNext[19] = (Enable & (Data[3] ^ Crc[31])) ^ Crc[15]; 
// assign CrcNext[20] = Crc[16]; 
// assign CrcNext[21] = Crc[17]; 
// assign CrcNext[22] = (Enable & (Data[0] ^ Crc[28])) ^ Crc[18]; 
// assign CrcNext[23] = (Enable & (Data[1] ^ Data[0] ^ Crc[29] ^ Crc[28])) ^ Crc[19]; 
// assign CrcNext[24] = (Enable & (Data[2] ^ Data[1] ^ Crc[30] ^ Crc[29])) ^ Crc[20]; 
// assign CrcNext[25] = (Enable & (Data[3] ^ Data[2] ^ Crc[31] ^ Crc[30])) ^ Crc[21]; 
// assign CrcNext[26] = (Enable & (Data[3] ^ Data[0] ^ Crc[31] ^ Crc[28])) ^ Crc[22]; 
// assign CrcNext[27] = (Enable & (Data[1] ^ Crc[29])) ^ Crc[23]; 
// assign CrcNext[28] = (Enable & (Data[2] ^ Crc[30])) ^ Crc[24]; 
// assign CrcNext[29] = (Enable & (Data[3] ^ Crc[31])) ^ Crc[25]; 
// assign CrcNext[30] = Crc[26]; 
// assign CrcNext[31] = Crc[27]; 


// always @ (posedge Clk or posedge Reset)
// begin
//   if (Reset)
//     Crc <=  32'hffffffff;
//   else
//   if(Initialize)
//     Crc <=  32'hffffffff;
//   else
//     Crc <=  CrcNext;
// end

// assign CrcError = Crc[31:0] != 32'hc704dd7b;  // CRC not equal to magic number

// endmodule


//-----------------------------------------------------------------------------
// Copyright (C) 2009 OutputLogic.com
// This source file may be used and distributed without restriction
// provided that this copyright statement is not removed from the file
// and that any derivative work contains the original copyright notice
// and the associated disclaimer.
//
// THIS SOURCE FILE IS PROVIDED "AS IS" AND WITHOUT ANY EXPRESS
// OR IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED
// WARRANTIES OF MERCHANTIBILITY AND FITNESS FOR A PARTICULAR PURPOSE.
//-----------------------------------------------------------------------------
// CRC module for data[7:0] ,   crc[31:0]=1+x^1+x^2+x^4+x^5+x^7+x^8+x^10+x^11+x^12+x^16+x^22+x^23+x^26+x^32;
//-----------------------------------------------------------------------------


class crc32_8() extends Module{
  class crc32IO_Bundle extends Bundle{
    val isEnable = Input(Bool())
    val dataIn = Input(UInt(8.W))

    val crc = Output(UInt(32.W))
  }

  val io = IO( new crc32IO_Bundle )

  // val LFSR_POLY = "h4c11db7".U

  val lfsr_c = for( i <- 0 until 32 ) yield Wire(Bool())
  val lfsr_q = for( i <- 0 until 32 ) yield RegEnable( lfsr_c(i), true.B, io.isEnable )
  io.crc := Cat( lfsr_q.reverse )


  lfsr_c( 0) := lfsr_q(24) ^ lfsr_q(30) ^ io.dataIn(0) ^ io.dataIn(6)
  lfsr_c( 1) := lfsr_q(24) ^ lfsr_q(25) ^ lfsr_q(30) ^ lfsr_q(31) ^ io.dataIn(0) ^ io.dataIn(1) ^ io.dataIn(6) ^ io.dataIn(7)
  lfsr_c( 2) := lfsr_q(24) ^ lfsr_q(25) ^ lfsr_q(26) ^ lfsr_q(30) ^ lfsr_q(31) ^ io.dataIn(0) ^ io.dataIn(1) ^ io.dataIn(2) ^ io.dataIn(6) ^ io.dataIn(7)
  lfsr_c( 3) := lfsr_q(25) ^ lfsr_q(26) ^ lfsr_q(27) ^ lfsr_q(31) ^ io.dataIn(1) ^ io.dataIn(2) ^ io.dataIn(3) ^ io.dataIn(7)
  lfsr_c( 4) := lfsr_q(24) ^ lfsr_q(26) ^ lfsr_q(27) ^ lfsr_q(28) ^ lfsr_q(30) ^ io.dataIn(0) ^ io.dataIn(2) ^ io.dataIn(3) ^ io.dataIn(4) ^ io.dataIn(6)
  lfsr_c( 5) := lfsr_q(24) ^ lfsr_q(25) ^ lfsr_q(27) ^ lfsr_q(28) ^ lfsr_q(29) ^ lfsr_q(30) ^ lfsr_q(31) ^ io.dataIn(0) ^ io.dataIn(1) ^ io.dataIn(3) ^ io.dataIn(4) ^ io.dataIn(5) ^ io.dataIn(6) ^ io.dataIn(7)
  lfsr_c( 6) := lfsr_q(25) ^ lfsr_q(26) ^ lfsr_q(28) ^ lfsr_q(29) ^ lfsr_q(30) ^ lfsr_q(31) ^ io.dataIn(1) ^ io.dataIn(2) ^ io.dataIn(4) ^ io.dataIn(5) ^ io.dataIn(6) ^ io.dataIn(7)
  lfsr_c( 7) := lfsr_q(24) ^ lfsr_q(26) ^ lfsr_q(27) ^ lfsr_q(29) ^ lfsr_q(31) ^ io.dataIn(0) ^ io.dataIn(2) ^ io.dataIn(3) ^ io.dataIn(5) ^ io.dataIn(7)
  lfsr_c( 8) := lfsr_q( 0) ^ lfsr_q(24) ^ lfsr_q(25) ^ lfsr_q(27) ^ lfsr_q(28) ^ io.dataIn(0) ^ io.dataIn(1) ^ io.dataIn(3) ^ io.dataIn(4)
  lfsr_c( 9) := lfsr_q( 1) ^ lfsr_q(25) ^ lfsr_q(26) ^ lfsr_q(28) ^ lfsr_q(29) ^ io.dataIn(1) ^ io.dataIn(2) ^ io.dataIn(4) ^ io.dataIn(5)
  lfsr_c(10) := lfsr_q( 2) ^ lfsr_q(24) ^ lfsr_q(26) ^ lfsr_q(27) ^ lfsr_q(29) ^ io.dataIn(0) ^ io.dataIn(2) ^ io.dataIn(3) ^ io.dataIn(5)
  lfsr_c(11) := lfsr_q( 3) ^ lfsr_q(24) ^ lfsr_q(25) ^ lfsr_q(27) ^ lfsr_q(28) ^ io.dataIn(0) ^ io.dataIn(1) ^ io.dataIn(3) ^ io.dataIn(4)
  lfsr_c(12) := lfsr_q( 4) ^ lfsr_q(24) ^ lfsr_q(25) ^ lfsr_q(26) ^ lfsr_q(28) ^ lfsr_q(29) ^ lfsr_q(30) ^ io.dataIn(0) ^ io.dataIn(1) ^ io.dataIn(2) ^ io.dataIn(4) ^ io.dataIn(5) ^ io.dataIn(6)
  lfsr_c(13) := lfsr_q( 5) ^ lfsr_q(25) ^ lfsr_q(26) ^ lfsr_q(27) ^ lfsr_q(29) ^ lfsr_q(30) ^ lfsr_q(31) ^ io.dataIn(1) ^ io.dataIn(2) ^ io.dataIn(3) ^ io.dataIn(5) ^ io.dataIn(6) ^ io.dataIn(7)
  lfsr_c(14) := lfsr_q( 6) ^ lfsr_q(26) ^ lfsr_q(27) ^ lfsr_q(28) ^ lfsr_q(30) ^ lfsr_q(31) ^ io.dataIn(2) ^ io.dataIn(3) ^ io.dataIn(4) ^ io.dataIn(6) ^ io.dataIn(7)
  lfsr_c(15) := lfsr_q( 7) ^ lfsr_q(27) ^ lfsr_q(28) ^ lfsr_q(29) ^ lfsr_q(31) ^ io.dataIn(3) ^ io.dataIn(4) ^ io.dataIn(5) ^ io.dataIn(7)
  lfsr_c(16) := lfsr_q( 8) ^ lfsr_q(24) ^ lfsr_q(28) ^ lfsr_q(29) ^ io.dataIn(0) ^ io.dataIn(4) ^ io.dataIn(5)
  lfsr_c(17) := lfsr_q( 9) ^ lfsr_q(25) ^ lfsr_q(29) ^ lfsr_q(30) ^ io.dataIn(1) ^ io.dataIn(5) ^ io.dataIn(6)
  lfsr_c(18) := lfsr_q(10) ^ lfsr_q(26) ^ lfsr_q(30) ^ lfsr_q(31) ^ io.dataIn(2) ^ io.dataIn(6) ^ io.dataIn(7)
  lfsr_c(19) := lfsr_q(11) ^ lfsr_q(27) ^ lfsr_q(31) ^ io.dataIn(3) ^ io.dataIn(7)
  lfsr_c(20) := lfsr_q(12) ^ lfsr_q(28) ^ io.dataIn(4)
  lfsr_c(21) := lfsr_q(13) ^ lfsr_q(29) ^ io.dataIn(5)
  lfsr_c(22) := lfsr_q(14) ^ lfsr_q(24) ^ io.dataIn(0)
  lfsr_c(23) := lfsr_q(15) ^ lfsr_q(24) ^ lfsr_q(25) ^ lfsr_q(30) ^ io.dataIn(0) ^ io.dataIn(1) ^ io.dataIn(6)
  lfsr_c(24) := lfsr_q(16) ^ lfsr_q(25) ^ lfsr_q(26) ^ lfsr_q(31) ^ io.dataIn(1) ^ io.dataIn(2) ^ io.dataIn(7)
  lfsr_c(25) := lfsr_q(17) ^ lfsr_q(26) ^ lfsr_q(27) ^ io.dataIn(2) ^ io.dataIn(3)
  lfsr_c(26) := lfsr_q(18) ^ lfsr_q(24) ^ lfsr_q(27) ^ lfsr_q(28) ^ lfsr_q(30) ^ io.dataIn(0) ^ io.dataIn(3) ^ io.dataIn(4) ^ io.dataIn(6)
  lfsr_c(27) := lfsr_q(19) ^ lfsr_q(25) ^ lfsr_q(28) ^ lfsr_q(29) ^ lfsr_q(31) ^ io.dataIn(1) ^ io.dataIn(4) ^ io.dataIn(5) ^ io.dataIn(7)
  lfsr_c(28) := lfsr_q(20) ^ lfsr_q(26) ^ lfsr_q(29) ^ lfsr_q(30) ^ io.dataIn(2) ^ io.dataIn(5) ^ io.dataIn(6)
  lfsr_c(29) := lfsr_q(21) ^ lfsr_q(27) ^ lfsr_q(30) ^ lfsr_q(31) ^ io.dataIn(3) ^ io.dataIn(6) ^ io.dataIn(7)
  lfsr_c(30) := lfsr_q(22) ^ lfsr_q(28) ^ lfsr_q(31) ^ io.dataIn(4) ^ io.dataIn(7)
  lfsr_c(31) := lfsr_q(23) ^ lfsr_q(29) ^ io.dataIn(5)



}




