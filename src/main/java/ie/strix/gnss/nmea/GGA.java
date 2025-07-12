package ie.strix.gnss.nmea;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class GGA extends Sentence {

	/** Milliseconds since midnight UTC */
	private int timeInDay;

	private Double latitude;
	private Double longitude;

	private Integer fixType;

	private Integer nSat;

	private Double hdop;

	private Double altitude;

	private Integer dgnssage;
	private Integer stationId;

	private Double accuracy;

	public GGA(String sentence) {
		super(sentence);

	}

	private void parse() {
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

		int timeStrLen = timeStr.length();
		Integer hh = Integer.valueOf(timeStr.substring(0, 2));
		Integer mm = Integer.valueOf(timeStr.substring(2, 4));
		Integer ss = Integer.valueOf(timeStr.substring(4, 6));
		timeInDay = (hh * 3600 + mm * 60 + ss) * 1000;

		// Is there a sub-second part after the radix point? I've seen 1 and 2 digits
		// after
		// the radix ('decimal') point. Is there any GNSS receiver that outputs ms?
		if (timeStrLen > 6) {
			Integer SSS = Integer.valueOf(timeStr.substring(7));
			if (timeStrLen == 9) {
				// 2 digits after the radix point
				timeInDay += SSS * 10;
			} else if (timeStrLen == 8) {
				// 1 digit after the radix point
				timeInDay += SSS * 100;
			} else if (timeStrLen == 10) {
				// 3 digits after the radix point (never seen this)
				timeInDay += SSS;
			}
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
