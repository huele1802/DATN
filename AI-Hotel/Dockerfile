#FROM maven:3-eclipse-temurin-21 AS build
#WORKDIR /app
#
#COPY . .
#RUN mvn clean package -DskipTests
#
## Run stage
#
#FROM eclipse-temurin:21-jdk-alpine
#WORKDIR /app
#
#COPY --from=build /app/target/AI-Hotel-0.0.1-SNAPSHOT.war aihotel.war
#EXPOSE 8080
#
#ENTRYPOINT ["java", "-jar", "aihotel.war"]

# https://hub.docker.com/_/eclipse-temurin
FROM eclipse-temurin:21-jdk-alpine

# Create and change to the app directory.
WORKDIR /app

# Copy local code to the container image.
COPY . ./

# Build the app.
RUN ./mvnw -DoutputFile=target/mvn-dependency-list.log -B -DskipTests clean dependency:list install

# Run the app by dynamically finding the JAR file in the target directory
CMD ["sh", "-c", "java -jar target/*.jar"]