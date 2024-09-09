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


#include <verilated.h>
#include "VSimTop.h"
#include <memory>
#include <iostream>
#include <getopt.h>

#include <sstream>



#if VM_TRACE
#include "verilated_fst_c.h"
#endif


char* img;
VSimTop *top;
#if VM_TRACE
VerilatedFstC* tfp;
#endif
vluint64_t main_time = 0;

double sc_time_stamp () {
	return main_time;
}


uint8_t flag_waveEnable = 0;
uint8_t flag_limitEnable = 0;
static void init_and_clock();


int prase_arg(int argc, char **argv) {
	int opt;
	while( -1 != ( opt = getopt( argc, argv, "pjldwf:" ) ) ) {
		switch(opt) {

			case 'l':
			flag_limitEnable = 1;
			break;

			case 'w':
				flag_waveEnable = 1;
				std::cout << "Waveform is Enable" << std::endl;
				break;
			case 'f':
				img = strdup(optarg);
				// std::cout << "load in image is " << img << std::endl;
				break;
			case '?':
				std::cout << "-w to enable waveform" << std::endl;
				std::cout << "-f FILENAME to testfile" << std::endl;
				return -1;
				break;
			default:
				std::cout << opt << std::endl;
				assert(0);
		}
	}
	return 0;
}

static void sim_exit(){
#if VM_TRACE
	if ( flag_waveEnable ) { tfp->close(); }
#endif

	top->final();

	delete top;
}





int main(int argc, char **argv, char **env) {


	if ( -1 == prase_arg(argc, argv) ) {
		std::cout << "Prase Error." << std::endl;
		return -1;
	}

	char * temp[2];
	char cmd[64] = "+";
	strcat(cmd, img);
	strcat(cmd, ".verilog");
	temp[0] = "Verilated";
	temp[1] = cmd;
	char **argv_temp = temp;
	Verilated::commandArgs(2, argv_temp);		



	top = new VSimTop();

#if VM_TRACE
	tfp = new VerilatedFstC;
	if (flag_waveEnable) {
		Verilated::traceEverOn(true);
		top->trace(tfp, 99); // Trace 99 levels of hierarchy

		tfp->open("./build/wave.fst");		
	}

#endif

	
	top->RSTn = 0;
	top->CLK = 0;
	top->tclk = 0;
	top->rclk = 0;



	// printf("start diff\n");
	while(!Verilated::gotFinish()) {

		Verilated::timeInc(1);

		init_and_clock();

		top->eval();

#if VM_TRACE
		if ( flag_waveEnable ) { tfp->dump(Verilated::time()); }
#endif

		if ( flag_limitEnable ) {
			if ( main_time > 300000 ){
				std::cout << "Timeout!!!!!" << std::endl;	
				sim_exit();
				return -1;
			} 			
		}

		main_time ++;
	}
	
	sim_exit();
	return -1;

}


static void init_and_clock(){

	//de-assert reset
	if ( main_time != 100 ){
	} else {
		top->RSTn = 1;
	}

	//main clock
	if ( main_time % 10 == 1 ) {
		top->CLK = 1;
	} else if ( main_time % 10 == 6 ) {
		top->CLK = 0;
	}

	if ( main_time % 100 == 10 ) {
		top->tclk = 1;
		top->rclk = 1;

	} else if ( main_time % 100 == 60 ) {
		top->tclk = 0;
		top->rclk = 0;

	}


}
