COMPONENT=DataMuleNodeAppC
#BUILD_EXTRA_DEPS = DataMuleMsg.py DataMuleMsg.class

CFLAGS += -I$(TOSDIR)/lib/net
CFLAGS += -I ../BCP

#DataMuleMsg.py: DataMule.h
#	mig python -target=$(PLATFORM) $(CFLAGS) -python-classname=DataMuleMsg DataMule.h data_mule_msg -o $@

#DataMuleMsg.class: DataMuleMsg.java
#	javac DataMuleMsg.java

#DataMuleMsg.java: DataMule.h
#	mig java -target=$(PLATFORM) $(CFLAGS) -java-classname=DataMuleMsg DataMule.h data_mule_msg -o $@

include $(MAKERULES)
