package ie.strix.gnss.nmea;

import java.io.Reader;

import lombok.Builder;

/**
 * Idea here is to create fastest possible NMEA-0183 parser. Allow for special
 * cases when we are dealing with one known device so we can make assumptions
 * about field widths.
 * 
 * Want:
 * ParserFactory.builder().withGaaExample(gga)...
 * 
 * 
 */
@Builder
public class ParserFactory {

	private String ggaExample;
	private String rmcExample;
	private String gsvExample;
	
	public Parser createParserByExample(Reader in) {
		return null;
	}
	
}
