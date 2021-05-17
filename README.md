# Queue-Limiter

How to start the Queue-Limiter application
---

1. Run `mvn clean install` to build your application
1. Start application with `java -jar target/queue-limiter-1.0-SNAPSHOT.jar server config.yml`
1. To check that your application is running enter url `http://localhost:8080`

Health Check
---

To see your applications health enter url `http://localhost:8081/healthcheck`


Dockerizing and uploading
---
1. Run `mvn clean install` to build your application
2. Build Dockerfile
`docker build -t queue_limiter . `
3. Tag the iterations
`docker tag 0972ddf8a84d jakobjaks/queue_limiter:firsttry`
4. Push it
`docker push jakobjaks/queue_limiter`


To start rabbitmq
---

rabbitmq-server