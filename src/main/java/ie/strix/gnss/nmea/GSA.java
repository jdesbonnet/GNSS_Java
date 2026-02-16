package ie.strix.gnss.nmea;

import lombok.Getter;

/**
 * Parser for NMEA {@code GSA} (GNSS DOP and Active Satellites) sentences.
 * <p>
 * {@code GSA} reports the receiver operating mode, fix dimension (no-fix/2D/3D),
 * the list of satellites used in the current navigation solution, and dilution of
 * precision values ({@code PDOP}, {@code HDOP}, {@code VDOP}).
 * </p>
 * <p>
 * This implementation supports the common sentence form where up to 12 satellite
 * PRN slots are present. Empty satellite slots are represented as {@code null} in
 * {@link #satellitePrns}.
 * </p>
 * <p>
 * Example sentence:
 * <br>{@code $GPGSA,A,3,04,05,09,12,24,25,29,31,,,,,1.8,1.0,1.5*33}
 * </p>
 * <p>Field map:</p>
 * <ol>
 * <li>{@code $GPGSA} - talker + sentence formatter.</li>
 * <li>{@code A} - mode ({@code M}=manual, {@code A}=automatic).</li>
 * <li>{@code 3} - fix type ({@code 1}=none, {@code 2}=2D, {@code 3}=3D).</li>
 * <li>{@code 04..31} - up to 12 PRNs used in solution (blank when unused).</li>
 * <li>{@code 1.8} - PDOP.</li>
 * <li>{@code 1.0} - HDOP.</li>
 * <li>{@code 1.5} - VDOP.</li>
 * <li>{@code *33} - checksum.</li>
 * </ol>
 */
@Getter
public class GSA extends Sentence {

	/**
	 * Selection mode, usually {@code M} (manual) or {@code A} (automatic).
	 */
	private String mode;

	/**
	 * Fix dimension: {@code 1}=no fix, {@code 2}=2D fix, {@code 3}=3D fix.
	 */
	private Integer fixType;

	/**
	 * Satellite PRNs used in the solution (12 slots).
	 * <p>
	 * Empty slots are {@code null}.
	 * </p>
	 */
	private Integer[] satellitePrns;

	/**
	 * Position dilution of precision.
	 */
	private Double pdop;

	/**
	 * Horizontal dilution of precision.
	 */
	private Double hdop;

	/**
	 * Vertical dilution of precision.
	 */
	private Double vdop;

	public GSA(String sentence) throws ChecksumFailException {
		super(sentence);
	}

	@Override
	protected void parse() {
		mode = getPart(1);
		fixType = parseOptionalInt(getPart(2));

		satellitePrns = new Integer[12];
		for (int i = 0; i < satellitePrns.length; i++) {
			satellitePrns[i] = parseOptionalInt(getPart(3 + i));
		}

		pdop = parseOptionalDouble(getPart(15));
		hdop = parseOptionalDouble(getPart(16));
		vdop = parseOptionalDouble(getPart(17));
	}

	private String getPart(int index) {
		return index < parts.length ? parts[index] : "";
	}

	private Integer parseOptionalInt(String value) {
		if (value == null || value.length() == 0) {
			return null;
		}
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private Double parseOptionalDouble(String value) {
		if (value == null || value.length() == 0) {
			return null;
		}
		return Double.valueOf(value);
	}
}
