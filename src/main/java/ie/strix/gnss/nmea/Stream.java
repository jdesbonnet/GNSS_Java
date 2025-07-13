package ie.strix.gnss.nmea;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

/**
 * Process a stream of NMEA-0183 sentences.
 * 
 * @author joe
 *
 */
@Slf4j
public class Stream {

	private long startOfCurrentDay = 0;
	private long timeInDay;
	private long prevTimeInDay;
	private Double latitude;
	private Double longitude;
	private Double altitude;
	private String dateIso;
	
	private int l1snr = 0;
	private int l2snr = 0;
	private int l5snr = 0;
	
	private List<SignalQuality> signals = new ArrayList<>();
	
	
	public void processSentence(String sentenceStr) {
		log.info(sentenceStr);
		
		if ( ! Util.isChecksumValid(sentenceStr)) {
			log.info("not valid");
			return;
		}
		
		Sentence sentence = Sentence.valueOf(sentenceStr);
		
		if (sentence == null) {
			log.info("sentence is null");
			return;
		}
		
		if (sentence == null || ! sentence.isChecksumValid()) {
			log.info("not valid: invalid checksum");
			return;
		}
		
		final int PRN = 14;
		
		//log.info("sss={}",sentence.getClass());
		if (sentence instanceof GGA) {
			GGA gga = (GGA)sentence;
			this.latitude = gga.getLatitude();
			this.longitude = gga.getLongitude();
			this.altitude = gga.getAltitude();
			
			this.timeInDay = gga.getTimeInDay();
			
			if (this.timeInDay != prevTimeInDay) {
				// process signals
				log.info("timeInDay={}",timeInDay);
				for (SignalQuality sq : signals) {
					log.info("SQ=" + sq);
					if ( sq.getSignal() == Signal.GPS_L1_CA && sq.getPrn() == PRN) {
						l1snr = sq.getSnr();
					}
					
					if ( sq.getSignal() == Signal.GPS_L2_PY &&  sq.getPrn() == PRN) {
						l2snr = sq.getSnr();
					}
					if ( sq.getSignal() == Signal.GPS_L5_Q &&  sq.getPrn() == PRN ) {
						l5snr = sq.getSnr();
					}
				}
				log.info("SIGQ {} {} {} {} {} {}",latitude,longitude,altitude,l1snr,l2snr,l5snr);
				System.out.println("" + latitude + " " + longitude + " " + altitude + " " + l1snr + " " + l2snr + " " + l5snr);

				signals = new ArrayList<>();
				prevTimeInDay = this.timeInDay;
			}
		}
		if (sentence instanceof RMC) {
			RMC rmc = (RMC)sentence;
			if ( ! rmc.getDateIso().equals(dateIso)) {
				// new date
				dateIso = rmc.getDateIso();
			}
		}
		if (sentence instanceof GSV) {
			GSV gsv = (GSV)sentence;
			signals.addAll(List.of(gsv.getSignals()));
		}
	}
	
	public static void main (String[] arg) throws IOException {
		File nmeaFile = new File(arg[0]);
		
		BufferedReader br = new BufferedReader(new FileReader(nmeaFile));
		
		String line;
		
		Stream stream = new Stream();
		
		while (  (line = br.readLine()) != null ) {
			stream.processSentence(line);
		}
	}
}
