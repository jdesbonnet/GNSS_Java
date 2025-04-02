package ie.strix.gnss;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;


//import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NTRIP {



	/**
	 * Network TCP socket to NTRIP service.
	 */
	private Socket ntripSocket;

	private String host;
	private String port;
	private String auth;

	public NTRIP (String host, String port, String auth) {
		this.host = host;
		this.port = port;
		this.auth = auth;
	}

	/**
	 * Open NTRIP connection and read corrections message and forward to to GNSS device.
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
				log.error("NTRIP corrections stream read loop: ",e);
			}
		}

		public void setDisconnectTime (long t) {
			this.disconnectTime = t;
		}

		private void readLoop() throws IOException {
			log.info("readLoop()");

			InputStream in = ntripSocket.getInputStream();
			OutputStream out = ntripSocket.getOutputStream();

			String request = "GET /NEAR-RTCM HTTP/1.1\r\n" +
				"Host: " + host + ":" + port + "\r\n" +
				"User-Agent: ie.strix.gnss.ntrip.NTRIPClient/0.1.0\r\n" +
				"Authorization: " + auth + "\r\n" +
				"Ntrip-Version: Ntrip/2.0\r\n" +
				"Accept: */*\r\n" +
				"Connection: close\r\n\r\n";
			out.write(request.getBytes(StandardCharsets.UTF_8));
			out.flush();

            // Before passing binary RTCM messages to GNSS module, first read past the NTRIP response header
            // which is terminated by two end-of-lines CR LF CR LF. We won't bother parsing any of this
            // for the moment.
            int endOfHeader = 0;
            int n = 0;
            while (endOfHeader != 0x0d0a0d0a) {
                int c = in.read() & 0xff;
                endOfHeader <<= 8;
                endOfHeader |= c;
                n++;
                //log.info("reading NTRIP header byte: " + (char)c + " endOfHeader=" + String.format("%08x",endOfHeader));
            }
            log.info("end of NTRIP response header found");

            byte[] buf = new byte[1024];
            int nbytes;
            while ( (nbytes = in.read(buf)) >= 0) {
                log.info("read " + nbytes + " from NTRIP stream");

                StringBuilder sb = new StringBuilder();
                int sumbytes = 0;
                for (int i = 0; i < nbytes; i++) {
                    sb.append(" " + String.format("%02x",buf[i]));
                    sumbytes += buf[i];
                }
                log.info(sb.toString() + " sum=" + sumbytes);

		/*
                if (outToGnssDevice != null) {
                    outToGnssDevice.write(buf,0,nbytes);
                    outToGnssDevice.flush();
                    log.info("writing " + nbytes + " from NTRIP stream to GNSS module");
                    //mCurrentUpdate.correctionBytesWritten += nbytes;
                }
		*/

                if (disconnectTime > 0 &&  System.currentTimeMillis() >= disconnectTime) {
                    log.info("NTRIP read stream disconnect time reached");
                    ntripSocket.close();
                    break;
                }

            }
            log.info("NTRIP read stream terminated");
        }
    }
}

