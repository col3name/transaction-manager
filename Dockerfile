FROM openjdk:17-jdk-slim-buster
WORKDIR /app

COPY build/libs/* build/libs/
COPY build/libs/anti-faud-system-0.0.1-SNAPSHOT.jar /app/build/app.jar

WORKDIR /app/build
EXPOSE 8080:8080

ENTRYPOINT java -jar /app/build/app.jar