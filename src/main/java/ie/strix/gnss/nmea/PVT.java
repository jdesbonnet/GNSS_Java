package ie.strix.gnss.nmea;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

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
	private Double latitude;
	private Double longitude;
	private Double altitude;
	private Integer fixType = 0;
	
	//private String getIsoTimestamp () {
		
	//}
	
	
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
}
