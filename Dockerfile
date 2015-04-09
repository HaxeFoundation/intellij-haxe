# This is the same os as Travis.ci
FROM  ubuntu:12.04

# Install all the build tools
RUN apt-get update
RUN apt-get install -y \
    default-jdk \
    ant \
    build-essential \
    wget \
    haxe

ENV JAVA_HOME=/usr/lib/jvm/java-1.6.0-openjdk

# Download the IDEA Intellij SDK we want to build the
# plugin for
RUN mkdir -p ~/Tools

# IDEA 13.1.6
RUN wget http://download.jetbrains.com/idea/ideaIU-13.1.6.tar.gz 
RUN cp ideaIU-13.1.6.tar.gz ~/Tools/ideaIU-13.1.6.tar.gz

# IDEA 14.1
RUN wget http://download.jetbrains.com/idea/ideaIU-14.1.tar.gz 
RUN cp ideaIU-13.1.6.tar.gz ~/Tools/ideaIU-14.1.tar.gz

# Allows to mount the project dir inside the container
# to build the code
VOLUME /intellij-haxe
WORKDIR /intellij-haxe

CMD ["make", "test"]
