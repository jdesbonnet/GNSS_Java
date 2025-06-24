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
				executor.submit(() -> handleClient(socket));
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
	private void handleClient(Socket socket) {

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
				log.error("unexpected request format");
				return;
			}
			String method = parts[0];
			String path = parts[1];
			// consume headers
			String line;
			while ((line = reader.readLine()) != null && !line.isEmpty()) {
			}

			if ("GET".equalsIgnoreCase(method) && ("/".equals(path) || "".equals(path))) {
				// handleSourceTable(socket);
				socket.close();
			} else if ("SOURCE".equalsIgnoreCase(method)) {
				String mountpoint = path.startsWith("/") ? path.substring(1) : path;
				handleSource(mountpoint, socket);
			} else if ("GET".equalsIgnoreCase(method)) {
				String mountpoint = path.startsWith("/") ? path.substring(1) : path;
				handleRover(mountpoint, socket);
			} else {
				socket.close();
			}
		} catch (IOException e) {
			log.error("Error handling client", e);
		}
		//log.info("client disconnect");
	}

	private void handleSourceTable(OutputStream out) throws IOException {
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
	private void handleSource(String mountpoint, Socket socket) {
		log.info("Base station connecting with mountpoint: {}", mountpoint);
		try {
			BaseStation station = new BaseStation(mountpoint, socket);
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
			this.logOut = new BufferedOutputStream(new FileOutputStream("rtcm-" + mountpoint + ".log", true));
		}

		void start() {
			log.info("start()");
			running = true;
			executor.submit(this::readLoop);
		}

		private void readLoop() {
			log.info("readLoop()");
			byte[] buf = new byte[4096];
			int len;
			try {
				while (running && (len = in.read(buf)) != -1) {
					
					
					/*
					log.info("read {} bytes on {}", len, this.mountpoint);
					logOut.write(buf, 0, len);
					logOut.flush();
					log.info("casting to {} rovers", rovers.size());
					for (RoverConnection r : rovers) {
						r.send(buf, len);
					}
					*/

					// We want to know where the base station is located. 
					// Parse RTCM messages looking for type 1005  which holds 
					// antenna location.
					
	                   // scan buffer for RTCM messages
                    int pos = 0;
                    while (pos + 3 < len) {
                        if ((buf[pos] & 0xFF) == 0xD3) {
                            int length = ((buf[pos+1] & 0x03) << 8) | (buf[pos+2] & 0xFF);
                            if (pos + 3 + length + 3 <= len) { // include 3 parity bytes
                                // parse message
                                int payloadOffset = pos + 3;
                                // message type: first 12 bits
                                int msgType = ((buf[payloadOffset] & 0xFF) << 4) |
                                              ((buf[payloadOffset+1] & 0xF0) >> 4);
                                log.info("RTCM message: type={}, length={}", msgType, length);
                                if (msgType == 1005) {
                                    // parse antenna position from 1005
                                    BitReader br = new BitReader(buf, payloadOffset*8 + 12);
                                    int stationID = (int) br.readBits(12);
                                    long x = br.readBits(38);
                                    long y = br.readBits(38);
                                    long z = br.readBits(38);
                                    double xM = x * 0.0001;
                                    double yM = y * 0.0001;
                                    double zM = z * 0.0001;
                                    log.info("Type 1005: stationID={}, ECEF X={}m, Y={}m, Z={}m",
                                             stationID, xM, yM, zM);
                                }
                                pos += 3 + length + 3;
                                continue;
                            } else {
                                break; // wait for more data
                            }
                        }
                        pos++;
                    }
                    // log raw stream
                    logOut.write(buf, 0, len);
                    logOut.flush();
                    for (RoverConnection r : rovers) {
                        r.send(buf, len);
                    }					
					
					
				}
			} catch (IOException e) {
				log.error("Error in base station {} stream: " + e.toString(), mountpoint, e);
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
			} catch (IOException ignored) {
			}
			log.info("Base station disconnected: {}", mountpoint);
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
				} catch (IOException ignored) {
				}
				close();
			});
		}

		void send(byte[] data, int len) {
			if (!open)
				return;
			try {
				out.write(data, 0, len);
			} catch (IOException e) {
				close();
			}
		}

		void close() {
			if (!open)
				return;
			open = false;
			try {
				socket.close();
			} catch (IOException ignored) {
			}
			log.info("Rover disconnected");
		}
	}

    // utility to read bits from byte array
    private static class BitReader {
        private final byte[] data;
        private int bitPos;
        BitReader(byte[] data, int bitPos) {
            this.data = data;
            this.bitPos = bitPos;
        }
        long readBits(int n) {
            long val = 0;
            for (int i = 0; i < n; i++) {
                int byteIndex = bitPos / 8;
                int bitIndex = 7 - (bitPos % 8);
                val = (val << 1) | ((data[byteIndex] >> bitIndex) & 1);
                bitPos++;
            }
            return val;
        }
    }
    
	public static void main(String[] args) throws IOException {
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
