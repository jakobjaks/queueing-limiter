FROM maven:3.6.0-jdk-11-slim AS build
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
RUN mvn -f /usr/src/app/pom.xml clean install

FROM openjdk:11
COPY config.yml /var/queueing-limiter/
COPY --from=build /usr/src/app/target/queue-limiter-1.0.jar /var/queueing-limiter/
EXPOSE 8082
WORKDIR /var/queueing-limiter
CMD ["java", "-jar", "-Done-jar.silent=true -Dcom.sun.management.jmxremote.port=9090", "queue-limiter-1.0.jar", "server", "config.yml"]