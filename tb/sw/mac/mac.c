#include <stdint.h>
#include "mac.h"

volatile uint32_t *mac_moder = (uint32_t*)( MAC_BASE + MODER );


uint32_t read_mac_moder(){
	return (*mac_moder);
}


