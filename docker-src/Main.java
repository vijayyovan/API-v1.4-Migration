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
        server.start();
        System.out.println("EMA API hardening-step1 service listening on :8008");
    }

    private static void handleLiveness(HttpExchange exchange) throws IOException {
        try {
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
        } finally {
            exchange.close();
        }
    }
}
