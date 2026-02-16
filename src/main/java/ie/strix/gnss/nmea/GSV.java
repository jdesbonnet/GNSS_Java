package ie.strix.gnss.nmea;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * Note: NMEA-0183 v4.10+ supports an extra signals/system id at the end of the sentence.
 *
 * @author joe
 *
 */
@Slf4j
@Getter
public class GSV extends Sentence {

	private int messageNumber;
	private int numberOfMessages;
	private int nSat;

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
