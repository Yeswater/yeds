ARG MAVEN_BUILD_IMAGE=maven:3.9.9-eclipse-temurin-21
ARG JAVA_RUN_IMAGE=eclipse-temurin:21-jre-jammy

FROM ${MAVEN_BUILD_IMAGE} AS build
WORKDIR /w

COPY foundation/pom.xml foundation/pom.xml
COPY foundation/common-web-starter/pom.xml foundation/common-web-starter/pom.xml
COPY foundation/common-web-starter/src foundation/common-web-starter/src
RUN mvn -q -f foundation/pom.xml install -DskipTests

COPY bids/backend bids/backend
WORKDIR /w/bids/backend
RUN mvn -q -pl bids-exec -am package -DskipTests

FROM ${JAVA_RUN_IMAGE}
WORKDIR /app
COPY --from=build /w/bids/backend/bids-exec/target/bids-exec-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8082
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
