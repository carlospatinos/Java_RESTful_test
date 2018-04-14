FROM openjdk:8-jdk-alpine
EXPOSE 8080
RUN mkdir -p /app/
ADD build/libs/java-restful-test-0.1.0.jar /app/java-restful-test-0.1.0.jar
#ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
#ENTRYPOINT ["java", "-jar", "/app/java-restful-test-0.1.0.jar"]

ARG ENV_USERS
RUN echo "Running environment: $ENV_USERS"

ENTRYPOINT ["java", "-Dspring.data.mongodb.uri=mongodb://mongo_db:27017/users","-Djava.security.egd=file:/dev/./urandom","-jar","/app/java-restful-test-0.1.0.jar"]