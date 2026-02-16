package ie.strix.gnss.nmea;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Parser for NMEA {@code GSV} (Satellites in View) sentences.
 * <p>
 * A {@code GSV} report describes the set of satellites currently visible to the receiver,
 * including per-satellite geometry (elevation/azimuth) and signal strength (SNR). The full
 * report may span multiple sentences; {@link #numberOfMessages} gives the total sentence count
 * for a report and {@link #messageNumber} gives the index of this sentence within that sequence.
 * </p>
 * <p>
 * This implementation also supports NMEA-0183 v4.10+ optional trailing fields containing
 * signal ID and system ID, which are used to resolve the reported signal/constellation when
 * available.
 * </p>
 *
 * @author joe
 * <p>
 * Example sentence:
 * <br>{@code $GPGSV,3,1,11,05,20,182,27,13,58,108,46,14,42,067,45,15,80,237,46*60}
 * </p>
 * <p>Field map:</p>
 * <ol>
 * <li>{@code $GPGSV} - talker + sentence formatter.</li>
 * <li>{@code 3} - total number of GSV messages in this report cycle.</li>
 * <li>{@code 1} - this message number within the cycle.</li>
 * <li>{@code 11} - total satellites in view.</li>
 * <li>{@code 05,20,182,27} - satellite block #1: PRN, elevation, azimuth, SNR.</li>
 * <li>{@code 13,58,108,46} - satellite block #2: PRN, elevation, azimuth, SNR.</li>
 * <li>{@code 14,42,067,45} - satellite block #3: PRN, elevation, azimuth, SNR.</li>
 * <li>{@code 15,80,237,46} - satellite block #4: PRN, elevation, azimuth, SNR.</li>
 * <li>{@code *60} - checksum.</li>
 * </ol>
 */
@Slf4j
@Getter
public class GSV extends Sentence {

	/**
	 * One-based index of this sentence within the current multi-sentence GSV report.
	 * <p>
	 * Example: for a 3-part report, values are {@code 1}, {@code 2}, and {@code 3}.
	 * </p>
	 */
	private int messageNumber;

	/**
	 * Total number of GSV sentences that make up the current report.
	 * <p>
	 * Receivers can only include up to four satellite entries per sentence, so this value is
	 * typically greater than {@code 1} when many satellites are in view.
	 * </p>
	 */
	private int numberOfMessages;

	/**
	 * Total number of satellites in view for the complete report.
	 * <p>
	 * This value is the report-wide satellite count, not just the number of satellites present in
	 * {@link #signals} for this sentence fragment.
	 * </p>
	 */
	private int nSat;

	/**
	 * Satellite/signal entries carried by this sentence fragment.
	 * <p>
	 * Each {@link SignalQuality} contains PRN, elevation, azimuth, and SNR for one visible
	 * satellite. NMEA GSV sentences carry up to four entries, so this array length is in the range
	 * {@code 0..4}. For multi-sentence reports, aggregate entries from all fragments to obtain the
	 * complete sky view.
	 * </p>
	 */
	private SignalQuality[] signals;

	public GSV(String sentence) throws ChecksumFailException {
		super(sentence);
	}

	protected void parse() {
		numberOfMessages = Integer.valueOf(parts[1]);
		messageNumber = Integer.valueOf(parts[2]);
		nSat = Integer.valueOf(parts[3]);

		final int nSignal = (parts.length - 4) / 4;
		assert nSignal <= 4;

		final int nExtra = (parts.length - 4) % 4;
		final int tailIndex = 4 + nSignal * 4;

		int signalId = nExtra > 0 ? Integer.valueOf(parts[tailIndex], 16) : 0;
		int systemId = nExtra > 1 ? Integer.valueOf(parts[tailIndex + 1]) : 0;
		Constellation sentenceConstellation = resolveConstellation(systemId);

		Signal signal = signalId > 0 ? Signal.getSignal(sentenceConstellation, signalId) : null;
		if (signalId > 0 && signal == null) {
			log.warn("signal not found for id={} constellation={} (systemId={})", signalId, sentenceConstellation, systemId);
		}

		log.debug("nSignal={}, signalId={}, systemId={}, constellation={}", nSignal, signalId, systemId, sentenceConstellation);

		signals = new SignalQuality[nSignal];
		for (int i = 0; i < nSignal; i++) {
			int prn = Integer.valueOf(parts[4 + i * 4]);
			int ele = Integer.valueOf(parts[4 + i * 4 + 1]);
			int azi = Integer.valueOf(parts[4 + i * 4 + 2]);
			int snr = parseNullableInt(parts[4 + i * 4 + 3]);
			signals[i] = new SignalQuality(signal, prn, ele, azi, snr, signalId);
		}
	}

	private int parseNullableInt(String value) {
		return value == null || value.isEmpty() ? -1 : Integer.valueOf(value);
	}

	private Constellation resolveConstellation(int systemId) {
		if (constellation != Constellation.GENERIC) {
			return constellation;
		}

		switch (systemId) {
		case 1:
			return Constellation.GPS;
		case 2:
			return Constellation.GLONASS;
		case 3:
			return Constellation.GALILEO;
		case 4:
			return Constellation.BEIDOU;
		case 6:
			return Constellation.NAVIC;
		default:
			return constellation;
		}
	}

}
