#include "Timer.h"
#include "DataMule.h"
#include "Bcp.h"
//#include "StorageVolumes.h"

module DataMuleRootC @safe(){
  uses {
    interface Leds;
    interface Boot;
//    interface AMSend as RadioSend;
//    interface Packet as RadioPacket;
    interface SplitControl as RadioControl;
    interface Send as RadioSend;
    interface Receive as RadioReceive;
    interface Packet as RadioPacket;
    interface BcpPacket;
    interface Get<uint16_t>;
    interface StdControl;
    interface RootControl;
    interface CC2420Config;
//    interface LogWrite;
    interface SplitControl as UartControl;
    interface Receive as UartReceive;
    interface AMSend as UartSend;
    interface Packet as UartPacket;
  }
}
implementation {

  message_t uart_pack;
  bool uart_locked = FALSE;

//  uint16_t counter = 0;

//  data_mule_log logData;
   
  event void Boot.booted() {
//    logData.txCount = 0;
//    logData.hopCount = 0;
//    logData.delay = 0;
//    logData.queueSize = 0;
    call CC2420Config.setAddressRecognition(FALSE, TRUE);
    call RadioControl.start();
    call UartControl.start();
  }

  event void RadioControl.startDone(error_t err) {
    if (err == SUCCESS) {
      call StdControl.start();
      call RootControl.setRoot();
      call Leds.led0On();
    }
    else {
      call RadioControl.start();
    }
  }

  event void UartControl.startDone(error_t err) {
    marker_msg_t* uart_mes;

    if (err == SUCCESS) {
      call Leds.led1On();

      uart_mes = (marker_msg_t*)call UartPacket.getPayload(&uart_pack, sizeof(marker_msg_t));
      if (!uart_mes) {
        uart_mes->data = 0xAAAA;
        if (call UartSend.send(AM_BROADCAST_ADDR, &uart_pack, sizeof(marker_msg_t)) == SUCCESS) {
          uart_locked = TRUE;
        }
      }
    }
    else {
      call UartControl.start();
    }
  }

  event void RadioControl.stopDone(error_t err) {}

  event void UartControl.stopDone(error_t err) {}

  event void CC2420Config.syncDone(error_t err) {}

  event void RadioSend.sendDone(message_t* bufPtr, error_t error) {}

  event message_t* RadioReceive.receive(message_t* msg, void* payload, uint8_t len) {
    if (len == sizeof(data_mule_msg_t)) {
      call Leds.led2Toggle();

//      counter++;

//      logData.txCount = call BcpPacket.getTxCount(msg);
//      logData.hopCount = call BcpPacket.getHopCount(msg);
//      logData.delay = call BcpPacket.getDelay(msg);
//      logData.queueSize = call Get.get();

//      if(counter%20 == 0) {
        //logData.queueSize = call Get.get();
//        call LogWrite.append(&logData, sizeof(logData));
//      }

      if (!uart_locked) {
        memcpy(&uart_pack, msg, sizeof(message_t));

        if (call UartSend.send(AM_BROADCAST_ADDR, &uart_pack, sizeof(data_mule_msg_t)) == SUCCESS) {
	  uart_locked = TRUE;
        }
      }
    }

    return msg;
  }

  event void UartSend.sendDone(message_t* bufPtr, error_t error) {
    if (&uart_pack == bufPtr) {
      uart_locked = FALSE;
      call Leds.led2Toggle();
    }
  }

  event message_t* UartReceive.receive(message_t* bufPtr, void* payload, uint8_t len) {
    return bufPtr;
  }

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
