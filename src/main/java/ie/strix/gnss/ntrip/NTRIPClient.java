package ie.strix.gnss.ntrip;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

//import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NTRIPClient {

	private static final String USER_AGENT = "ie.strix.gnss.ntrip.NTRIPClient/0.1.0";

	private String host;
	private Integer port;
	private String username;
	private String password;
	
	private OutputStream gnssOut;

	private InputStream ntripServerIn;
	private OutputStream ntripServerOut;
	
	private final SubmissionPublisher<byte[]> publisher = new SubmissionPublisher<>();

	
	public NTRIPClient(String host, Integer port, String username, String password) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
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
		while (endOfHeader != 0x0d0a0d0a) {
			int c = ntripServerIn.read() & 0xff;
			endOfHeader <<= 8;
			endOfHeader |= c;
			n++;
			// log.info("reading NTRIP header byte: " + (char)c + " endOfHeader=" +
			// String.format("%08x",endOfHeader));
		}
		log.info("end of NTRIP response header found after reading {} bytes",n);
		
		
		
		String gga = "$GNGGA,160656.50,5316.95450720,N,00858.95182605,W,1,24,0.6,28.8719,M,57.9852,M,,*56\r\n";
		log.info("writing initial GGA");
		ntripServerOut.write(gga.getBytes());
		
		byte[] buf = new byte[2048];
		while (true) {
			final int nbytes = ntripServerIn.read(buf);
			if (nbytes<0) {
				log.info("end of stream detected");
				break;
			}
			log.info("read packet {} bytes",nbytes);
			
			byte[] packet = new byte[nbytes];
			System.arraycopy(buf, 0, packet, 0, nbytes);
			log.info(byteArrayToHex(packet));
			
			int s = publisher.submit(packet);
			if (s > 10) {
				log.warn("message build up s={}",s);
			}
			
		}
	}
	
	public void sendGGA (String gga) throws IOException {
		log.info("received GGA to send to NTRIP server: {}",gga);
		ntripServerOut.write((gga + "\r\n").getBytes());
		ntripServerOut.flush();
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
	 * Allows callers to subscribe to RTCM message stream. 
	 */
	public void subscribe(Flow.Subscriber<? super byte[]> subscriber) {
		publisher.subscribe(subscriber);
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
