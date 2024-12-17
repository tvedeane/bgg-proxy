FROM openjdk:21-jdk
COPY target/bgg-proxy-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

