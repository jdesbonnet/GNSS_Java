package ie.strix.gnss.nmea;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestGST {

    @DisplayName("GST parses time and pseudorange error statistics")
    @Test
    public void testGST() throws ChecksumFailException {
        GST gst = new GST("$GPGST,024603.00,1.8,0.9,1.2,45.0,0.7,0.8,1.5*6D");

        assertEquals(2 * 3600000 + 46 * 60000 + 3000, gst.getTimeInDay());
        assertEquals("024603.00", gst.getTime());
        assertEquals(1.8, gst.getRms(), 1e-9);
        assertEquals(0.9, gst.getStdMajor(), 1e-9);
        assertEquals(1.2, gst.getStdMinor(), 1e-9);
        assertEquals(45.0, gst.getOrientation(), 1e-9);
        assertEquals(0.7, gst.getStdLatitude(), 1e-9);
        assertEquals(0.8, gst.getStdLongitude(), 1e-9);
        assertEquals(1.5, gst.getStdAltitude(), 1e-9);
    }
}
