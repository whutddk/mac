
.PHONY: compile tb ethernet vcd

compile:
	rm -rf ./generated/
	rm -rf ./rocket-chip/vsim/generated-src/
	mkdir -p ./generated/
	cd ./rocket-chip/ && rm -f rocketchip.jar
	cd ./rocket-chip/vsim && make verilog CONFIG=freechips.rocketchip.system.UserFPGAConfig
	cp ./rocket-chip/vsim/generated-src/freechips.rocketchip.system.UserFPGAConfig.v ./generated/UserFPGAConfig.v
	cp ./rocket-chip/vsim/generated-src/freechips.rocketchip.system.UserFPGAConfig.behav_srams.v ./generated/behav_srams.v
# 	sbt "test:runMain test.testModule"



ef: 
	sbt "test:runMain test.testModule"

VSimTop: 
	rm -rf ./tb/build/
	mkdir -p ./tb/build/
	verilator -Wno-fatal  \
	--timescale "1 ns / 1 ps" \
	-y ${PWD}/tb/ \
	-y ${PWD}/tb/vtb/ \
	--top-module SimTop \
	--trace-fst \
	--cc ./tb/behav_srams.v   \
	--cc ./tb/UserFPGAConfig.v \
	--cc ./tb/vtb/SimTop.v  \
	+define+RANDOMIZE_GARBAGE_ASSIGN \
	+define+RANDOMIZE_INVALID_ASSIGN \
	+define+RANDOMIZE_REG_INIT \
	+define+RANDOMIZE_MEM_INIT \
	+define+RANDOMIZE_DELAY=0 \
	+define+USE_POWER_PINS \
	--exe --build \
	${PWD}/tb/sim_main.cpp \
	-Mdir ./tb/build/ \
	-j 30


test:
	./tb/build/VSimTop -w -l -f ./tb/sw/build/test




sw:
	riscv64-unknown-elf-gcc -Os -ggdb -march=rv32im -mabi=ilp32 -Wall -mcmodel=medany -mexplicit-relocs \
	-I ./tb/sw/ -I ./tb/sw/src -I ./tb/sw/axi_gpio -I ./tb/sw/axi_uart -I ./tb/sw/axi_timer \
	-c ./tb/sw/src/main.c \
	-o ./tb/sw/build/main.o

	riscv64-unknown-elf-gcc -Os -ggdb -march=rv32im -mabi=ilp32 -Wall -mcmodel=medany -mexplicit-relocs \
	-I ./tb/sw/ -I ./tb/sw/src -I ./tb/sw/axi_gpio -I ./tb/sw/axi_uart -I ./tb/sw/axi_timer \
	-c ./tb/sw/axi_uart/uart.c \
	-o ./tb/sw/build/uart.o

	riscv64-unknown-elf-gcc -Os -ggdb -march=rv32im -mabi=ilp32 -Wall -mcmodel=medany -mexplicit-relocs -mcmodel=medany -mexplicit-relocs \
	-I ./tb/sw/ -I ./tb/sw/src -I ./tb/sw/axi_gpio -I ./tb/sw/axi_uart -I ./tb/sw/axi_timer \
	-c ./tb/sw/src/startup.S \
	-o ./tb/sw/build/startup.o

	riscv64-unknown-elf-gcc -Os -ggdb -march=rv32im -mabi=ilp32 -Wall -mcmodel=medany -mexplicit-relocs -nostdlib -nodefaultlibs -nostartfiles \
	-T ./tb/sw/linker.lds \
	./tb/sw/build/startup.o \
	./tb/sw/build/uart.o \
	./tb/sw/build/main.o \
	-o ./tb/sw/build/test.elf

	riscv64-unknown-elf-objcopy -O binary ./tb/sw/build/test.elf  ./tb/sw/build/test.bin

	riscv64-unknown-elf-objdump --disassemble-all --disassemble-zeroes --section=.text --section=.text.startup --section=.text.init --section=.data --section=.bss --section=.rodata ./tb/sw/build/test.elf > ./tb/sw/build/test.dump


	riscv64-unknown-elf-objcopy -O verilog ./tb/sw/build/test.elf  ./tb/sw/build/test.verilog
	sed -i 's/@800/@000/g' ./tb/sw/build/test.verilog

fst:
	gtkwave ./tb/build/wave.fst &



ethernet:
	rm -f ./generated/ethernet/*
	cd ./rocket-chip/ && rm -f rocketchip.jar
	sbt "test:runMain test.testModule"


tb:
	cp ./generated/ethernet/GmiiTx_AxisRx.v ./tb
	iverilog -Wall \
	-o ./build/wave.iverilog  \
	-y ./tb  \
	-I ./tb  \
	-D RANDOMIZE_REG_INIT \
	./tb/axis_gmii_tx_tb.v 
	vvp  -N ./build/wave.iverilog -lxt2

vcd:
	gtkwave ./build/wave.vcd &
