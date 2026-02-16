package ie.strix.gnss.nmea;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestGGA {

    @DisplayName("GGA")
    @Test
    public void testGGA() throws ChecksumFailException {
        String[] times = { "160041", "160041.6", "160041.60", "160041.600" };
        int[] expectedTimeInDay = {
                16 * 3600000 + 0 * 60000 + 41000,
                16 * 3600000 + 0 * 60000 + 41600,
                16 * 3600000 + 0 * 60000 + 41600,
                16 * 3600000 + 0 * 60000 + 41600,
        };

        for (int i = 0; i < times.length; i++) {
            GGA gga = new GGA(buildGga(times[i]));
            assertEquals(expectedTimeInDay[i], gga.getTimeInDay());
            assertEquals(-8.993121592, gga.getLongitude(), 1e-8);
            assertEquals(53.281625938, gga.getLatitude(), 1e-8);
            assertEquals(27.7125, gga.getAltitude(), 1e-4);
            assertEquals(4, gga.getFixType());
            assertEquals(23, gga.getNSat());
        }
    }

    private static String buildGga(String time) {
        String payload = "GNGGA," + time
                + ",5316.89755629,N,00859.58729552,W,4,23,0.7,27.7125,M,57.9942,M,1.6,295";
        return "$" + payload + "*" + checksum(payload);
    }

    private static String checksum(String payload) {
        int checksum = 0;
        for (char c : payload.toCharArray()) {
            checksum ^= c;
        }
        return String.format("%02X", checksum);
    }
}
