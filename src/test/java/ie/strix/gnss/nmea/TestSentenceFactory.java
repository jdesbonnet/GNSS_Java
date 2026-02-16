package ie.strix.gnss.nmea;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestSentenceFactory {

    @DisplayName("valueOf dispatches supported sentence IDs via registry")
    @Test
    public void testKnownSentenceDispatch() throws ChecksumFailException {
        Sentence sentence = Sentence.valueOf("$GPGLL,4916.45,N,12311.12,W,225444,A,A*5C");

        assertNotNull(sentence);
        assertInstanceOf(GLL.class, sentence);
    }

    @DisplayName("valueOf returns UnknownSentence for valid unsupported sentence IDs")
    @Test
    public void testUnknownSentenceFallback() throws ChecksumFailException {
        String payload = "GPXYZ,1,2,3";
        Sentence sentence = Sentence.valueOf(withChecksum(payload));

        assertNotNull(sentence);
        assertInstanceOf(UnknownSentence.class, sentence);
    }

    private static String withChecksum(String payload) {
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
