FROM eclipse-temurin:21.0.2_13-jdk-jammy

COPY  . /project
WORKDIR /project

RUN chmod +x ./gradlew

RUN mkdir /root/.gradle
