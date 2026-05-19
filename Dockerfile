FROM maven:3.9.6-eclipse-temurin-21-jammy
WORKDIR /app
CMD ["mvn", "spring-boot:run", "-Dspring-boot.run.profiles=dev"]
