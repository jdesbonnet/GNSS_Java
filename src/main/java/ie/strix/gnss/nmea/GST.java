package ie.strix.gnss.nmea;

import lombok.Getter;

/**
 * Parser for NMEA {@code GST} (GNSS Pseudorange Error Statistics) sentences.
 * <p>
 * {@code GST} provides quality/error estimates for the current position solution,
 * including RMS pseudorange residual and estimated one-sigma standard deviations
 * on latitude, longitude, and altitude.
 * </p>
 */
@Getter
public class GST extends Sentence {

	/**
	 * UTC time-of-fix expressed as milliseconds since midnight.
	 */
	private Integer timeInDay;

	/**
	 * Raw NMEA UTC timestamp in {@code hhmmss.SS} format.
	 */
	private String time;

	/**
	 * RMS value of pseudorange residuals.
	 */
	private Double rms;

	/**
	 * Error ellipse semi-major axis one-sigma deviation (meters).
	 */
	private Double stdMajor;

	/**
	 * Error ellipse semi-minor axis one-sigma deviation (meters).
	 */
	private Double stdMinor;

	/**
	 * Orientation of the error ellipse semi-major axis, in true degrees.
	 */
	private Double orientation;

	/**
	 * One-sigma latitude error estimate (meters).
	 */
	private Double stdLatitude;

	/**
	 * One-sigma longitude error estimate (meters).
	 */
	private Double stdLongitude;

	/**
	 * One-sigma altitude error estimate (meters).
	 */
	private Double stdAltitude;

	public GST(String sentence) throws ChecksumFailException {
		super(sentence);
	}

	@Override
	protected void parse() {
		time = getPart(1);
		if (time.length() > 0) {
			timeInDay = Util.parseNmeaTimestamp(time);
		}

		rms = parseOptionalDouble(getPart(2));
		stdMajor = parseOptionalDouble(getPart(3));
		stdMinor = parseOptionalDouble(getPart(4));
		orientation = parseOptionalDouble(getPart(5));
		stdLatitude = parseOptionalDouble(getPart(6));
		stdLongitude = parseOptionalDouble(getPart(7));
		stdAltitude = parseOptionalDouble(getPart(8));
	}

	private String getPart(int index) {
		return index < parts.length ? parts[index] : "";
	}

	private Double parseOptionalDouble(String value) {
		if (value == null || value.length() == 0) {
			return null;
		}
		return Double.valueOf(value);
	}
}
