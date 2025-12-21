package ie.strix.gnss.nmea;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import ie.strix.gnss.PVT;
import lombok.extern.slf4j.Slf4j;

/**
 * Process a stream of NMEA-0183 sentences.
 * @author joe
 *
 */
@Slf4j
public class Stream {

	//private Reader reader;
	private BufferedReader br;
	private long startOfCurrentDay = 0;
	private long timeInDay;
	private long prevTimeInDay;
	private String isoDate;
	private int linesRead = 0;
	
	private Double latitude;
	private Double longitude;
	private Double altitude;
	
	private int l1snr = 0;
	private int l2snr = 0;
	private int l5snr = 0;
	
	private List<SignalQuality> signals = new ArrayList<>();
	
	public Stream () {
		
	}
	
	public Stream (Reader reader) {
		//this.reader = reader;
		this.br = new BufferedReader(reader);
	}
	
	public Stream (File file) throws FileNotFoundException, IOException {
		if (file.getName().endsWith(".gz")) {
			Reader reader = new InputStreamReader(new GZIPInputStream(new FileInputStream(file)));
			this.br = new BufferedReader(reader);
		} else {
			Reader reader = new FileReader(file);
			this.br = new BufferedReader(reader);
		}
	}
	
	public Sentence readNextSentence () throws IOException {
		while (true) {
			String sentenceStr = this.br.readLine();
			if (sentenceStr == null) {
				throw new IOException ("end of stream");
			}
			linesRead++;
			return processSentence(sentenceStr);
		}
	}
	
	public GGA readNextGGA () throws IOException {
		while (true) {
			Sentence sentence = readNextSentence();
			if (sentence instanceof GGA) {
				return (GGA)sentence;
			}
		}
	}
	
	public PVT readNextPVT() throws IOException {
	
		GGA gga;
		do {
			gga = readNextGGA();
		} while (isoDate == null || gga.getLatitude() == null);
		
		return PVT.fromGGA(isoDate, gga);
	}
	
	public List<PVT> readAllPVT() {
		List<PVT> list = new ArrayList<>();
		while (true) {
			try {
				list.add(readNextPVT());
			} catch (IOException e) {
				return list;
			}
		}
	}
	
	public Sentence processSentence(String sentenceStr) {
		log.debug(sentenceStr);
		
		if ( ! Util.isChecksumValid(sentenceStr)) {
			log.info("NMEA not valid {}", sentenceStr);
			return null;
		}
		
		Sentence sentence;
		try {
			sentence = Sentence.valueOf(sentenceStr);
		} catch (ChecksumFailException e) {
			log.info("NMEA checksum fail: {}", sentenceStr);
			return null;
		}
		
		if (sentence == null) {
			log.info("NMEA sentence is null");
			return null;
		}
		
		if (sentence == null || ! sentence.isChecksumValid()) {
			log.info("NMEA not valid: invalid checksum");
			return null;
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
				//log.info("timeInDay={}",timeInDay);
				for (SignalQuality sq : signals) {
					//log.info("SQ=" + sq);
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
				//log.info("SIGQ {} {} {} {} {} {}",latitude,longitude,altitude,l1snr,l2snr,l5snr);
				//System.out.println("" + latitude + " " + longitude + " " + altitude + " " + l1snr + " " + l2snr + " " + l5snr);

				signals = new ArrayList<>();
				prevTimeInDay = this.timeInDay;
			}
		}
		
		// RMC is useful for getting date in a NMEA stream
		if (sentence instanceof RMC) {
			RMC rmc = (RMC)sentence;
			String rmcIsoDate = rmc.getDateIso();
			if (rmcIsoDate != null) {
				if ( ! rmcIsoDate.equals(this.isoDate)) {
					// new date
					this.isoDate = rmcIsoDate;
					log.info("setting new isoDate={} len={}", this.isoDate, this.isoDate.length());
				}	
			}
		}
		
		
		if (sentence instanceof GSV) {
			GSV gsv = (GSV)sentence;
			signals.addAll(List.of(gsv.getSignals()));
		}
		
		return sentence;
	}
	
	public static void main (String[] arg) throws IOException,ChecksumFailException {
		File nmeaFile = new File(arg[0]);
		
		BufferedReader br = new BufferedReader(new FileReader(nmeaFile));
		
		String line;
		
		Stream stream = new Stream();
		
		while (  (line = br.readLine()) != null ) {
			stream.processSentence(line);
		}
	}
	
	public void close () {
		try {
			br.close();
		} catch (IOException e) {
			log.error("error on close()",e);
		}
	}
}
