FROM centos
LABEL maintainer alechenninger@gmail.com

ARG version

RUN yum install java-1.8.0-openjdk -y; yum clean all

ADD bin/build/distributions/monarch-bin-${version}.tar /opt/

ENV version $version
ENV JAVA_HOME /etc/alternatives/jre_1.8.0/

ENTRYPOINT /opt/monarch-bin-${version}/bin/monarch-bin

