#include "DataMule.h"

configuration DataMuleNodeAppC {}
implementation {
  components MainC, DataMuleNodeC as App, LedsC, RandomC;
//  components BcpC, BcpDebugIFImpC;
  components ActiveMessageC as Radio;
  components new AMSenderC(AM_DATA_MULE_MSG);
  components new TimerMilliC();
//  components new LogStorageC(VOLUME_LOGTEST, TRUE);
//  components CC2420ControlC;
  
  App.Boot -> MainC;

//  BcpC.BcpDebugIF -> BcpDebugIFImpC;
  
  App.Send -> AMSenderC;
  App.Packet -> AMSenderC;
//  App.Send -> BcpC;
//  App.Receive -> BcpC;
//  App.Packet -> BcpC;
//  App.BcpPacket -> BcpC;
//  App.Get -> BcpC;

//  App.StdControl -> BcpC;
//  App.RootControl -> BcpC;

  App.RadioControl -> Radio;

//  App.CC2420Config -> CC2420ControlC;

//  App.LogWrite -> LogStorageC;

  App.Leds -> LedsC;
  App.MilliTimer -> TimerMilliC;
  App.Random -> RandomC;
}
