package ie.strix.gnss;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ie.strix.gnss.nmea.ChecksumFailException;
import ie.strix.gnss.nmea.GGA;
import ie.strix.gnss.nmea.Util;

public class TestPVTFromGga {

    @DisplayName("PVT.fromGGA supports 0..3 fractional time digits")
    @Test
    public void testFromGgaTimePrecision() throws ChecksumFailException {
        String isoDate = "2024-01-02";

        PVT pvt0 = PVT.fromGGA(isoDate, new GGA(buildGga("160041")));
        assertEquals("2024-01-02T16:00:41.000Z", pvt0.getIsoTimestamp());

        PVT pvt1 = PVT.fromGGA(isoDate, new GGA(buildGga("160041.6")));
        assertEquals("2024-01-02T16:00:41.600Z", pvt1.getIsoTimestamp());

        PVT pvt2 = PVT.fromGGA(isoDate, new GGA(buildGga("160041.60")));
        assertEquals("2024-01-02T16:00:41.600Z", pvt2.getIsoTimestamp());

        PVT pvt3 = PVT.fromGGA(isoDate, new GGA(buildGga("160041.600")));
        assertEquals("2024-01-02T16:00:41.600Z", pvt3.getIsoTimestamp());

        assertEquals(600L, pvt1.getTimestamp() - pvt0.getTimestamp());
    }

    @DisplayName("Util parses NMEA time for String and char[] consistently")
    @Test
    public void testUtilTimestampVariants() {
        assertEquals(57_641_000, Util.parseNmeaTimestamp("160041"));
        assertEquals(57_641_600, Util.parseNmeaTimestamp("160041.6"));
        assertEquals(57_641_600, Util.parseNmeaTimestamp("160041.60"));
        assertEquals(57_641_600, Util.parseNmeaTimestamp("160041.600"));

        assertEquals(57_641_000, Util.parseNmeaTimestamp("160041,".toCharArray(), 0));
        assertEquals(57_641_600, Util.parseNmeaTimestamp("160041.6,".toCharArray(), 0));
        assertEquals(57_641_600, Util.parseNmeaTimestamp("160041.60,".toCharArray(), 0));
        assertEquals(57_641_600, Util.parseNmeaTimestamp("160041.600,".toCharArray(), 0));
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
