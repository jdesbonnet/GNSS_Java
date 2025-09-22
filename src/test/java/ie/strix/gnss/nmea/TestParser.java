package ie.strix.gnss.nmea;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestParser {


	@Test
	public void testLongitudeParser() {
		String nmeaLongitude = "00859.58729342,W";
		double longitude = Util.parseNmeaLongitude(nmeaLongitude.toCharArray(), 0, 13, 15);
		assertEquals(-8.993121557, longitude, 0.000000001);
	}

}
