package ie.strix.gnss.nmea;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class GGA extends Sentence {

	/** Milliseconds since midnight UTC */
	private int timeInDay;

	@Getter
	private Double latitude;
	
	@Getter
	private Double longitude;

	private Integer fixType;

	private Integer nSat;

	private Double hdop;

	private Double altitude;

	private Integer dgnssage;
	private Integer stationId;

	private Double accuracy;


	public GGA (String sentence) throws ChecksumFailException {
		super(sentence);
	}
	
	@Override
	protected void parse() {
		
		log.info("parse()");
		
		String timeStr = parts[1];
		String latStr = parts[2];
		String latSign = parts[3];
		String lngStr = parts[4];
		String lngSign = parts[5];
		String fixTypeStr = parts[6];
		String nSatStr = parts[7];
		String hdopStr = parts[8];
		String mslAltStr = parts[9];

		// If there is no time we have nothing, ignore
		if (timeStr.length() == 0 || latStr.length() == 0 || lngStr.length() == 0) {
			return;
		}
		
		this.timeInDay = Util.parseNmeaTimestamp(timeStr);

		

		try {
			nSat = Integer.valueOf(nSatStr);
		} catch (NumberFormatException e) {
			// Likely just ""
			nSat = null;
		}

		try {
			hdop = Double.valueOf(hdopStr);
		} catch (NumberFormatException e) {
			// Likely just ""
			hdop = null;
		}

		// TODO: is the *= -1 going to cause threading issues?
		if (latStr.length() > 0 && lngStr.length() > 0) {

			double latDeg = Double.valueOf(latStr.substring(0, 2));
			double latMin = Double.valueOf(latStr.substring(2));
			latitude = latDeg * 1.0 + latMin / 60.0;
			if ("S".equals(latSign)) {
				latitude *= -1;
			}

			double lngDeg = Double.valueOf(lngStr.substring(0, 3));
			double lngMin = Double.valueOf(lngStr.substring(3));
			longitude = lngDeg * 1.0 + lngMin / 60.0;
			if ("W".equals(lngSign)) {
				longitude *= -1;
			}

			if (mslAltStr.length() > 0) {
				altitude = Double.valueOf(mslAltStr);
			}

		} else {
			latitude = null;
			longitude = null;
			altitude = null;
		}

		try {
			fixType = Integer.valueOf(fixTypeStr);
		} catch (Exception e) {
			log.info("fixType parse error GGA=" + sentence, e);
			fixType = null;
		}

		if (fixType == 4) {
			accuracy = 0.2;
		} else if (fixType == 5) {
			accuracy = 1.0;
		} else if (fixType == 2) {
			accuracy = 2.0;
		} else {
			// Estimate 'accuracy' in m using HDOP*5m. I don't have any reference for this.
			Double hdop = Double.valueOf(hdopStr);
			accuracy = hdop * 5.0;
		}

	}
}
