ARG MAVEN_BUILD_IMAGE=maven:3.9.9-eclipse-temurin-21
ARG JAVA_RUN_IMAGE=eclipse-temurin:21-jre-jammy

FROM ${MAVEN_BUILD_IMAGE} AS build
WORKDIR /w
COPY backend backend
WORKDIR /w/backend
RUN mvn -q -pl bids-exec -am package -DskipTests

FROM ${JAVA_RUN_IMAGE}
WORKDIR /app
COPY --from=build /w/backend/bids-exec/target/bids-exec-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8082
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
