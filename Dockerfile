# Estágio 1: compila o projeto
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q clean package -DskipTests

# Estágio 2: imagem final, só com o jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/MontechLogs-jar-with-dependencies.jar app.jar
CMD ["java", "-jar", "app.jar"]