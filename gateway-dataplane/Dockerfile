FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY auth-service/pom.xml ./auth-service/pom.xml
COPY gateway-dataplane/pom.xml ./gateway-dataplane/pom.xml
COPY gateway-dataplane/src ./gateway-dataplane/src

RUN mvn -pl gateway-dataplane -am -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=builder /workspace/gateway-dataplane/target/gateway-dataplane-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080
EXPOSE 5006
ENTRYPOINT ["java","-jar","/app/app.jar"]
