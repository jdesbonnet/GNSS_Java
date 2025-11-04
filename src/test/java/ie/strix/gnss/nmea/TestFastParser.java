package ie.strix.gnss.nmea;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestFastParser {

	
	@Test
	public void testLongitudeParser() {
		
		double longitudePositive = Util.parseNmeaLongitude("00859.58729342,W".toCharArray(), 0, 15);
		assertEquals(-8.993121557, longitudePositive, 0.000000001);
		
		double longitudeNegative = Util.parseNmeaLongitude("00859.58729342,W".toCharArray(), 0, 15);
		assertEquals(-8.993121557, longitudeNegative, 0.000000001);
	}
	
	@Test
	public void testLatitudeParser() {		
		
		double latitudePositive = Util.parseNmeaLatitude("5316.89755755,N".toCharArray(), 0, 14);
		assertEquals(53.28162596, latitudePositive, 0.000000001);
		
		double latitudeNegative = Util.parseNmeaLatitude("5316.89755755,S".toCharArray(), 0, 14);
		assertEquals(-53.28162596, latitudeNegative, 0.000000001);		
	}

}
