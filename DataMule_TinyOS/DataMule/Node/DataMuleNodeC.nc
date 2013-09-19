#include "Timer.h"
#include "DataMule.h"
#include "Bcp.h"
//#include "StorageVolumes.h"

module DataMuleNodeC @safe(){
  uses {
    interface Leds;
    interface Boot;
    interface AMSend as Send;
    interface Timer<TMilli> as MilliTimer;
    interface Packet;
    interface SplitControl as RadioControl;
    interface Random;
//    interface Send;
//    interface Receive;
//    interface Packet;
//    interface BcpPacket;
//    interface Get<uint16_t>;
//    interface StdControl;
//    interface RootControl;
//    interface CC2420Config;
//    interface LogWrite;
  }
}
implementation {

  message_t radio_pack;
  bool radio_locked = FALSE;

  uint16_t counter = 0;

//  data_mule_log logData;
  
  event void Boot.booted() {
//    logData.txCount = 0;
//    logData.hopCount = 0;
//    logData.delay = 0;
//    logData.queueSize = 0;
//    call CC2420Config.setAddressRecognition(FALSE, TRUE);
    call RadioControl.start();
  }

  event void RadioControl.startDone(error_t err) {
    if (err == SUCCESS) {
      call Leds.led1On();
//      call StdControl.start();
      call MilliTimer.startPeriodic(3000);
    }
    else {
      call RadioControl.start();
    }
  }

  event void RadioControl.stopDone(error_t err) {}

//  event void CC2420Config.syncDone(error_t err) {}

  event void MilliTimer.fired() {
    counter++;

    if (!radio_locked) {
      data_mule_msg_t* radio_mes;

      radio_mes = (data_mule_msg_t*)call Packet.getPayload(&radio_pack, sizeof(data_mule_msg_t));
      if (radio_mes == NULL) {
	return;
      }
//      radio_mes->header = 0xAAAA;
      radio_mes->nodeid = TOS_NODE_ID;
      radio_mes->counter = counter;
      radio_mes->data = call Random.rand16();
//      radio_mes->footer = 0xBBBB;
      if (call Send.send(AM_BROADCAST_ADDR, &radio_pack, sizeof(data_mule_msg_t)) == SUCCESS) { // AM_BROADCAST_ADDR
	radio_locked = TRUE;
      }
    }
  }

  event void Send.sendDone(message_t* bufPtr, error_t err) {
    if (&radio_pack == bufPtr) {
      radio_locked = FALSE;
      call Leds.led2Toggle();
    }

//    if(counter%10 == 0) {
      //logData.queueSize = call Get.get();
//      call LogWrite.append(&logData, sizeof(logData));
//    }
  }

/*  event message_t* Receive.receive(message_t* msg, void* payload, uint8_t len) {
    return msg;
  } */

/*  event void LogWrite.eraseDone(error_t err) {}

  event void LogWrite.syncDone(error_t err) {
    call Leds.led0Toggle();
  }

  event void LogWrite.appendDone(void* buf, storage_len_t len, bool recordsLost, error_t err) {
    if (err == SUCCESS) {
      call LogWrite.sync();
    }
  }*/
}
