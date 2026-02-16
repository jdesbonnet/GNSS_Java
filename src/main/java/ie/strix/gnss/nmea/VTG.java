package ie.strix.gnss.nmea;

import lombok.Getter;

/**
 * Parser for NMEA {@code VTG} (Course Over Ground and Ground Speed) sentences.
 * <p>
 * {@code VTG} carries track angle over ground (true and optionally magnetic),
 * speed over ground (knots and km/h), and an optional NMEA mode indicator.
 * </p>
 * <p>
 * Example sentence:
 * <br>{@code $GPVTG,054.7,T,034.4,M,005.5,N,010.2,K,A*23}
 * </p>
 * <p>Field map:</p>
 * <ol>
 * <li>{@code $GPVTG} - talker + sentence formatter.</li>
 * <li>{@code 054.7} - true course over ground (degrees).</li>
 * <li>{@code T} - true course reference flag.</li>
 * <li>{@code 034.4} - magnetic course over ground (degrees).</li>
 * <li>{@code M} - magnetic course reference flag.</li>
 * <li>{@code 005.5} - speed over ground in knots.</li>
 * <li>{@code N} - knots unit flag.</li>
 * <li>{@code 010.2} - speed over ground in km/h.</li>
 * <li>{@code K} - km/h unit flag.</li>
 * <li>{@code A} - mode indicator.</li>
 * <li>{@code *23} - checksum.</li>
 * </ol>
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
