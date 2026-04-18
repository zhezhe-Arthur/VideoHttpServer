import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Executors;

public class VideoFolderServer {
    private static Path VIDEO_DIR;
    private static int DEFAULT_PORT;

    public static void main(String[] args) throws IOException {
        loadConfig();

        System.out.println("video dir = " + VIDEO_DIR.toAbsolutePath());
        System.out.println("exists = " + Files.exists(VIDEO_DIR));
        System.out.println("isDirectory = " + Files.isDirectory(VIDEO_DIR));
        System.out.println("preferred port = " + DEFAULT_PORT);

        if (!Files.exists(VIDEO_DIR) || !Files.isDirectory(VIDEO_DIR)) {
            throw new IllegalArgumentException("Video directory not found: " + VIDEO_DIR.toAbsolutePath());
        }

        List<Path> videoFiles = listMp4Files(VIDEO_DIR);
        System.out.println("mp4 file count = " + videoFiles.size());
        for (Path file : videoFiles) {
            System.out.println("video file = " + file.getFileName());
        }

        HttpServer server = createServerWithFallbackPort(DEFAULT_PORT);

        server.createContext("/", exchange -> {
            Path htmlPath = Path.of("static/index.html");

            if (!Files.exists(htmlPath)) {
                byte[] bytes = "index.html not found".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                return;
            }

            byte[] bytes = Files.readAllBytes(htmlPath);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        server.createContext("/app.js", exchange -> {
            Path jsPath = Path.of("static/app.js");

            if (!Files.exists(jsPath)) {
                byte[] bytes = "app.js not found".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                return;
            }

            byte[] bytes = Files.readAllBytes(jsPath);
            exchange.getResponseHeaders().set("Content-Type", "application/javascript; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        server.createContext("/api/videos", exchange -> {
            List<Path> files = listMp4Files(VIDEO_DIR);

            String json = files.stream()
                    .map(path -> path.getFileName().toString())
                    .map(VideoFolderServer::toJsonString)
                    .collect(Collectors.joining(",", "[", "]"));

            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        server.createContext("/player.html", exchange -> {
            Path htmlPath = Path.of("static/player.html");

            if (!Files.exists(htmlPath)) {
                byte[] bytes = "player.html not found".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                return;
            }

            byte[] bytes = Files.readAllBytes(htmlPath);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        server.createContext("/player.js", exchange -> {
            Path jsPath = Path.of("static/player.js");

            if (!Files.exists(jsPath)) {
                byte[] bytes = "player.js not found".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                return;
            }

            byte[] bytes = Files.readAllBytes(jsPath);
            exchange.getResponseHeaders().set("Content-Type", "application/javascript; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        server.createContext("/video", exchange -> {
            String fileName = getQueryParam(exchange.getRequestURI().getRawQuery(), "name");

            if (fileName == null || fileName.isBlank()) {
                byte[] bytes = "Missing video name".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(400, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                return;
            }

            Path videoPath = VIDEO_DIR.resolve(fileName).normalize();

            if (!videoPath.startsWith(VIDEO_DIR)) {
                byte[] bytes = "Invalid file path".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(403, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                return;
            }

            if (!Files.exists(videoPath) || !Files.isRegularFile(videoPath)) {
                byte[] bytes = "Video file not found".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                return;
            }

            long fileLength = Files.size(videoPath);
            String contentType = getContentType(videoPath);

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Accept-Ranges", "bytes");

            String rangeHeader = exchange.getRequestHeaders().getFirst("Range");

            if (rangeHeader == null || rangeHeader.isBlank()) {
                exchange.sendResponseHeaders(200, fileLength);
                try (OutputStream os = exchange.getResponseBody()) {
                    Files.copy(videoPath, os);
                }
                return;
            }

            if (!rangeHeader.startsWith("bytes=")) {
                exchange.sendResponseHeaders(416, -1);
                return;
            }

            String rangeValue = rangeHeader.substring("bytes=".length());
            String[] parts = rangeValue.split("-", 2);

            long start;
            long end;

            try {
                start = parts[0].isBlank() ? 0 : Long.parseLong(parts[0]);
                end = (parts.length < 2 || parts[1].isBlank()) ? fileLength - 1 : Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                exchange.sendResponseHeaders(416, -1);
                return;
            }

            if (start < 0 || end >= fileLength || start > end) {
                exchange.sendResponseHeaders(416, -1);
                return;
            }

            long contentLength = end - start + 1;

            exchange.getResponseHeaders().set("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
            exchange.sendResponseHeaders(206, contentLength);

            try (var input = Files.newInputStream(videoPath);
                 OutputStream os = exchange.getResponseBody()) {

                input.skip(start);

                byte[] buffer = new byte[8192];
                long remaining = contentLength;

                while (remaining > 0) {
                    int bytesToRead = (int) Math.min(buffer.length, remaining);
                    int bytesRead = input.read(buffer, 0, bytesToRead);

                    if (bytesRead == -1) {
                        break;
                    }

                    os.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }
            }
        });

        server.setExecutor(Executors.newFixedThreadPool(8));

        server.start();

        String localIp = InetAddress.getLocalHost().getHostAddress();
        int actualPort = server.getAddress().getPort();
        System.out.println("Server started");
        System.out.println("Computer access: http://localhost:" + actualPort + "/");
        System.out.println("Phone/iPad access: http://" + localIp + ":" + actualPort + "/");
        System.out.println("Preferred port 7777 is unavailable, switched to random port: " + actualPort);
    }

    private static String toJsonString(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"") + "\"";
    }

    private static HttpServer createServerWithFallbackPort(int preferredPort) throws IOException {
        try {
            System.out.println("Trying preferred port: " + preferredPort);
            return HttpServer.create(new InetSocketAddress("0.0.0.0", preferredPort), 0);
        } catch (IOException e) {
            System.out.println("Preferred port " + preferredPort + " is unavailable, using a random free port.");
            return HttpServer.create(new InetSocketAddress("0.0.0.0", 0), 0);
        }
    }

    private static List<Path> listMp4Files(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> isVideoFile(path))
                    .collect(Collectors.toList());
        }
    }

    private static String getQueryParam(String rawQuery, String key) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }

        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }

        return null;
    }
    private static boolean isVideoFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".mp4")
                || fileName.endsWith(".webm")
                || fileName.endsWith(".mkv")
                || fileName.endsWith(".mov")
                || fileName.endsWith(".avi");
    }
    private static String getContentType(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".mp4")) {
            return "video/mp4";
        }
        if (fileName.endsWith(".webm")) {
            return "video/webm";
        }
        if (fileName.endsWith(".mkv")) {
            return "video/x-matroska";
        }
        if (fileName.endsWith(".mov")) {
            return "video/quicktime";
        }
        if (fileName.endsWith(".avi")) {
            return "video/x-msvideo";
        }

        return "application/octet-stream";
    }
    private static void loadConfig() throws IOException {
        Path configPath = Path.of("config.properties");

        if (!Files.exists(configPath)) {
            createDefaultConfig(configPath);
            System.out.println("config.properties created at: " + configPath.toAbsolutePath());
            System.out.println("Please edit config.properties and restart the program.");
            System.exit(0);
        }

        Properties properties = new Properties();

        try (InputStream input = Files.newInputStream(configPath)) {
            properties.load(input);
        }

        String videoDir = properties.getProperty("video.dir");
        String portText = properties.getProperty("port");

        if (videoDir == null || videoDir.isBlank()) {
            throw new IllegalArgumentException("Missing config: video.dir");
        }

        if (portText == null || portText.isBlank()) {
            throw new IllegalArgumentException("Missing config: port");
        }

        VIDEO_DIR = Path.of(videoDir);
        DEFAULT_PORT = Integer.parseInt(portText);
    }

    private static void createDefaultConfig(Path configPath) throws IOException {
        String defaultConfig = """
            video.dir=D:/MovieServer
            port=7777
            """;

        Files.writeString(configPath, defaultConfig, StandardCharsets.UTF_8);
    }


}