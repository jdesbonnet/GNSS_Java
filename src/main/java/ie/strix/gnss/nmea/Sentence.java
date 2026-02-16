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
	private boolean checksumValid = false;
	
	private String talkerId;
	protected String[] parts;
	protected Constellation constellation;
	
	protected void parse() {
		
	}
	
	public Sentence (String sentence) throws ChecksumFailException {
		this.sentence = sentence;
				
		if (Util.isChecksumValid(sentence)) {
			checksumValid = true;
			talkerId = Util.getTalkerId(sentence);
			parts = sentence.split(",", -1);
			
			// Remove the checksum from the last part.
			String lastPart = parts[parts.length-1];
			parts[parts.length-1] = lastPart.substring(0,lastPart.indexOf('*'));
			constellation = talkerIdToConstellation(talkerId);
			
		} else {
			throw new ChecksumFailException(this.sentence + " fails checksum");
		}
		
		
		parse();
	}
	
	//public Talker getTalker () {
	//}

	
	public boolean isChecksumValid () {
		return checksumValid;
	}
	
	public static Sentence valueOf(String sentence) throws ChecksumFailException {
		
		if ( ! Util.isChecksumValid(sentence)) {
			log.warn("valueOf() sentence invalid checksum: {}",sentence);
			return null;
		}
		
		String sentenceId = sentence.substring(3,6);
		log.debug("sentenceId={}", sentenceId);
		switch (sentenceId) {
		case "GGA": return new GGA(sentence);
		case "GSA": return new GSA(sentence);
		case "GST": return new GST(sentence);
		case "GSV": return new GSV(sentence);
		case "GLL": return new GLL(sentence);
		case "RMC": return new RMC(sentence);
		case "VTG": return new VTG(sentence);
		default: {log.error("unknown sentence {}",sentenceId); return null;}
		}
	}
	
	private static Constellation talkerIdToConstellation( String talkerId) {
		switch (talkerId) {
		case "GP": return Constellation.GPS;
		case "GL": return Constellation.GLONASS;
		case "GA": return Constellation.GALILEO;
		case "GB": return Constellation.BEIDOU;
		case "GI": return Constellation.NAVIC;
		case "GN": return Constellation.GENERIC;
		default: return null;
		}
	}

}
