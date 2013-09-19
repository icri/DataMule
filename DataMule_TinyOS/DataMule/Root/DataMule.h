#ifndef DATA_MULE_H
#define DATA_MULE_H

typedef nx_struct data_mule_msg {
//  nx_uint16_t header;
  nx_uint16_t nodeid;
  nx_uint16_t counter;
  nx_uint16_t data;
//  nx_uint16_t footer;
} data_mule_msg_t;

typedef nx_struct marker_msg {
  nx_uint16_t data;
} marker_msg_t;

/*typedef nx_struct data_mule_log {
  nx_uint16_t txCount;
  nx_uint16_t hopCount;
  nx_uint16_t delay;
  nx_uint16_t queueSize;
} data_mule_log;*/

enum {
  AM_DATA_MULE_MSG = 7,
};

#endif
