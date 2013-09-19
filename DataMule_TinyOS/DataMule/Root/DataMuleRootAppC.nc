#include "DataMule.h"

configuration DataMuleRootAppC {}
implementation {
  components MainC, BcpC, BcpDebugIFImpC, DataMuleRootC as App, LedsC;
  components ActiveMessageC as Radio;
  components SerialActiveMessageC as Uart;
//  components new AMSenderC(AM_DATA_MULE_MSG);
//  components new LogStorageC(VOLUME_LOGTEST, TRUE);
  components CC2420ControlC;
  
  App.Boot -> MainC;

  BcpC.BcpDebugIF -> BcpDebugIFImpC;
  
//  App.RadioSend -> AMSenderC;
  App.RadioSend -> BcpC.Send;
  App.RadioReceive -> BcpC.Receive;
  App.RadioPacket -> BcpC.Packet;
  App.BcpPacket -> BcpC;
  App.Get -> BcpC;

  App.UartControl -> Uart;
  App.UartReceive -> Uart.Receive[AM_DATA_MULE_MSG];
  App.UartSend -> Uart.AMSend[AM_DATA_MULE_MSG];
  App.UartPacket -> Uart.Packet;

  App.StdControl -> BcpC;
  App.RootControl -> BcpC;

  App.RadioControl -> Radio;

  App.CC2420Config -> CC2420ControlC;

//  App.LogWrite -> LogStorageC;

  App.Leds -> LedsC;
//  App.RadioPacket -> AMSenderC.Packet;
}
