package ie.strix.gnss.nmea;

import lombok.Data;
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
		nSat = Integer.valueOf(3);
		
		final int nSignal = (parts.length - 4)/4;
		
		// This can't ever be more than 4
		assert nSignal <= 4;
		
		final int nExtra = (parts.length - 4) % 4;
		
		// Signal id 
		int signalId = nExtra > 0 ? Integer.valueOf(parts[4 + nSignal*4],16) : 0;
		Signal signal = Signal.getSignal(constellation, signalId);
		if (signal == null) {
			String msg = "signal not found for id=" + signalId + " constellation={}" + constellation;
			log.error(msg);;
			throw new IllegalArgumentException(msg);
		}
		
		log.info("nSignal={}, signalId={}", nSignal, signalId);
		
		signals = new SignalQuality[nSignal];
		for (int i = 0; i < nSignal; i++) {
			int prn = Integer.valueOf(parts[4+i*4]);
			int ele = Integer.valueOf(parts[4+i*4 + 1]);
			int azi = Integer.valueOf(parts[4+i*4 + 2]);
			int snr = Integer.valueOf(parts[4+i*4 + 3]);
			signals[i] = new SignalQuality(signal,prn,ele,azi,snr, signalId);
		}
		

	}
	
}
