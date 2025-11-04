package ie.strix.gnss.nmea;


/**
 * Goal is a fast NMEA parser that does not create any intermediate objects.
 * Uses sentence examples to define breaks in sentences (assuming that all
 * similar sentences in a stream are formatted identially).
 */
public class Parser {

	 

	public void parseGga (String sentence, GGA gga) {
		Util.parseNmeaTimestamp(null);
	}
}
