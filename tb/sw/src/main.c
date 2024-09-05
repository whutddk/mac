
#include <stdint.h>
#include "uart.h"
// #include "gpio.h"
// #include "timer.h"
#include "mac.h"

volatile uint64_t *trigger     = (uint64_t*)( MAC_BASE + 7 << 3 );



int main()
{
	// uart_init();

	print_uart("Hello World, RocketChip is now Waking Up!\r\n");

	// gpio_write( 0xfffffffa );

	uint8_t i = 0;
	uint64_t data = 0x0123456789ABCDEF;
	volatile uint64_t* txBuf = (uint64_t*)(0x80001000);
	volatile uint64_t* rxBuf = (uint64_t*)(0x80002000);





	for ( i = 0; i < 255; i++ )
	{
		*(txBuf+i) = data;
		data = data << 4 | data >> 60;
	}

	*trigger = 1;

	// volatile uint32_t *DBG_END = (uint32_t*)( 0x60000010 );
	// *DBG_END = 1;
	while(1)
	{
		;
	}
	return 0;

}

void handle_trap(void)
{

}


