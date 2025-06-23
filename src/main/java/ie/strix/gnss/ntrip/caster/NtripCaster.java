package ie.strix.gnss.ntrip.caster;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import com.sun.net.httpserver.*;

public class NtripCaster {
    private static final Logger logger = Logger.getLogger("general");
    private static final int NTRIP_PORT = 2101;
    private static final int API_PORT = 5005;

    private ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, BaseStation> stations = new ConcurrentHashMap<>();
    private HttpServer apiServer;

    public NtripCaster() throws IOException {
        configureLogging();
        serverSocket = new ServerSocket(NTRIP_PORT);
        apiServer = HttpServer.create(new InetSocketAddress(API_PORT), 0);
        apiServer.createContext("/stations", new StationsHandler());
        apiServer.setExecutor(Executors.newSingleThreadExecutor());
    }

    private void configureLogging() throws IOException {
        Logger root = Logger.getLogger("");
        FileHandler fh = new FileHandler("general.log", true);
        fh.setFormatter(new SimpleFormatter());
        root.addHandler(fh);
        logger.info("Logging configured");
    }

    public void start() {
        logger.info("Starting NTRIP caster on port " + NTRIP_PORT);
        executor.submit(this::acceptLoop);
        apiServer.start();
        logger.info("API server started on port " + API_PORT);
    }

    private void acceptLoop() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                executor.submit(() -> handleClient(socket));
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    logger.log(Level.SEVERE, "Error accepting connection", e);
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             OutputStream out = socket.getOutputStream()) {

            String requestLine = reader.readLine();
            if (requestLine == null) {
                socket.close();
                return;
            }
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                socket.close();
                return;
            }
            String method = parts[0];
            String path = parts[1];
            String mountpoint = path.startsWith("/") ? path.substring(1) : path;
            // consume headers
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {}
            if ("SOURCE".equalsIgnoreCase(method)) {
                handleSource(mountpoint, socket);
            } else if ("GET".equalsIgnoreCase(method)) {
                handleRover(mountpoint, socket, out);
            } else {
                socket.close();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error handling client", e);
        }
    }

    private void handleSource(String mountpoint, Socket socket) {
        logger.info("Base station connecting: " + mountpoint);
        try {
            BaseStation station = new BaseStation(mountpoint, socket);
            BaseStation old = stations.put(mountpoint, station);
            if (old != null) {
                old.stop();
            }
            station.start();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to register base station " + mountpoint, e);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void handleRover(String mountpoint, Socket socket, OutputStream out) throws IOException {
        BaseStation station = stations.get(mountpoint);
        if (station != null && station.isRunning()) {
            logger.info("Rover connecting to " + mountpoint);
            out.write("ICY 200 OK\r\n\r\n".getBytes());
            RoverConnection rover = new RoverConnection(socket, out);
            station.addRover(rover);
            rover.start(executor);
        } else {
            out.write("ICY 404 Not Found\r\n\r\n".getBytes());
            socket.close();
        }
    }

    public List<StationStatus> listStations() {
        List<StationStatus> list = new ArrayList<>();
        stations.forEach((name, st) -> list.add(new StationStatus(name, st.isRunning())));
        return list;
    }

    private class StationsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String response = buildStationsJson();
            byte[] bytes = response.getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String buildStationsJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (StationStatus s : listStations()) {
                sb.append("{")
                  .append("\"mountpoint\":\"").append(s.mountpoint).append("\",")
                  .append("\"live\":").append(s.live)
                  .append("},");
            }
            if (sb.charAt(sb.length() - 1) == ',') {
                sb.setLength(sb.length() - 1);
            }
            sb.append("]");
            return sb.toString();
        }
    }

    public static class StationStatus {
        public final String mountpoint;
        public final boolean live;
        public StationStatus(String mountpoint, boolean live) {
            this.mountpoint = mountpoint;
            this.live = live;
        }
    }

    private class BaseStation {
        private final String mountpoint;
        private final Socket socket;
        private final InputStream in;
        private final OutputStream logOut;
        private final List<RoverConnection> rovers = new CopyOnWriteArrayList<>();
        private volatile boolean running = false;

        BaseStation(String mountpoint, Socket socket) throws IOException {
            this.mountpoint = mountpoint;
            this.socket = socket;
            this.in = socket.getInputStream();
            this.logOut = new BufferedOutputStream(
                new FileOutputStream("rtcm-" + mountpoint + ".log", true)
            );
        }

        void start() {
            running = true;
            executor.submit(this::readLoop);
        }

        private void readLoop() {
            byte[] buf = new byte[4096];
            int len;
            try {
                while (running && (len = in.read(buf)) != -1) {
                    logOut.write(buf, 0, len);
                    logOut.flush();
                    for (RoverConnection r : rovers) {
                        r.send(buf, len);
                    }
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error in base station " + mountpoint + " stream", e);
            } finally {
                stop();
            }
        }

        boolean isRunning() {
            return running;
        }

        void addRover(RoverConnection rover) {
            rovers.add(rover);
        }

        void stop() {
            running = false;
            stations.remove(mountpoint);
            rovers.forEach(RoverConnection::close);
            try {
                logOut.close();
                socket.close();
            } catch (IOException ignored) {}
            logger.info("Base station disconnected: " + mountpoint);
        }
    }

    private static class RoverConnection {
        private final Socket socket;
        private final OutputStream out;
        private volatile boolean open = true;

        RoverConnection(Socket socket, OutputStream out) {
            this.socket = socket;
            this.out = out;
        }

        void start(ExecutorService executor) {
            executor.submit(() -> {
                try {
                    socket.getInputStream().read();
                } catch (IOException ignored) {}
                close();
            });
        }

        void send(byte[] data, int len) {
            if (!open) return;
            try {
                out.write(data, 0, len);
            } catch (IOException e) {
                close();
            }
        }

        void close() {
            if (!open) return;
            open = false;
            try {
                socket.close();
            } catch (IOException ignored) {}
            logger.info("Rover disconnected");
        }
    }

    public static void main(String[] args) throws IOException {
        NtripCaster caster = new NtripCaster();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                caster.serverSocket.close();
                caster.apiServer.stop(0);
                caster.executor.shutdownNow();
                logger.info("Shutdown complete");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error during shutdown", e);
            }
        }));
        caster.start();
    }
}

