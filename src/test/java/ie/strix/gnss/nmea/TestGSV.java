package ie.strix.gnss.nmea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestGSV {

    @DisplayName("GSV NMEA 4.10+ supports multi-signal / multi-constellation through system+signal IDs")
    @Test
    public void testGSVNmea410() throws ChecksumFailException {
        GSV gpsGsv = new GSV("$GNGSV,2,1,08,01,40,083,42,03,17,308,30,12,05,172,20,19,63,120,45,1,1*66");
        assertEquals(2, gpsGsv.getNumberOfMessages());
        assertEquals(1, gpsGsv.getMessageNumber());
        assertEquals(8, gpsGsv.getNSat());
        assertEquals(4, gpsGsv.getSignals().length);
        assertEquals(Signal.GPS_L1_CA, gpsGsv.getSignals()[0].getSignal());

        GSV glonassGsv = new GSV("$GNGSV,2,2,08,65,45,123,38,66,30,300,35,72,15,050,20,75,70,180,50,1,2*6C");
        assertEquals(2, glonassGsv.getNumberOfMessages());
        assertEquals(2, glonassGsv.getMessageNumber());
        assertEquals(8, glonassGsv.getNSat());
        assertEquals(4, glonassGsv.getSignals().length);
        assertEquals(Signal.GLONASS_G1_CA, glonassGsv.getSignals()[0].getSignal());

        GSV galileoGsv = new GSV("$GNGSV,1,1,04,11,55,100,41,19,25,210,37,24,12,310,29,31,05,045,18,7,3*68");
        assertEquals(1, galileoGsv.getNumberOfMessages());
        assertEquals(1, galileoGsv.getMessageNumber());
        assertEquals(4, galileoGsv.getNSat());
        assertEquals(4, galileoGsv.getSignals().length);
        assertEquals(Signal.GALILEO_L1BC, galileoGsv.getSignals()[0].getSignal());

        for (SignalQuality signalQuality : galileoGsv.getSignals()) {
            assertNotNull(signalQuality.getSignal());
        }
    }

    @DisplayName("GSV parses mixed constellation samples")
    @Test
    public void testGSV() throws ChecksumFailException {
        final String testSentence = "$GPGSV,3,1,11,05,20,182,27,13,58,108,46,14,42,067,45,15,80,237,46,1*60\n"
                + "$GPGSV,3,2,11,22,50,089,45,23,41,298,34,24,42,250,31,17,08,095,28,1*6D\n"
                + "$GPGSV,3,3,11,18,09,264,24,30,13,069,25,10,08,327,33,1*54\n"
                + "$GPGSV,2,1,05,13,58,108,40,14,42,067,40,15,80,237,45,22,50,089,40,4*68\n"
                + "$GPGSV,2,2,05,23,41,298,30,4*50\n"
                + "$GPGSV,2,1,08,05,20,182,24,14,42,067,45,15,80,237,42,23,41,298,31,6*68\n"
                + "$GPGSV,2,2,08,24,42,250,22,17,08,095,24,18,09,264,24,30,13,069,22,6*60\n"
                + "$GPGSV,2,1,05,14,42,067,46,23,41,298,29,24,42,250,29,18,09,264,18,8*62\n"
                + "$GPGSV,2,2,05,30,13,069,24,8*50\n"
                + "$GPGSV,1,1,03,14,42,067,45,23,41,298,36,18,09,264,26,9*5A\n"
                + "$GLGSV,2,1,08,66,27,315,32,75,72,174,48,65,80,310,41,81,07,298,23,1*79\n"
                + "$GLGSV,2,2,08,72,38,134,46,74,45,044,43,82,15,342,32,76,15,207,28,1*7F\n"
                + "$GLGSV,2,1,06,66,27,315,29,75,72,174,46,81,07,298,22,72,38,134,45,3*75\n"
                + "$GLGSV,2,2,06,82,15,342,29,76,15,207,30,3*7F\n"
                + "$GLGSV,1,1,01,75,72,174,43,5*4F\n"
                + "$GBGSV,3,1,10,22,59,290,42,36,49,204,49,19,55,162,49,11,05,338,29,1*71\n"
                + "$GBGSV,3,2,10,20,10,143,44,37,13,051,25,07,16,050,26,40,19,042,25,1*7A\n"
                + "$GBGSV,3,3,10,10,19,063,28,21,10,312,25,1*74\n"
                + "$GBGSV,2,1,06,22,59,290,40,36,49,204,48,19,55,162,48,20,10,143,43,3*73\n"
                + "$GBGSV,2,2,06,37,13,051,25,40,19,042,23,3*7C\n"
                + "$GBGSV,2,1,08,22,59,290,36,36,49,204,45,19,55,162,46,20,10,143,43,5*79\n"
                + "$GBGSV,2,2,08,37,13,051,22,40,19,042,22,21,10,312,18,46,64,082,46,5*71\n"
                + "$GBGSV,1,1,04,22,59,290,39,36,49,204,47,19,55,162,46,20,10,143,43,6*78\n"
                + "$GBGSV,3,1,11,22,59,290,40,36,49,204,47,19,55,162,47,11,05,338,30,8*73\n"
                + "$GBGSV,3,2,11,20,10,143,42,37,13,051,22,07,16,050,22,40,19,042,24,8*76\n"
                + "$GBGSV,3,3,11,10,19,063,27,21,10,312,23,46,64,082,46,8*4D\n"
                + "$GBGSV,1,1,03,11,05,338,29,07,16,050,22,10,19,063,25,B*3E\n"
                + "$GAGSV,3,1,11,05,56,289,36,06,40,111,44,09,65,110,44,15,17,320,26,1*79\n"
                + "$GAGSV,3,2,11,31,52,105,46,16,08,186,22,04,15,110,45,23,25,044,27,1*75\n"
                + "$GAGSV,3,3,11,24,27,170,32,34,17,268,23,03,06,290,13,1*41\n"
                + "$GAGSV,3,1,11,05,56,289,38,06,40,111,45,09,65,110,46,15,17,320,26,2*77\n"
                + "$GAGSV,3,2,11,31,52,105,48,16,08,186,25,04,15,110,48,23,25,044,27,2*72\n"
                + "$GAGSV,3,3,11,24,27,170,33,34,17,268,25,03,06,290,17,2*41\n"
                + "$GAGSV,3,1,11,05,56,289,40,06,40,111,46,09,65,110,46,15,17,320,28,5*72\n"
                + "$GAGSV,3,2,11,31,52,105,45,16,08,186,24,04,15,110,44,23,25,044,28,5*7A\n"
                + "$GAGSV,3,3,11,24,27,170,31,34,17,268,30,03,06,290,16,5*41\n"
                + "$GAGSV,3,1,11,05,56,289,40,06,40,111,43,09,65,110,46,15,17,320,29,7*74\n"
                + "$GAGSV,3,2,11,31,52,105,44,16,08,186,22,04,15,110,39,23,25,044,32,7*7E\n"
                + "$GAGSV,3,3,11,24,27,170,30,34,17,268,26,03,06,290,16,7*45";

        String[] lines = testSentence.split("\\n");
        for (String line : lines) {
            GSV gsv = new GSV(line);
            SignalQuality[] signals = gsv.getSignals();
            for (SignalQuality signal : signals) {
                log.info("    {}", signal);
            }
        }
    }
}
