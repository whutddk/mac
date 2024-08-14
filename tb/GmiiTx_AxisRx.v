module crc32_8(
  input         clock,
  input         reset,
  input         io_isEnable,
  input  [7:0]  io_dataIn,
  output [31:0] io_crc
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_7;
  reg [31:0] _RAND_8;
  reg [31:0] _RAND_9;
  reg [31:0] _RAND_10;
  reg [31:0] _RAND_11;
  reg [31:0] _RAND_12;
  reg [31:0] _RAND_13;
  reg [31:0] _RAND_14;
  reg [31:0] _RAND_15;
  reg [31:0] _RAND_16;
  reg [31:0] _RAND_17;
  reg [31:0] _RAND_18;
  reg [31:0] _RAND_19;
  reg [31:0] _RAND_20;
  reg [31:0] _RAND_21;
  reg [31:0] _RAND_22;
  reg [31:0] _RAND_23;
  reg [31:0] _RAND_24;
  reg [31:0] _RAND_25;
  reg [31:0] _RAND_26;
  reg [31:0] _RAND_27;
  reg [31:0] _RAND_28;
  reg [31:0] _RAND_29;
  reg [31:0] _RAND_30;
  reg [31:0] _RAND_31;
`endif // RANDOMIZE_REG_INIT
  reg  lfsr_q_0; // @[Reg.scala 35:20]
  reg  lfsr_q_24; // @[Reg.scala 35:20]
  reg  lfsr_q_30; // @[Reg.scala 35:20]
  wire  lfsr_c_0 = lfsr_q_24 ^ lfsr_q_30 ^ io_dataIn[0] ^ io_dataIn[6]; // @[crc32.scala 135:56]
  wire  _GEN_0 = io_isEnable ? lfsr_c_0 : lfsr_q_0; // @[Reg.scala 36:18 35:20 36:22]
  reg  lfsr_q_1; // @[Reg.scala 35:20]
  reg  lfsr_q_25; // @[Reg.scala 35:20]
  wire  _lfsr_c_1_T = lfsr_q_24 ^ lfsr_q_25; // @[crc32.scala 136:28]
  reg  lfsr_q_31; // @[Reg.scala 35:20]
  wire  lfsr_c_1 = lfsr_q_24 ^ lfsr_q_25 ^ lfsr_q_30 ^ lfsr_q_31 ^ io_dataIn[0] ^ io_dataIn[1] ^ io_dataIn[6] ^
    io_dataIn[7]; // @[crc32.scala 136:112]
  wire  _GEN_1 = io_isEnable ? lfsr_c_1 : lfsr_q_1; // @[Reg.scala 36:18 35:20 36:22]
  reg  lfsr_q_2; // @[Reg.scala 35:20]
  reg  lfsr_q_26; // @[Reg.scala 35:20]
  wire  lfsr_c_2 = _lfsr_c_1_T ^ lfsr_q_26 ^ lfsr_q_30 ^ lfsr_q_31 ^ io_dataIn[0] ^ io_dataIn[1] ^ io_dataIn[2] ^
    io_dataIn[6] ^ io_dataIn[7]; // @[crc32.scala 137:140]
  wire  _GEN_2 = io_isEnable ? lfsr_c_2 : lfsr_q_2; // @[Reg.scala 36:18 35:20 36:22]
  reg  lfsr_q_3; // @[Reg.scala 35:20]
  wire  _lfsr_c_3_T = lfsr_q_25 ^ lfsr_q_26; // @[crc32.scala 138:28]
  reg  lfsr_q_27; // @[Reg.scala 35:20]
  wire  lfsr_c_3 = lfsr_q_25 ^ lfsr_q_26 ^ lfsr_q_27 ^ lfsr_q_31 ^ io_dataIn[1] ^ io_dataIn[2] ^ io_dataIn[3] ^
    io_dataIn[7]; // @[crc32.scala 138:112]
  wire  _GEN_3 = io_isEnable ? lfsr_c_3 : lfsr_q_3; // @[Reg.scala 36:18 35:20 36:22]
  reg  lfsr_q_4; // @[Reg.scala 35:20]
  wire  _lfsr_c_4_T_1 = lfsr_q_24 ^ lfsr_q_26 ^ lfsr_q_27; // @[crc32.scala 139:41]
  reg  lfsr_q_28; // @[Reg.scala 35:20]
  wire  lfsr_c_4 = lfsr_q_24 ^ lfsr_q_26 ^ lfsr_q_27 ^ lfsr_q_28 ^ lfsr_q_30 ^ io_dataIn[0] ^ io_dataIn[2] ^ io_dataIn[3
    ] ^ io_dataIn[4] ^ io_dataIn[6]; // @[crc32.scala 139:140]
  wire  _GEN_4 = io_isEnable ? lfsr_c_4 : lfsr_q_4; // @[Reg.scala 36:18 35:20 36:22]
  reg  lfsr_q_5; // @[Reg.scala 35:20]
  reg  lfsr_q_29; // @[Reg.scala 35:20]
  wire  lfsr_c_5 = _lfsr_c_1_T ^ lfsr_q_27 ^ lfsr_q_28 ^ lfsr_q_29 ^ lfsr_q_30 ^ lfsr_q_31 ^ io_dataIn[0] ^ io_dataIn[1]
     ^ io_dataIn[3] ^ io_dataIn[4] ^ io_dataIn[5] ^ io_dataIn[6] ^ io_dataIn[7]; // @[crc32.scala 140:196]
  wire  _GEN_5 = io_isEnable ? lfsr_c_5 : lfsr_q_5; // @[Reg.scala 36:18 35:20 36:22]
  reg  lfsr_q_6; // @[Reg.scala 35:20]
  wire  lfsr_c_6 = _lfsr_c_3_T ^ lfsr_q_28 ^ lfsr_q_29 ^ lfsr_q_30 ^ lfsr_q_31 ^ io_dataIn[1] ^ io_dataIn[2] ^ io_dataIn
    [4] ^ io_dataIn[5] ^ io_dataIn[6] ^ io_dataIn[7]; // @[crc32.scala 141:168]
  wire  _GEN_6 = io_isEnable ? lfsr_c_6 : lfsr_q_6; // @[Reg.scala 36:18 35:20 36:22]
  reg  lfsr_q_7; // @[Reg.scala 35:20]
  wire  lfsr_c_7 = _lfsr_c_4_T_1 ^ lfsr_q_29 ^ lfsr_q_31 ^ io_dataIn[0] ^ io_dataIn[2] ^ io_dataIn[3] ^ io_dataIn[5] ^
    io_dataIn[7]; // @[crc32.scala 142:140]
  wire  _GEN_7 = io_isEnable ? lfsr_c_7 : lfsr_q_7; // @[Reg.scala 36:18 35:20 36:22]
  reg  lfsr_q_8; // @[Reg.scala 35:20]
  wire  lfsr_c_8 = lfsr_q_0 ^ lfsr_q_24 ^ lfsr_q_25 ^ lfsr_q_27 ^ lfsr_q_28 ^ io_dataIn[0] ^ io_dataIn[1] ^ io_dataIn[3]
     ^ io_dataIn[4]; // @[crc32.scala 143:125]
  wire  _GEN_8 = io_isEnable ? lfsr_c_8 : lfsr_q_8; // @[Reg.scala 36:18 35:20 36:22]
  reg  lfsr_q_9; // @[Reg.scala 35:20]
  wire  lfsr_c_9 = lfsr_q_1 ^ lfsr_q_25 ^ lfsr_q_26 ^ lfsr_q_28 ^ lfsr_q_29 ^ io_dataIn[1] ^ io_dataIn[2] ^ io_dataIn[4]
     ^ io_dataIn[5]; // @[crc32.scala 144:125]
  wire  _GEN_9 = io_isEnable ? lfsr_c_9 : lfsr_q_9; // @[Reg.scala 36:18 35:20 36:22]
  reg  lfsr_q_10; // @[Reg.scala 35:20]
  wire  lfsr_c_10 = lfsr_q_2 ^ lfsr_q_24 ^ lfsr_q_26 ^ lfsr_q_27 ^ lfsr_q_29 ^ io_dataIn[0] ^ io_dataIn[2] ^ io_dataIn[3
    ] ^ io_dataIn[5]; // @[crc32.scala 145:125]
  wire  _GEN_10 = io_isEnable ? lfsr_c_10 : lfsr_q_10; // @[Reg.scala 36:18 35:20 36:22]
  reg  lfsr_q_11; // @[Reg.scala 35:20]
  wire  lfsr_c_11 = lfsr_q_3 ^ lfsr_q_24 ^ lfsr_q_25 ^ lfsr_q_27 ^ lfsr_q_28 ^ io_dataIn[0] ^ io_dataIn[1] ^ io_dataIn[3
    ] ^ io_dataIn[4]; // @[crc32.scala 146:125]
  wire  _GEN_11 = io_isEnable ? lfsr_c_11 : lfsr_q_11; // @[Reg.scala 36:18 35:20 36:22]
  reg  lfsr_q_12; // @[Reg.scala 35:20]
  wire  lfsr_c_12 = lfsr_q_4 ^ lfsr_q_24 ^ lfsr_q_25 ^ lfsr_q_26 ^ lfsr_q_28 ^ lfsr_q_29 ^ lfsr_q_30 ^ io_dataIn[0] ^
    io_dataIn[1] ^ io_dataIn[2] ^ io_dataIn[4] ^ io_dataIn[5] ^ io_dataIn[6]; // @[crc32.scala 147:181]
  wire  _GEN_12 = io_isEnable ? lfsr_c_12 : lfsr_q_12; // @[Reg.scala 36:18 35:20 36:22]
  reg  lfsr_q_13; // @[Reg.scala 35:20]
  wire  lfsr_c_13 = lfsr_q_5 ^ lfsr_q_25 ^ lfsr_q_26 ^ lfsr_q_27 ^ lfsr_q_29 ^ lfsr_q_30 ^ lfsr_q_31 ^ io_dataIn[1] ^
    io_dataIn[2] ^ io_dataIn[3] ^ io_dataIn[5] ^ io_dataIn[6] ^ io_dataIn[7]; // @[crc32.scala 148:181]
  wire  _GEN_13 = io_isEnable ? lfsr_c_13 : lfsr_q_13; // @[Reg.scala 36:18 35:20 36:22]
  reg  lfsr_q_14; // @[Reg.scala 35:20]
  wire  lfsr_c_14 = lfsr_q_6 ^ lfsr_q_26 ^ lfsr_q_27 ^ lfsr_q_28 ^ lfsr_q_30 ^ lfsr_q_31 ^ io_dataIn[2] ^ io_dataIn[3]
     ^ io_dataIn[4] ^ io_dataIn[6] ^ io_dataIn[7]; // @[crc32.scala 149:153]
  wire  _GEN_14 = io_isEnable ? lfsr_c_14 : lfsr_q_14; // @[Reg.scala 36:18 35:20 36:22]
  reg  lfsr_q_15; // @[Reg.scala 35:20]
  wire  lfsr_c_15 = lfsr_q_7 ^ lfsr_q_27 ^ lfsr_q_28 ^ lfsr_q_29 ^ lfsr_q_31 ^ io_dataIn[3] ^ io_dataIn[4] ^ io_dataIn[5
    ] ^ io_dataIn[7]; // @[crc32.scala 150:125]
  wire  _GEN_15 = io_isEnable ? lfsr_c_15 : lfsr_q_15; // @[Reg.scala 36:18 35:20 36:22]
  reg  lfsr_q_16; // @[Reg.scala 35:20]
  wire  lfsr_c_16 = lfsr_q_8 ^ lfsr_q_24 ^ lfsr_q_28 ^ lfsr_q_29 ^ io_dataIn[0] ^ io_dataIn[4] ^ io_dataIn[5]; // @[crc32.scala 151:97]
  wire  _GEN_16 = io_isEnable ? lfsr_c_16 : lfsr_q_16; // @[Reg.scala 36:18 35:20 36:22]
  reg  lfsr_q_17; // @[Reg.scala 35:20]
  wire  lfsr_c_17 = lfsr_q_9 ^ lfsr_q_25 ^ lfsr_q_29 ^ lfsr_q_30 ^ io_dataIn[1] ^ io_dataIn[5] ^ io_dataIn[6]; // @[crc32.scala 152:97]
  wire  _GEN_17 = io_isEnable ? lfsr_c_17 : lfsr_q_17; // @[Reg.scala 36:18 35:20 36:22]
  reg  lfsr_q_18; // @[Reg.scala 35:20]
  wire  lfsr_c_18 = lfsr_q_10 ^ lfsr_q_26 ^ lfsr_q_30 ^ lfsr_q_31 ^ io_dataIn[2] ^ io_dataIn[6] ^ io_dataIn[7]; // @[crc32.scala 153:97]
  wire  _GEN_18 = io_isEnable ? lfsr_c_18 : lfsr_q_18; // @[Reg.scala 36:18 35:20 36:22]
  reg  lfsr_q_19; // @[Reg.scala 35:20]
  wire  lfsr_c_19 = lfsr_q_11 ^ lfsr_q_27 ^ lfsr_q_31 ^ io_dataIn[3] ^ io_dataIn[7]; // @[crc32.scala 154:69]
  wire  _GEN_19 = io_isEnable ? lfsr_c_19 : lfsr_q_19; // @[Reg.scala 36:18 35:20 36:22]
  reg  lfsr_q_20; // @[Reg.scala 35:20]
  wire  lfsr_c_20 = lfsr_q_12 ^ lfsr_q_28 ^ io_dataIn[4]; // @[crc32.scala 155:41]
  wire  _GEN_20 = io_isEnable ? lfsr_c_20 : lfsr_q_20; // @[Reg.scala 36:18 35:20 36:22]
  reg  lfsr_q_21; // @[Reg.scala 35:20]
  wire  lfsr_c_21 = lfsr_q_13 ^ lfsr_q_29 ^ io_dataIn[5]; // @[crc32.scala 156:41]
  wire  _GEN_21 = io_isEnable ? lfsr_c_21 : lfsr_q_21; // @[Reg.scala 36:18 35:20 36:22]
  reg  lfsr_q_22; // @[Reg.scala 35:20]
  wire  lfsr_c_22 = lfsr_q_14 ^ lfsr_q_24 ^ io_dataIn[0]; // @[crc32.scala 157:41]
  wire  _GEN_22 = io_isEnable ? lfsr_c_22 : lfsr_q_22; // @[Reg.scala 36:18 35:20 36:22]
  reg  lfsr_q_23; // @[Reg.scala 35:20]
  wire  lfsr_c_23 = lfsr_q_15 ^ lfsr_q_24 ^ lfsr_q_25 ^ lfsr_q_30 ^ io_dataIn[0] ^ io_dataIn[1] ^ io_dataIn[6]; // @[crc32.scala 158:97]
  wire  _GEN_23 = io_isEnable ? lfsr_c_23 : lfsr_q_23; // @[Reg.scala 36:18 35:20 36:22]
  wire  lfsr_c_24 = lfsr_q_16 ^ lfsr_q_25 ^ lfsr_q_26 ^ lfsr_q_31 ^ io_dataIn[1] ^ io_dataIn[2] ^ io_dataIn[7]; // @[crc32.scala 159:97]
  wire  _GEN_24 = io_isEnable ? lfsr_c_24 : lfsr_q_24; // @[Reg.scala 36:18 35:20 36:22]
  wire  lfsr_c_25 = lfsr_q_17 ^ lfsr_q_26 ^ lfsr_q_27 ^ io_dataIn[2] ^ io_dataIn[3]; // @[crc32.scala 160:69]
  wire  _GEN_25 = io_isEnable ? lfsr_c_25 : lfsr_q_25; // @[Reg.scala 36:18 35:20 36:22]
  wire  lfsr_c_26 = lfsr_q_18 ^ lfsr_q_24 ^ lfsr_q_27 ^ lfsr_q_28 ^ lfsr_q_30 ^ io_dataIn[0] ^ io_dataIn[3] ^ io_dataIn[
    4] ^ io_dataIn[6]; // @[crc32.scala 161:125]
  wire  _GEN_26 = io_isEnable ? lfsr_c_26 : lfsr_q_26; // @[Reg.scala 36:18 35:20 36:22]
  wire  lfsr_c_27 = lfsr_q_19 ^ lfsr_q_25 ^ lfsr_q_28 ^ lfsr_q_29 ^ lfsr_q_31 ^ io_dataIn[1] ^ io_dataIn[4] ^ io_dataIn[
    5] ^ io_dataIn[7]; // @[crc32.scala 162:125]
  wire  _GEN_27 = io_isEnable ? lfsr_c_27 : lfsr_q_27; // @[Reg.scala 36:18 35:20 36:22]
  wire  lfsr_c_28 = lfsr_q_20 ^ lfsr_q_26 ^ lfsr_q_29 ^ lfsr_q_30 ^ io_dataIn[2] ^ io_dataIn[5] ^ io_dataIn[6]; // @[crc32.scala 163:97]
  wire  _GEN_28 = io_isEnable ? lfsr_c_28 : lfsr_q_28; // @[Reg.scala 36:18 35:20 36:22]
  wire  lfsr_c_29 = lfsr_q_21 ^ lfsr_q_27 ^ lfsr_q_30 ^ lfsr_q_31 ^ io_dataIn[3] ^ io_dataIn[6] ^ io_dataIn[7]; // @[crc32.scala 164:97]
  wire  _GEN_29 = io_isEnable ? lfsr_c_29 : lfsr_q_29; // @[Reg.scala 36:18 35:20 36:22]
  wire  lfsr_c_30 = lfsr_q_22 ^ lfsr_q_28 ^ lfsr_q_31 ^ io_dataIn[4] ^ io_dataIn[7]; // @[crc32.scala 165:69]
  wire  _GEN_30 = io_isEnable ? lfsr_c_30 : lfsr_q_30; // @[Reg.scala 36:18 35:20 36:22]
  wire  lfsr_c_31 = lfsr_q_23 ^ lfsr_q_29 ^ io_dataIn[5]; // @[crc32.scala 166:41]
  wire  _GEN_31 = io_isEnable ? lfsr_c_31 : lfsr_q_31; // @[Reg.scala 36:18 35:20 36:22]
  wire [7:0] io_crc_lo_lo = {lfsr_q_7,lfsr_q_6,lfsr_q_5,lfsr_q_4,lfsr_q_3,lfsr_q_2,lfsr_q_1,lfsr_q_0}; // @[Cat.scala 33:92]
  wire [15:0] io_crc_lo = {lfsr_q_15,lfsr_q_14,lfsr_q_13,lfsr_q_12,lfsr_q_11,lfsr_q_10,lfsr_q_9,lfsr_q_8,io_crc_lo_lo}; // @[Cat.scala 33:92]
  wire [7:0] io_crc_hi_lo = {lfsr_q_23,lfsr_q_22,lfsr_q_21,lfsr_q_20,lfsr_q_19,lfsr_q_18,lfsr_q_17,lfsr_q_16}; // @[Cat.scala 33:92]
  wire [15:0] io_crc_hi = {lfsr_q_31,lfsr_q_30,lfsr_q_29,lfsr_q_28,lfsr_q_27,lfsr_q_26,lfsr_q_25,lfsr_q_24,io_crc_hi_lo}
    ; // @[Cat.scala 33:92]
  assign io_crc = {io_crc_hi,io_crc_lo}; // @[Cat.scala 33:92]
  always @(posedge clock) begin
    lfsr_q_0 <= reset | _GEN_0; // @[Reg.scala 35:{20,20}]
    lfsr_q_24 <= reset | _GEN_24; // @[Reg.scala 35:{20,20}]
    lfsr_q_30 <= reset | _GEN_30; // @[Reg.scala 35:{20,20}]
    lfsr_q_1 <= reset | _GEN_1; // @[Reg.scala 35:{20,20}]
    lfsr_q_25 <= reset | _GEN_25; // @[Reg.scala 35:{20,20}]
    lfsr_q_31 <= reset | _GEN_31; // @[Reg.scala 35:{20,20}]
    lfsr_q_2 <= reset | _GEN_2; // @[Reg.scala 35:{20,20}]
    lfsr_q_26 <= reset | _GEN_26; // @[Reg.scala 35:{20,20}]
    lfsr_q_3 <= reset | _GEN_3; // @[Reg.scala 35:{20,20}]
    lfsr_q_27 <= reset | _GEN_27; // @[Reg.scala 35:{20,20}]
    lfsr_q_4 <= reset | _GEN_4; // @[Reg.scala 35:{20,20}]
    lfsr_q_28 <= reset | _GEN_28; // @[Reg.scala 35:{20,20}]
    lfsr_q_5 <= reset | _GEN_5; // @[Reg.scala 35:{20,20}]
    lfsr_q_29 <= reset | _GEN_29; // @[Reg.scala 35:{20,20}]
    lfsr_q_6 <= reset | _GEN_6; // @[Reg.scala 35:{20,20}]
    lfsr_q_7 <= reset | _GEN_7; // @[Reg.scala 35:{20,20}]
    lfsr_q_8 <= reset | _GEN_8; // @[Reg.scala 35:{20,20}]
    lfsr_q_9 <= reset | _GEN_9; // @[Reg.scala 35:{20,20}]
    lfsr_q_10 <= reset | _GEN_10; // @[Reg.scala 35:{20,20}]
    lfsr_q_11 <= reset | _GEN_11; // @[Reg.scala 35:{20,20}]
    lfsr_q_12 <= reset | _GEN_12; // @[Reg.scala 35:{20,20}]
    lfsr_q_13 <= reset | _GEN_13; // @[Reg.scala 35:{20,20}]
    lfsr_q_14 <= reset | _GEN_14; // @[Reg.scala 35:{20,20}]
    lfsr_q_15 <= reset | _GEN_15; // @[Reg.scala 35:{20,20}]
    lfsr_q_16 <= reset | _GEN_16; // @[Reg.scala 35:{20,20}]
    lfsr_q_17 <= reset | _GEN_17; // @[Reg.scala 35:{20,20}]
    lfsr_q_18 <= reset | _GEN_18; // @[Reg.scala 35:{20,20}]
    lfsr_q_19 <= reset | _GEN_19; // @[Reg.scala 35:{20,20}]
    lfsr_q_20 <= reset | _GEN_20; // @[Reg.scala 35:{20,20}]
    lfsr_q_21 <= reset | _GEN_21; // @[Reg.scala 35:{20,20}]
    lfsr_q_22 <= reset | _GEN_22; // @[Reg.scala 35:{20,20}]
    lfsr_q_23 <= reset | _GEN_23; // @[Reg.scala 35:{20,20}]
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{`RANDOM}};
  lfsr_q_0 = _RAND_0[0:0];
  _RAND_1 = {1{`RANDOM}};
  lfsr_q_24 = _RAND_1[0:0];
  _RAND_2 = {1{`RANDOM}};
  lfsr_q_30 = _RAND_2[0:0];
  _RAND_3 = {1{`RANDOM}};
  lfsr_q_1 = _RAND_3[0:0];
  _RAND_4 = {1{`RANDOM}};
  lfsr_q_25 = _RAND_4[0:0];
  _RAND_5 = {1{`RANDOM}};
  lfsr_q_31 = _RAND_5[0:0];
  _RAND_6 = {1{`RANDOM}};
  lfsr_q_2 = _RAND_6[0:0];
  _RAND_7 = {1{`RANDOM}};
  lfsr_q_26 = _RAND_7[0:0];
  _RAND_8 = {1{`RANDOM}};
  lfsr_q_3 = _RAND_8[0:0];
  _RAND_9 = {1{`RANDOM}};
  lfsr_q_27 = _RAND_9[0:0];
  _RAND_10 = {1{`RANDOM}};
  lfsr_q_4 = _RAND_10[0:0];
  _RAND_11 = {1{`RANDOM}};
  lfsr_q_28 = _RAND_11[0:0];
  _RAND_12 = {1{`RANDOM}};
  lfsr_q_5 = _RAND_12[0:0];
  _RAND_13 = {1{`RANDOM}};
  lfsr_q_29 = _RAND_13[0:0];
  _RAND_14 = {1{`RANDOM}};
  lfsr_q_6 = _RAND_14[0:0];
  _RAND_15 = {1{`RANDOM}};
  lfsr_q_7 = _RAND_15[0:0];
  _RAND_16 = {1{`RANDOM}};
  lfsr_q_8 = _RAND_16[0:0];
  _RAND_17 = {1{`RANDOM}};
  lfsr_q_9 = _RAND_17[0:0];
  _RAND_18 = {1{`RANDOM}};
  lfsr_q_10 = _RAND_18[0:0];
  _RAND_19 = {1{`RANDOM}};
  lfsr_q_11 = _RAND_19[0:0];
  _RAND_20 = {1{`RANDOM}};
  lfsr_q_12 = _RAND_20[0:0];
  _RAND_21 = {1{`RANDOM}};
  lfsr_q_13 = _RAND_21[0:0];
  _RAND_22 = {1{`RANDOM}};
  lfsr_q_14 = _RAND_22[0:0];
  _RAND_23 = {1{`RANDOM}};
  lfsr_q_15 = _RAND_23[0:0];
  _RAND_24 = {1{`RANDOM}};
  lfsr_q_16 = _RAND_24[0:0];
  _RAND_25 = {1{`RANDOM}};
  lfsr_q_17 = _RAND_25[0:0];
  _RAND_26 = {1{`RANDOM}};
  lfsr_q_18 = _RAND_26[0:0];
  _RAND_27 = {1{`RANDOM}};
  lfsr_q_19 = _RAND_27[0:0];
  _RAND_28 = {1{`RANDOM}};
  lfsr_q_20 = _RAND_28[0:0];
  _RAND_29 = {1{`RANDOM}};
  lfsr_q_21 = _RAND_29[0:0];
  _RAND_30 = {1{`RANDOM}};
  lfsr_q_22 = _RAND_30[0:0];
  _RAND_31 = {1{`RANDOM}};
  lfsr_q_23 = _RAND_31[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module GmiiTx_AxisRx(
  input        clock,
  input        reset,
  output [7:0] io_gmii_txd,
  output       io_gmii_tx_en,
  output       io_gmii_tx_er,
  output       io_axis_ready,
  input        io_axis_valid,
  input  [7:0] io_axis_bits_tdata,
  input        io_axis_bits_tlast,
  input        io_axis_bits_tuser,
  input        io_clkEn,
  input        io_miiSel,
  input  [7:0] io_ifg_delay
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_7;
  reg [31:0] _RAND_8;
`endif // RANDOMIZE_REG_INIT
  wire  crcUnit_clock; // @[axis_gmii_tx.scala 39:23]
  wire  crcUnit_reset; // @[axis_gmii_tx.scala 39:23]
  wire  crcUnit_io_isEnable; // @[axis_gmii_tx.scala 39:23]
  wire [7:0] crcUnit_io_dataIn; // @[axis_gmii_tx.scala 39:23]
  wire [31:0] crcUnit_io_crc; // @[axis_gmii_tx.scala 39:23]
  reg  mii_odd; // @[axis_gmii_tx.scala 56:20]
  wire  _stateCurr_T = io_miiSel & mii_odd; // @[axis_gmii_tx.scala 60:69]
  wire  _stateCurr_T_2 = io_clkEn & ~(io_miiSel & mii_odd); // @[axis_gmii_tx.scala 60:55]
  reg [2:0] stateCurr; // @[Reg.scala 35:20]
  wire  _stateNext_T = stateCurr == 3'h0; // @[axis_gmii_tx.scala 72:16]
  wire  _stateNext_T_31 = _stateNext_T & io_axis_valid; // @[Mux.scala 27:73]
  wire  _stateNext_T_2 = stateCurr == 3'h1; // @[axis_gmii_tx.scala 73:16]
  reg [15:0] frame_ptr; // @[axis_gmii_tx.scala 63:26]
  wire  _stateNext_T_3 = frame_ptr == 16'h7; // @[axis_gmii_tx.scala 73:56]
  wire [1:0] _stateNext_T_4 = frame_ptr == 16'h7 ? 2'h2 : 2'h1; // @[axis_gmii_tx.scala 73:44]
  wire [1:0] _stateNext_T_32 = _stateNext_T_2 ? _stateNext_T_4 : 2'h0; // @[Mux.scala 27:73]
  wire [1:0] _GEN_62 = {{1'd0}, _stateNext_T_31}; // @[Mux.scala 27:73]
  wire [1:0] _stateNext_T_39 = _GEN_62 | _stateNext_T_32; // @[Mux.scala 27:73]
  wire  _stateNext_T_5 = stateCurr == 3'h2; // @[axis_gmii_tx.scala 74:16]
  wire [2:0] _stateNext_T_6 = io_axis_bits_tuser ? 3'h7 : 3'h3; // @[axis_gmii_tx.scala 74:89]
  wire [2:0] _stateNext_T_7 = io_axis_bits_tlast ? _stateNext_T_6 : 3'h2; // @[axis_gmii_tx.scala 74:64]
  wire [2:0] _stateNext_T_8 = io_axis_valid ? _stateNext_T_7 : 3'h6; // @[axis_gmii_tx.scala 74:44]
  wire [2:0] _stateNext_T_33 = _stateNext_T_5 ? _stateNext_T_8 : 3'h0; // @[Mux.scala 27:73]
  wire [2:0] _GEN_63 = {{1'd0}, _stateNext_T_39}; // @[Mux.scala 27:73]
  wire [2:0] _stateNext_T_40 = _GEN_63 | _stateNext_T_33; // @[Mux.scala 27:73]
  wire  _stateNext_T_9 = stateCurr == 3'h3; // @[axis_gmii_tx.scala 75:16]
  wire [6:0] _stateNext_T_11 = 7'h40 - 7'h5; // @[axis_gmii_tx.scala 75:92]
  wire [15:0] _GEN_64 = {{9'd0}, _stateNext_T_11}; // @[axis_gmii_tx.scala 75:74]
  wire  _stateNext_T_12 = frame_ptr < _GEN_64; // @[axis_gmii_tx.scala 75:74]
  wire [2:0] _stateNext_T_14 = frame_ptr < _GEN_64 ? 3'h4 : 3'h5; // @[axis_gmii_tx.scala 75:44]
  wire [2:0] _stateNext_T_34 = _stateNext_T_9 ? _stateNext_T_14 : 3'h0; // @[Mux.scala 27:73]
  wire [2:0] _stateNext_T_41 = _stateNext_T_40 | _stateNext_T_34; // @[Mux.scala 27:73]
  wire  _stateNext_T_15 = stateCurr == 3'h4; // @[axis_gmii_tx.scala 76:16]
  wire [2:0] _stateNext_T_35 = _stateNext_T_15 ? _stateNext_T_14 : 3'h0; // @[Mux.scala 27:73]
  wire [2:0] _stateNext_T_42 = _stateNext_T_41 | _stateNext_T_35; // @[Mux.scala 27:73]
  wire  _stateNext_T_20 = stateCurr == 3'h5; // @[axis_gmii_tx.scala 77:16]
  wire [2:0] _stateNext_T_22 = frame_ptr < 16'h3 ? 3'h5 : 3'h7; // @[axis_gmii_tx.scala 77:44]
  wire [2:0] _stateNext_T_36 = _stateNext_T_20 ? _stateNext_T_22 : 3'h0; // @[Mux.scala 27:73]
  wire [2:0] _stateNext_T_43 = _stateNext_T_42 | _stateNext_T_36; // @[Mux.scala 27:73]
  wire  _stateNext_T_23 = stateCurr == 3'h6; // @[axis_gmii_tx.scala 78:16]
  wire  _stateNext_T_24 = io_axis_valid & io_axis_bits_tlast; // @[axis_gmii_tx.scala 78:60]
  wire [2:0] _stateNext_T_25 = io_axis_valid & io_axis_bits_tlast ? 3'h7 : 3'h6; // @[axis_gmii_tx.scala 78:44]
  wire [2:0] _stateNext_T_37 = _stateNext_T_23 ? _stateNext_T_25 : 3'h0; // @[Mux.scala 27:73]
  wire [2:0] _stateNext_T_44 = _stateNext_T_43 | _stateNext_T_37; // @[Mux.scala 27:73]
  wire  _stateNext_T_26 = stateCurr == 3'h7; // @[axis_gmii_tx.scala 79:16]
  reg [7:0] ifg; // @[axis_gmii_tx.scala 68:16]
  wire [7:0] _stateNext_T_28 = io_ifg_delay - 8'h1; // @[axis_gmii_tx.scala 79:64]
  wire  _stateNext_T_29 = ifg < _stateNext_T_28; // @[axis_gmii_tx.scala 79:50]
  wire [2:0] _stateNext_T_30 = ifg < _stateNext_T_28 ? 3'h7 : 3'h0; // @[axis_gmii_tx.scala 79:44]
  wire [2:0] _stateNext_T_38 = _stateNext_T_26 ? _stateNext_T_30 : 3'h0; // @[Mux.scala 27:73]
  wire [2:0] stateNext = _stateNext_T_44 | _stateNext_T_38; // @[Mux.scala 27:73]
  reg [7:0] s_tdata; // @[axis_gmii_tx.scala 62:20]
  reg  s_axis_tready; // @[axis_gmii_tx.scala 64:30]
  wire  _fcs_T_5 = frame_ptr == 16'h3; // @[axis_gmii_tx.scala 67:125]
  wire  _T_4 = frame_ptr == 16'h0; // @[axis_gmii_tx.scala 95:25]
  wire  _T_6 = ~reset; // @[axis_gmii_tx.scala 95:13]
  wire  _GEN_3 = io_axis_valid ? 1'h0 : s_axis_tready; // @[axis_gmii_tx.scala 96:28 98:23 64:30]
  wire [15:0] _frame_ptr_T_1 = frame_ptr + 16'h1; // @[axis_gmii_tx.scala 101:30]
  wire  _T_9 = frame_ptr == 16'h6; // @[axis_gmii_tx.scala 102:23]
  wire [15:0] _GEN_4 = _stateNext_T_3 ? 16'h0 : _frame_ptr_T_1; // @[axis_gmii_tx.scala 101:17 106:39 107:19]
  wire  _GEN_5 = _stateNext_T_3 | s_axis_tready; // @[axis_gmii_tx.scala 106:39 108:23 64:30]
  wire [7:0] _GEN_6 = _stateNext_T_3 ? io_axis_bits_tdata : s_tdata; // @[axis_gmii_tx.scala 106:39 109:17 62:20]
  wire  _GEN_7 = frame_ptr == 16'h6 | _GEN_5; // @[axis_gmii_tx.scala 102:32 104:23]
  wire [15:0] _GEN_9 = frame_ptr == 16'h6 ? _frame_ptr_T_1 : _GEN_4; // @[axis_gmii_tx.scala 101:17 102:32]
  wire [15:0] _GEN_10 = io_axis_bits_tuser ? 16'h0 : _frame_ptr_T_1; // @[axis_gmii_tx.scala 117:19 120:37 121:23]
  wire [15:0] _GEN_11 = io_axis_bits_tlast ? _GEN_10 : _frame_ptr_T_1; // @[axis_gmii_tx.scala 117:19 118:35]
  wire [15:0] _GEN_12 = io_axis_valid ? _GEN_11 : 16'h0; // @[axis_gmii_tx.scala 115:28 126:19]
  wire [7:0] _GEN_13 = _stateNext_T_12 ? 8'h0 : s_tdata; // @[axis_gmii_tx.scala 130:63 131:17 62:20]
  wire [15:0] _GEN_14 = _stateNext_T_12 ? _frame_ptr_T_1 : 16'h0; // @[axis_gmii_tx.scala 129:17 130:63 133:19]
  wire [15:0] _GEN_15 = frame_ptr == _GEN_64 ? 16'h0 : _frame_ptr_T_1; // @[axis_gmii_tx.scala 138:17 141:49 142:19]
  wire [15:0] _GEN_16 = _fcs_T_5 ? 16'h0 : _frame_ptr_T_1; // @[axis_gmii_tx.scala 146:17 147:32 148:19]
  wire  _GEN_17 = _stateNext_T_24 ? 1'h0 : 1'h1; // @[axis_gmii_tx.scala 152:21 153:49 154:23]
  wire [15:0] _frame_ptr_T_17 = _stateNext_T_29 ? _frame_ptr_T_1 : 16'h0; // @[axis_gmii_tx.scala 157:23]
  wire [15:0] _GEN_18 = _stateNext_T_26 ? _frame_ptr_T_17 : frame_ptr; // @[axis_gmii_tx.scala 156:43 157:17 63:26]
  wire [15:0] _GEN_19 = _stateNext_T_23 ? _frame_ptr_T_1 : _GEN_18; // @[axis_gmii_tx.scala 150:44 151:17]
  wire  _GEN_20 = _stateNext_T_23 ? _GEN_17 : s_axis_tready; // @[axis_gmii_tx.scala 150:44 64:30]
  wire [15:0] _GEN_21 = _stateNext_T_20 ? _GEN_16 : _GEN_19; // @[axis_gmii_tx.scala 144:43]
  wire  _GEN_22 = _stateNext_T_20 ? s_axis_tready : _GEN_20; // @[axis_gmii_tx.scala 144:43 64:30]
  wire [15:0] _GEN_23 = _stateNext_T_15 ? _GEN_15 : _GEN_21; // @[axis_gmii_tx.scala 135:43]
  wire [7:0] _GEN_24 = _stateNext_T_15 ? 8'h0 : s_tdata; // @[axis_gmii_tx.scala 135:43 139:15 62:20]
  wire  _GEN_25 = _stateNext_T_15 ? s_axis_tready : _GEN_22; // @[axis_gmii_tx.scala 135:43 64:30]
  wire [15:0] _GEN_26 = _stateNext_T_9 ? _GEN_14 : _GEN_23; // @[axis_gmii_tx.scala 128:44]
  wire [7:0] _GEN_27 = _stateNext_T_9 ? _GEN_13 : _GEN_24; // @[axis_gmii_tx.scala 128:44]
  wire  _GEN_28 = _stateNext_T_9 ? s_axis_tready : _GEN_25; // @[axis_gmii_tx.scala 128:44 64:30]
  wire  _GEN_30 = _stateNext_T_5 | _GEN_28; // @[axis_gmii_tx.scala 111:47 114:21]
  wire [15:0] _GEN_31 = _stateNext_T_5 ? _GEN_12 : _GEN_26; // @[axis_gmii_tx.scala 111:47]
  wire  _GEN_33 = _stateNext_T_2 ? _GEN_7 : _GEN_30; // @[axis_gmii_tx.scala 100:49]
  wire  _GEN_36 = _stateNext_T ? _GEN_3 : _GEN_33; // @[axis_gmii_tx.scala 94:37]
  wire  _GEN_39 = _stateCurr_T_2 & _GEN_36; // @[axis_gmii_tx.scala 160:19 93:44]
  wire  reset_crc = _stateCurr_T_2 & (_stateNext_T | _stateNext_T_2 | _stateNext_T_23 | _stateNext_T_26); // @[axis_gmii_tx.scala 167:52]
  wire  _update_crc_T_5 = _stateNext_T_5 | _stateNext_T_9; // @[axis_gmii_tx.scala 168:84]
  wire [7:0] _ifg_T_1 = ifg + 8'h1; // @[axis_gmii_tx.scala 189:18]
  reg [7:0] gmii_txd; // @[axis_gmii_tx.scala 207:19]
  wire [7:0] _io_gmii_txd_T = gmii_txd & 8'hf; // @[axis_gmii_tx.scala 207:75]
  reg  gmii_tx_en; // @[axis_gmii_tx.scala 208:25]
  reg  gmii_tx_er; // @[axis_gmii_tx.scala 209:25]
  wire [7:0] _gmii_txd_T_2 = _stateNext_T_3 ? 8'hd5 : 8'h55; // @[axis_gmii_tx.scala 227:22]
  wire [7:0] _gmii_txd_T_5 = ~crcUnit_io_crc[7:0]; // @[axis_gmii_tx.scala 234:32]
  wire  _gmii_txd_T_6 = frame_ptr == 16'h1; // @[axis_gmii_tx.scala 235:20]
  wire [7:0] _gmii_txd_T_8 = ~crcUnit_io_crc[15:8]; // @[axis_gmii_tx.scala 235:32]
  wire  _gmii_txd_T_9 = frame_ptr == 16'h2; // @[axis_gmii_tx.scala 236:20]
  wire [7:0] _gmii_txd_T_11 = ~crcUnit_io_crc[23:16]; // @[axis_gmii_tx.scala 236:32]
  wire [7:0] _gmii_txd_T_14 = ~crcUnit_io_crc[31:24]; // @[axis_gmii_tx.scala 237:32]
  wire [7:0] _gmii_txd_T_15 = _T_4 ? _gmii_txd_T_5 : 8'h0; // @[Mux.scala 27:73]
  wire [7:0] _gmii_txd_T_16 = _gmii_txd_T_6 ? _gmii_txd_T_8 : 8'h0; // @[Mux.scala 27:73]
  wire [7:0] _gmii_txd_T_17 = _gmii_txd_T_9 ? _gmii_txd_T_11 : 8'h0; // @[Mux.scala 27:73]
  wire [7:0] _gmii_txd_T_18 = _fcs_T_5 ? _gmii_txd_T_14 : 8'h0; // @[Mux.scala 27:73]
  wire [7:0] _gmii_txd_T_19 = _gmii_txd_T_15 | _gmii_txd_T_16; // @[Mux.scala 27:73]
  wire [7:0] _gmii_txd_T_20 = _gmii_txd_T_19 | _gmii_txd_T_17; // @[Mux.scala 27:73]
  wire [7:0] _gmii_txd_T_21 = _gmii_txd_T_20 | _gmii_txd_T_18; // @[Mux.scala 27:73]
  wire [7:0] _GEN_47 = _stateNext_T_20 ? _gmii_txd_T_21 : gmii_txd; // @[axis_gmii_tx.scala 232:43 233:16 207:19]
  wire [7:0] _GEN_48 = _stateNext_T_15 ? 8'h0 : _GEN_47; // @[axis_gmii_tx.scala 230:43 231:16]
  wire [7:0] _GEN_49 = _update_crc_T_5 ? s_tdata : _GEN_48; // @[axis_gmii_tx.scala 228:74 229:16]
  wire  _T_68 = io_axis_bits_tlast & io_axis_bits_tuser; // @[axis_gmii_tx.scala 268:30]
  wire  _GEN_60 = io_axis_valid ? _T_68 : 1'h1; // @[axis_gmii_tx.scala 267:25 274:16]
  wire  _GEN_61 = _stateCurr_T_2 & _stateNext_T_5 & _GEN_60; // @[axis_gmii_tx.scala 266:72 277:16]
  wire  _GEN_75 = _stateCurr_T_2 & ~_stateNext_T; // @[axis_gmii_tx.scala 103:15]
  wire  _GEN_92 = _GEN_75 & ~_stateNext_T_2 & ~_stateNext_T_5 & ~_stateNext_T_9; // @[axis_gmii_tx.scala 136:13]
  crc32_8 crcUnit ( // @[axis_gmii_tx.scala 39:23]
    .clock(crcUnit_clock),
    .reset(crcUnit_reset),
    .io_isEnable(crcUnit_io_isEnable),
    .io_dataIn(crcUnit_io_dataIn),
    .io_crc(crcUnit_io_crc)
  );
  assign io_gmii_txd = io_miiSel ? _io_gmii_txd_T : gmii_txd; // @[axis_gmii_tx.scala 207:54]
  assign io_gmii_tx_en = gmii_tx_en; // @[axis_gmii_tx.scala 208:50]
  assign io_gmii_tx_er = gmii_tx_er; // @[axis_gmii_tx.scala 209:50]
  assign io_axis_ready = s_axis_tready; // @[axis_gmii_tx.scala 64:55]
  assign crcUnit_clock = clock;
  assign crcUnit_reset = reset | reset_crc; // @[axis_gmii_tx.scala 177:33]
  assign crcUnit_io_isEnable = _stateCurr_T_2 & (_stateNext_T_5 | _stateNext_T_9 | _stateNext_T_15); // @[axis_gmii_tx.scala 168:52]
  assign crcUnit_io_dataIn = s_tdata; // @[axis_gmii_tx.scala 176:21]
  always @(posedge clock) begin
    if (io_clkEn) begin // @[axis_gmii_tx.scala 244:16]
      if (_stateCurr_T) begin // @[axis_gmii_tx.scala 245:29]
        mii_odd <= 1'h0; // @[axis_gmii_tx.scala 246:13]
      end else if (_stateNext_T) begin // @[axis_gmii_tx.scala 248:37]
        mii_odd <= io_axis_valid; // @[axis_gmii_tx.scala 249:15]
      end else begin
        mii_odd <= 1'h1; // @[axis_gmii_tx.scala 251:15]
      end
    end
    if (reset) begin // @[Reg.scala 35:20]
      stateCurr <= 3'h0; // @[Reg.scala 35:20]
    end else if (_stateCurr_T_2) begin // @[Reg.scala 36:18]
      stateCurr <= stateNext; // @[Reg.scala 36:22]
    end
    if (reset) begin // @[axis_gmii_tx.scala 63:26]
      frame_ptr <= 16'h0; // @[axis_gmii_tx.scala 63:26]
    end else if (_stateCurr_T_2) begin // @[axis_gmii_tx.scala 93:44]
      if (_stateNext_T) begin // @[axis_gmii_tx.scala 94:37]
        if (io_axis_valid) begin // @[axis_gmii_tx.scala 96:28]
          frame_ptr <= 16'h1; // @[axis_gmii_tx.scala 97:19]
        end
      end else if (_stateNext_T_2) begin // @[axis_gmii_tx.scala 100:49]
        frame_ptr <= _GEN_9;
      end else begin
        frame_ptr <= _GEN_31;
      end
    end
    if (_stateCurr_T_2) begin // @[axis_gmii_tx.scala 182:42]
      if (_stateNext_T_23) begin // @[axis_gmii_tx.scala 183:35]
        if (_stateNext_T_24) begin // @[axis_gmii_tx.scala 184:47]
          ifg <= 8'h0; // @[axis_gmii_tx.scala 185:11]
        end
      end else if (_stateNext_T_26) begin // @[axis_gmii_tx.scala 187:41]
        if (_stateNext_T_29) begin // @[axis_gmii_tx.scala 188:37]
          ifg <= _ifg_T_1; // @[axis_gmii_tx.scala 189:11]
        end
      end
    end
    if (_stateCurr_T_2) begin // @[axis_gmii_tx.scala 93:44]
      if (!(_stateNext_T)) begin // @[axis_gmii_tx.scala 94:37]
        if (_stateNext_T_2) begin // @[axis_gmii_tx.scala 100:49]
          if (frame_ptr == 16'h6) begin // @[axis_gmii_tx.scala 102:32]
            s_tdata <= io_axis_bits_tdata; // @[axis_gmii_tx.scala 105:17]
          end else begin
            s_tdata <= _GEN_6;
          end
        end else if (_stateNext_T_5) begin // @[axis_gmii_tx.scala 111:47]
          s_tdata <= io_axis_bits_tdata; // @[axis_gmii_tx.scala 113:15]
        end else begin
          s_tdata <= _GEN_27;
        end
      end
    end
    if (reset) begin // @[axis_gmii_tx.scala 64:30]
      s_axis_tready <= 1'h0; // @[axis_gmii_tx.scala 64:30]
    end else begin
      s_axis_tready <= _GEN_39;
    end
    if (io_clkEn) begin // @[axis_gmii_tx.scala 212:17]
      if (_stateCurr_T) begin // @[axis_gmii_tx.scala 213:30]
        gmii_txd <= {{4'd0}, gmii_txd[7:4]}; // @[axis_gmii_tx.scala 215:14]
      end else if (_stateNext_T) begin // @[axis_gmii_tx.scala 222:37]
        if (io_axis_valid) begin // @[axis_gmii_tx.scala 223:28]
          gmii_txd <= 8'h55; // @[axis_gmii_tx.scala 224:18]
        end
      end else if (_stateNext_T_2) begin // @[axis_gmii_tx.scala 226:48]
        gmii_txd <= _gmii_txd_T_2; // @[axis_gmii_tx.scala 227:16]
      end else begin
        gmii_txd <= _GEN_49;
      end
    end
    if (reset) begin // @[axis_gmii_tx.scala 208:25]
      gmii_tx_en <= 1'h0; // @[axis_gmii_tx.scala 208:25]
    end else if (_stateCurr_T_2) begin // @[axis_gmii_tx.scala 256:42]
      if (_stateNext_T) begin // @[axis_gmii_tx.scala 257:35]
        gmii_tx_en <= io_axis_valid; // @[axis_gmii_tx.scala 258:16]
      end else begin
        gmii_tx_en <= _stateNext_T_2 | _stateNext_T_5 | _stateNext_T_9 | _stateNext_T_15 | _stateNext_T_20; // @[axis_gmii_tx.scala 260:16]
      end
    end
    if (reset) begin // @[axis_gmii_tx.scala 209:25]
      gmii_tx_er <= 1'h0; // @[axis_gmii_tx.scala 209:25]
    end else begin
      gmii_tx_er <= _GEN_61;
    end
    `ifndef SYNTHESIS
    `ifdef PRINTF_COND
      if (`PRINTF_COND) begin
    `endif
        if (_stateCurr_T_2 & _stateNext_T & ~reset & ~(frame_ptr == 16'h0)) begin
          $fwrite(32'h80000002,"Assertion failed\n    at axis_gmii_tx.scala:95 assert( frame_ptr === 0.U )\n"); // @[axis_gmii_tx.scala 95:13]
        end
    `ifdef PRINTF_COND
      end
    `endif
    `endif // SYNTHESIS
    `ifndef SYNTHESIS
    `ifdef STOP_COND
      if (`STOP_COND) begin
    `endif
        if (_stateCurr_T_2 & _stateNext_T & ~reset & ~(frame_ptr == 16'h0)) begin
          $fatal; // @[axis_gmii_tx.scala 95:13]
        end
    `ifdef STOP_COND
      end
    `endif
    `endif // SYNTHESIS
    `ifndef SYNTHESIS
    `ifdef PRINTF_COND
      if (`PRINTF_COND) begin
    `endif
        if (_stateCurr_T_2 & ~_stateNext_T & _stateNext_T_2 & _T_9 & _T_6 & ~io_axis_valid) begin
          $fwrite(32'h80000002,"Assertion failed\n    at axis_gmii_tx.scala:103 assert( io.axis.valid === true.B )\n"); // @[axis_gmii_tx.scala 103:15]
        end
    `ifdef PRINTF_COND
      end
    `endif
    `endif // SYNTHESIS
    `ifndef SYNTHESIS
    `ifdef STOP_COND
      if (`STOP_COND) begin
    `endif
        if (_stateCurr_T_2 & ~_stateNext_T & _stateNext_T_2 & _T_9 & _T_6 & ~io_axis_valid) begin
          $fatal; // @[axis_gmii_tx.scala 103:15]
        end
    `ifdef STOP_COND
      end
    `endif
    `endif // SYNTHESIS
    `ifndef SYNTHESIS
    `ifdef PRINTF_COND
      if (`PRINTF_COND) begin
    `endif
        if (_GEN_75 & ~_stateNext_T_2 & ~_stateNext_T_5 & ~_stateNext_T_9 & _stateNext_T_15 & _T_6 & ~(frame_ptr <=
          _GEN_64)) begin
          $fwrite(32'h80000002,
            "Assertion failed\n    at axis_gmii_tx.scala:136 assert( frame_ptr <= MIN_FRAME_LENGTH-5.U )\n"); // @[axis_gmii_tx.scala 136:13]
        end
    `ifdef PRINTF_COND
      end
    `endif
    `endif // SYNTHESIS
    `ifndef SYNTHESIS
    `ifdef STOP_COND
      if (`STOP_COND) begin
    `endif
        if (_GEN_75 & ~_stateNext_T_2 & ~_stateNext_T_5 & ~_stateNext_T_9 & _stateNext_T_15 & _T_6 & ~(frame_ptr <=
          _GEN_64)) begin
          $fatal; // @[axis_gmii_tx.scala 136:13]
        end
    `ifdef STOP_COND
      end
    `endif
    `endif // SYNTHESIS
    `ifndef SYNTHESIS
    `ifdef PRINTF_COND
      if (`PRINTF_COND) begin
    `endif
        if (_GEN_92 & ~_stateNext_T_15 & _stateNext_T_20 & _T_6 & ~(frame_ptr <= 16'h3)) begin
          $fwrite(32'h80000002,"Assertion failed\n    at axis_gmii_tx.scala:145 assert( frame_ptr <= 3.U )\n"); // @[axis_gmii_tx.scala 145:13]
        end
    `ifdef PRINTF_COND
      end
    `endif
    `endif // SYNTHESIS
    `ifndef SYNTHESIS
    `ifdef STOP_COND
      if (`STOP_COND) begin
    `endif
        if (_GEN_92 & ~_stateNext_T_15 & _stateNext_T_20 & _T_6 & ~(frame_ptr <= 16'h3)) begin
          $fatal; // @[axis_gmii_tx.scala 145:13]
        end
    `ifdef STOP_COND
      end
    `endif
    `endif // SYNTHESIS
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{`RANDOM}};
  mii_odd = _RAND_0[0:0];
  _RAND_1 = {1{`RANDOM}};
  stateCurr = _RAND_1[2:0];
  _RAND_2 = {1{`RANDOM}};
  frame_ptr = _RAND_2[15:0];
  _RAND_3 = {1{`RANDOM}};
  ifg = _RAND_3[7:0];
  _RAND_4 = {1{`RANDOM}};
  s_tdata = _RAND_4[7:0];
  _RAND_5 = {1{`RANDOM}};
  s_axis_tready = _RAND_5[0:0];
  _RAND_6 = {1{`RANDOM}};
  gmii_txd = _RAND_6[7:0];
  _RAND_7 = {1{`RANDOM}};
  gmii_tx_en = _RAND_7[0:0];
  _RAND_8 = {1{`RANDOM}};
  gmii_tx_er = _RAND_8[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
