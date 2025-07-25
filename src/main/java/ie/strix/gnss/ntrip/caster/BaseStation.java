package ie.strix.gnss.ntrip.caster;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


import ie.strix.gnss.ECEF;

import lombok.extern.slf4j.Slf4j;

/**
 * Information related to base station, and connected rovers.
 *
 */

@Slf4j
public class BaseStation {
	
	/** Radius of the earh (approx) in m */
	private final double R = 6378000;
	
	/**
	 * 
	 */
	private final transient NtripCaster ntripCaster;
	private final String mountpoint;
	
	
	private transient final Socket socket;
	private transient final InputStream in;
	private transient final OutputStream logOut;
	private transient final List<RoverConnection> rovers = new CopyOnWriteArrayList<>();
	private final List<BaseStationStatusDTO> statusUpdates = new ArrayList<>();
	
	//private volatile boolean running = false;
	private boolean running = false;

	private long bytesReceived = 0;
	private long bytesBroadcast = 0;

	public BaseStation(NtripCaster ntripCaster, String mountpoint, Socket socket) throws IOException {
		this.ntripCaster = ntripCaster;
		this.mountpoint = mountpoint;
		this.socket = socket;
		this.in = socket.getInputStream();
		this.logOut = new BufferedOutputStream(new FileOutputStream("rtcm-" + mountpoint + ".log", true));
	}

	void start() {
		log.info("start()");
		running = true;
		this.ntripCaster.executor.submit(this::readLoop);
	}

	/**
	 * Continue to read from base station forwarding RTCM messages on to connected rovers.
	 */
	private void readLoop() {
		log.info("readLoop()");
		byte[] buf = new byte[4096];
		int len;
		try {
			while (running && (len = in.read(buf)) != -1) {
				
				//log.info("read {} bytes from base station {}", len, mountpoint);
				bytesReceived += len;
				// We want to know where the base station is located. 
				// Parse RTCM messages looking for type 1005  which holds 
				// antenna location.

				// scan buffer for RTCM messages
				int pos = 0;
				while (pos + 3 < len) {
					if ((buf[pos] & 0xFF) == 0xD3) {
						int length = ((buf[pos + 1] & 0x03) << 8) | (buf[pos + 2] & 0xFF);
						if (pos + 3 + length + 3 <= len) { // include 3 parity bytes
							// parse message
							int payloadOffset = pos + 3;
							// message type: first 12 bits
							int msgType = ((buf[payloadOffset] & 0xFF) << 4)
									| ((buf[payloadOffset + 1] & 0xF0) >> 4);
							log.info("RTCM message on mountpoint {}: type={}, length={}", mountpoint, msgType, length);
							if (msgType == 1005 || msgType == 1006) {
								// parse antenna position from 1005
								BitReader br = new BitReader(buf, payloadOffset * 8 + 12);
								int stationID = (int) br.readBits(12);
								long x = br.readBits(38);
								long y = br.readBits(38);
								long z = br.readBits(38);
								double xm = x * 0.0001;
								double ym = y * 0.0001;
								double zm = z * 0.0001;
								
								// Approx lat/lng
								// TODO this is not converting correctly
								//double lng = Math.atan2(yM, xM) * 180 / Math.PI;
								//double lat = Math.asin(zM/R) * 180 / Math.PI;
								double[] latLngAlt = ECEF.ecefToLatLngAlt(xm,ym,zm);
								log.info("{} stationID={}, ECEF X={}m, Y={}m, Z={}m lat={}, lng={}", mountpoint, stationID, xm, ym, zm, latLngAlt[0], latLngAlt[1]);
								

								
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
				
				// Send message to all connected rovers
				for (RoverConnection r : rovers) {
					log.info("    sending to {}  bytesReceived={}, bytesBroadcast={}", r.toString(), bytesReceived, bytesBroadcast);
					try {
						r.send(buf, len);
						bytesBroadcast += len;
					} catch (IOException e) {
						log.error("error sending to rover, removing rover from list",e);
						rovers.remove(r);
					}
				}

			}
		} catch (IOException e) {
			log.error("Error in base station {} stream: " + e.toString(), mountpoint, e);
		} finally {
			stop();
		}
	}

	public BaseStationStatusDTO getStatus() {
		if (statusUpdates.size() == 0) {
			return null;
		}
		return statusUpdates.get(statusUpdates.size());
	}
	
	
	boolean isRunning() {
		return running;
	}

	void addRover(RoverConnection rover) {
		rovers.add(rover);
	}

	void stop() {
		running = false;
		
		// No - want to keep the mount point around for a while incase it is a temporary  disconnect
		// this.ntripCaster.stations.remove(mountpoint);
		
		rovers.forEach(RoverConnection::close);
		try {
			logOut.close();
			socket.close();
		} catch (IOException ignored) {
		}
		log.info("stop(): base station {} disconnected", mountpoint);
	}
	
	public void addUpdate(BaseStationStatusDTO update) {
		this.statusUpdates.add(update);
	}
}
