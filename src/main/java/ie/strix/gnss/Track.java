package ie.strix.gnss;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
		//pvts = track;
		//begin = track.get(0).getTimestamp();
		//end   = track.get(track.size()-1).getTimestamp();
		addAll(track);
	}
	
	/**
	 * Given a timestamp interpolate between points in a track or return null if outside of track time range.
	 * 
	 * @param ts
	 * @return
	 */
	public PVT interpolate(long ts) {
		
		if ( ts < begin) {
			//log.info("t before start of file dt=" + (begin-ts) + "ms");
			return null;
		}

		if ( ts > end) {
			//log.info("t after end of file dt=" + (end-ts) + "ms");
			//log.warn("t=" + t + " is after any PVT data");
			return null;
		}
		

		PVT pvt = new PVT();
		pvt.setTimestamp(ts);
		
		// Use binary search to find nearest PVT in time
		// the index of the search key, if it is contained in the list; otherwise, (-(insertion point) - 1). 
		// The insertion point is defined as the point at which the key would be inserted into the list: 
		// the index of the first element greater than the key, or list.size() if all elements in the list
		// are less than the specified key. Note that this guarantees that the return value will be >= 0 if and only if the key is found.
		
		int nearestIndex = Collections.binarySearch(pvts, pvt, pvtComparator);
				
		// If not exact match get negative of insertion point
		if (nearestIndex < 0) {
			nearestIndex = -nearestIndex - 1;
		}
		
		log.info("nearestIndex={}",nearestIndex);

		PVT p0 = pvts.get(nearestIndex-1);
		PVT p1 = pvts.get(nearestIndex);
		PVT p2 = pvts.get(nearestIndex+1);
		
		double lat = p1.getLatitude();
		double lng = p1.getLongitude();
		double alt = p1.getAltitude();
		
		//pvt.setLatitude (p1.getLatitude());
		//pvt.setLongitude (p1.getLongitude());
		//pvt.setAltitude  (p1.getAltitude());
		pvt.setFixType( p1.getFixType());
		
		// How far off in time (seconds) is the desired time to the nearest data point?
		double et = ((double)(ts - p1.getTimestamp()))/1000.0;
		
		if (et>0) {
			// If between p1 and p2, interpolate between p1->p2
			final double dlng = p2.getLongitude() - p1.getLongitude();
			final double dlat = p2.getLatitude() - p1.getLatitude();
			final double dalt = p2.getAltitude() - p1.getAltitude();
			final double dt = ((double)(p2.getTimestamp() - p1.getTimestamp()))/1000.0;
			lng += (et/dt) * dlng;
			lat += (et/dt) * dlat;
			alt += (et/dt) * dalt;
		} else if (et < 0) {
			// If between p0 and p1, interpolate between p0->p1			
			final double dlng = p1.getLongitude() - p0.getLongitude();
			final double dlat = p1.getLatitude() - p0.getLatitude();
			final double dalt = p1.getAltitude() - p0.getAltitude();
			final double dt = ((double)(p2.getTimestamp() - p1.getTimestamp()))/1000.0;
			lng += (et/dt) * dlng;
			lat += (et/dt) * dlat;
			alt += (et/dt) * dalt;
			
		}
		
		// Set interpolated position
		pvt.setLatitude(lat);
		pvt.setLongitude(lng);
		pvt.setAltitude(alt);
		
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
			this.begin = pvts.get(0).getTimestamp();
		}
		pvts.addAll(pvts);
		this.end = pvts.get(pvts.size()-1).getTimestamp();
	}
}
