package ie.strix.gnss.nmea;

import lombok.Getter;

/**
 * Parser for NMEA {@code VTG} (Course Over Ground and Ground Speed) sentences.
 * <p>
 * {@code VTG} carries track angle over ground (true and optionally magnetic),
 * speed over ground (knots and km/h), and an optional NMEA mode indicator.
 * </p>
 */
@Getter
public class VTG extends Sentence {

	/**
	 * Track made good relative to true north, in degrees.
	 */
	private Double courseTrue;

	/**
	 * Track made good relative to magnetic north, in degrees.
	 */
	private Double courseMagnetic;

	/**
	 * Speed over ground in knots.
	 */
	private Double speedKnots;

	/**
	 * Speed over ground in kilometers per hour.
	 */
	private Double speedKmh;

	/**
	 * Optional NMEA mode indicator (for example {@code A}, {@code D}, {@code N}).
	 */
	private String mode;

	public VTG(String sentence) throws ChecksumFailException {
		super(sentence);
	}

	@Override
	protected void parse() {
		courseTrue = parseOptionalDouble(getPart(1));
		courseMagnetic = parseOptionalDouble(getPart(3));
		speedKnots = parseOptionalDouble(getPart(5));
		speedKmh = parseOptionalDouble(getPart(7));

		String modePart = getPart(9);
		mode = modePart.length() > 0 ? modePart : null;
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
