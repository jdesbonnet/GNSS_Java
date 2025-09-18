package ie.strix.gnss.nmea;

import lombok.Builder;

/**
 * Want:
 * ParserFactory.builder().withGaaExample(gga).with
 */
@Builder
public class ParserFactory {

	private String ggaExample;
	private String rmcExample;
	private String gsvExample;
	
}
