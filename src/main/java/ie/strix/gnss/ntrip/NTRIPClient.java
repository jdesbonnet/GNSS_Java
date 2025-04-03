package ie.strix.gnss.ntrip;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

//import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NTRIPClient {

	private static final String USER_AGENT = "ie.strix.gnss.ntrip.NTRIPClient/0.1.0";

	private String host;
	private Integer port;
	private String username;
	private String password;
	
	private OutputStream writeTo;

	private InputStream ntripServerIn;
	private OutputStream ntripServerOut;
	
	public NTRIPClient(String host, Integer port, String username, String password) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
	}
	
	public void setOutputStream (OutputStream out) {
		this.writeTo = out;
	}
	public OutputStream getNtripServerOut() {
		return ntripServerOut;
	}

	public void connect() throws UnknownHostException, IOException {
		
		log.info("opening TCP socket to {}:{}", host,port);
		Socket socket = new Socket(host, port);
		log.info("socket opened");
		
		ntripServerIn = socket.getInputStream();
		ntripServerOut = socket.getOutputStream();
		
		log.info("making NTRIP request");
		String req = makeNtripRequest();
		log.info("request={}",req);
		ntripServerOut.write(req.getBytes());
		ntripServerOut.flush();
		
		
		String gga = "$GNGGA,160656.50,5316.95450720,N,00858.95182605,W,1,24,0.6,28.8719,M,57.9852,M,,*56\r\n";
		log.info("writing GGA");
		ntripServerOut.write(gga.getBytes());
		
		byte[] buf = new byte[2048];
		while (true) {
			final int nbytes = ntripServerIn.read(buf);
			log.info("read {} bytes and writing to {}",nbytes,writeTo);
			//log.info(new String(buf));
			writeTo.write(buf,0,nbytes);
		}
	}
	
	
	
	
	private String makeNtripRequest() {
		
		// Combine username and password with a colon
		String authString = username + ":" + password;

		// Encode the combined string with Base64
		String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));

		// Construct the header
		String authHeader = "Basic " + encodedAuth;
		
		String request = "GET /NEAR-RTCM HTTP/1.1\r\n" + "Host: " + host + ":" + port + "\r\n"
				+ "User-Agent: " + USER_AGENT + "\r\n" 
				+ "Authorization: " + authHeader + "\r\n"
				+ "Ntrip-Version: Ntrip/2.0\r\n" 
				+ "Accept: */*\r\n" 
				+ "Connection: close\r\n\r\n";
		
		return request;
	}
	
	
	/**
	 * Open NTRIP connection and read corrections message and forward to to GNSS
	 * device.
	 */
	private class NTRIPReader implements Runnable {

		private Socket ntripSocket;

		private long disconnectTime = 0;

		public NTRIPReader(Socket socket) {
			this.ntripSocket = socket;
		}

		public void run() {
			try {
				readLoop();
			} catch (IOException e) {
				log.error("NTRIP corrections stream read loop: ", e);
			}
		}

		public void setDisconnectTime(long t) {
			this.disconnectTime = t;
		}

		private void readLoop() throws IOException {
			log.info("readLoop()");

			InputStream in = ntripSocket.getInputStream();
			OutputStream out = ntripSocket.getOutputStream();

			// Combine username and password with a colon
			String authString = username + ":" + password;

			// Encode the combined string with Base64
			String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));

			// Construct the header
			String authHeader = "Basic " + encodedAuth;

			String request = "GET /NEAR-RTCM HTTP/1.1\r\n" + "Host: " + host + ":" + port + "\r\n"
					+ "User-Agent: " + USER_AGENT + "\r\n" 
					+ "Authorization: " + authHeader + "\r\n"
					+ "Ntrip-Version: Ntrip/2.0\r\n" 
					+ "Accept: */*\r\n" 
					+ "Connection: close\r\n\r\n";
			log.info("NTRIP request: {}", request);
			out.write(request.getBytes(StandardCharsets.UTF_8));
			out.flush();

			// Before passing binary RTCM messages to GNSS module, first read past the NTRIP
			// response header
			// which is terminated by two end-of-lines CR LF CR LF. We won't bother parsing
			// any of this
			// for the moment.
			int endOfHeader = 0;
			int n = 0;
			while (endOfHeader != 0x0d0a0d0a) {
				int c = in.read() & 0xff;
				endOfHeader <<= 8;
				endOfHeader |= c;
				n++;
				// log.info("reading NTRIP header byte: " + (char)c + " endOfHeader=" +
				// String.format("%08x",endOfHeader));
			}
			log.info("end of NTRIP response header found");

			byte[] buf = new byte[1024];
			int nbytes;
			while ((nbytes = in.read(buf)) >= 0) {
				log.info("read " + nbytes + " from NTRIP stream");

				StringBuilder sb = new StringBuilder();
				int sumbytes = 0;
				for (int i = 0; i < nbytes; i++) {
					sb.append(" " + String.format("%02x", buf[i]));
					sumbytes += buf[i];
				}
				log.info(sb.toString() + " sum=" + sumbytes);

				/*
				 * if (outToGnssDevice != null) { outToGnssDevice.write(buf,0,nbytes);
				 * outToGnssDevice.flush(); log.info("writing " + nbytes +
				 * " from NTRIP stream to GNSS module"); //mCurrentUpdate.correctionBytesWritten
				 * += nbytes; }
				 */

				if (disconnectTime > 0 && System.currentTimeMillis() >= disconnectTime) {
					log.info("NTRIP read stream disconnect time reached");
					ntripSocket.close();
					break;
				}

			}
			log.info("NTRIP read stream terminated");
		}
	}
}
