FROM openjdk:11
COPY config.yml /var/queueing-limiter/
COPY target/queue-limiter-1.0.jar /var/queueing-limiter/
EXPOSE 8082
WORKDIR /var/queueing-limiter
CMD ["java", "-jar", "-Done-jar.silent=true", "queue-limiter-1.0.jar", "server", "config.yml"]