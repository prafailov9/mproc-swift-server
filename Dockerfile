# Use the official OpenJDK image for a lean runtime.
FROM eclipse-temurin:21

# Set environment variables for debugging
ENV JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# Copy the pre-built JAR file from your target directory into the image
COPY /bootstrap/target/*.jar /app/mproc-swift.jar
# Copy persistence.yml into the image
COPY /persistence/src/main/resources/persistence.yml /app/persistence.yml

# Set the container's working directory to /app
WORKDIR /app

# Expose the application port and the debug port
EXPOSE 8080 5005

# run JAR file
# Using '-Dspring.config.additional-location' to include multiple config files in the spring app
# https://stackoverflow.com/questions/25855795/spring-boot-and-multiple-external-configuration-files/25862357#25862357
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Dspring.config.additional-location=classpath:/application.yml,/app/persistence.yml -jar /app/mproc-swift.jar"]
