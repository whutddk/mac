



/*
  Copyright (c) 2020 - 2023 Wuhan University of Technology <295054118@whut.edu.cn>

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

	   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/


`timescale 1 ns / 1 ps




module SimTop (

	output success,
	output fail,

	input CLK,
	input RSTn
	

);


  wire        mem_axi4_0_aw_ready;
  wire        mem_axi4_0_aw_valid;
  wire [3:0]  mem_axi4_0_aw_bits_id;
  wire [31:0] mem_axi4_0_aw_bits_addr;
  wire [7:0]  mem_axi4_0_aw_bits_len;
  wire [2:0]  mem_axi4_0_aw_bits_size;
  wire [1:0]  mem_axi4_0_aw_bits_burst;
  wire        mem_axi4_0_aw_bits_lock;
  wire [3:0]  mem_axi4_0_aw_bits_cache;
  wire [2:0]  mem_axi4_0_aw_bits_prot;
  wire [3:0]  mem_axi4_0_aw_bits_qos;
  wire        mem_axi4_0_w_ready;
  wire        mem_axi4_0_w_valid;
  wire [31:0] mem_axi4_0_w_bits_data;
  wire [3:0]  mem_axi4_0_w_bits_strb;
  wire        mem_axi4_0_w_bits_last;
  wire        mem_axi4_0_b_ready;
  wire        mem_axi4_0_b_valid;
  wire [3:0]  mem_axi4_0_b_bits_id;
  wire [1:0]  mem_axi4_0_b_bits_resp;
  wire        mem_axi4_0_ar_ready;
  wire        mem_axi4_0_ar_valid;
  wire [3:0]  mem_axi4_0_ar_bits_id;
  wire [31:0] mem_axi4_0_ar_bits_addr;
  wire [7:0]  mem_axi4_0_ar_bits_len;
  wire [2:0]  mem_axi4_0_ar_bits_size;
  wire [1:0]  mem_axi4_0_ar_bits_burst;
  wire        mem_axi4_0_ar_bits_lock;
  wire [3:0]  mem_axi4_0_ar_bits_cache;
  wire [2:0]  mem_axi4_0_ar_bits_prot;
  wire [3:0]  mem_axi4_0_ar_bits_qos;
  wire        mem_axi4_0_r_ready;
  wire        mem_axi4_0_r_valid;
  wire [3:0]  mem_axi4_0_r_bits_id;
  wire [31:0] mem_axi4_0_r_bits_data;
  wire [1:0]  mem_axi4_0_r_bits_resp;
  wire        mem_axi4_0_r_bits_last;

  wire        mmio_axi4_0_aw_ready;
  wire        mmio_axi4_0_aw_valid;
  wire [3:0]  mmio_axi4_0_aw_bits_id;
  wire [30:0] mmio_axi4_0_aw_bits_addr;
  wire [7:0]  mmio_axi4_0_aw_bits_len;
  wire [2:0]  mmio_axi4_0_aw_bits_size;
  wire [1:0]  mmio_axi4_0_aw_bits_burst;
  wire        mmio_axi4_0_aw_bits_lock;
  wire [3:0]  mmio_axi4_0_aw_bits_cache;
  wire [2:0]  mmio_axi4_0_aw_bits_prot;
  wire [3:0]  mmio_axi4_0_aw_bits_qos;
  wire        mmio_axi4_0_w_ready;
  wire        mmio_axi4_0_w_valid;
  wire [31:0] mmio_axi4_0_w_bits_data;
  wire [3:0]  mmio_axi4_0_w_bits_strb;
  wire        mmio_axi4_0_w_bits_last;
  wire        mmio_axi4_0_b_ready;
  wire        mmio_axi4_0_b_valid;
  wire [3:0]  mmio_axi4_0_b_bits_id;
  wire [1:0]  mmio_axi4_0_b_bits_resp;
  wire        mmio_axi4_0_ar_ready;
  wire        mmio_axi4_0_ar_valid;
  wire [3:0]  mmio_axi4_0_ar_bits_id;
  wire [30:0] mmio_axi4_0_ar_bits_addr;
  wire [7:0]  mmio_axi4_0_ar_bits_len;
  wire [2:0]  mmio_axi4_0_ar_bits_size;
  wire [1:0]  mmio_axi4_0_ar_bits_burst;
  wire        mmio_axi4_0_ar_bits_lock;
  wire [3:0]  mmio_axi4_0_ar_bits_cache;
  wire [2:0]  mmio_axi4_0_ar_bits_prot;
  wire [3:0]  mmio_axi4_0_ar_bits_qos;
  wire        mmio_axi4_0_r_ready;
  wire        mmio_axi4_0_r_valid;
  wire [3:0]  mmio_axi4_0_r_bits_id;
  wire [31:0] mmio_axi4_0_r_bits_data;
  wire [1:0]  mmio_axi4_0_r_bits_resp;
  wire        mmio_axi4_0_r_bits_last;



  wire        Mdi_I;
  wire        Mdc_O;
  wire        Mdo_O;
  wire        Mdo_OE;
  wire        mtx_clk;
  wire [3:0]  MTxD;
  wire        MTxEn;
  wire        MTxErr;
  wire        mrx_clk;
  wire [3:0]  MRxD;
  wire        MRxDV;
  wire        MRxErr;
  wire        MColl;
  wire        MCrs;
  wire        Mdio_IO;

  assign Mdio_IO = Mdo_OE ? Mdo_O : 1'bz;
  assign Mdi_I   = Mdio_IO;

wire dmactive;


ExampleRocketSystem i_rocket(
  .clock(CLK),
  .reset(~RSTn),

  .resetctrl_hartIsInReset_0(~RSTn),
  .debug_clock(CLK),
  .debug_reset(~RSTn),
  .debug_systemjtag_jtag_TCK(1'b0),
  .debug_systemjtag_jtag_TMS(1'b0),
  .debug_systemjtag_jtag_TDI(1'b0),
  .debug_systemjtag_jtag_TDO_data(),
  .debug_systemjtag_jtag_TDO_driven(),
  .debug_systemjtag_reset(~RSTn),
  .debug_systemjtag_mfr_id(11'b0),
  .debug_systemjtag_part_number(16'b0),
  .debug_systemjtag_version(4'b0),
  .debug_ndreset(),
  .debug_dmactive(dmactive),
  .debug_dmactiveAck(dmactive),

  .mem_axi4_0_aw_ready     (mem_axi4_0_aw_ready),
  .mem_axi4_0_aw_valid     (mem_axi4_0_aw_valid),
  .mem_axi4_0_aw_bits_id   (mem_axi4_0_aw_bits_id),
  .mem_axi4_0_aw_bits_addr (mem_axi4_0_aw_bits_addr),
  .mem_axi4_0_aw_bits_len  (mem_axi4_0_aw_bits_len),
  .mem_axi4_0_aw_bits_size (mem_axi4_0_aw_bits_size),
  .mem_axi4_0_aw_bits_burst(mem_axi4_0_aw_bits_burst),
  .mem_axi4_0_aw_bits_lock (mem_axi4_0_aw_bits_lock),
  .mem_axi4_0_aw_bits_cache(mem_axi4_0_aw_bits_cache),
  .mem_axi4_0_aw_bits_prot (mem_axi4_0_aw_bits_prot),
  .mem_axi4_0_aw_bits_qos  (mem_axi4_0_aw_bits_qos),
  .mem_axi4_0_w_ready      (mem_axi4_0_w_ready),
  .mem_axi4_0_w_valid      (mem_axi4_0_w_valid),
  .mem_axi4_0_w_bits_data  (mem_axi4_0_w_bits_data),
  .mem_axi4_0_w_bits_strb  (mem_axi4_0_w_bits_strb),
  .mem_axi4_0_w_bits_last  (mem_axi4_0_w_bits_last),
  .mem_axi4_0_b_ready      (mem_axi4_0_b_ready),
  .mem_axi4_0_b_valid      (mem_axi4_0_b_valid),
  .mem_axi4_0_b_bits_id    (mem_axi4_0_b_bits_id),
  .mem_axi4_0_b_bits_resp  (mem_axi4_0_b_bits_resp),
  .mem_axi4_0_ar_ready     (mem_axi4_0_ar_ready),
  .mem_axi4_0_ar_valid     (mem_axi4_0_ar_valid),
  .mem_axi4_0_ar_bits_id   (mem_axi4_0_ar_bits_id),
  .mem_axi4_0_ar_bits_addr (mem_axi4_0_ar_bits_addr),
  .mem_axi4_0_ar_bits_len  (mem_axi4_0_ar_bits_len),
  .mem_axi4_0_ar_bits_size (mem_axi4_0_ar_bits_size),
  .mem_axi4_0_ar_bits_burst(mem_axi4_0_ar_bits_burst),
  .mem_axi4_0_ar_bits_lock (mem_axi4_0_ar_bits_lock),
  .mem_axi4_0_ar_bits_cache(mem_axi4_0_ar_bits_cache),
  .mem_axi4_0_ar_bits_prot (mem_axi4_0_ar_bits_prot),
  .mem_axi4_0_ar_bits_qos  (mem_axi4_0_ar_bits_qos),
  .mem_axi4_0_r_ready      (mem_axi4_0_r_ready),
  .mem_axi4_0_r_valid      (mem_axi4_0_r_valid),
  .mem_axi4_0_r_bits_id    (mem_axi4_0_r_bits_id),
  .mem_axi4_0_r_bits_data  (mem_axi4_0_r_bits_data),
  .mem_axi4_0_r_bits_resp  (mem_axi4_0_r_bits_resp),
  .mem_axi4_0_r_bits_last  (mem_axi4_0_r_bits_last),

  .mmio_axi4_0_aw_ready     (mmio_axi4_0_aw_ready),
  .mmio_axi4_0_aw_valid     (mmio_axi4_0_aw_valid),
  .mmio_axi4_0_aw_bits_id   (mmio_axi4_0_aw_bits_id),
  .mmio_axi4_0_aw_bits_addr (mmio_axi4_0_aw_bits_addr),
  .mmio_axi4_0_aw_bits_len  (mmio_axi4_0_aw_bits_len),
  .mmio_axi4_0_aw_bits_size (mmio_axi4_0_aw_bits_size),
  .mmio_axi4_0_aw_bits_burst(mmio_axi4_0_aw_bits_burst),
  .mmio_axi4_0_aw_bits_lock (mmio_axi4_0_aw_bits_lock),
  .mmio_axi4_0_aw_bits_cache(mmio_axi4_0_aw_bits_cache),
  .mmio_axi4_0_aw_bits_prot (mmio_axi4_0_aw_bits_prot),
  .mmio_axi4_0_aw_bits_qos  (mmio_axi4_0_aw_bits_qos),
  .mmio_axi4_0_w_ready      (mmio_axi4_0_w_ready),
  .mmio_axi4_0_w_valid      (mmio_axi4_0_w_valid),
  .mmio_axi4_0_w_bits_data  (mmio_axi4_0_w_bits_data),
  .mmio_axi4_0_w_bits_strb  (mmio_axi4_0_w_bits_strb),
  .mmio_axi4_0_w_bits_last  (mmio_axi4_0_w_bits_last),
  .mmio_axi4_0_b_ready      (mmio_axi4_0_b_ready),
  .mmio_axi4_0_b_valid      (mmio_axi4_0_b_valid),
  .mmio_axi4_0_b_bits_id    (mmio_axi4_0_b_bits_id),
  .mmio_axi4_0_b_bits_resp  (mmio_axi4_0_b_bits_resp),
  .mmio_axi4_0_ar_ready     (mmio_axi4_0_ar_ready),
  .mmio_axi4_0_ar_valid     (mmio_axi4_0_ar_valid),
  .mmio_axi4_0_ar_bits_id   (mmio_axi4_0_ar_bits_id),
  .mmio_axi4_0_ar_bits_addr (mmio_axi4_0_ar_bits_addr),
  .mmio_axi4_0_ar_bits_len  (mmio_axi4_0_ar_bits_len),
  .mmio_axi4_0_ar_bits_size (mmio_axi4_0_ar_bits_size),
  .mmio_axi4_0_ar_bits_burst(mmio_axi4_0_ar_bits_burst),
  .mmio_axi4_0_ar_bits_lock (mmio_axi4_0_ar_bits_lock),
  .mmio_axi4_0_ar_bits_cache(mmio_axi4_0_ar_bits_cache),
  .mmio_axi4_0_ar_bits_prot (mmio_axi4_0_ar_bits_prot),
  .mmio_axi4_0_ar_bits_qos  (mmio_axi4_0_ar_bits_qos),
  .mmio_axi4_0_r_ready      (mmio_axi4_0_r_ready),
  .mmio_axi4_0_r_valid      (mmio_axi4_0_r_valid),
  .mmio_axi4_0_r_bits_id    (mmio_axi4_0_r_bits_id),
  .mmio_axi4_0_r_bits_data  (mmio_axi4_0_r_bits_data),
  .mmio_axi4_0_r_bits_resp  (mmio_axi4_0_r_bits_resp),
  .mmio_axi4_0_r_bits_last  (mmio_axi4_0_r_bits_last),


  .interrupts(2'b0),

  .macIO_mdi(Mdi_I),
  .macIO_mdc(Mdc_O),
  .macIO_mdo(Mdo_O),
  .macIO_mdoEn(Mdo_OE),
  .macIO_mtx_clk_pad_i(mtx_clk),
  .macIO_mtxd_pad_o(MTxD),
  .macIO_mtxen_pad_o(MTxEn),
  .macIO_mtxerr_pad_o(MTxErr),
  .macIO_mrx_clk_pad_i(mrx_clk),
  .macIO_mrxd_pad_i(MRxD),
  .macIO_mrxdv_pad_i(MRxDV),
  .macIO_mrxerr_pad_i(MRxErr),
  .macIO_mcoll_pad_i(MColl),
  .macIO_mcrs_pad_i(MCrs)
);



integer phy_log_file_desc;

// eth_phy s_phy// This PHY model simulate simplified Intel LXT971A PHY
// (

//   .m_rst_n_i(RSTn),
//   .mtx_clk_o(mtx_clk),
//   .mtxd_i(MTxD),
//   .mtxen_i(MTxEn),
//   .mtxerr_i(MTxErr),

//   .mrx_clk_o(mrx_clk),
//   .mrxd_o(MRxD),
//   .mrxdv_o(MRxDV),
//   .mrxerr_o(MRxErr),
//   .mcoll_o(MColl),
//   .mcrs_o(MCrs),

//   .mdc_i(Mdc_O),
//   .md_io(Mdio_IO),

//   // SYSTEM
//   .phy_log(phy_log_file_desc)
// );

// initial
// begin
//   phy_log_file_desc = $fopen("./log/eth_tb_phy.log");

//   $display(phy_log_file_desc, "================ PHY Module  Testbench access log ================");
//   $display(phy_log_file_desc, " ");
// end



axi_full_slv_sram # ( .DW(32), .AW(18) ) s_axi_full_slv_sram 
(

	.MEM_AWID   (mem_axi4_0_aw_bits_id),
	.MEM_BID    (mem_axi4_0_b_bits_id),
	.MEM_ARID   (mem_axi4_0_ar_bits_id),
	.MEM_RID    (mem_axi4_0_r_bits_id),

	.MEM_AWADDR (mem_axi4_0_aw_bits_addr),
	.MEM_AWLEN  (mem_axi4_0_aw_bits_len),
	.MEM_AWSIZE (mem_axi4_0_aw_bits_size),
	.MEM_AWBURST(mem_axi4_0_aw_bits_burst),
	.MEM_AWVALID(mem_axi4_0_aw_valid),
	.MEM_AWREADY(mem_axi4_0_aw_ready),
	.MEM_WDATA  (mem_axi4_0_w_bits_data),
	.MEM_WSTRB  (mem_axi4_0_w_bits_strb),
	.MEM_WLAST  (mem_axi4_0_w_bits_last),
	.MEM_WVALID (mem_axi4_0_w_valid),
	.MEM_WREADY (mem_axi4_0_w_ready),
	.MEM_BRESP  (mem_axi4_0_b_bits_resp),
	.MEM_BVALID (mem_axi4_0_b_valid),
	.MEM_BREADY (mem_axi4_0_b_ready),
	.MEM_ARADDR (mem_axi4_0_ar_bits_addr),
	.MEM_ARLEN  (mem_axi4_0_ar_bits_len),
	.MEM_ARSIZE (mem_axi4_0_ar_bits_size),
	.MEM_ARBURST(mem_axi4_0_ar_bits_burst),
	.MEM_ARVALID(mem_axi4_0_ar_valid),
	.MEM_ARREADY(mem_axi4_0_ar_ready),
	.MEM_RDATA  (mem_axi4_0_r_bits_data),
	.MEM_RRESP  (mem_axi4_0_r_bits_resp),
	.MEM_RLAST  (mem_axi4_0_r_bits_last),
	.MEM_RVALID (mem_axi4_0_r_valid),
	.MEM_RREADY (mem_axi4_0_r_ready),

	.CLK        (CLK),
	.RSTn       (RSTn)
);

wire debugger_success;

debuger i_debuger(
  .success(debugger_success),
	.DEBUGER_AWID   (mmio_axi4_0_aw_bits_id),
	.DEBUGER_BID    (mmio_axi4_0_b_bits_id),
	.DEBUGER_ARID   (mmio_axi4_0_ar_bits_id),
	.DEBUGER_RID    (mmio_axi4_0_r_bits_id),

	.DEBUGER_AWADDR (mmio_axi4_0_aw_bits_addr),
	.DEBUGER_AWVALID(mmio_axi4_0_aw_valid),
	.DEBUGER_AWREADY(mmio_axi4_0_aw_ready),

	.DEBUGER_WDATA (mmio_axi4_0_w_bits_data),   
	.DEBUGER_WSTRB (mmio_axi4_0_w_bits_strb),
	.DEBUGER_WVALID(mmio_axi4_0_w_valid),
	.DEBUGER_WREADY(mmio_axi4_0_w_ready),

	.DEBUGER_BRESP (mmio_axi4_0_b_bits_rsp),
	.DEBUGER_BVALID(mmio_axi4_0_b_valid),
	.DEBUGER_BREADY(mmio_axi4_0_b_ready),

	.DEBUGER_ARADDR (mmio_axi4_0_ar_bits_addr),
	.DEBUGER_ARVALID(mmio_axi4_0_ar_valid),
	.DEBUGER_ARREADY(mmio_axi4_0_ar_ready),

	.DEBUGER_RDATA (mmio_axi4_0_r_bits_data),
	.DEBUGER_RRESP (mmio_axi4_0_r_bits_rsp),
	.DEBUGER_RVALID(mmio_axi4_0_r_valid),
	.DEBUGER_RREADY(mmio_axi4_0_r_ready),

	.CLK(CLK),
	.RSTn(RSTn)
	
);












assign success = debugger_success;




string testName;

`define MEM s_axi_full_slv_sram.i_sram.ram
reg [7:0] mem [0:200000];

localparam DP = 2**18;
integer i, by;
initial begin

#20

	if ( $value$plusargs("%s",testName) ) begin
		$display("%s",testName);
	  $readmemh(testName, mem);		

	end
	else begin 
    $display("%s",testName);
		$error("Failed to read Files!");
	end

	
	for ( i = 0; i < DP; i = i + 1 ) begin
		for ( by = 0; by < 4; by = by + 1 ) begin
			if ( | mem[i*4+by] ) begin
				`MEM[i][8*by +: 8] = mem[i*4+by];
			end
			else begin
				`MEM[i][8*by +: 8] = 8'h0;
			end
		end
	end

  $display("%x",`MEM[0]);
  $display("%x",`MEM[1]);
  $display("%x",`MEM[2]);
  $display("%x",`MEM[3]);

end 





endmodule






