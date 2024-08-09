package MAC

import chisel3._
import chisel3.util._

class GMII_TX_Bundle extends Bundle{
  val txd = UInt(8.W)
  val tx_en = Bool()
  val tx_er = Bool()
}

class AXIS_Bundle extends Bundle{
  val tdata = UInt(8.W)
  val tlast = Bool()
  val tuser = Bool()
}



class GmiiTx_AxisRx_IO extends Bundle{
  val gmii = Ouput(new GMII_TX_Bundle)
  val axis = Flipped(Decoupled(new AXIS_Bundle))

  val clkEn = Input(Bool()),
  val miiSel = Input(Bool()),

  val ifg_delay = Input(UInt(8.W))

}


/*
 * AXI4-Stream GMII frame transmitter (AXI in, GMII out)
 */
class GmiiTx_AxisRx extends Module{
  val io = IO(new GmiiTx_AxisRx_IO)


  val crcUnit = Module(new crc32_8)

  def STATE_IDLE  = 0
  def STATE_PREAMBLE = 1
  def STATE_PAYLOAD = 2
  def STATE_LAST = 3
  def STATE_PAD = 4
  def STATE_FCS = 5
  def STATE_WAIT = 6
  def STATE_IFG = 7

  val stateNext = Wire(UInt(3.W))
  val stateCurr = RegEnable( stateNext, 0.U, io.clkEn & ~(io.miiSel && mii_odd_reg) )


  stateNext := Mux1H(Seq(
    (stateCurr === STATE_IDLE)     -> ( Mux( s_axis_tvalid, STATE_PREAMBLE, STATE_IDLE )),
    (stateCurr === STATE_PREAMBLE) -> ( Mux( frame_ptr_reg == 16'd7, STATE_PAYLOAD, STATE_PREAMBLE )),
    (stateCurr === STATE_PAYLOAD)  -> ( Mux( s_axis_tvalid, Mux( s_axis_tlast, Mux( s_axis_tuser, STATE_IFG, STATE_LAST ), STATE_PAYLOAD ),STATE_WAIT_END ) ),
    (stateCurr === STATE_LAST)     -> ( Mux( ENABLE_PADDING && frame_ptr_reg < MIN_FRAME_LENGTH-5, STATE_PAD, STATE_FCS )),
    (stateCurr === STATE_PAD)      -> ( Mux( (frame_ptr_reg < MIN_FRAME_LENGTH-5), STATE_PAD, STATE_FCS )),
    (stateCurr === STATE_FCS)      -> ( Mux( frame_ptr_reg < 3, STATE_FCS, STATE_IFG )),
    (stateCurr === STATE_WAIT)     -> ( Mux( s_axis_tvalid & s_axis_tlast, STATE_IFG, STATE_WAIT ) ),
    (stateCurr === STATE_IFG)      -> ( Mux( ifg_reg < ifg_delay-1, STATE_IFG, STATE_IDLE )),
  ))





























  val crcOut = crcUnit.io.crc
  val crcCnt = RegInit(0.U(2.W))


  crcUnit.io.isEnable := 
  crcUnit.io.dataIn := s_tdata_reg
  crcUnit.reset := 

        if (reset_crc) begin
            crc_state <= 32'hFFFFFFFF;
        end else if (update_crc) begin

        end

rgmii_lfsr #(
    .LFSR_WIDTH(32),
    .LFSR_POLY(32'h4c11db7),
    .LFSR_CONFIG("GALOIS"),
    .LFSR_FEED_FORWARD(0),
    .REVERSE(1),
    .DATA_WIDTH(8),
    .STYLE("AUTO")
)
eth_crc_8 (
    .data_in(),
    .state_in(crc_state),
    .data_out(),
    .state_out(crc_next)
);



}



localparam [7:0]
    ETH_PRE = 8'h55,
    ETH_SFD = 8'hD5;

localparam [2:0]
    STATE_IDLE = 3'd0,
    STATE_PREAMBLE = 3'd1,
    STATE_PAYLOAD = 3'd2,
    STATE_LAST = 3'd3,
    STATE_PAD = 3'd4,
    STATE_FCS = 3'd5,
    STATE_WAIT_END = 3'd6,
    STATE_IFG = 3'd7;

reg [2:0] state_reg, state_next;

// datapath control signals
reg reset_crc;
reg update_crc;

reg [7:0] s_tdata_reg, s_tdata_next, ifg_reg, ifg_next;

reg mii_odd_reg, mii_odd_next;
reg [3:0] mii_msn_reg, mii_msn_next;

reg [15:0] frame_ptr_reg, frame_ptr_next;

reg [7:0] gmii_txd_reg, gmii_txd_next;
reg gmii_tx_en_reg, gmii_tx_en_next;
reg gmii_tx_er_reg, gmii_tx_er_next;

reg s_axis_tready_reg, s_axis_tready_next;
reg [31:0] crc_state, fcs_next;   



assign s_axis_tready = s_axis_tready_reg;

assign gmii_txd = gmii_txd_reg;
assign gmii_tx_en = gmii_tx_en_reg;
assign gmii_tx_er = gmii_tx_er_reg;














always @* begin
    reset_crc = 1'b0;
    update_crc = 1'b0;

    mii_odd_next = mii_odd_reg;
    mii_msn_next = mii_msn_reg;

    frame_ptr_next = frame_ptr_reg;
    fcs_next = fcs_reg;
    ifg_next = ifg_reg;

    s_axis_tready_next = 1'b0;

    s_tdata_next = s_tdata_reg;

    gmii_txd_next = 8'd0;
    gmii_tx_en_next = 1'b0;
    gmii_tx_er_next = 1'b0;

    if (!clk_enable) begin
        // clock disabled - hold state and outputs
        gmii_txd_next = gmii_txd_reg;
        gmii_tx_en_next = gmii_tx_en_reg;
        gmii_tx_er_next = gmii_tx_er_reg;

    end else if (mii_select && mii_odd_reg) begin
        // MII odd cycle - hold state, output MSN
        mii_odd_next = 1'b0;
        gmii_txd_next = {4'd0, mii_msn_reg};
        gmii_tx_en_next = gmii_tx_en_reg;
        gmii_tx_er_next = gmii_tx_er_reg;

    end else begin
        case (state_reg)
            STATE_IDLE: begin
                // idle state - wait for packet
                reset_crc = 1'b1;
                mii_odd_next = 1'b0;

                if (s_axis_tvalid) begin
                    mii_odd_next = 1'b1;
                    frame_ptr_next = 16'd1;
                    gmii_txd_next = ETH_PRE;
                    gmii_tx_en_next = 1'b1;

                end else begin

                end
            end
            STATE_PREAMBLE: begin
                // send preamble
                reset_crc = 1'b1;

                mii_odd_next = 1'b1;
                frame_ptr_next = frame_ptr_reg + 16'd1;

                gmii_txd_next = ETH_PRE;
                gmii_tx_en_next = 1'b1;

                if (frame_ptr_reg == 16'd6) begin
                    s_axis_tready_next = 1'b1;
                    s_tdata_next = s_axis_tdata;

                end else if (frame_ptr_reg == 16'd7) begin
                    // end of preamble; start payload
                    frame_ptr_next = 16'd0;
                    if (s_axis_tready_reg) begin
                        s_axis_tready_next = 1'b1;
                        s_tdata_next = s_axis_tdata;
                    end
                    gmii_txd_next = ETH_SFD;

                end
            end
            STATE_PAYLOAD: begin
                // send payload

                update_crc = 1'b1;
                s_axis_tready_next = 1'b1;

                mii_odd_next = 1'b1;
                frame_ptr_next = frame_ptr_reg + 16'd1;

                gmii_txd_next = s_tdata_reg;
                gmii_tx_en_next = 1'b1;

                s_tdata_next = s_axis_tdata;

                if (s_axis_tvalid) begin
                    if (s_axis_tlast) begin
                        s_axis_tready_next = !s_axis_tready_reg;
                        if (s_axis_tuser) begin
                            gmii_tx_er_next = 1'b1;
                            frame_ptr_next = 1'b0;
                        end
                    end
                end else begin
                    // tvalid deassert, fail frame
                    gmii_tx_er_next = 1'b1;
                    frame_ptr_next = 16'd0;
                end
            end
            STATE_LAST: begin
                // last payload word

                update_crc = 1'b1;

                mii_odd_next = 1'b1;
                frame_ptr_next = frame_ptr_reg + 16'd1;

                gmii_txd_next = s_tdata_reg;
                gmii_tx_en_next = 1'b1;

                if (ENABLE_PADDING && frame_ptr_reg < MIN_FRAME_LENGTH-5) begin
                    s_tdata_next = 8'd0;
                end else begin
                    frame_ptr_next = 16'd0;
                end
            end
            STATE_PAD: begin
                // send padding

                update_crc = 1'b1;
                mii_odd_next = 1'b1;
                frame_ptr_next = frame_ptr_reg + 16'd1;

                gmii_txd_next = 8'd0;
                gmii_tx_en_next = 1'b1;

                s_tdata_next = 8'd0;

                if (frame_ptr_reg < MIN_FRAME_LENGTH-5) begin
                end else begin
                    frame_ptr_next = 16'd0;
                end
            end
            STATE_FCS: begin
                // send FCS

                mii_odd_next = 1'b1;
                frame_ptr_next = frame_ptr_reg + 16'd1;

                case (frame_ptr_reg)
                    2'd0: gmii_txd_next = ~crc_state[7:0];
                    2'd1: gmii_txd_next = ~crc_state[15:8];
                    2'd2: gmii_txd_next = ~crc_state[23:16];
                    2'd3: gmii_txd_next = ~crc_state[31:24];
                    default:;
                endcase
                gmii_tx_en_next = 1'b1;

                if (frame_ptr_reg < 3) begin
                end else begin
                    frame_ptr_next = 16'd0;
                    fcs_next = crc_state;
                end
            end
            STATE_WAIT_END: begin
                // wait for end of frame

                reset_crc = 1'b1;

                mii_odd_next = 1'b1;
                frame_ptr_next = frame_ptr_reg + 16'd1;
                s_axis_tready_next = 1'b1;

                if (s_axis_tvalid & s_axis_tlast) begin
                  s_axis_tready_next = 1'b0;
                  ifg_next = 8'b0;
                end
            end
            STATE_IFG: begin
                // send IFG

                reset_crc = 1'b1;

                mii_odd_next = 1'b1;
                frame_ptr_next = frame_ptr_reg + 16'd1;

                if (ifg_reg < ifg_delay-1) begin
                    ifg_next = ifg_reg + 1;
                end
            end
        endcase

        if (mii_select) begin
            mii_msn_next = gmii_txd_next[7:4];
            gmii_txd_next[7:4] = 4'd0;
        end
    end
end
















always @(posedge clk) begin
    if (rst) begin

        frame_ptr_reg <= 16'd0;

        s_axis_tready_reg <= 1'b0;

        gmii_tx_en_reg <= 1'b0;
        gmii_tx_er_reg <= 1'b0;


        fcs_reg <= 32'hFFFFFFFF;
    end else begin


        frame_ptr_reg <= frame_ptr_next;

        s_axis_tready_reg <= s_axis_tready_next;

        gmii_tx_en_reg <= gmii_tx_en_next;
        gmii_tx_er_reg <= gmii_tx_er_next;

        fcs_reg <= fcs_next;
        ifg_reg <= ifg_next;
        // datapath

    end

    mii_odd_reg <= mii_odd_next;
    mii_msn_reg <= mii_msn_next;

    s_tdata_reg <= s_tdata_next;

    gmii_txd_reg <= gmii_txd_next;
end

endmodule

