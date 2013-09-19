/**
 * <p>On the serial link, BaseStation sends and receives simple active
 * messages (not particular radio packets): on the radio link, it
 * sends radio active messages, whose format depends on the network
 * stack being used. BaseStation will copy its compiled-in group ID to
 * messages moving from the serial link to the radio, and will filter
 * out incoming radio messages that do not contain that group ID.</p>
 *
 * <p>BaseStation includes queues in both directions, with a guarantee
 * that once a message enters a queue, it will eventually leave on the
 * other interface. The queues allow the BaseStation to handle load
 * spikes.</p>
 *
 * <p>BaseStation acknowledges a message arriving over the serial link
 * only if that message was successfully enqueued for delivery to the
 * radio link.</p>
 *
 * <p>The LEDS are programmed to toggle as follows:</p>
 * <ul>
 * <li><b>RED Toggle:</b>: Message bridged from serial to radio</li>
 * <li><b>GREEN Toggle:</b> Message bridged from radio to serial</li>
 * <li><b>YELLOW/BLUE Toggle:</b> Dropped message due to queue overflow in either direction</li>
 * </ul>
 */

configuration DataMuleRootC {
}
implementation {
  components MainC, DataMuleRootP, LedsC;
  components ActiveMessageC as Radio, SerialActiveMessageC as Serial;
  
  MainC.Boot <- DataMuleRootP;

  DataMuleRootP.RadioControl -> Radio;
  DataMuleRootP.SerialControl -> Serial;
  
  DataMuleRootP.UartSend -> Serial;
  DataMuleRootP.UartReceive -> Serial.Receive;
  DataMuleRootP.UartPacket -> Serial;
  DataMuleRootP.UartAMPacket -> Serial;
  
  DataMuleRootP.RadioSend -> Radio;
  DataMuleRootP.RadioReceive -> Radio.Receive;
  DataMuleRootP.RadioSnoop -> Radio.Snoop;
  DataMuleRootP.RadioPacket -> Radio;
  DataMuleRootP.RadioAMPacket -> Radio;
  
  DataMuleRootP.Leds -> LedsC;
}
