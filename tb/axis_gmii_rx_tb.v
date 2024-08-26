`timescale 1 ns / 1 ps


module axis_gmii_rx_tb();




    reg clock = 0;
    reg reset = 1;

    wire [7:0] gmii_data;
    wire gmii_en;
    wire gmii_err;


  wire        io_axis_rx_ready;
  reg         io_axis_rx_valid;
  reg   [7:0] io_axis_rx_bits_tdata;
  reg         io_axis_rx_bits_tlast;
  reg         io_axis_rx_bits_tuser;
  
  reg        io_axis_tx_ready = 1'b1;
  wire       io_axis_tx_valid;
  wire [7:0] io_axis_tx_bits_tdata;
  wire       io_axis_tx_bits_tlast;
  wire       io_axis_tx_bits_tuser;




Core s_Core(
  .clock(clock),
  .reset(reset),

  .io_gmii_tx_txd(gmii_data),
  .io_gmii_tx_tx_en(gmii_en),
  .io_gmii_tx_tx_er(gmii_err),
  .io_gmii_rx_rxd(gmii_data),
  .io_gmii_rx_rx_dv(gmii_en),
  .io_gmii_rx_rx_er(gmii_err),

  .io_axis_rx_ready(io_axis_rx_ready),
  .io_axis_rx_valid(io_axis_rx_valid),
  .io_axis_rx_bits_tdata(io_axis_rx_bits_tdata),
  .io_axis_rx_bits_tlast(io_axis_rx_bits_tlast),
  .io_axis_rx_bits_tuser(io_axis_rx_bits_tuser),

  .io_axis_tx_ready(io_axis_tx_ready),
  .io_axis_tx_valid(io_axis_tx_valid),
  .io_axis_tx_bits_tdata(io_axis_tx_bits_tdata),
  .io_axis_tx_bits_tlast(io_axis_tx_bits_tlast),
  .io_axis_tx_bits_tuser(io_axis_tx_bits_tuser),

  .io_clkEn(1'b1),
  .io_miiSel(1'b1),

  .io_ifg_delay(12),
  .io_error_bad_frame(),
  .io_error_bad_fcs()
);


initial begin
    forever #20 clock = ~clock;
end

initial begin
    # 100 reset = 1;
    # 100 reset = 0;
end


reg [7:0] cnt = 0;

    always @(posedge clock) begin
        if( cnt == 8'd0 ) begin
            #2 io_axis_rx_valid <= 1'b1;
            #2 io_axis_rx_bits_tdata <= cnt;
            #2 io_axis_rx_bits_tlast <= 1'b0;
            #2 io_axis_rx_bits_tuser <= 1'b0;
            #2 cnt <= cnt + 8'd1;
        end else begin
            if( io_axis_rx_valid & io_axis_rx_ready ) begin
                if ( io_axis_rx_bits_tlast ) begin
                    #2 io_axis_rx_valid <= 1'b0;
                    #2 io_axis_rx_bits_tlast <= 1'b0;
                end else begin
                    #2 io_axis_rx_bits_tdata <= cnt;
                    #2 io_axis_rx_bits_tuser <= 1'b0;
                    if( cnt == 8'd63 ) begin
                        #2 io_axis_rx_bits_tlast <= 1'b1;
                    end else begin
                        #2 cnt <= cnt + 8'd1;
                    end
                end
            end
        end
    end



    initial begin
        # 100000
        $finish;

    end

    initial begin
        $dumpfile("./build/wave.vcd"); //生成的vcd文件名称
        $dumpvars(0, axis_gmii_rx_tb);//tb模块名称
    end


endmodule
