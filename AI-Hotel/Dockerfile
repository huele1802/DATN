FROM maven:3-openjdk17 AS build
WORKDIR /app

COPY . .
RUN mvn clean package -DskipTests

# Run stage

FROM openjdk:17-jdk-slim
WORKDIR /app

COPY --from=build /app/target/AI-Hotel-0.0.1-SNAPSHOT.war aihotel.war
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "aihotel.war"]