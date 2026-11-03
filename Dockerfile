FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /build/target/futurevet.jar /app/app.jar
ENV SERVER_ADDRESS=0.0.0.0
ENV SERVER_FORWARD_HEADERS_STRATEGY=framework
ENV SERVER_SERVLET_SESSION_COOKIE_SECURE=true
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65.0"
USER 10001:10001
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
