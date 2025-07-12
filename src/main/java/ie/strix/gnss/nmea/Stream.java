package ie.strix.gnss.nmea;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Process a stream of NMEA-0183 sentences.
 * 
 * @author joe
 *
 */
public class Stream {

	private long startOfCurrentDay = 0;
	private Double latitude;
	private Double longitude;
	private Double altitude;
	
	
	public void processSentence(String stentence) {
		Sentence sentence = Sentence.valueOf(stentence);
		if (sentence instanceof GGA) {
			GGA gga = (GGA)sentence;
			this.latitude = gga.getLatitude();
			this.latitude = gga.getLongitude();
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
