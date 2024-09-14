
#include <stdint.h>
#include "uart.h"
// #include "gpio.h"
// #include "timer.h"
#include "mac.h"




int phy_init()
{
	volatile uint32_t *isMDIOreq     = (uint32_t*)( 0x30000000 + (8 << 3) );
	volatile uint32_t *fiad          = (uint32_t*)( 0x30000000 + (9 << 3) );
	volatile uint32_t *rgad          = (uint32_t*)( 0x30000000 + (10 << 3) );
	volatile uint32_t *wrData        = (uint32_t*)( 0x30000000 + (11 << 3) );
	volatile uint32_t *isWR          = (uint32_t*)( 0x30000000 + (12 << 3) );
	volatile uint32_t *rdData        = (uint32_t*)( 0x30000000 + (13 << 3) );


	*fiad     = 0x01;
	*rgad     = 0x17;
	*wrData   = 0x00;
	*isWR     = 0;
	*isMDIOreq     = 1;

	udelay(100);
	uart_sendByte(0xaa );
	uart_sendByte( (uint8_t) (*rdData) );
	uart_sendByte(0x55 );


	*fiad     = 0x01;
	*rgad     = 0x17;
	*wrData   = 0x03;
	*isWR     = 1;
	*isMDIOreq     = 1;



	udelay(100);

	*fiad     = 0x01;
	*rgad     = 0x17;
	*wrData   = 0x00;
	*isWR     = 0;
	*isMDIOreq     = 1;

	udelay(100);
	uart_sendByte(0xaa );
	uart_sendByte( (uint8_t) (*rdData) );
	uart_sendByte(0x55 );

	return 0;

}

typedef struct
{
	uint32_t val_low;
	uint32_t val_high;
} riscv_machine_timer_t;
#define CLINT_BASE_ADDRESS			0x2000000
#define OFFSET_MTIME				0xbff8
#define OFFSET_MTIMECMP				0x4000		
void udelay(uint64_t us)
{
	static volatile riscv_machine_timer_t *mtime = (riscv_machine_timer_t *)(CLINT_BASE_ADDRESS + OFFSET_MTIME);
	static volatile riscv_machine_timer_t *mtimecmp = (riscv_machine_timer_t *)(CLINT_BASE_ADDRESS + OFFSET_MTIMECMP);

	uint64_t current_time;
	uint64_t end_time;
	int i;


	current_time = mtime->val_low;
	current_time |= ((uint64_t)mtime->val_high << 32);

	//end_time = current_time + us * configCPU_CLOCK_HZ / 1000000;
	end_time = current_time + (us >> 1);

	do {
		for (i = 0; i < 20; i++)
			asm volatile("nop");

		current_time = mtime->val_low;
		current_time |= ((uint64_t)mtime->val_high << 32);
	} while (current_time < end_time);
}

int main()
{
	uart_init();

	print_uart("Hello World, RocketChip is now Waking Up!\r\n");

	// gpio_write( 0xfffffffa );

	uint8_t i = 0;
	uint32_t data = 0x0123456789ABCDEF;
	// volatile uint32_t* txBuf = (uint32_t*)(0x80001000);
	// volatile uint32_t* rxBuf = (uint32_t*)(0x80002000);
	volatile uint32_t *trigger     = (uint32_t*)( 0x30000000 + (7 << 3) );
	volatile uint32_t *srcAddr     = (uint32_t*)( 0x30000000 + (3 << 3) );


	static const uint8_t gdata[] = {0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x80, 0x00, 
				  0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
				  0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f,
				  0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f,
				  0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a, 0x3b, 0x3c, 0x3d, 0x3e, 0x3f};

	// for ( i = 0; i < 255; i++ )
	// {
	// 	*(txBuf+i) = data;
	// 	data = data << 4 | data >> 60;
	// }

	phy_init();
	udelay(50);

	*srcAddr = gdata;
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


