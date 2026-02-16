package ie.strix.gnss.nmea;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Parser for NMEA {@code RMC} (Recommended Minimum Specific GNSS Data) sentences.
 * <p>
 * {@code RMC} provides a compact navigation solution that includes UTC time, data validity,
 * position, speed over ground, course over ground, magnetic variation, and date.
 * </p>
 */
@Slf4j
@Getter
public class RMC extends Sentence {

	/**
	 * UTC time-of-fix expressed as milliseconds since midnight.
	 */
	private int timeInDay;

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
	 * Parsed NMEA date in ISO-like calendar form (for example {@code 2024-11-03}).
	 */
	private String dateIso;

	/**
	 * Status reported in the sentence. {@code A} means valid/active data, {@code V} means void.
	 */
	private String status;

	/**
	 * Speed over ground in knots.
	 */
	private Double speedOverGroundKt;

	/**
	 * Course over ground in true degrees.
	 */
	private Double trackAngle;

	/**
	 * Magnetic variation in degrees. West values are represented with a negative sign.
	 */
	private Double magneticVariation;

	public RMC(String sentence) throws ChecksumFailException {
		super(sentence);
	}

	protected void parse() {
		final String timeStr = getPart(1);
		final String statusStr = getPart(2);
		final String latStr = getPart(3);
		final String latSign = getPart(4);
		final String lngStr = getPart(5);
		final String lngSign = getPart(6);
		
		final String speedOverGroundKtStr = getPart(7);
		final String trackAngleStr        = getPart(8);
		final String dateStr              = getPart(9);
		final String magvarStr            = getPart(10);
		final String magvarDirection      = getPart(11);

		status = statusStr.length() > 0 ? statusStr : null;

		// If there is no time we have nothing, ignore
		if (timeStr.length() == 0 || latStr.length() == 0 || lngStr.length() == 0) {
			return;
		}

		this.timeInDay = Util.parseNmeaTimestamp(timeStr);
		
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

		} else {
			latitude = null;
			longitude = null;
		}

		speedOverGroundKt = parseOptionalDouble(speedOverGroundKtStr);
		trackAngle = parseOptionalDouble(trackAngleStr);
		dateIso = dateStr.length() > 0 ? Util.parseNmeaDate(dateStr) : null;

		magneticVariation = parseOptionalDouble(magvarStr);
		if (magneticVariation != null && "W".equals(magvarDirection)) {
			magneticVariation *= -1;
		}
	}

	private String getPart(int index) {
		if (index < parts.length) {
			return parts[index];
		}
		return "";
	}

	private Double parseOptionalDouble(String value) {
		if (value == null || value.length() == 0) {
			return null;
		}
		return Double.valueOf(value);
	}
}
