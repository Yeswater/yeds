FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY auth-service/pom.xml ./auth-service/pom.xml
COPY gateway-dataplane/pom.xml ./gateway-dataplane/pom.xml
COPY auth-service/src ./auth-service/src

RUN mvn -pl auth-service -am -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=builder /workspace/auth-service/target/auth-service-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8083
EXPOSE 5005
ENTRYPOINT ["java","-jar","/app/app.jar"]
