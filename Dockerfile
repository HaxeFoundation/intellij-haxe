# This is the same os as Travis.ci
FROM  ubuntu:12.04

# Install all the build tools
RUN apt-get update && apt-get install -y \
    default-jdk \
    git \
    ant \
    sudo \
    zlib1g-dev \
    build-essential \
    ocaml-native-compilers \
    libgc-dev \
    gcc-multilib \
    g++-multilib \
    libpcre3-dev \
    wget

# Build neko
RUN git clone https://github.com/HaxeFoundation/neko.git /neko
WORKDIR /neko
RUN make && sudo make install

# Build haxe
RUN git clone --recursive https://github.com/HaxeFoundation/haxe.git /haxe
WORKDIR /haxe
RUN make OCAMLOPT=ocamlopt.opt ADD_REVISION=1
RUN make tools && sudo make install

ENV JAVA_HOME /usr/lib/jvm/java-1.6.0-openjdk

# Download the IDEA Intellij SDK we want to build the
# plugin for
RUN mkdir -p ~/Tools

# IDEA 13.1.6
RUN wget http://download.jetbrains.com/idea/ideaIU-13.1.6.tar.gz
RUN cp ideaIU-13.1.6.tar.gz ~/Tools/ideaIU-13.1.6.tar.gz

# IDEA 14.0.1
RUN wget http://download.jetbrains.com/idea/ideaIU-14.0.1.tar.gz
RUN cp ideaIU-14.0.1.tar.gz ~/Tools/ideaIU-14.0.1.tar.gz

# IDEA 14.1
RUN wget http://download.jetbrains.com/idea/ideaIU-14.1.tar.gz
RUN cp ideaIU-13.1.6.tar.gz ~/Tools/ideaIU-14.1.tar.gz

# Allows to mount the project dir inside the container
# to build the code
VOLUME /intellij-haxe
WORKDIR /intellij-haxe

CMD ["make", "test"]
