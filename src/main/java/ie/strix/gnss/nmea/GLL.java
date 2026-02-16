package ie.strix.gnss.nmea;

import lombok.Getter;

/**
 * Parser for NMEA {@code GLL} (Geographic Position - Latitude/Longitude)
 * sentences.
 * <p>
 * {@code GLL} reports geographic coordinates, UTC time, data status, and an
 * optional mode indicator. It is a compact position-only sentence compared to
 * richer messages such as {@code GGA} or {@code RMC}.
 * </p>
 */
@Getter
public class GLL extends Sentence {

	/**
	 * UTC time-of-fix expressed as milliseconds since midnight.
	 */
	private Integer timeInDay;

	/**
	 * Geodetic latitude in signed decimal degrees (north positive).
	 */
	private Double latitude;

	/**
	 * Geodetic longitude in signed decimal degrees (east positive).
	 */
	private Double longitude;

	/**
	 * Data status: typically {@code A} (valid) or {@code V} (invalid/void).
	 */
	private String status;

	/**
	 * Optional NMEA mode indicator.
	 */
	private String mode;

	public GLL(String sentence) throws ChecksumFailException {
		super(sentence);
	}

	@Override
	protected void parse() {
		final String latStr = getPart(1);
		final String latSign = getPart(2);
		final String lonStr = getPart(3);
		final String lonSign = getPart(4);
		final String time = getPart(5);

		status = emptyToNull(getPart(6));
		mode = emptyToNull(getPart(7));

		if (time.length() > 0) {
			timeInDay = Util.parseNmeaTimestamp(time);
		}

		if (latStr.length() > 0 && lonStr.length() > 0) {
			double latDeg = Double.valueOf(latStr.substring(0, 2));
			double latMin = Double.valueOf(latStr.substring(2));
			latitude = latDeg + latMin / 60.0;
			if ("S".equals(latSign)) {
				latitude *= -1;
			}

			double lonDeg = Double.valueOf(lonStr.substring(0, 3));
			double lonMin = Double.valueOf(lonStr.substring(3));
			longitude = lonDeg + lonMin / 60.0;
			if ("W".equals(lonSign)) {
				longitude *= -1;
			}
		}
	}

	private String getPart(int index) {
		return index < parts.length ? parts[index] : "";
	}

	private String emptyToNull(String value) {
		if (value == null || value.length() == 0) {
			return null;
		}
		return value;
	}
}
