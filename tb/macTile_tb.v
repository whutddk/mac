`timescale 1 ns / 1 ps


module macTile_tb();




    reg clock = 0;
    reg reset = 1;




    wire [7:0]   code;
    reg          trigger;

    wire [7:0]   gmii_data;
    wire         gmii_en;
    wire         gmii_er;

    wire         interrupt;


    reg         axiSlv_0_aw_ready;
    wire        axiSlv_0_aw_valid;
    wire [3:0]  axiSlv_0_aw_bits_id;

    reg         axiSlv_0_w_ready;
    wire        axiSlv_0_w_valid;
    wire [63:0] axiSlv_0_w_bits_data;
    wire [7:0]  axiSlv_0_w_bits_strb;
    wire        axiSlv_0_w_bits_last;

    wire        axiSlv_0_b_ready;
    reg         axiSlv_0_b_valid;
    reg  [3:0]  axiSlv_0_b_bits_id;


    reg         axiSlv_0_ar_ready;
    wire        axiSlv_0_ar_valid;
    wire [3:0]  axiSlv_0_ar_bits_id;

    wire         axiSlv_0_r_ready;
    reg          axiSlv_0_r_valid;
    reg   [3:0]  axiSlv_0_r_bits_id;
    reg   [63:0] axiSlv_0_r_bits_data;



    MacTile s_macTile(
        .clock(clock),
        .reset(reset),

        .axiSlv_0_aw_ready(axiSlv_0_aw_ready),
        .axiSlv_0_aw_valid(axiSlv_0_aw_valid),
        .axiSlv_0_aw_bits_id(axiSlv_0_aw_bits_id),
        .axiSlv_0_aw_bits_addr(),
        .axiSlv_0_aw_bits_len(),
        .axiSlv_0_aw_bits_size(),
        .axiSlv_0_aw_bits_burst(),
        .axiSlv_0_aw_bits_lock(),
        .axiSlv_0_aw_bits_cache(),
        .axiSlv_0_aw_bits_prot(),
        .axiSlv_0_aw_bits_qos(),

        .axiSlv_0_w_ready(axiSlv_0_w_ready),
        .axiSlv_0_w_valid(axiSlv_0_w_valid),
        .axiSlv_0_w_bits_data(axiSlv_0_w_bits_data),
        .axiSlv_0_w_bits_strb(axiSlv_0_w_bits_strb),
        .axiSlv_0_w_bits_last(axiSlv_0_w_bits_last),

        .axiSlv_0_b_ready(axiSlv_0_b_ready),
        .axiSlv_0_b_valid(axiSlv_0_b_valid),
        .axiSlv_0_b_bits_id(axiSlv_0_b_bits_id),
        .axiSlv_0_b_bits_resp(2'b00),

        .axiSlv_0_ar_ready(axiSlv_0_ar_ready),
        .axiSlv_0_ar_valid(axiSlv_0_ar_valid),
        .axiSlv_0_ar_bits_id(axiSlv_0_ar_bits_id),
        .axiSlv_0_ar_bits_addr(),
        .axiSlv_0_ar_bits_len(),
        .axiSlv_0_ar_bits_size(),
        .axiSlv_0_ar_bits_burst(),
        .axiSlv_0_ar_bits_lock(),
        .axiSlv_0_ar_bits_cache(),
        .axiSlv_0_ar_bits_prot(),
        .axiSlv_0_ar_bits_qos(),
        .axiSlv_0_r_ready(axiSlv_0_r_ready),
        .axiSlv_0_r_valid(axiSlv_0_r_valid),
        .axiSlv_0_r_bits_id(axiSlv_0_r_bits_id),
        .axiSlv_0_r_bits_data(axiSlv_0_r_bits_data),
        .axiSlv_0_r_bits_resp(2'b00),
        .axiSlv_0_r_bits_last(1'b1),

        .io_srcAddress(32'h81000000),
        .io_txLen(8'h32),
        .io_destAddress(32'h82000000),

        .io_code(code),
        .io_trigger(trigger),

        .io_gmii_tx_txd(gmii_data),
        .io_gmii_tx_tx_en(gmii_en),
        .io_gmii_tx_tx_er(gmii_er),

        .io_gmii_rx_rxd(gmii_data),
        .io_gmii_rx_rx_dv(gmii_en),
        .io_gmii_rx_rx_er(gmii_er),

        .io_clkEn(1'b1),
        .io_miiSel(1'b0),
        .io_ifg_delay(12),
        .io_interrupt(interrupt)
    );



    initial begin
        forever #20 clock = ~clock;
    end

    initial begin
        # 100 
        reset = 1;
        trigger = 1'b0;

        # 100 
        reset = 0;

        #1003
        trigger = 1;
        # 40 
        trigger = 0;

    end







    always @( posedge clock ) begin
        if( reset ) begin
            axiSlv_0_aw_ready  <= #3 1'b0;
            axiSlv_0_w_ready   <= #3 1'b0;
            axiSlv_0_b_valid   <= #3 1'b0;
            axiSlv_0_b_bits_id <= #3 0;
        end else begin
            if( axiSlv_0_aw_ready & axiSlv_0_aw_valid ) begin
                axiSlv_0_aw_ready  <= #3 1'b0;
                axiSlv_0_b_bits_id <= #3 axiSlv_0_aw_bits_id;
            end else if( ~axiSlv_0_aw_ready & axiSlv_0_aw_valid ) begin
                axiSlv_0_aw_ready <= #3 1'b1;
            end
            if( axiSlv_0_w_ready & axiSlv_0_w_valid ) begin
                axiSlv_0_w_ready <= #3 1'b0;
                axiSlv_0_b_valid <= #3 1'b1;
            end else if( ~axiSlv_0_w_ready & axiSlv_0_w_valid ) begin
                axiSlv_0_w_ready <= #3 1'b1;
            end

            if( axiSlv_0_b_valid & axiSlv_0_b_ready ) begin
                axiSlv_0_b_valid <= #3 1'b0;
            end else if( axiSlv_0_w_ready & axiSlv_0_w_valid ) begin
                axiSlv_0_b_valid <= #3 1'b1;
            end
        end

    end





    always @( posedge clock ) begin
        if( reset ) begin
            axiSlv_0_ar_ready    <= #3 1'b0;
            axiSlv_0_r_valid     <= #3 1'b0;
            axiSlv_0_r_bits_id   <= #3 0;
            axiSlv_0_r_bits_data <= #3 0;
        end else begin
            if( axiSlv_0_ar_valid & axiSlv_0_ar_ready ) begin

                axiSlv_0_ar_ready    <= #3 1'b0;
                axiSlv_0_r_bits_id   <= #3 axiSlv_0_ar_bits_id;
                axiSlv_0_r_bits_data <= #3 0;
            end else if( axiSlv_0_ar_valid & ~axiSlv_0_ar_ready ) begin
                axiSlv_0_ar_ready    <= #3 1'b1;
            end

            if( axiSlv_0_r_valid & axiSlv_0_r_ready ) begin
                axiSlv_0_r_valid <= #3 1'b0;
                axiSlv_0_r_bits_data <= #3 axiSlv_0_r_bits_data + 1;
            end else if( axiSlv_0_ar_valid & axiSlv_0_ar_ready ) begin
                axiSlv_0_r_valid     <= #3 1'b1;
            end
        end

    end

















    initial begin
        # 100000
        $finish;

    end

    initial begin
        $dumpfile("./build/wave.vcd"); //生成的vcd文件名称
        $dumpvars(0, macTile_tb);//tb模块名称
    end


endmodule
