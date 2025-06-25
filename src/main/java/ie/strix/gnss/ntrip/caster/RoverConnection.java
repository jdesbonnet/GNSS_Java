package ie.strix.gnss.ntrip.caster;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RoverConnection implements Runnable {

	private String mountpoint;
	private Socket roverSocket;
	private InputStream in;
	private OutputStream out;
	private boolean open = false;
	
	public RoverConnection (String mountpoint, Socket roverSocket) {
		this.mountpoint = mountpoint;
		this.roverSocket = roverSocket;

	}
	
	@Override
	public void run() {
		
		try {
			mainLoop();
		} catch (IOException e) {
			log.error("error in mainLoop():",e);
		}
		
	}
	
	private void mainLoop () throws IOException {
		
		this.open = true;
		
		this.in = roverSocket.getInputStream();
		this.out = roverSocket.getOutputStream();
		
		// We may receive GxGGA sententences from rover. Ignore them.
		BufferedReader br = new BufferedReader( new InputStreamReader(in));
		String line;
		while ( (line = br.readLine()) != null) {
			log.info("received from rover: {}", line);
		}
		
	}
	
	public void send (byte[] data,int len) throws IOException {
		//try {
			out.write(data,0,len);
		//} catch (IOException e) {
		//	log.error("send():",e);
		//	open = false;
		//}
	}

	public void close () {
		try {
			this.roverSocket.close();
		} catch (IOException e) {
			log.error("close():",e);
		}
	}
	
	public String toString () {
		return "rover_" + this.mountpoint + "_at_" + this.roverSocket.getInetAddress().getHostName();
	}
}
