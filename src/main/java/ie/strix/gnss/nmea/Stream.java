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
		Sentence sentence = Sentence.valueOf(sentenceStr);
		if (sentence == null) {
			return;
		}
		log.info("sss={}",sentence.getClass());
		if (sentence instanceof GGA) {
			GGA gga = (GGA)sentence;
			this.latitude = gga.getLatitude();
			this.latitude = gga.getLongitude();
			this.altitude = gga.getAltitude();
			
			this.timeInDay = gga.getTimeInDay();
			
			if (this.timeInDay != prevTimeInDay) {
				// process signals
				log.info("SIGQ {} {} {} {} {} {}",latitude,longitude,altitude,l1snr,l2snr,l5snr);

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
			//signals.add(List.of(gsv.getSignals());
			
			// Group by constellation-prn
			Map<String,List<SignalQuality>> byPrn = new HashMap<>();
			for (SignalQuality sq : signals) {
				log.info("sq={}",sq);
				String key = sq.getSignal().getConstellation() + "_" + sq.getPrn();
				List<SignalQuality> list = byPrn.get(key);
				if (list == null) {
					list = new ArrayList<SignalQuality>();
					byPrn.put(key, list);
				}
				list.add(sq);
			}
			

			int PRN = 14;
			
			for (String key : byPrn.keySet()) {
				List<SignalQuality> list = byPrn.get(key);
				Set<Integer> prns = new HashSet<>();
				for (SignalQuality sq : list) {
					prns.add(sq.getPrn());
					
					if ( sq.getPrn() == PRN && sq.getSignal().getName()=="GPS_L1_CA") {
						l1snr = sq.getSnr();
					}
					
					if ( sq.getPrn() == PRN && sq.getSignal().getName()=="GPS_L2_PY") {
						l2snr = sq.getSnr();
					}
					if ( sq.getPrn() == PRN && sq.getSignal().getName()=="GPS_L5_Q") {
						l5snr = sq.getSnr();
					}
				}
				StringBuilder sb = new StringBuilder();
				for (Integer prn : prns) {
					sb.append("" + prn + " ");
				}
				log.info("{}: {}", key, sb.toString());
			}
			signals = new ArrayList<SignalQuality>();
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
