FROM eclipse-temurin:21-jre
WORKDIR /app
ARG JAR_FILE
COPY build/libs/${JAR_FILE} app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]