`timescale 1 ns / 1 ps


module axis_gmii_tx_tb();




    reg clock = 0;
    reg reset = 1;



    wire [7:0] gmii_txd;
    wire       gmii_tx_en;
    wire       gmii_tx_er;
    wire       axis_ready;

    reg       axis_valid = 0;
    reg [7:0] axis_bits_tdata;
    reg       axis_bits_tlast;
    reg       axis_bits_tuser;




GmiiTx_AxisRx s_GmiiTx_AxisRx(
  .clock(clock),
  .reset(reset),

  .io_gmii_txd(gmii_txd),
  .io_gmii_tx_en(gmii_tx_en),
  .io_gmii_tx_er(gmii_tx_er),

  .io_axis_ready(axis_ready),
  .io_axis_valid(axis_valid),
  .io_axis_bits_tdata(axis_bits_tdata),
  .io_axis_bits_tlast(axis_bits_tlast),
  .io_axis_bits_tuser(axis_bits_tuser),

  .io_clkEn(1'b1),
  .io_miiSel(1'b1),
  .io_ifg_delay(12)
);


initial begin
    forever #20 clock = ~clock;
end

initial begin
    # 100 reset = 1;
    # 100 reset = 0;
end


reg [7:0] cnt = 0;


    // always @(posedge clock) begin
    //     if( cnt == 8'd0 ) begin
    //         #2 axis_valid <= 1'b1;
    //         #2 axis_bits_tdata <= cnt;
    //         #2 axis_bits_tlast <= 1'b0;
    //         #2 axis_bits_tuser <= 1'b0;
    //         #2 cnt <= cnt + 8'd1;
    //     end else begin
    //         if( axis_valid & axis_ready ) begin
    //             axis_valid <= 1'b0;
    //         end else if( ~axis_valid ) begin
    //             #2 axis_valid <= 1'b1;
    //             if ( cnt == 8'd255 ) begin
    //                 #2 axis_bits_tlast <= 1'b1;
    //             end else begin
    //                 #2 axis_bits_tlast <= 1'b0;
    //                 #2 cnt <= cnt + 8'd1;
    //             end
    //         end
    //     end
    // end

    always @(posedge clock) begin
        if( cnt == 8'd0 ) begin
            #2 axis_valid <= 1'b1;
            #2 axis_bits_tdata <= cnt;
            #2 axis_bits_tlast <= 1'b0;
            #2 axis_bits_tuser <= 1'b0;
            #2 cnt <= cnt + 8'd1;
        end else begin
            if( axis_valid & axis_ready ) begin
                if ( axis_bits_tlast ) begin
                    #2 axis_valid <= 1'b0;
                    #2 axis_bits_tlast <= 1'b0;
                end else begin
                    #2 axis_bits_tdata <= cnt;
                    #2 axis_bits_tuser <= 1'b0;
                    if( cnt == 8'd31 ) begin
                        #2 axis_bits_tlast <= 1'b1;
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
        $dumpvars(0, axis_gmii_tx_tb);//tb模块名称
    end


endmodule
