package ie.strix.gnss.nmea;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestVTG {

    @DisplayName("VTG parses course and speed in multiple units")
    @Test
    public void testVTG() throws ChecksumFailException {
        VTG vtg = new VTG("$GPVTG,054.7,T,034.4,M,005.5,N,010.2,K,A*25");

        assertEquals(54.7, vtg.getCourseTrue(), 1e-9);
        assertEquals(34.4, vtg.getCourseMagnetic(), 1e-9);
        assertEquals(5.5, vtg.getSpeedKnots(), 1e-9);
        assertEquals(10.2, vtg.getSpeedKmh(), 1e-9);
        assertEquals("A", vtg.getMode());
    }
}
