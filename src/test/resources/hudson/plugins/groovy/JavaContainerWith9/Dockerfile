FROM jenkins/java:6894df180381
ENV VERSION 9.0.4
RUN wget -O /tmp/jdk.tgz https://download.java.net/java/GA/jdk9/9.0.4/binaries/openjdk-${VERSION}_linux-x64_bin.tar.gz && \
    cd /opt && \
    tar xvfz /tmp/jdk.tgz && \
    rm /tmp/jdk.tgz && \
    mv jdk-$VERSION jdk9
RUN /opt/jdk9/bin/java -version
