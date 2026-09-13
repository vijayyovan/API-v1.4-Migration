FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build
RUN cat <<'EOF' > Main.java
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class Main {
    private static final String LIVENESS_PATH = "/net-ops/ema/health/v1.4/liveness";

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8008), 0);
        server.createContext(LIVENESS_PATH, Main::handleLiveness);
        server.createContext("/", Main::handleNotFound);
        server.start();
        System.out.println("EMA API hardening-step1 service listening on :8008");
    }

    private static void handleLiveness(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        byte[] body = "Alive".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private static void handleNotFound(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        exchange.sendResponseHeaders(404, -1);
    }
}
EOF
RUN javac Main.java && jar --create --file app.jar --main-class Main Main.class

FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S ema && adduser -S -G ema ema
WORKDIR /app
COPY --from=builder /build/app.jar /app/app.jar
USER ema
EXPOSE 8008
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
