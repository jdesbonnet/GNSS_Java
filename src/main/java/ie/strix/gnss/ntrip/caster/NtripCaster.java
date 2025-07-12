package ie.strix.gnss.ntrip.caster;

import lombok.extern.slf4j.Slf4j;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import com.sun.net.httpserver.*;

/**
 * Simple NTRIP caster.
 * 
 * 
 * @author Joe Desbonnet
 */

@Slf4j
public class NtripCaster {
	private static final int NTRIP_PORT = 2101;
	private static final int API_PORT = 5005;

	ServerSocket serverSocket;
	final ExecutorService executor = Executors.newCachedThreadPool();
	final Map<String, BaseStation> stations = new ConcurrentHashMap<>();
	private HttpServer apiServer;

	public NtripCaster() throws IOException {
		serverSocket = new ServerSocket(NTRIP_PORT);
		apiServer = HttpServer.create(new InetSocketAddress(API_PORT), 0);
		apiServer.createContext("/stations", new StationsHandler());
		apiServer.setExecutor(Executors.newSingleThreadExecutor());
	}

	public void start() {
		log.info("Starting NTRIP caster on port {}", NTRIP_PORT);
		executor.submit(this::acceptLoop);

		log.info("Starting API server started on port {}", API_PORT);
		apiServer.start();
	}

	private void acceptLoop() {
		while (!serverSocket.isClosed()) {
			try {
				Socket socket = serverSocket.accept();
				log.info("connection received from " + socket);
				executor.submit(() -> handleIncomingConnection(socket));
			} catch (IOException e) {
				if (!serverSocket.isClosed()) {
					log.error("Error accepting connection", e);
				}
			}
		}
	}

	/**
	 * Handle incoming TCP connection on NTRIP port.
	 */
	private void handleIncomingConnection(Socket socket) {

		log.info("handleClient()");

		try {
			InputStream in = socket.getInputStream();
			// OutputStream out = socket.getOutputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));

			String requestLine = reader.readLine();
			log.info("requestLine={}",requestLine);
			if (requestLine == null) {
				socket.close();
				log.error("unexpected null request line");
				return;
			}
			String[] parts = requestLine.split(" ");
			if (parts.length < 2) {
				socket.close();
				log.error("unexpected request format: expected two or more space separated words in request line");
				return;
			}
			String method = parts[0];
			String path = parts[1];
			// consume headers
			String line;
			while ((line = reader.readLine()) != null && !line.isEmpty()) {
			}

			if ("GET".equalsIgnoreCase(method) && ("/".equals(path) || "".equals(path))) {
				handleSourceTable(socket);
				socket.close();
			} else if ("SOURCE".equalsIgnoreCase(method)) {
				String mountpoint = path.startsWith("/") ? path.substring(1) : path;
				handleBasestation(mountpoint, socket);
			} else if ("GET".equalsIgnoreCase(method)) {
				String mountpoint = path.startsWith("/") ? path.substring(1) : path;
				handleRover(mountpoint, socket);
			} else {
				log.error("unexpected method {}", method);
				socket.close();
			}
		} catch (IOException e) {
			log.error("Error handling client", e);
		}
		//log.info("client disconnect");
	}

	private void handleSourceTable(Socket socket) throws IOException {
		OutputStream out = socket.getOutputStream();
		log.info("Source-table request received");
		out.write("SOURCETABLE 200 OK\r\n".getBytes());
		for (String mount : stations.keySet()) {
			BaseStation st = stations.get(mount);
			if (st != null && st.isRunning()) {
				String line = String.format("STR;%s;;;;;;\r\n", mount);
				out.write(line.getBytes());
			}
		}
		out.write("ENDSOURCETABLE\r\n".getBytes());
		out.write("\r\n".getBytes());
		out.flush();
	}

	/**
	 * After parsing header, handle RTCM data feed from base station.
	 */
	private void handleBasestation(String mountpoint, Socket socket) {
		log.info("Base station connecting with mountpoint: {}", mountpoint);
		try {
			BaseStation station = new BaseStation(this, mountpoint, socket);
			BaseStation old = stations.put(mountpoint, station);
			if (old != null) {
				old.stop();
			}
			station.start();
		} catch (IOException e) {
			log.error("Failed to register base station with mountpoint: {}", mountpoint, e);
			try {
				socket.close();
			} catch (IOException ignored) {
			}
		}
	}

	private void handleRover(String mountpoint, Socket socket) throws IOException {
		BaseStation station = stations.get(mountpoint);
		OutputStream out = socket.getOutputStream();
		if (station != null && station.isRunning()) {
			log.info("Rover connecting to {}", mountpoint);
			out.write("ICY 200 OK\r\n\r\n".getBytes());
			RoverConnection rover = new RoverConnection(mountpoint,socket);
			station.addRover(rover);
			//rover.start(executor);
			Thread roverThread = new Thread(rover);
			roverThread.start();
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
				sb.append("{").append("\"mountpoint\":\"").append(s.mountpoint).append("\",").append("\"live\":")
						.append(s.live).append("},");
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



	public static void main(String[] args) throws IOException {


		System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
		System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd'T'HH:mm:ss'Z'");
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
		System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd HH:mm:ss");


		NtripCaster caster = new NtripCaster();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				caster.serverSocket.close();
				caster.apiServer.stop(0);
				caster.executor.shutdownNow();
				log.info("Shutdown complete");
			} catch (IOException e) {
				log.error("Error during shutdown", e);
			}
		}));
		caster.start();
	}
}
