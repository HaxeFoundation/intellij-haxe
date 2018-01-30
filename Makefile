#Default IDEA version to build against, can be overriden
#by env variable
IDEA_VERSION?=2017.3.3

#build the intellij-haxe.jar file which can be
#installed in Intellij
default: protocol
	./build.sh $(IDEA_VERSION)

#Build the JavaProtocol.hx class into a Java class.
#This class is used to communicate with the CPP debbugger
#through socket
protocol:
	./build-haxe-protocol.sh

#Build and run the unit tests
test:
	./travis.sh $(IDEA_VERSION) $(ANT_TARGET)

