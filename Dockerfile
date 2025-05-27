FROM maven:3-amazoncorretto-21

ADD . /usr/src/blur
WORKDIR /usr/src/blur
EXPOSE 4567
ENTRYPOINT ["mvn", "clean", "verify", "exec:java"]
