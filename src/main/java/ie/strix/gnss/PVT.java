package ie.strix.gnss;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import ie.strix.gnss.nmea.GGA;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * PVT is a aggregate of several NMEA sentences in one epoch. Since no one NMEA sentence 
 * holds all the epoch information this class performs that function.
 */
@Data
@Slf4j
public class PVT {

	//private String isoDate;
	private String isoTimestamp;
	private Long timestamp;
	
	/**
	 * Latitude in degrees.
	 */
	private Double latitude;
	
	/**
	 * Longitude in degrees
	 */
	private Double longitude;
	
	/**
	 * Altitude [define] in meters above [define].
	 */
	private Double altitude;
	
	/**
	 * GNSS fix type using NMEA 0183 GGA convention.
	 */
	private Integer fixType = 0;
	
	/**
	 * This is the direction in the horizontal plane of the velocity vector. Not to be confused with the heading/pose of the device which 
	 * can be different.
	 */
	private Double course;
	
	/**
	 * The velocity (in 3D).
	 */
	private Double speed;
	
	/**
	 * The device pose heading where 0=north. This is different to course (eg the device could be traveling north but looking east at the same time).
	 */
	private Double heading;
	private Double pitch;
	private Double roll;
	
	//private String getIsoTimestamp () {
		
	//}
	
	public PVT () {
		
	}
	
	public PVT (long timestamp, double lat, double lng, double alt) {
		this.timestamp = timestamp;
		this.latitude = lat;
		this.longitude = lng;
		this.altitude = alt;
	}
	
	
	public static PVT fromGGA (String isoDate, GGA gga) {
		
		if (isoDate == null) {
			throw new IllegalArgumentException("isoDate cannot be null");
		}
		log.debug("fromGGA() isoDate={}",isoDate);
		
		String nmeaTime = gga.getNmeaTime();
		String hh = nmeaTime.substring(0,2);
		String mm = nmeaTime.substring(2,4);
		String ss = nmeaTime.substring(4,6);
		String SSS = nmeaTime.substring(7);
		
		PVT pvt = new PVT();
		//pvt.isoTimestamp = isoDate + "T" + gga.getIsoTime();
		
		// Some GNSS module do time with 0, 1, 2 (and presumably 3?) decimal places in the NMEA time.
		String zsuffix;
		if (SSS.length()==1) {
			zsuffix = "00Z";
		} else if (SSS.length()==2) {
			zsuffix = "0Z";
		} else {
			zsuffix = "Z";
		}
		pvt.isoTimestamp = isoDate + "T" + hh + ":" + mm + ":" + ss + "." + SSS + zsuffix;
		
		SimpleDateFormat isodf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS'Z'");
		isodf.setTimeZone(TimeZone.getTimeZone("UTC"));
		try {
			pvt.timestamp = isodf.parse(pvt.isoTimestamp).getTime();
		} catch (ParseException e) {
			log.error("cannot parse timestamp",e);
		}

		pvt.latitude = gga.getLatitude();
		pvt.longitude = gga.getLongitude();
		pvt.altitude = gga.getAltitude();
		pvt.fixType = gga.getFixType();
		
		return pvt;
	}
	
	/**
	 * Calculate spatial displacement from current PVT to pvt2 in meters.
	 * @param pvt2
	 * @return
	 */
	public Vector3D calcDisplacementTo (PVT pvt2) {
		final double dlat = pvt2.getLatitude() - this.latitude;
		final double dlng = pvt2.getLongitude() - this.longitude;
		final double dalt = pvt2.getAltitude() - this.altitude;
		final double de = dlng * 111320 * Math.cos(this.latitude*Math.PI/180);
		final double dn = dlat * 111320;
		Vector3D displacement = new Vector3D(de,dn,dalt);
		return displacement;
	}
	public Vector3D calcAttitudeVector () {
		final double pitchRad = this.pitch*Math.PI/180.0;
		final double headingRad = this.heading*Math.PI/180.0;
		double i = Math.cos(headingRad) * Math.cos(pitchRad);
		double j = Math.sin(headingRad) * Math.cos(pitchRad);
		double k = Math.sin(pitchRad);
		return new Vector3D(i,j,k);
	}
	
	public double calcBearingTo (PVT pvt2) {
		final double dlat = pvt2.getLatitude() - this.latitude;
		final double dlng = pvt2.getLongitude() - this.longitude;
		//final double dalt = pvt2.getAltitude() - this.altitude;
		final double de = dlng * 111320 * Math.cos(this.latitude*Math.PI/180);
		final double dn = dlat * 111320;
		return Math.atan2(dn, de) * 180.0 / Math.PI;
	}
	public double calcSpeedTo (PVT pvt2) {
		final double d = calcDisplacementTo(pvt2).getNorm();
		final double dt = (pvt2.getTimestamp() - this.timestamp)/1000.0;
		return d/dt;
	}
}
