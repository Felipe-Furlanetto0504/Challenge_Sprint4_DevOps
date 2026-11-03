FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY src ./src
RUN mvn -B -ntp clean package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /build/target/servidor-eureka.jar /app/app.jar
ENV SERVER_ADDRESS=0.0.0.0
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65.0"
USER 10001:10001
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
