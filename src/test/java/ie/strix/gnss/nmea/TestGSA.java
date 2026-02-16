package ie.strix.gnss.nmea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestGSA {

    @DisplayName("GSA parses active satellites and DOP values")
    @Test
    public void testGSA() throws ChecksumFailException {
        GSA gsa = new GSA("$GPGSA,A,3,04,05,09,12,24,25,29,31,,,,1.8,1.0,1.5*11");

        assertEquals("A", gsa.getMode());
        assertEquals(3, gsa.getFixType());
        assertEquals(12, gsa.getSatellitePrns().length);
        assertEquals(4, gsa.getSatellitePrns()[0]);
        assertEquals(31, gsa.getSatellitePrns()[7]);
        assertNull(gsa.getSatellitePrns()[8]);
        assertEquals(1.8, gsa.getPdop(), 1e-9);
        assertEquals(1.0, gsa.getHdop(), 1e-9);
        assertEquals(1.5, gsa.getVdop(), 1e-9);
    }
    @DisplayName("GSA tolerates missing final satellite slots before DOP fields")
    @Test
    public void testGSAMissingSatelliteSlots() throws ChecksumFailException {
        GSA gsa = new GSA("$GPGSA,A,3,04,05,09,12,24,25,29,31,,,1.8,1.0,1.5*3D");

        assertEquals(12, gsa.getSatellitePrns().length);
        assertNull(gsa.getSatellitePrns()[8]);
        assertNull(gsa.getSatellitePrns()[9]);
        assertNull(gsa.getSatellitePrns()[10]);
        assertNull(gsa.getSatellitePrns()[11]);
        assertEquals(1.8, gsa.getPdop(), 1e-9);
        assertEquals(1.0, gsa.getHdop(), 1e-9);
        assertEquals(1.5, gsa.getVdop(), 1e-9);
    }

}
