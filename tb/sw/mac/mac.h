#ifndef _MAC_H_
#define _MAC_H_


#include <stdint.h>


#define MAC_BASE 0x30000000U



#define MODER         0x00
#define INT_SOURCE    0x04
#define INT_MASK      0x08
#define IPGT          0x0c
#define IPGR1         0x10
#define IPGR2         0x14
#define PACKETLEN     0x18
#define COLLCONF      0x1c
#define TX_BD_NUM     0x20
#define CTRLMODER     0x24
#define MIIMODER      0x28
#define MIICOMMAND    0x2c
#define MIIADDRESS    0x30
#define MIITX_DATA    0x34
#define MIIRX_DATA    0x38
#define MIISTATUS     0x3c
#define MAC_ADDR0     0x40
#define MAC_ADDR1     0x44
#define ETH_HASH0_ADR 0x48
#define ETH_HASH1_ADR 0x4c
#define ETH_TXCTRL    0X50












#endif
