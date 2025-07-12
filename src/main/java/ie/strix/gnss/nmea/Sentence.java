package ie.strix.gnss.nmea;

import lombok.extern.slf4j.Slf4j;

/**
 * Superclass of all NMEA-0183 sentences. 
 * 
 * @author Joe Desbonnet
 *
 */
@Slf4j
public class Sentence {

	protected String sentence;
	private boolean valid = false;
	private boolean nmea  = false;
	private String talkerId;
	protected String[] parts;
	
	public Sentence (String sentence) {
		this.sentence = sentence;
		
		log.info("sentence={}",sentence);
		if (sentence.startsWith("$") && isChecksumValid(sentence)) {
			valid = true;
			nmea = true;
			talkerId = sentence.substring(1,3);
			parts = sentence.split(",");
			
			// Remove the checksum from the last part.
			String lastPart = parts[parts.length-1];
			parts[parts.length-1] = lastPart.substring(0,lastPart.indexOf('*'));
		}
	}
	
	//public Talker getTalker () {
	//}
	
	/**
	 * Validates the checksum of an NMEA-0183 sentence.
	 *
	 * @param sentence The full NMEA sentence, e.g.
	 *                 "$GPGGA,123456.78,4916.45,N,12311.12,W,1,08,0.9,545.4,M,46.9,M,,*47"
	 * @return true if the checksum is valid, false otherwise
	 */
	public static boolean isChecksumValid(String sentence) {
		if (sentence == null || !sentence.startsWith("$") || !sentence.contains("*")) {
			return false;
		}
		// Extract the data and checksum
		int asteriskIndex = sentence.indexOf('*');
		String data = sentence.substring(1, asteriskIndex); // Exclude the $
		String checksumStr = sentence.substring(asteriskIndex + 1).trim();

		// Calculate the checksum
		int checksum = 0;
		for (char c : data.toCharArray()) {
			checksum ^= c;
		}

		// Convert to two-digit hex
		String expectedChecksum = String.format("%02X", checksum);
		return expectedChecksum.equalsIgnoreCase(checksumStr);

	}

}
