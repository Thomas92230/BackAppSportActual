FROM eclipse-temurin:21-jre
LABEL maintainer="SportActual Team"

WORKDIR /app

# Copie du JAR généré par Maven
# Assurez-vous d'avoir fait un 'mvn package' avant le 'docker-compose build'
COPY target/*.jar app.jar

# Expose le port Spring Boot
EXPOSE 8080

# Options JVM
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# Profil Spring actif (Docker Compose peut override)
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-prod}

# Lancement de l'application
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
