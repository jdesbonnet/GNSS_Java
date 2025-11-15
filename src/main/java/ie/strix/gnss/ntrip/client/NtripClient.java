package ie.strix.gnss.ntrip.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

import ie.strix.gnss.ntrip.NtripUri;
//import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NtripClient {

	private static final String USER_AGENT = "ie.strix.gnss.ntrip.client.NtripClient/0.1.0";

	private String host;
	private Integer port;
	private String username;
	private String password;
	private String mountPoint = "/NEAR-RTCM";
	
	private OutputStream gnssOut;

	private InputStream ntripServerIn;
	private OutputStream ntripServerOut;
	
	/** Timestamp at which the last GGA sentence was sent to the NTRIP server */
	private long lastGgaSent = 0;
	
	private final SubmissionPublisher<byte[]> publisher = new SubmissionPublisher<>();

	
	/**
	 * @deprecated Missing mountPoint
	 * @param host
	 * @param port
	 * @param username
	 * @param password
	 */
	public NtripClient(String host, Integer port, String username, String password) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
	}
	
	public NtripClient (NtripUri ntripUri) {
		this.host = ntripUri.getHost();
		this.port = ntripUri.getPort();
		this.username = ntripUri.getUsername();
		this.password = ntripUri.getPassword();
		this.mountPoint = ntripUri.getPassword();
	}
	
	public NtripClient (String ntripUriStr) {
		NtripUri ntripUri = NtripUri.parse(ntripUriStr);
		this.host = ntripUri.getHost();
		this.port = ntripUri.getPort();
		this.username = ntripUri.getUsername();
		this.password = ntripUri.getPassword();
		this.mountPoint = ntripUri.getMountpoint();
	}
	
	
	public NtripClient(String host, Integer port, String mountPoint, String username, String password) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.mountPoint = mountPoint;
	}
	
	
	public void setOutputStream (OutputStream out) {
		log.info("setting GNSS config/RTCM stream to {}",out);
		this.gnssOut = out;
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
		
		// Before passing binary RTCM messages to GNSS module, first read past the NTRIP
		// response header
		// which is terminated by two end-of-lines CR LF CR LF. We won't bother parsing
		// any of this
		// for the moment.
		int endOfHeader = 0;
		int n = 0;
		StringBuilder response = new StringBuilder();
		while (endOfHeader != 0x0d0a0d0a) {
			int c = ntripServerIn.read() & 0xff;
			endOfHeader <<= 8;
			endOfHeader |= c;
			n++;
			response.append((char)c);
			// log.info("reading NTRIP header byte: " + (char)c + " endOfHeader=" +
			// String.format("%08x",endOfHeader));
		}
		log.info("end of NTRIP response header found after reading {} bytes",n);
		log.info("response={}",response);

		
		
		//String gga = "$GNGGA,160656.50,5316.95450720,N,00858.95182605,W,1,24,0.6,28.8719,M,57.9852,M,,*56\r\n";
		String gga = "$GPGGA,233625.20,5316.9572745,N,00858.9463966,W,2,12,1.7,33.4127,M,58.1621,M,3.2,0121*67\r\n";
		log.info("writing initial GGA: {}",gga);
		ntripServerOut.write(gga.getBytes());
		ntripServerOut.flush();

		//
		// Expecting a stream of RTCM messages from here on.
		//
		byte[] buf = new byte[2048];
		while (true) {
			final int nbytes = ntripServerIn.read(buf);
			if (nbytes<0) {
				log.info("end of stream detected");
				break;
			}
			
			byte[] packet = new byte[nbytes];
			System.arraycopy(buf, 0, packet, 0, nbytes);
			//int checksum = checksum(packet);
			//log.info("read packet {} bytes checksum={}",nbytes,String.format("%02X", checksum));
			log.info("read packet {} bytes",nbytes);

			log.debug(byteArrayToHex(packet));
			
			int s = publisher.submit(packet);
			if (s > 10) {
				log.warn("message build up s={}",s);
			}
			
		}
	}
	
	public void sendGGA (String gga) throws IOException {
		long now = System.currentTimeMillis();
		
		// Limit rate at which we send GGA to NTRIP server
		if (now - lastGgaSent > 3000) {
			log.info("sending GGA to NTRIP server: {}",gga);
			ntripServerOut.write((gga + "\r\n").getBytes());
			ntripServerOut.flush();
			lastGgaSent = now;
		}
	}
	
	private String makeNtripRequest() {
		
		// Combine username and password with a colon
		String authString = username + ":" + password;

		// Encode the combined string with Base64
		String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));

		// Construct the header
		String authHeader = "Basic " + encodedAuth;
		
		String request = "GET " + mountPoint + " HTTP/1.1\r\n" + "Host: " + host + ":" + port + "\r\n"
				+ "User-Agent: " + USER_AGENT + "\r\n" 
				+ "Authorization: " + authHeader + "\r\n"
				+ "Ntrip-Version: Ntrip/2.0\r\n" 
				+ "Accept: */*\r\n" 
				+ "Connection: close\r\n\r\n";
		
		
		return request;
	}
	
	
	/**
	 * Allows callers to subscribe to RTCM message stream. 
	 */
	public void subscribe(Flow.Subscriber<? super byte[]> subscriber) {
		publisher.subscribe(subscriber);
	}
	
	
	/**
	 * Calculate checksum by XOR all bytes.
	 * NB: this is not RTCM checksum.
	 * 
	 * @param buf
	 * @return Checksum
	 */
	private static int checksum (byte[] buf) {
		int a = 0;
		for (int i = 0; i < buf.length; i++) {
			a ^= buf[i];
		}
		return a;
	}
	private static String byteArrayToHex (byte[] buf, int offset, int len) {
		StringBuilder s = new StringBuilder();
		for (int i = offset; i < (offset+len); i++) {
			s.append(String.format(" %02X", buf[i]));
		}
		return s.toString();
	}
	private static String byteArrayToHex (byte[] buf) {
		return byteArrayToHex(buf,0,buf.length);
	}
}
