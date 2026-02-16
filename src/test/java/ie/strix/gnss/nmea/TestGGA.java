package ie.strix.gnss.nmea;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestGGA {

    @DisplayName("GGA")
    @Test
    public void testGGA() throws ChecksumFailException {
        String[] ggaSamples = {
                // "$GNGGA,160041,5316.89755629,N,00859.58729552,W,4,23,0.7,27.7125,M,57.9942,M,1.6,295*4D",
                // "$GNGGA,160041.6,5316.89755629,N,00859.58729552,W,4,23,0.7,27.7125,M,57.9942,M,1.6,295*4D",
                "$GNGGA,160041.60,5316.89755629,N,00859.58729552,W,4,23,0.7,27.7125,M,57.9942,M,1.6,295*4D",
                // "$GNGGA,160041.600,5316.89755629,N,00859.58729552,W,4,23,0.7,27.7125,M,57.9942,M,1.6,295*4D",
        };

        for (String ggaString : ggaSamples) {
            GGA gga = new GGA(ggaString);
            assertEquals(16 * 3600000 + 0 * 60000 + 41600, gga.getTimeInDay());
            assertEquals(-8.993121592, gga.getLongitude(), 1e-8);
            assertEquals(53.281625938, gga.getLatitude(), 1e-8);
            assertEquals(27.7125, gga.getAltitude(), 1e-4);
            assertEquals(4, gga.getFixType());
            assertEquals(23, gga.getNSat());
        }
    }
}
