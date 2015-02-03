# This is an example Dockerfile for running a tesla-microservice app in a docker container.
#
# Instructions:
# 1. build uber jar:
#       ./lein.sh clean
#       ./lein.sh uberjar
# 2. build docker image
#       docker build -t tesla-example:latest .
# 3. run docker container
#       docker run -d -p 8080:8080 tesla-example:latest

FROM centos:6
MAINTAINER Felix Bechstein <felix.bechstein@otto.de>
EXPOSE 8080

# prepare image
RUN yum install -y java-1.8.0-openjdk-headless
USER daemon

# set command line
CMD ["java", "-Dlog_level=info", "-jar", "/tesla-microservice-standalone.jar"]

# instead of logging to stdout, you may log to file in /log. create volume or mount host volume to /log
# RUN mkdir /log && chown daemon /log
# CMD ["java", "-Dlog_level=info", "-Dlog_appender=fileAppender", "-Dlog_location=/log", "-jar", "/tesla-microservice-standalone.jar"]

# drop in uber jar
ADD target/tesla-microservice-*-standalone.jar /tesla-microservice-standalone.jar
