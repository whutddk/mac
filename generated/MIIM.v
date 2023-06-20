module MIIM(
  input         clock,
  input         reset,
  input         io_mdi,
  output        io_mdc,
  output        io_mdo,
  output        io_mdoEn,
  input  [7:0]  io_Divider,
  input         io_NoPre,
  input         io_WCtrlData,
  input  [15:0] io_CtrlData,
  input  [4:0]  io_Fiad,
  input  [4:0]  io_Rgad,
  input         io_RStat,
  input         io_ScanStat,
  output        io_Busy,
  output        io_LinkFail,
  output        io_Nvalid,
  output [15:0] io_Prsd,
  output        io_WCtrlDataStart,
  output        io_RStatStart,
  output        io_UpdateMIIRX_DATAReg
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
  reg [31:0] _RAND_32;
  reg [31:0] _RAND_33;
  reg [31:0] _RAND_34;
  reg [31:0] _RAND_35;
  reg [31:0] _RAND_36;
  reg [31:0] _RAND_37;
  reg [31:0] _RAND_38;
  reg [31:0] _RAND_39;
  reg [31:0] _RAND_40;
`endif // RANDOMIZE_REG_INIT
  reg [7:0] Counter; // @[MII.scala 46:25]
  reg  mdc; // @[MII.scala 47:20]
  wire  _mdcEn_T = Counter == 8'h0; // @[MII.scala 48:27]
  wire  _mdcEn_T_1 = ~mdc; // @[MII.scala 48:38]
  wire  mdcEn = Counter == 8'h0 & ~mdc; // @[MII.scala 48:36]
  wire  mdcEn_n = _mdcEn_T & mdc; // @[MII.scala 49:36]
  reg [7:0] ShiftReg; // @[MII.scala 51:25]
  reg [15:0] Prsd; // @[MII.scala 52:25]
  reg  LinkFail; // @[MII.scala 53:25]
  reg [6:0] BitCounter; // @[MII.scala 56:27]
  wire  EndOp = BitCounter == 7'h3f; // @[MII.scala 57:26]
  reg  InProgress; // @[MII.scala 60:27]
  reg  InProgress_q_0; // @[Reg.scala 35:20]
  reg  InProgress_q_1; // @[Reg.scala 35:20]
  reg  InProgress_q_2; // @[Reg.scala 35:20]
  wire  _EndBusy_T = ~InProgress_q_1; // @[MII.scala 62:32]
  wire  _EndBusy_T_1 = ~InProgress_q_1 & InProgress_q_2; // @[MII.scala 62:49]
  reg  EndBusy_r; // @[Reg.scala 35:20]
  reg  EndBusy; // @[Reg.scala 35:20]
  reg  WriteOp; // @[MII.scala 64:27]
  wire [7:0] TempDivider = io_Divider < 8'h2 ? 8'h2 : io_Divider; // @[MII.scala 82:26]
  wire [6:0] CounterPreset = TempDivider[7:1] - 7'h1; // @[MII.scala 83:44]
  wire [7:0] _Counter_T_1 = Counter - 8'h1; // @[MII.scala 89:24]
  wire  _LatchByte0_T = ~WriteOp; // @[MII.scala 97:47]
  wire  _LatchByte0_T_1 = InProgress & ~WriteOp; // @[MII.scala 97:45]
  wire  _LatchByte0_T_3 = InProgress & ~WriteOp & EndOp; // @[MII.scala 97:56]
  reg  LatchByte0_r; // @[Reg.scala 35:20]
  reg  LatchByte0; // @[Reg.scala 35:20]
  wire  _LatchByte1_T_3 = _LatchByte0_T_1 & BitCounter == 7'h37; // @[MII.scala 98:56]
  reg  LatchByte1_r; // @[Reg.scala 35:20]
  reg  LatchByte1; // @[Reg.scala 35:20]
  wire  _ByteSelect_0_T = BitCounter == 7'h0; // @[MII.scala 100:58]
  wire  _ByteSelect_0_T_1 = io_NoPre & BitCounter == 7'h0; // @[MII.scala 100:44]
  wire  ByteSelect_0 = InProgress & (io_NoPre & BitCounter == 7'h0 | ~io_NoPre & BitCounter == 7'h20); // @[MII.scala 100:31]
  wire  ByteSelect_1 = InProgress & BitCounter == 7'h28; // @[MII.scala 101:31]
  wire  _ByteSelect_2_T = InProgress & WriteOp; // @[MII.scala 102:31]
  wire  ByteSelect_2 = InProgress & WriteOp & BitCounter == 7'h30; // @[MII.scala 102:41]
  wire  ByteSelect_3 = _ByteSelect_2_T & BitCounter == 7'h38; // @[MII.scala 103:41]
  wire [7:0] _ShiftReg_T_2 = {2'h1,_LatchByte0_T,WriteOp,io_Fiad[4:1]}; // @[Cat.scala 33:92]
  wire [7:0] _ShiftReg_T_5 = {io_Fiad[0],io_Rgad,2'h2}; // @[Cat.scala 33:92]
  wire [7:0] _ShiftReg_T_8 = ByteSelect_0 ? _ShiftReg_T_2 : 8'h0; // @[Mux.scala 27:73]
  wire [7:0] _ShiftReg_T_9 = ByteSelect_1 ? _ShiftReg_T_5 : 8'h0; // @[Mux.scala 27:73]
  wire [7:0] _ShiftReg_T_10 = ByteSelect_2 ? io_CtrlData[15:8] : 8'h0; // @[Mux.scala 27:73]
  wire [7:0] _ShiftReg_T_11 = ByteSelect_3 ? io_CtrlData[7:0] : 8'h0; // @[Mux.scala 27:73]
  wire [7:0] _ShiftReg_T_12 = _ShiftReg_T_8 | _ShiftReg_T_9; // @[Mux.scala 27:73]
  wire [7:0] _ShiftReg_T_13 = _ShiftReg_T_12 | _ShiftReg_T_10; // @[Mux.scala 27:73]
  wire [7:0] _ShiftReg_T_14 = _ShiftReg_T_13 | _ShiftReg_T_11; // @[Mux.scala 27:73]
  wire [7:0] _ShiftReg_T_16 = {ShiftReg[6:0],io_mdi}; // @[Cat.scala 33:92]
  wire [15:0] _Prsd_T_2 = {Prsd[15:8],ShiftReg[6:0],io_mdi}; // @[Cat.scala 33:92]
  wire  _GEN_11 = io_Rgad == 5'h1 ? ~ShiftReg[1] : LinkFail; // @[MII.scala 117:30 118:20 53:25]
  wire [15:0] _Prsd_T_5 = {ShiftReg[6:0],io_mdi,Prsd[7:0]}; // @[Cat.scala 33:92]
  wire [15:0] _GEN_12 = LatchByte1 ? _Prsd_T_5 : Prsd; // @[MII.scala 120:30 121:14 52:25]
  wire  _SerialEn_T_1 = BitCounter > 7'h1f; // @[MII.scala 131:57]
  wire  _SerialEn_T_3 = _ByteSelect_0_T & io_NoPre; // @[MII.scala 131:91]
  wire  _SerialEn_T_14 = _LatchByte0_T & InProgress & (_SerialEn_T_1 & BitCounter < 7'h2e | _SerialEn_T_3); // @[MII.scala 132:42]
  wire  SerialEn = WriteOp & InProgress & (BitCounter > 7'h1f | _ByteSelect_0_T & io_NoPre) | _SerialEn_T_14; // @[MII.scala 131:107]
  wire  _mdoEn_T = BitCounter < 7'h20; // @[MII.scala 134:66]
  wire  _mdoEn_T_2 = SerialEn | InProgress & BitCounter < 7'h20; // @[MII.scala 134:40]
  reg  mdoEn_r; // @[Reg.scala 35:20]
  reg  mdoEn_r_1; // @[Reg.scala 35:20]
  reg  mdoEn; // @[Reg.scala 35:20]
  wire  _mdo_2d_T_2 = ~SerialEn & _mdoEn_T; // @[MII.scala 135:37]
  reg  mdo_2d; // @[Reg.scala 35:20]
  wire  _mdo_d_T_1 = ShiftReg[7] | mdo_2d; // @[MII.scala 136:47]
  reg  mdo_d; // @[Reg.scala 35:20]
  reg  mdo; // @[Reg.scala 35:20]
  reg  WCtrlData_q_0; // @[Reg.scala 35:20]
  reg  WCtrlData_q_1; // @[Reg.scala 35:20]
  reg  WCtrlData_q_2; // @[Reg.scala 35:20]
  reg  WCtrlDataStart; // @[MII.scala 147:31]
  reg  WCtrlDataStart_q_0; // @[Reg.scala 35:20]
  reg  WCtrlDataStart_q_1; // @[Reg.scala 35:20]
  wire  WriteDataOp = WCtrlDataStart_q_0 & ~WCtrlDataStart_q_1; // @[MII.scala 149:42]
  wire  _GEN_32 = WCtrlData_q_1 & ~WCtrlData_q_2 | WCtrlDataStart; // @[MII.scala 162:50 163:20 147:31]
  wire  _WCtrlDataStart_q0_T = ~EndBusy; // @[MII.scala 167:62]
  reg  WCtrlDataStart_q0; // @[Reg.scala 35:20]
  reg  UpdateMIIRX_DATAReg; // @[MII.scala 168:36]
  reg  RStat_q_0; // @[Reg.scala 35:20]
  reg  RStat_q_1; // @[Reg.scala 35:20]
  reg  RStat_q_2; // @[Reg.scala 35:20]
  reg  RStatStart; // @[MII.scala 174:27]
  reg  RStatStart_q_0; // @[Reg.scala 35:20]
  reg  RStatStart_q_1; // @[Reg.scala 35:20]
  wire  ReadStatusOp = RStatStart_q_0 & ~RStatStart_q_1; // @[MII.scala 176:42]
  wire  _GEN_40 = RStat_q_1 & ~RStat_q_2 | RStatStart; // @[MII.scala 181:40 182:16 174:27]
  wire  _GEN_42 = EndOp ? 1'h0 : InProgress; // @[MII.scala 191:24 192:18 60:27]
  reg  SyncStatmdcEn; // @[Reg.scala 35:20]
  wire  _ScanStatusOp_T = ~InProgress; // @[MII.scala 200:44]
  wire  ScanStatusOp = SyncStatmdcEn & ~InProgress & ~InProgress_q_0 & _EndBusy_T; // @[MII.scala 200:75]
  wire  StartOp = WriteDataOp | ReadStatusOp | ScanStatusOp; // @[MII.scala 239:41]
  wire  _GEN_43 = StartOp | _GEN_42; // @[MII.scala 189:19 190:18]
  reg  ScanStat_q_0; // @[Reg.scala 35:20]
  reg  ScanStat_q_1; // @[Reg.scala 35:20]
  reg  Nvalid; // @[MII.scala 203:23]
  wire  _GEN_48 = ScanStat_q_1 & ~SyncStatmdcEn | Nvalid; // @[MII.scala 208:48 209:12 203:23]
  wire [6:0] _BitCounter_T_1 = BitCounter + 7'h1; // @[MII.scala 231:34]
  assign io_mdc = mdc; // @[MII.scala 69:10]
  assign io_mdo = mdo; // @[MII.scala 138:10]
  assign io_mdoEn = mdoEn; // @[MII.scala 139:12]
  assign io_Busy = io_WCtrlData | WCtrlDataStart | io_RStat | RStatStart | SyncStatmdcEn | EndBusy | InProgress |
    InProgress_q_2 | Nvalid; // @[MII.scala 240:125]
  assign io_LinkFail = LinkFail; // @[MII.scala 70:15]
  assign io_Nvalid = Nvalid; // @[MII.scala 204:13]
  assign io_Prsd = Prsd; // @[MII.scala 71:11]
  assign io_WCtrlDataStart = WCtrlDataStart; // @[MII.scala 150:21]
  assign io_RStatStart = RStatStart; // @[MII.scala 177:17]
  assign io_UpdateMIIRX_DATAReg = UpdateMIIRX_DATAReg; // @[MII.scala 169:26]
  always @(posedge clock) begin
    if (reset) begin // @[MII.scala 46:25]
      Counter <= 8'h1; // @[MII.scala 46:25]
    end else if (_mdcEn_T) begin // @[MII.scala 85:27]
      Counter <= {{1'd0}, CounterPreset}; // @[MII.scala 87:13]
    end else begin
      Counter <= _Counter_T_1; // @[MII.scala 89:13]
    end
    if (reset) begin // @[MII.scala 47:20]
      mdc <= 1'h0; // @[MII.scala 47:20]
    end else if (_mdcEn_T) begin // @[MII.scala 85:27]
      mdc <= _mdcEn_T_1; // @[MII.scala 86:9]
    end
    if (reset) begin // @[MII.scala 51:25]
      ShiftReg <= 8'h0; // @[MII.scala 51:25]
    end else if (mdcEn_n) begin // @[MII.scala 105:16]
      if (ByteSelect_0 | ByteSelect_1 | ByteSelect_2 | ByteSelect_3) begin // @[MII.scala 106:34]
        ShiftReg <= _ShiftReg_T_14; // @[MII.scala 107:16]
      end else begin
        ShiftReg <= _ShiftReg_T_16; // @[MII.scala 114:16]
      end
    end
    if (reset) begin // @[MII.scala 52:25]
      Prsd <= 16'h0; // @[MII.scala 52:25]
    end else if (mdcEn_n) begin // @[MII.scala 105:16]
      if (!(ByteSelect_0 | ByteSelect_1 | ByteSelect_2 | ByteSelect_3)) begin // @[MII.scala 106:34]
        if (LatchByte0) begin // @[MII.scala 115:23]
          Prsd <= _Prsd_T_2; // @[MII.scala 116:14]
        end else begin
          Prsd <= _GEN_12;
        end
      end
    end
    if (reset) begin // @[MII.scala 53:25]
      LinkFail <= 1'h0; // @[MII.scala 53:25]
    end else if (mdcEn_n) begin // @[MII.scala 105:16]
      if (!(ByteSelect_0 | ByteSelect_1 | ByteSelect_2 | ByteSelect_3)) begin // @[MII.scala 106:34]
        if (LatchByte0) begin // @[MII.scala 115:23]
          LinkFail <= _GEN_11;
        end
      end
    end
    if (reset) begin // @[MII.scala 56:27]
      BitCounter <= 7'h0; // @[MII.scala 56:27]
    end else if (mdcEn) begin // @[MII.scala 226:16]
      if (InProgress) begin // @[MII.scala 227:24]
        if (_ByteSelect_0_T_1) begin // @[MII.scala 228:45]
          BitCounter <= 7'h21; // @[MII.scala 229:20]
        end else begin
          BitCounter <= _BitCounter_T_1; // @[MII.scala 231:20]
        end
      end else begin
        BitCounter <= 7'h0; // @[MII.scala 234:18]
      end
    end
    if (reset) begin // @[MII.scala 60:27]
      InProgress <= 1'h0; // @[MII.scala 60:27]
    end else if (mdcEn) begin // @[MII.scala 188:14]
      InProgress <= _GEN_43;
    end
    if (reset) begin // @[Reg.scala 35:20]
      InProgress_q_0 <= 1'h0; // @[Reg.scala 35:20]
    end else if (mdcEn) begin // @[Reg.scala 36:18]
      InProgress_q_0 <= InProgress; // @[Reg.scala 36:22]
    end
    if (reset) begin // @[Reg.scala 35:20]
      InProgress_q_1 <= 1'h0; // @[Reg.scala 35:20]
    end else if (mdcEn) begin // @[Reg.scala 36:18]
      InProgress_q_1 <= InProgress_q_0; // @[Reg.scala 36:22]
    end
    if (reset) begin // @[Reg.scala 35:20]
      InProgress_q_2 <= 1'h0; // @[Reg.scala 35:20]
    end else if (mdcEn) begin // @[Reg.scala 36:18]
      InProgress_q_2 <= InProgress_q_1; // @[Reg.scala 36:22]
    end
    if (reset) begin // @[Reg.scala 35:20]
      EndBusy_r <= 1'h0; // @[Reg.scala 35:20]
    end else begin
      EndBusy_r <= _EndBusy_T_1;
    end
    if (reset) begin // @[Reg.scala 35:20]
      EndBusy <= 1'h0; // @[Reg.scala 35:20]
    end else begin
      EndBusy <= EndBusy_r;
    end
    if (reset) begin // @[MII.scala 64:27]
      WriteOp <= 1'h0; // @[MII.scala 64:27]
    end else if (mdcEn) begin // @[MII.scala 215:14]
      if (StartOp) begin // @[MII.scala 216:19]
        if (_ScanStatusOp_T) begin // @[MII.scala 217:26]
          WriteOp <= WriteDataOp; // @[MII.scala 218:17]
        end
      end else if (EndOp) begin // @[MII.scala 220:24]
        WriteOp <= 1'h0; // @[MII.scala 221:15]
      end
    end
    if (reset) begin // @[Reg.scala 35:20]
      LatchByte0_r <= 1'h0; // @[Reg.scala 35:20]
    end else if (mdcEn) begin // @[Reg.scala 36:18]
      LatchByte0_r <= _LatchByte0_T_3; // @[Reg.scala 36:22]
    end
    if (reset) begin // @[Reg.scala 35:20]
      LatchByte0 <= 1'h0; // @[Reg.scala 35:20]
    end else if (mdcEn) begin // @[Reg.scala 36:18]
      LatchByte0 <= LatchByte0_r; // @[Reg.scala 36:22]
    end
    if (reset) begin // @[Reg.scala 35:20]
      LatchByte1_r <= 1'h0; // @[Reg.scala 35:20]
    end else if (mdcEn) begin // @[Reg.scala 36:18]
      LatchByte1_r <= _LatchByte1_T_3; // @[Reg.scala 36:22]
    end
    if (reset) begin // @[Reg.scala 35:20]
      LatchByte1 <= 1'h0; // @[Reg.scala 35:20]
    end else if (mdcEn) begin // @[Reg.scala 36:18]
      LatchByte1 <= LatchByte1_r; // @[Reg.scala 36:22]
    end
    if (reset) begin // @[Reg.scala 35:20]
      mdoEn_r <= 1'h0; // @[Reg.scala 35:20]
    end else if (mdcEn_n) begin // @[Reg.scala 36:18]
      mdoEn_r <= _mdoEn_T_2; // @[Reg.scala 36:22]
    end
    if (reset) begin // @[Reg.scala 35:20]
      mdoEn_r_1 <= 1'h0; // @[Reg.scala 35:20]
    end else if (mdcEn_n) begin // @[Reg.scala 36:18]
      mdoEn_r_1 <= mdoEn_r; // @[Reg.scala 36:22]
    end
    if (reset) begin // @[Reg.scala 35:20]
      mdoEn <= 1'h0; // @[Reg.scala 35:20]
    end else if (mdcEn_n) begin // @[Reg.scala 36:18]
      mdoEn <= mdoEn_r_1; // @[Reg.scala 36:22]
    end
    if (reset) begin // @[Reg.scala 35:20]
      mdo_2d <= 1'h0; // @[Reg.scala 35:20]
    end else if (mdcEn_n) begin // @[Reg.scala 36:18]
      mdo_2d <= _mdo_2d_T_2; // @[Reg.scala 36:22]
    end
    if (reset) begin // @[Reg.scala 35:20]
      mdo_d <= 1'h0; // @[Reg.scala 35:20]
    end else if (mdcEn_n) begin // @[Reg.scala 36:18]
      mdo_d <= _mdo_d_T_1; // @[Reg.scala 36:22]
    end
    if (reset) begin // @[Reg.scala 35:20]
      mdo <= 1'h0; // @[Reg.scala 35:20]
    end else if (mdcEn_n) begin // @[Reg.scala 36:18]
      mdo <= mdo_d; // @[Reg.scala 36:22]
    end
    if (reset) begin // @[Reg.scala 35:20]
      WCtrlData_q_0 <= 1'h0; // @[Reg.scala 35:20]
    end else begin
      WCtrlData_q_0 <= io_WCtrlData;
    end
    if (reset) begin // @[Reg.scala 35:20]
      WCtrlData_q_1 <= 1'h0; // @[Reg.scala 35:20]
    end else begin
      WCtrlData_q_1 <= WCtrlData_q_0;
    end
    if (reset) begin // @[Reg.scala 35:20]
      WCtrlData_q_2 <= 1'h0; // @[Reg.scala 35:20]
    end else begin
      WCtrlData_q_2 <= WCtrlData_q_1;
    end
    if (reset) begin // @[MII.scala 147:31]
      WCtrlDataStart <= 1'h0; // @[MII.scala 147:31]
    end else if (EndBusy) begin // @[MII.scala 160:18]
      WCtrlDataStart <= 1'h0; // @[MII.scala 161:20]
    end else begin
      WCtrlDataStart <= _GEN_32;
    end
    if (reset) begin // @[Reg.scala 35:20]
      WCtrlDataStart_q_0 <= 1'h0; // @[Reg.scala 35:20]
    end else if (mdcEn) begin // @[Reg.scala 36:18]
      WCtrlDataStart_q_0 <= WCtrlDataStart; // @[Reg.scala 36:22]
    end
    if (reset) begin // @[Reg.scala 35:20]
      WCtrlDataStart_q_1 <= 1'h0; // @[Reg.scala 35:20]
    end else if (mdcEn) begin // @[Reg.scala 36:18]
      WCtrlDataStart_q_1 <= WCtrlDataStart_q_0; // @[Reg.scala 36:22]
    end
    if (reset) begin // @[Reg.scala 35:20]
      WCtrlDataStart_q0 <= 1'h0; // @[Reg.scala 35:20]
    end else if (_WCtrlDataStart_q0_T) begin // @[Reg.scala 36:18]
      WCtrlDataStart_q0 <= WCtrlDataStart; // @[Reg.scala 36:22]
    end
    if (reset) begin // @[MII.scala 168:36]
      UpdateMIIRX_DATAReg <= 1'h0; // @[MII.scala 168:36]
    end else begin
      UpdateMIIRX_DATAReg <= EndBusy & ~WCtrlDataStart_q0; // @[MII.scala 168:36]
    end
    if (reset) begin // @[Reg.scala 35:20]
      RStat_q_0 <= 1'h0; // @[Reg.scala 35:20]
    end else begin
      RStat_q_0 <= io_RStat;
    end
    if (reset) begin // @[Reg.scala 35:20]
      RStat_q_1 <= 1'h0; // @[Reg.scala 35:20]
    end else begin
      RStat_q_1 <= RStat_q_0;
    end
    if (reset) begin // @[Reg.scala 35:20]
      RStat_q_2 <= 1'h0; // @[Reg.scala 35:20]
    end else begin
      RStat_q_2 <= RStat_q_1;
    end
    if (reset) begin // @[MII.scala 174:27]
      RStatStart <= 1'h0; // @[MII.scala 174:27]
    end else if (EndBusy) begin // @[MII.scala 179:18]
      RStatStart <= 1'h0; // @[MII.scala 180:16]
    end else begin
      RStatStart <= _GEN_40;
    end
    if (reset) begin // @[Reg.scala 35:20]
      RStatStart_q_0 <= 1'h0; // @[Reg.scala 35:20]
    end else if (mdcEn) begin // @[Reg.scala 36:18]
      RStatStart_q_0 <= RStatStart; // @[Reg.scala 36:22]
    end
    if (reset) begin // @[Reg.scala 35:20]
      RStatStart_q_1 <= 1'h0; // @[Reg.scala 35:20]
    end else if (mdcEn) begin // @[Reg.scala 36:18]
      RStatStart_q_1 <= RStatStart_q_0; // @[Reg.scala 36:22]
    end
    if (reset) begin // @[Reg.scala 35:20]
      SyncStatmdcEn <= 1'h0; // @[Reg.scala 35:20]
    end else if (mdcEn) begin // @[Reg.scala 36:18]
      SyncStatmdcEn <= ScanStat_q_1; // @[Reg.scala 36:22]
    end
    if (reset) begin // @[Reg.scala 35:20]
      ScanStat_q_0 <= 1'h0; // @[Reg.scala 35:20]
    end else begin
      ScanStat_q_0 <= io_ScanStat;
    end
    if (reset) begin // @[Reg.scala 35:20]
      ScanStat_q_1 <= 1'h0; // @[Reg.scala 35:20]
    end else begin
      ScanStat_q_1 <= ScanStat_q_0;
    end
    if (reset) begin // @[MII.scala 203:23]
      Nvalid <= 1'h0; // @[MII.scala 203:23]
    end else if (_EndBusy_T_1) begin // @[MII.scala 206:46]
      Nvalid <= 1'h0; // @[MII.scala 207:12]
    end else begin
      Nvalid <= _GEN_48;
    end
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
  Counter = _RAND_0[7:0];
  _RAND_1 = {1{`RANDOM}};
  mdc = _RAND_1[0:0];
  _RAND_2 = {1{`RANDOM}};
  ShiftReg = _RAND_2[7:0];
  _RAND_3 = {1{`RANDOM}};
  Prsd = _RAND_3[15:0];
  _RAND_4 = {1{`RANDOM}};
  LinkFail = _RAND_4[0:0];
  _RAND_5 = {1{`RANDOM}};
  BitCounter = _RAND_5[6:0];
  _RAND_6 = {1{`RANDOM}};
  InProgress = _RAND_6[0:0];
  _RAND_7 = {1{`RANDOM}};
  InProgress_q_0 = _RAND_7[0:0];
  _RAND_8 = {1{`RANDOM}};
  InProgress_q_1 = _RAND_8[0:0];
  _RAND_9 = {1{`RANDOM}};
  InProgress_q_2 = _RAND_9[0:0];
  _RAND_10 = {1{`RANDOM}};
  EndBusy_r = _RAND_10[0:0];
  _RAND_11 = {1{`RANDOM}};
  EndBusy = _RAND_11[0:0];
  _RAND_12 = {1{`RANDOM}};
  WriteOp = _RAND_12[0:0];
  _RAND_13 = {1{`RANDOM}};
  LatchByte0_r = _RAND_13[0:0];
  _RAND_14 = {1{`RANDOM}};
  LatchByte0 = _RAND_14[0:0];
  _RAND_15 = {1{`RANDOM}};
  LatchByte1_r = _RAND_15[0:0];
  _RAND_16 = {1{`RANDOM}};
  LatchByte1 = _RAND_16[0:0];
  _RAND_17 = {1{`RANDOM}};
  mdoEn_r = _RAND_17[0:0];
  _RAND_18 = {1{`RANDOM}};
  mdoEn_r_1 = _RAND_18[0:0];
  _RAND_19 = {1{`RANDOM}};
  mdoEn = _RAND_19[0:0];
  _RAND_20 = {1{`RANDOM}};
  mdo_2d = _RAND_20[0:0];
  _RAND_21 = {1{`RANDOM}};
  mdo_d = _RAND_21[0:0];
  _RAND_22 = {1{`RANDOM}};
  mdo = _RAND_22[0:0];
  _RAND_23 = {1{`RANDOM}};
  WCtrlData_q_0 = _RAND_23[0:0];
  _RAND_24 = {1{`RANDOM}};
  WCtrlData_q_1 = _RAND_24[0:0];
  _RAND_25 = {1{`RANDOM}};
  WCtrlData_q_2 = _RAND_25[0:0];
  _RAND_26 = {1{`RANDOM}};
  WCtrlDataStart = _RAND_26[0:0];
  _RAND_27 = {1{`RANDOM}};
  WCtrlDataStart_q_0 = _RAND_27[0:0];
  _RAND_28 = {1{`RANDOM}};
  WCtrlDataStart_q_1 = _RAND_28[0:0];
  _RAND_29 = {1{`RANDOM}};
  WCtrlDataStart_q0 = _RAND_29[0:0];
  _RAND_30 = {1{`RANDOM}};
  UpdateMIIRX_DATAReg = _RAND_30[0:0];
  _RAND_31 = {1{`RANDOM}};
  RStat_q_0 = _RAND_31[0:0];
  _RAND_32 = {1{`RANDOM}};
  RStat_q_1 = _RAND_32[0:0];
  _RAND_33 = {1{`RANDOM}};
  RStat_q_2 = _RAND_33[0:0];
  _RAND_34 = {1{`RANDOM}};
  RStatStart = _RAND_34[0:0];
  _RAND_35 = {1{`RANDOM}};
  RStatStart_q_0 = _RAND_35[0:0];
  _RAND_36 = {1{`RANDOM}};
  RStatStart_q_1 = _RAND_36[0:0];
  _RAND_37 = {1{`RANDOM}};
  SyncStatmdcEn = _RAND_37[0:0];
  _RAND_38 = {1{`RANDOM}};
  ScanStat_q_0 = _RAND_38[0:0];
  _RAND_39 = {1{`RANDOM}};
  ScanStat_q_1 = _RAND_39[0:0];
  _RAND_40 = {1{`RANDOM}};
  Nvalid = _RAND_40[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
