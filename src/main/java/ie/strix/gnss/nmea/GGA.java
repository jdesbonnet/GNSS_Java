package ie.strix.gnss.nmea;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Parser for NMEA {@code GGA} (Global Positioning System Fix Data) sentences.
 * <p>
 * A {@code GGA} sentence provides a position fix snapshot at a specific UTC time, including
 * geodetic position (latitude/longitude), fix quality, number of satellites used in the solution,
 * dilution of precision, and mean-sea-level altitude.
 * </p>
 * <p>
 * This class parses and exposes commonly used fields from the sentence. It also derives a coarse
 * horizontal accuracy estimate via {@link #accuracy} based on fix type (when available) or
 * fallback HDOP heuristics.
 * </p>
 */
@Slf4j
@Getter
public class GGA extends Sentence {

	/**
	 * UTC time-of-fix expressed as milliseconds since midnight.
	 */
	private int timeInDay;
	
	/**
	 * Raw NMEA UTC timestamp in {@code hhmmss.SS} format.
	 */
	private String time;

	/**
	 * Geodetic latitude in signed decimal degrees.
	 * <p>
	 * North is positive; South is negative.
	 * </p>
	 */
	private Double latitude;
	
	/**
	 * Geodetic longitude in signed decimal degrees.
	 * <p>
	 * East is positive; West is negative.
	 * </p>
	 */
	private Double longitude;

	/**
	 * Fix quality/type code from GGA field 6.
	 * <p>
	 * Typical values include: {@code 0}=invalid, {@code 1}=GPS fix, {@code 2}=DGPS,
	 * {@code 4}=RTK fixed, {@code 5}=RTK float.
	 * </p>
	 */
	private Integer fixType;

	/**
	 * Number of satellites used in the navigation solution.
	 */
	private Integer nSat;

	/**
	 * Horizontal dilution of precision (HDOP).
	 */
	private Double hdop;

	/**
	 * Antenna altitude above mean sea level, in meters.
	 */
	private Double altitude;

	/**
	 * Age of differential corrections (DGPS/RTCM), if reported.
	 */
	private Integer dgnssage;

	/**
	 * Differential reference station identifier, if reported.
	 */
	private Integer stationId;

	/**
	 * Estimated horizontal accuracy in meters.
	 * <p>
	 * For selected fix types this is set to representative constants; otherwise a heuristic
	 * {@code HDOP * 5m} estimate is used.
	 * </p>
	 */
	private Double accuracy;


	public GGA (String sentence) throws ChecksumFailException {
		super(sentence);
	}
	
	@Override
	protected void parse() {
		this.time = parts[1];
		String latStr = parts[2];
		String latSign = parts[3];
		String lngStr = parts[4];
		String lngSign = parts[5];
		String fixTypeStr = parts[6];
		String nSatStr = parts[7];
		String hdopStr = parts[8];
		String mslAltStr = parts[9];

		// If there is no time we have nothing, ignore
		if (this.time.length() == 0) {
			return;
		}
		
		this.timeInDay = Util.parseNmeaTimestamp(this.time);
		
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
	
	/**
	 * Returns the parsed UTC time with a trailing {@code Z} timezone designator.
	 *
	 * @return timestamp formatted as {@code HH:mm:ss.SSSZ}
	 */
	public String getIsoTime () {
		if (time == null || time.isEmpty()) {
			return null;
		}
		return Util.formatIsoUtcTime(timeInDay);
	}

	/**
	 * Returns the raw NMEA UTC timestamp.
	 *
	 * @return timestamp in {@code hhmmss[.S[S[S]]]} format
	 */
	public String getNmeaTime() {
		return time;
	}
}
