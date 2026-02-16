package ie.strix.gnss.nmea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestRMC {

    @DisplayName("RMC parses time, position, date and navigation fields")
    @Test
    public void testRMC() throws ChecksumFailException {
        RMC rmc = new RMC("$GPRMC,123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A");

        assertEquals(12 * 3600000 + 35 * 60000 + 19 * 1000, rmc.getTimeInDay());
        assertEquals("A", rmc.getStatus());
        assertEquals(48.1173, rmc.getLatitude(), 1e-8);
        assertEquals(11.516666667, rmc.getLongitude(), 1e-8);
        assertEquals(22.4, rmc.getSpeedOverGroundKt(), 1e-8);
        assertEquals(84.4, rmc.getTrackAngle(), 1e-8);
        assertEquals("1994-03-23", rmc.getDateIso());
        assertEquals(-3.1, rmc.getMagneticVariation(), 1e-8);
    }

    @DisplayName("RMC handles missing optional navigation fields")
    @Test
    public void testRMCOptionalFields() throws ChecksumFailException {
        RMC rmc = new RMC("$GPRMC,172814,V,3723.2475,N,12158.3416,W,,,050180,,,N*71");

        assertEquals("V", rmc.getStatus());
        assertEquals(17 * 3600000 + 28 * 60000 + 14 * 1000, rmc.getTimeInDay());
        assertEquals(37.387458333, rmc.getLatitude(), 1e-8);
        assertEquals(-121.97236, rmc.getLongitude(), 1e-8);
        assertEquals("1980-01-05", rmc.getDateIso());
        assertNull(rmc.getSpeedOverGroundKt());
        assertNull(rmc.getTrackAngle());
        assertNull(rmc.getMagneticVariation());
    }
}
