package ie.strix.gnss.nmea;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestGLL {

    @DisplayName("GLL parses latitude, longitude, time and status")
    @Test
    public void testGLL() throws ChecksumFailException {
        GLL gll = new GLL("$GPGLL,4916.45,N,12311.12,W,225444,A,A*5C");

        assertEquals(22 * 3600000 + 54 * 60000 + 44 * 1000, gll.getTimeInDay());
        assertEquals(49.274166667, gll.getLatitude(), 1e-8);
        assertEquals(-123.185333333, gll.getLongitude(), 1e-8);
        assertEquals("A", gll.getStatus());
        assertEquals("A", gll.getMode());
    }
}
