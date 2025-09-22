package ie.strix.gnss.nmea;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestParser {


	
	@Test
	public void testLongitudeParser() {
		String nmeaLongitude = "00859.58729342,W";

		double longitude = Util.parseNmeaLongitude(nmeaLongitude.toCharArray(), 0, 15);
		assertEquals(-8.993121557, longitude, 0.000000001);
	}
	
	public void testLatitudeParser() {
		String nmeaLatitude = "5316.89755755,N";
		
		double latitude = Util.parseNmeaLatitude(nmeaLatitude.toCharArray(), 0, 14);
		assertEquals(53.28162596, latitude, 0.000000001);
	}

}
