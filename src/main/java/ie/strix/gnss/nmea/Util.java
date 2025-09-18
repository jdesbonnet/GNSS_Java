package ie.strix.gnss.nmea;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Util {

	/**
	 * Return the NMEA timestamp as milliseconds since midnight.
	 * 
	 * @param nmeaTime Time string from GGA, RMC etc.
	 * @return Milliseconds since midnight.
	 */
	public static int parseNmeaTimestamp (String nmeaTime) {
		int timeStrLen = nmeaTime.length();
		Integer hh = Integer.valueOf(nmeaTime.substring(0, 2));
		Integer mm = Integer.valueOf(nmeaTime.substring(2, 4));
		Integer ss = Integer.valueOf(nmeaTime.substring(4, 6));
		int timeInDay = (hh * 3600 + mm * 60 + ss) * 1000;		
		// Is there a sub-second part after the radix point? I've seen 1 and 2 digits
		// after
		// the radix ('decimal') point. Is there any GNSS receiver that outputs ms?
		if (timeStrLen > 6) {
			Integer SSS = Integer.valueOf(nmeaTime.substring(7));
			if (timeStrLen == 9) {
				// 2 digits after the radix point
				timeInDay += SSS * 10;
			} else if (timeStrLen == 8) {
				// 1 digit after the radix point
				timeInDay += SSS * 100;
			} else if (timeStrLen == 10) {
				// 3 digits after the radix point (never seen this)
				timeInDay += SSS;
			}
		}
		log.info("timeInDay=" + timeInDay);
		return timeInDay;
	}
	
	
	/**
	 * Return time from midnight UTC is milliseconds.
	 * @param sentence
	 * @param timeIndex
	 * @return
	 */
	public static int parseNmeaTimestamp (char[] sentence, int timeIndex) {
		int hh = parsePositiveTwoDigitInt(sentence,timeIndex);
		int mm = parsePositiveTwoDigitInt(sentence,timeIndex+2);
		int ss = parsePositiveTwoDigitInt(sentence,timeIndex+4);
		int timeInDay = (hh * 3600 + mm * 60 + ss) * 1000;		
		return timeInDay;
	}
	
	/**
	 * Parse two digit positive integer.
	 * 
	 * @param buf
	 * @param offset
	 * @return
	 */
	public static final int parsePositiveTwoDigitInt(char[] buf, int offset) {
	    return (buf[offset]-'0')*10 + (buf[offset+1]-'0');
	}
	
	public static final int parseInt(char[] buf, int offset, int len) {
	    int result = 0;
	    boolean negative = false;

	    int i = offset;
	    if (len > 0 && buf[i] == '-') {
	        negative = true;
	        i++;
	        len--;
	    }

	    for (int end = i + len; i < end; i++) {
	        result = result * 10 + (buf[i] - '0');
	    }

	    return negative ? -result : result;
	}

	
	public static String parseNmeaDate (String nmeaDate) {
		final String dd = nmeaDate.substring(0,2);
		final String MM = nmeaDate.substring(2,4);
		// Why 2 digit years?!!
		final String YY = nmeaDate.substring(4,6);
		
		// Two digit years are deliberate bugs:/
		// This is good until 2077.
		int year = Integer.valueOf(YY);
		if (year < 78) {
			year += 2000;
		} else {
			year += 1900;
		}
		
		String isoDate = year + "-" + MM + "-" + dd;
		return isoDate;
	}

	
	
	/**
	 * Validates the checksum of an NMEA-0183 sentence.
	 *
	 * @param sentence The full NMEA sentence, e.g.
	 *                 "$GPGGA,123456.78,4916.45,N,12311.12,W,1,08,0.9,545.4,M,46.9,M,,*47"
	 * @return true if the checksum is valid, false otherwise
	 */
	public static boolean isChecksumValid(String sentence) {
		if (sentence == null || !sentence.startsWith("$") || !sentence.contains("*")) {
			return false;
		}
		// Extract the data and checksum
		int asteriskIndex = sentence.indexOf('*');
		String data = sentence.substring(1, asteriskIndex); // Exclude the $
		String checksumStr = sentence.substring(asteriskIndex + 1).trim();

		// Calculate the checksum
		int checksum = 0;
		for (char c : data.toCharArray()) {
			checksum ^= c;
		}

		// Convert to two-digit hex
		String expectedChecksum = String.format("%02X", checksum);
		return expectedChecksum.equalsIgnoreCase(checksumStr);

	}
	
	public static String getTalkerId (String sentence) {
		if ( ! sentence.startsWith("$")) {
			throw new IllegalArgumentException("NMEA-0183 stentences start with '$'");
		}
		if (sentence.length()<6) {
			throw new IllegalArgumentException("sentence too short");
		}
		return sentence.substring(1,3);
	}
	
}
