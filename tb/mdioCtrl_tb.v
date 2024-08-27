`timescale 1 ns / 1 ps


module mdioCtrl_tb();




    reg clock = 0;
    reg reset = 1;

    reg         mdi;
    wire        mdc;
    wire        mdo;
    wire        mdoEn;

    wire        req_ready;
    reg         req_valid;
    reg  [4:0]  req_bits_fiad;
    reg  [4:0]  req_bits_rgad;
    reg  [15:0] req_bits_data;
    reg         req_bits_isWR;

    reg         resp_ready;
    wire        resp_valid;
    wire [15:0] resp_bits_data;




MDIOCtrl s_mdioCtrl(
    .clock(clock),
    .reset(reset),

    .io_mdi(mdi),
    .io_mdc(mdc),
    .io_mdo(mdo),
    .io_mdoEn(mdoEn),

    .io_req_ready(req_ready),
    .io_req_valid(req_valid),
    .io_req_bits_fiad(req_bits_fiad),
    .io_req_bits_rgad(req_bits_rgad),
    .io_req_bits_data(req_bits_data),
    .io_req_bits_isWR(req_bits_isWR),

    .io_resp_ready(resp_ready),
    .io_resp_valid(resp_valid),
    .io_resp_bits_data(resp_bits_data),

    .io_div(8'd10),
    .io_noPre(1'b1)
);


    initial begin
        forever #20 clock = ~clock;
    end

    initial begin
        # 100 reset = 1;
        # 100 reset = 0;
    end


    // initial begin
    //     #5 
    //     resp_ready = 1'b1;

    //     mdi = 1'b0;

    //     req_valid = 1'b0;
    //     req_bits_fiad = 5'b0;
    //     req_bits_rgad = 5'b0;
    //     req_bits_data = 16'b0;
    //     req_bits_isWR = 1'b0;

    //     #300
    //     req_valid = 1'b1;
    //     req_bits_fiad = 5'd11;
    //     req_bits_rgad = 5'd22;
    //     req_bits_data = 16'd55;
    //     req_bits_isWR = 1'b1; 

    //     #40
    //     req_valid = 1'b0;
    //     req_bits_fiad = 5'b0;
    //     req_bits_rgad = 5'b0;
    //     req_bits_data = 16'b0;
    //     req_bits_isWR = 1'b0;       
    // end


    initial begin
        #5 
        resp_ready = 1'b1;

        mdi = 1'b0;

        req_valid = 1'b0;
        req_bits_fiad = 5'b0;
        req_bits_rgad = 5'b0;
        req_bits_data = 16'b0;
        req_bits_isWR = 1'b0;

        #300
        req_valid = 1'b1;

        #40
        req_valid = 1'b0;

    end

    always @(posedge mdc) begin
        #5 mdi <= ~mdi;
    end

    initial begin
        # 100000
        $finish;

    end

    initial begin
        $dumpfile("./build/wave.vcd"); //生成的vcd文件名称
        $dumpvars(0, mdioCtrl_tb);//tb模块名称
    end


endmodule
