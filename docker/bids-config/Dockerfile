FROM public.ecr.aws/docker/library/eclipse-temurin:21-jdk-jammy AS build
ARG MAVEN_VERSION=3.9.9
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*
RUN curl -fsSL "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
    | tar xz -C /opt \
    && ln -s "/opt/apache-maven-${MAVEN_VERSION}" /opt/maven
ENV PATH="/opt/maven/bin:${PATH}"
WORKDIR /w
COPY backend backend
WORKDIR /w/backend
RUN mvn -q -pl bids-config -am package -DskipTests

FROM public.ecr.aws/docker/library/eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /w/backend/bids-config/target/bids-config-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8081
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
