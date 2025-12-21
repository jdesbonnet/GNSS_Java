package ie.strix.gnss;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

import ie.strix.gnss.nmea.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * A track ...
 */
@Slf4j
public class Track {

	private List<PVT> pvts = new ArrayList<>();
	private long begin;
	private long end;
	
	/**
	 * Comparator to allow chronological ordering of PVT
	 */
	private static final Comparator<PVT> pvtComparator = (o1, o2) -> o1.getTimestamp() > o2.getTimestamp() ? 1 : ((o1.getTimestamp() < o2.getTimestamp()) ? -1 : 0) ;

	public Track () {
	}
	
	public Track (List<PVT> track) {
		addAll(track);
	}
	
	public Track (File gnssNmeaFile) {
		Stream nmeaStream;
		try {
			nmeaStream = new Stream(gnssNmeaFile);
			addAll(nmeaStream.readAllPVT());
			nmeaStream.close();
		} catch (IOException e) {
			log.error("error creating Track",e);
		}
	}
	
	
	/**
	 * Given a timestamp interpolate between points in a track or return null if outside of track time range.
	 * 
	 * @param ts
	 * @return
	 */
	public PVT interpolate(long ts) {
		
		long t = -System.currentTimeMillis();
		
		SimpleDateFormat isodf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		isodf.setTimeZone(TimeZone.getTimeZone("UTC"));
		String isoTs = isodf.format(ts);
		
		if ( ts < begin) {
			log.info("interpolate(): t before start of file dt=" + (begin-ts) + "ms");
			//return null;
			return this.pvts.get(0);
		}

		if ( ts > end) {
			log.info("interpolate(): t after end of file dt=" + (end-ts) + "ms");
			//log.warn("t=" + t + " is after any PVT data");
			return null;
		}
		
		log.info("interpolate(): ts={} is in track time frame",ts);
		
		PVT pvt = new PVT();
		pvt.setTimestamp(ts);
		pvt.setIsoTimestamp(isoTs);
		
		// Use binary search to find nearest PVT in time
		// the index of the search key, if it is contained in the list; otherwise, (-(insertion point) - 1). 
		// The insertion point is defined as the point at which the key would be inserted into the list: 
		// the index of the first element greater than the key, or list.size() if all elements in the list
		// are less than the specified key. Note that this guarantees that the return value will be >= 0 if and only if the key is found.
		
		int nearestIndex = Collections.binarySearch(pvts, pvt, pvtComparator);
		
		if (nearestIndex == 0) {
			log.info("nearestIndex = 0, not found?");
		}
		
		// If not exact match get negative of insertion point
		if (nearestIndex < 0) {
			nearestIndex = -nearestIndex - 1;
		}
		
		//log.info("nearestIndex={}",nearestIndex);

		// The point before the nearest point
		PVT p0 = pvts.get(nearestIndex>0?nearestIndex-1:0);
		
		// The nearest point
		PVT p1 = pvts.get(nearestIndex);
		
		// The point after the nearest point
		PVT p2 = pvts.get(nearestIndex < this.pvts.size()-1 ?  nearestIndex+1 : this.pvts.size()-1);
		
		double lat = p1.getLatitude();
		double lng = p1.getLongitude();
		double alt = p1.getAltitude();
		double hdg = p1.getHeading();
		double pitch = p1.getPitch();
		


		
		// How far off in time (seconds) is the desired time to the nearest data point?
		double et = ((double)(ts - p1.getTimestamp()))/1000.0;
		
		if (Math.abs(et) > 0.1) {
			log.warn("interpolate(): time offset to nearest track point: {} at {}",et,isoTs);
			pvt.setFixType(0); // do not use this
		} else {
			// Use the same fix type as nearest point
			pvt.setFixType( p1.getFixType());
		}
		
		
		if (et>0) {
			// If between p1 and p2, interpolate between p1->p2
			final double dlng = p2.getLongitude() - p1.getLongitude();
			final double dlat = p2.getLatitude() - p1.getLatitude();
			final double dalt = p2.getAltitude() - p1.getAltitude();
			
			final double dt = ((double)(p2.getTimestamp() - p1.getTimestamp()))/1000.0;
			lng += (et/dt) * dlng;
			lat += (et/dt) * dlat;
			alt += (et/dt) * dalt;
			
			// Heading is optional
			if (p2.getHeading() != null && p1.getHeading() != null) {
				final double dhdg = p2.getHeading() - p1.getHeading();
				hdg += (et/dt) * dhdg;
			}
			if (p2.getPitch() != null && p1.getPitch() != null) {
				final double dpitch = p2.getPitch() - p1.getPitch();
				pitch += (et/dt) * dpitch;
			}
			
		} else if (et < 0) {
			// If between p0 and p1, interpolate between p0->p1			
			final double dlng = p1.getLongitude() - p0.getLongitude();
			final double dlat = p1.getLatitude() - p0.getLatitude();
			final double dalt = p1.getAltitude() - p0.getAltitude();
			final double dt = ((double)(p1.getTimestamp() - p0.getTimestamp()))/1000.0;
			lng += (et/dt) * dlng;
			lat += (et/dt) * dlat;
			alt += (et/dt) * dalt;
			
			// Heading is optional
			if (p1.getHeading() != null && p0.getHeading() != null) {
				final double dhdg = p1.getHeading() - p0.getHeading();
				hdg += (et/dt) * dhdg;
			}
			if (p1.getPitch() != null && p0.getPitch() != null) {
				final double dpitch = p1.getPitch() - p0.getPitch();
				pitch += (et/dt) * dpitch;
			}
		}
		
		// Set interpolated position
		pvt.setLatitude(lat);
		pvt.setLongitude(lng);
		pvt.setAltitude(alt);
		pvt.setHeading(hdg);
		pvt.setPitch(pitch);
		
		// Calculate velocity direction
		double dx = (p2.getLongitude() - p0.getLongitude())*111320*Math.cos(lat*Math.PI/180.0);
		double dy = (p2.getLatitude() - p0.getLatitude())*111320;
		double dz = (p2.getAltitude() - p0.getAltitude());
		double dt = p2.getTimestamp() - p0.getTimestamp();
		// 0 = heading north
		// TODO: Unclear if dy,dx or dx,dy.
		//pvt.direction = Math.atan2(dx, dy); 
		pvt.setCourse(Math.atan2(dy, dx)*180.0/Math.PI);
		pvt.setSpeed(Math.sqrt(dx*dx + dy*dy + dz*dz)/dt);
		
		t += System.currentTimeMillis();
		//log.info("time to execute interpolation: {}ms",t);
		
		return pvt;
	}
	
	/**
	 * Note: must be ordered chronologically.
	 * @param pvts
	 */
	public void addPvt (PVT pvt) {
		if (pvts.size() == 0) {
			this.begin = pvt.getTimestamp();
		}
		pvts.add(pvt);
		this.end = pvt.getTimestamp();
	}
	
	/**
	 * Note: must be ordered chronologically.
	 * @param pvts
	 */
	public void addAll (List<PVT> pvts) {
		if (pvts.size() == 0) {
			return;
		}
		if (this.pvts.size() == 0) {
			this.begin = pvts.get(0).getTimestamp();
		}
		this.pvts.addAll(pvts);
		this.end = this.pvts.get(this.pvts.size()-1).getTimestamp();
	}
	
	public void add (Track track) {
		addAll(track.pvts);
	}
	
	/**
	 * Reduce the number of points in a track. 
	 * @return
	 */
	public Track simplify() {
		return simplify(10);
	}
	
	/**
	 * Reduce the number of points in a track. 
	 * @return
	 */
	public Track simplify(final double sep) {
		Track simpleTrack = new Track();
		PVT prevPvt = this.pvts.get(0);
		for (PVT pvt : this.pvts) {
			if (pvt.calcDistanceTo(prevPvt) > sep) {
				simpleTrack.addPvt(pvt);
				prevPvt = pvt;
			}
		}
		return simpleTrack;
	}
	
}
