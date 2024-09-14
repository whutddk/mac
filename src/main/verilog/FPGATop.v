

module FPGATop(
  input         clock,
  input         reset,
  (* X_INTERFACE_INFO = "xilinx.com:interface:jtag:1.0 JTAG TCK" *)
  input         debug_systemjtag_jtag_TCK,
  (* X_INTERFACE_INFO = "xilinx.com:interface:jtag:1.0 JTAG TMS" *)
  input         debug_systemjtag_jtag_TMS,
  (* X_INTERFACE_INFO = "xilinx.com:interface:jtag:1.0 JTAG TD_I" *)
  input         debug_systemjtag_jtag_TDI,
  (* X_INTERFACE_INFO = "xilinx.com:interface:jtag:1.0 JTAG TD_O" *)
  output        debug_systemjtag_jtag_TDO_data,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem AWREADY" *)
  input         mem_axi4_0_aw_ready,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem AWVALID" *)
  output        mem_axi4_0_aw_valid,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem AWID" *)
  output [3:0]  mem_axi4_0_aw_bits_id,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem AWADDR" *)
  output [31:0] mem_axi4_0_aw_bits_addr,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem AWLEN" *)
  output [7:0]  mem_axi4_0_aw_bits_len,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem AWSIZE" *)
  output [2:0]  mem_axi4_0_aw_bits_size,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem AWBURST" *)
  output [1:0]  mem_axi4_0_aw_bits_burst,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem AWLOCK" *)
  output        mem_axi4_0_aw_bits_lock,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem AWCACHE" *)
  output [3:0]  mem_axi4_0_aw_bits_cache,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem AWPROT" *)
  output [2:0]  mem_axi4_0_aw_bits_prot,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem AWQOS" *)
  output [3:0]  mem_axi4_0_aw_bits_qos,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem WREADY" *)
  input         mem_axi4_0_w_ready,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem WVALID" *)
  output        mem_axi4_0_w_valid,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem WDATA" *)
  output [63:0] mem_axi4_0_w_bits_data,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem WSTRB" *)
  output [7:0]  mem_axi4_0_w_bits_strb,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem WLAST" *)
  output        mem_axi4_0_w_bits_last,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem BREADY" *)
  output        mem_axi4_0_b_ready,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem BVALID" *)
  input         mem_axi4_0_b_valid,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem BID" *)
  input  [3:0]  mem_axi4_0_b_bits_id,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem BRESP" *)
  input  [1:0]  mem_axi4_0_b_bits_resp,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem ARREADY" *)
  input         mem_axi4_0_ar_ready,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem ARVALID" *)
  output        mem_axi4_0_ar_valid,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem ARID" *)
  output [3:0]  mem_axi4_0_ar_bits_id,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem ARADDR" *)
  output [31:0] mem_axi4_0_ar_bits_addr,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem ARLEN" *)
  output [7:0]  mem_axi4_0_ar_bits_len,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem ARSIZE" *)
  output [2:0]  mem_axi4_0_ar_bits_size,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem ARBURST" *)
  output [1:0]  mem_axi4_0_ar_bits_burst,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem ARLOCK" *)
  output        mem_axi4_0_ar_bits_lock,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem ARCACHE" *)
  output [3:0]  mem_axi4_0_ar_bits_cache,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem ARPROT" *)
  output [2:0]  mem_axi4_0_ar_bits_prot,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem ARQOS" *)
  output [3:0]  mem_axi4_0_ar_bits_qos,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem RREADY" *)
  output        mem_axi4_0_r_ready,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem RVALID" *)
  input         mem_axi4_0_r_valid,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem RID" *)
  input  [3:0]  mem_axi4_0_r_bits_id,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem RDATA" *)
  input  [63:0] mem_axi4_0_r_bits_data,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem RRESP" *)
  input  [1:0]  mem_axi4_0_r_bits_resp,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mem RLAST" *)
  input         mem_axi4_0_r_bits_last,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio AWREADY" *)
  input         mmio_axi4_0_aw_ready,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio AWVALID" *)
  output        mmio_axi4_0_aw_valid,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio AWID" *)
  output [3:0]  mmio_axi4_0_aw_bits_id,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio AWADDR" *)
  output [30:0] mmio_axi4_0_aw_bits_addr,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio AWLEN" *)
  output [7:0]  mmio_axi4_0_aw_bits_len,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio AWSIZE" *)
  output [2:0]  mmio_axi4_0_aw_bits_size,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio AWBURST" *)
  output [1:0]  mmio_axi4_0_aw_bits_burst,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio AWLOCK" *)
  output        mmio_axi4_0_aw_bits_lock,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio AWCACHE" *)
  output [3:0]  mmio_axi4_0_aw_bits_cache,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio AWPROT" *)
  output [2:0]  mmio_axi4_0_aw_bits_prot,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio AWQOS" *)
  output [3:0]  mmio_axi4_0_aw_bits_qos,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio WREADY" *)
  input         mmio_axi4_0_w_ready,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio WVALID" *)
  output        mmio_axi4_0_w_valid,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio WDATA" *)
  output [63:0] mmio_axi4_0_w_bits_data,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio WSTRB" *)
  output [7:0]  mmio_axi4_0_w_bits_strb,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio WLAST" *)
  output        mmio_axi4_0_w_bits_last,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio BREADY" *)
  output        mmio_axi4_0_b_ready,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio BVALID" *)
  input         mmio_axi4_0_b_valid,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio BID" *)
  input  [3:0]  mmio_axi4_0_b_bits_id,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio BRESP" *)
  input  [1:0]  mmio_axi4_0_b_bits_resp,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio ARREADY" *)
  input         mmio_axi4_0_ar_ready,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio ARVALID" *)
  output        mmio_axi4_0_ar_valid,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio ARID" *)
  output [3:0]  mmio_axi4_0_ar_bits_id,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio ARADDR" *)
  output [30:0] mmio_axi4_0_ar_bits_addr,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio ARLEN" *)
  output [7:0]  mmio_axi4_0_ar_bits_len,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio ARSIZE" *)
  output [2:0]  mmio_axi4_0_ar_bits_size,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio ARBURST" *)
  output [1:0]  mmio_axi4_0_ar_bits_burst,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio ARLOCK" *)
  output        mmio_axi4_0_ar_bits_lock,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio ARCACHE" *)
  output [3:0]  mmio_axi4_0_ar_bits_cache,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio ARPROT" *)
  output [2:0]  mmio_axi4_0_ar_bits_prot,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio ARQOS" *)
  output [3:0]  mmio_axi4_0_ar_bits_qos,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio RREADY" *)
  output        mmio_axi4_0_r_ready,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio RVALID" *)
  input         mmio_axi4_0_r_valid,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio RID" *)
  input  [3:0]  mmio_axi4_0_r_bits_id,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio RDATA" *)
  input  [63:0] mmio_axi4_0_r_bits_data,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio RRESP" *)
  input  [1:0]  mmio_axi4_0_r_bits_resp,
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 mmio RLAST" *)
  input         mmio_axi4_0_r_bits_last,

  output        macIO_mdc,
  inout mdio,

  (* X_INTERFACE_INFO = "xilinx.com:interface:mii:1.0 MII TX_CLK" *)
  input         macIO_mtx_clk_pad_i,
  (* X_INTERFACE_INFO = "xilinx.com:interface:mii:1.0 MII TXD" *)
  output [3:0]  macIO_mtxd_pad_o,
  (* X_INTERFACE_INFO = "xilinx.com:interface:mii:1.0 MII TX_EN" *)
  output        macIO_mtxen_pad_o,
  (* X_INTERFACE_INFO = "xilinx.com:interface:mii:1.0 MII RX_CLK" *)
  input         macIO_mrx_clk_pad_i,
  (* X_INTERFACE_INFO = "xilinx.com:interface:mii:1.0 MII RXD" *)
  input  [3:0]  macIO_mrxd_pad_i,
  (* X_INTERFACE_INFO = "xilinx.com:interface:mii:1.0 MII RX_DV" *)
  input         macIO_mrxdv_pad_i,
  (* X_INTERFACE_INFO = "xilinx.com:interface:mii:1.0 MII RX_ER" *)
  input         macIO_mrxerr_pad_i,
  
  output [7:0] tx_mirror,
  output [7:0] rx_mirror
);




wire active;
wire mdi;
wire mdo;
wire mdoEn;

// wire macIO_isLoopBack;
// wire switch_rtx;

// BUFGMUX_CTRL BUFGMUX_CTRL_inst (
//       .O(switch_rtx),   // 1-bit output: Clock output
//       .I0(macIO_mrx_clk_pad_i), // 1-bit input: Clock input (S=0)
//       .I1(macIO_mtx_clk_pad_i), // 1-bit input: Clock input (S=1)
//       .S(macIO_isLoopBack)    // 1-bit input: Clock select
// );



ExampleRocketSystem i_rocketChip(
  .clock(clock),
  .reset(reset),
  .resetctrl_hartIsInReset_0(reset),
  .debug_clock(clock),
  .debug_reset(reset),
  .debug_systemjtag_jtag_TCK(debug_systemjtag_jtag_TCK),
  .debug_systemjtag_jtag_TMS(debug_systemjtag_jtag_TMS),
  .debug_systemjtag_jtag_TDI(debug_systemjtag_jtag_TDI),
  .debug_systemjtag_jtag_TDO_data(debug_systemjtag_jtag_TDO_data),
  .debug_systemjtag_jtag_TDO_driven(),
  .debug_systemjtag_reset(reset),
  .debug_systemjtag_mfr_id(11'b0),
  .debug_systemjtag_part_number(16'b0),
  .debug_systemjtag_version(4'b0),
  .debug_ndreset(),
  .debug_dmactive(active),
  .debug_dmactiveAck(active),
  .mem_axi4_0_aw_ready(mem_axi4_0_aw_ready),
  .mem_axi4_0_aw_valid(mem_axi4_0_aw_valid),
  .mem_axi4_0_aw_bits_id(mem_axi4_0_aw_bits_id),
  .mem_axi4_0_aw_bits_addr(mem_axi4_0_aw_bits_addr),
  .mem_axi4_0_aw_bits_len(mem_axi4_0_aw_bits_len),
  .mem_axi4_0_aw_bits_size(mem_axi4_0_aw_bits_size),
  .mem_axi4_0_aw_bits_burst(mem_axi4_0_aw_bits_burst),
  .mem_axi4_0_aw_bits_lock(mem_axi4_0_aw_bits_lock),
  .mem_axi4_0_aw_bits_cache(mem_axi4_0_aw_bits_cache),
  .mem_axi4_0_aw_bits_prot(mem_axi4_0_aw_bits_prot),
  .mem_axi4_0_aw_bits_qos(mem_axi4_0_aw_bits_qos),
  .mem_axi4_0_w_ready(mem_axi4_0_w_ready),
  .mem_axi4_0_w_valid(mem_axi4_0_w_valid),
  .mem_axi4_0_w_bits_data(mem_axi4_0_w_bits_data),
  .mem_axi4_0_w_bits_strb(mem_axi4_0_w_bits_strb),
  .mem_axi4_0_w_bits_last(mem_axi4_0_w_bits_last),
  .mem_axi4_0_b_ready(mem_axi4_0_b_ready),
  .mem_axi4_0_b_valid(mem_axi4_0_b_valid),
  .mem_axi4_0_b_bits_id(mem_axi4_0_b_bits_id),
  .mem_axi4_0_b_bits_resp(mem_axi4_0_b_bits_resp),
  .mem_axi4_0_ar_ready(mem_axi4_0_ar_ready),
  .mem_axi4_0_ar_valid(mem_axi4_0_ar_valid),
  .mem_axi4_0_ar_bits_id(mem_axi4_0_ar_bits_id),
  .mem_axi4_0_ar_bits_addr(mem_axi4_0_ar_bits_addr),
  .mem_axi4_0_ar_bits_len(mem_axi4_0_ar_bits_len),
  .mem_axi4_0_ar_bits_size(mem_axi4_0_ar_bits_size),
  .mem_axi4_0_ar_bits_burst(mem_axi4_0_ar_bits_burst),
  .mem_axi4_0_ar_bits_lock(mem_axi4_0_ar_bits_lock),
  .mem_axi4_0_ar_bits_cache(mem_axi4_0_ar_bits_cache),
  .mem_axi4_0_ar_bits_prot(mem_axi4_0_ar_bits_prot),
  .mem_axi4_0_ar_bits_qos(mem_axi4_0_ar_bits_qos),
  .mem_axi4_0_r_ready(mem_axi4_0_r_ready),
  .mem_axi4_0_r_valid(mem_axi4_0_r_valid),
  .mem_axi4_0_r_bits_id(mem_axi4_0_r_bits_id),
  .mem_axi4_0_r_bits_data(mem_axi4_0_r_bits_data),
  .mem_axi4_0_r_bits_resp(mem_axi4_0_r_bits_resp),
  .mem_axi4_0_r_bits_last(mem_axi4_0_r_bits_last),

  .mmio_axi4_0_aw_ready(mmio_axi4_0_aw_ready),
  .mmio_axi4_0_aw_valid(mmio_axi4_0_aw_valid),
  .mmio_axi4_0_aw_bits_id(mmio_axi4_0_aw_bits_id),
  .mmio_axi4_0_aw_bits_addr(mmio_axi4_0_aw_bits_addr),
  .mmio_axi4_0_aw_bits_len(mmio_axi4_0_aw_bits_len),
  .mmio_axi4_0_aw_bits_size(mmio_axi4_0_aw_bits_size),
  .mmio_axi4_0_aw_bits_burst(mmio_axi4_0_aw_bits_burst),
  .mmio_axi4_0_aw_bits_lock(mmio_axi4_0_aw_bits_lock),
  .mmio_axi4_0_aw_bits_cache(mmio_axi4_0_aw_bits_cache),
  .mmio_axi4_0_aw_bits_prot(mmio_axi4_0_aw_bits_prot),
  .mmio_axi4_0_aw_bits_qos(mmio_axi4_0_aw_bits_qos),
  .mmio_axi4_0_w_ready(mmio_axi4_0_w_ready),
  .mmio_axi4_0_w_valid(mmio_axi4_0_w_valid),
  .mmio_axi4_0_w_bits_data(mmio_axi4_0_w_bits_data),
  .mmio_axi4_0_w_bits_strb(mmio_axi4_0_w_bits_strb),
  .mmio_axi4_0_w_bits_last(mmio_axi4_0_w_bits_last),
  .mmio_axi4_0_b_ready(mmio_axi4_0_b_ready),
  .mmio_axi4_0_b_valid(mmio_axi4_0_b_valid),
  .mmio_axi4_0_b_bits_id(mmio_axi4_0_b_bits_id),
  .mmio_axi4_0_b_bits_resp(mmio_axi4_0_b_bits_resp),
  .mmio_axi4_0_ar_ready(mmio_axi4_0_ar_ready),
  .mmio_axi4_0_ar_valid(mmio_axi4_0_ar_valid),
  .mmio_axi4_0_ar_bits_id(mmio_axi4_0_ar_bits_id),
  .mmio_axi4_0_ar_bits_addr(mmio_axi4_0_ar_bits_addr),
  .mmio_axi4_0_ar_bits_len(mmio_axi4_0_ar_bits_len),
  .mmio_axi4_0_ar_bits_size(mmio_axi4_0_ar_bits_size),
  .mmio_axi4_0_ar_bits_burst(mmio_axi4_0_ar_bits_burst),
  .mmio_axi4_0_ar_bits_lock(mmio_axi4_0_ar_bits_lock),
  .mmio_axi4_0_ar_bits_cache(mmio_axi4_0_ar_bits_cache),
  .mmio_axi4_0_ar_bits_prot(mmio_axi4_0_ar_bits_prot),
  .mmio_axi4_0_ar_bits_qos(mmio_axi4_0_ar_bits_qos),
  .mmio_axi4_0_r_ready(mmio_axi4_0_r_ready),
  .mmio_axi4_0_r_valid(mmio_axi4_0_r_valid),
  .mmio_axi4_0_r_bits_id(mmio_axi4_0_r_bits_id),
  .mmio_axi4_0_r_bits_data(mmio_axi4_0_r_bits_data),
  .mmio_axi4_0_r_bits_resp(mmio_axi4_0_r_bits_resp),
  .mmio_axi4_0_r_bits_last(mmio_axi4_0_r_bits_last),
  .interrupts(2'b0),

  .io_mdio_mdi(mdi),
  .io_mdio_mdc(macIO_mdc),
  .io_mdio_mdo(mdo),
  .io_mdio_mdoEn(mdoEn),
  .io_gmii_tx_txd(macIO_mtxd_pad_o),
  .io_gmii_tx_tx_en(macIO_mtxen_pad_o),
  .io_gmii_tx_tx_er(),
  .io_gmii_rx_rxd({4'b0,macIO_mrxd_pad_i}),
//    .io_gmii_rx_rx_dv(macIO_mrxdv_pad_i),
  .io_gmii_rx_rx_dv(1'b0),
  .io_gmii_rx_rx_er(macIO_mrxerr_pad_i),
  .io_gmii_tclk(macIO_mtx_clk_pad_i),
  .io_gmii_rclk(macIO_mrx_clk_pad_i)


);





 IOBUF #(
      .DRIVE(12), // Specify the output drive strength
      .IBUF_LOW_PWR("TRUE"),  // Low Power - "TRUE", High Performance = "FALSE" 
      .IOSTANDARD("DEFAULT"), // Specify the I/O standard
      .SLEW("SLOW") // Specify the output slew rate
   ) IOBUF_inst (
      .O(mdi),     // Buffer output
      .IO(mdio),   // Buffer inout port (connect directly to top-level port)
      .I(mdo),     // Buffer input
      .T(~mdoEn)      // 3-state enable input, high=input, low=output
   );



assign tx_mirror[3:0] = macIO_mtxd_pad_o;
assign tx_mirror[4]   = macIO_mtx_clk_pad_i;
assign tx_mirror[5]   = macIO_mtxen_pad_o;
assign tx_mirror[6]   = macIO_mdc;
assign tx_mirror[7]   = mdo;

assign rx_mirror[3:0] = macIO_mrxd_pad_i;
assign rx_mirror[4] = macIO_mrx_clk_pad_i;
assign rx_mirror[5] = macIO_mrxdv_pad_i;
assign rx_mirror[6] = macIO_mrxerr_pad_i;
assign rx_mirror[7] = mdi;

endmodule
