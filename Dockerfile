FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build
COPY docker-src/Main.java Main.java
RUN javac Main.java && jar --create --file app.jar --main-class Main *.class

FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S ema && adduser -S -G ema ema
WORKDIR /app
COPY --from=builder --chown=ema:ema /build/app.jar /app/app.jar
USER ema
EXPOSE 8008
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
