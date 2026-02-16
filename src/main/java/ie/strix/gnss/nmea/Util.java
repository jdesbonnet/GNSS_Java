package ie.strix.gnss.nmea;

import lombok.extern.slf4j.Slf4j;

/**
 * NMEA 0183 utility functions.
 */
@Slf4j
public class Util {

	/**
	 * Return the NMEA timestamp as milliseconds since midnight.
	 * An example of NMEA timestamp is "140312.00".
	 * 
	 * @param nmeaTime Time string from GGA, RMC etc.
	 * @return Milliseconds since midnight.
	 */
	public static int parseNmeaTimestamp (String nmeaTime) {
		final int hh = Integer.parseInt(nmeaTime.substring(0, 2));
		final int mm = Integer.parseInt(nmeaTime.substring(2, 4));
		final int ss = Integer.parseInt(nmeaTime.substring(4, 6));
		int timeInDay = (hh * 3600 + mm * 60 + ss) * 1000;

		if (nmeaTime.length() > 6 && nmeaTime.charAt(6) == '.') {
			int digitCount = 0;
			int fractionMs = 0;
			for (int i = 7; i < nmeaTime.length() && digitCount < 3; i++) {
				final char c = nmeaTime.charAt(i);
				if (c < '0' || c > '9') {
					break;
				}
				fractionMs = fractionMs * 10 + (c - '0');
				digitCount++;
			}

			if (digitCount == 1) {
				timeInDay += fractionMs * 100;
			} else if (digitCount == 2) {
				timeInDay += fractionMs * 10;
			} else if (digitCount == 3) {
				timeInDay += fractionMs;
			}
		}

		return timeInDay;
	}
	
	
	/**
	 * Return time from midnight UTC is milliseconds.
	 * @param sentence
	 * @param timeIndex
	 * @return
	 */
	public final static int parseNmeaTimestamp (final char[] sentence, final int timeIndex) {
		final int hh = parsePositiveTwoDigitInt(sentence,timeIndex);
		final int mm = parsePositiveTwoDigitInt(sentence,timeIndex+2);
		final int ss = parsePositiveTwoDigitInt(sentence,timeIndex+4);
		int timeInDay = (hh * 3600  +  mm * 60  +  ss) * 1000;

		final int radixIndex = timeIndex + 6;
		if (radixIndex < sentence.length && sentence[radixIndex] == '.') {
			int fractionMs = 0;
			int digitCount = 0;
			for (int i = radixIndex + 1; i < sentence.length && digitCount < 3; i++) {
				final char c = sentence[i];
				if (c < '0' || c > '9') {
					break;
				}
				fractionMs = fractionMs * 10 + (c - '0');
				digitCount++;
			}

			if (digitCount == 1) {
				timeInDay += fractionMs * 100;
			} else if (digitCount == 2) {
				timeInDay += fractionMs * 10;
			} else if (digitCount == 3) {
				timeInDay += fractionMs;
			}
		}

		return timeInDay;
	}

	/**
	 * Format milliseconds since midnight as an ISO UTC time fragment.
	 *
	 * @param timeInDay Milliseconds since midnight.
	 * @return Time string in {@code HH:mm:ss.SSSZ} format.
	 */
	public static String formatIsoUtcTime(final int timeInDay) {
		final int hh = timeInDay / 3_600_000;
		final int mm = (timeInDay % 3_600_000) / 60_000;
		final int ss = (timeInDay % 60_000) / 1_000;
		final int ms = timeInDay % 1_000;
		return String.format("%02d:%02d:%02d.%03dZ", hh, mm, ss, ms);
	}

	
	/**
	 * Return decimal degrees given latitude or longitude in NMEA0183 format.
	 * Example:  "00859.58729342". 
	 * 
	 * @param sentence The NMEA sentence in char[].
	 * @param startIndex The position in the sentence array where the longitude starts. It is assumed the sign (E,W)
	 * always follows directly after.
	 * @return
	 */
	public final static double parseNmeaLongitude (final char[] sentence, final int startIndex, int signIndex) {
		final int degrees = parsePositiveThreeDigitInt(sentence, startIndex);
		final int wholeMinutes = parsePositiveTwoDigitInt(sentence, startIndex+3);
		final double minutesFrac = parseFractionalPart(sentence,startIndex+6, signIndex-1);
		
		double minutes = degrees*60.0 + wholeMinutes + minutesFrac;
		
		if (sentence[signIndex] == 'W') {
			minutes *= -1.0;
		}
		
		// Final result expected in degrees
		return minutes / 60.0;
	}
	
	/**
	 * Return decimal degrees given latitude or longitude in NMEA0183 format.
	 * Example:  "5316.89755755,N".
	 * 
	 * @param sentence The NMEA sentence in char[].
	 * @param startIndex The position in the sentence array where the latitude starts. It is assumed the sign (S,N)
	 * always follows directly after.
	 * @return
	 */
	public final static double parseNmeaLatitude (final char[] sentence, final int startIndex, final int signIndex) {
		final int degrees = parsePositiveTwoDigitInt(sentence, startIndex);
		final int wholeMinutes = parsePositiveTwoDigitInt(sentence, startIndex+2);
		final double minutesFrac = parseFractionalPart(sentence,startIndex+5, signIndex-1);
		
		double minutes = degrees*60.0 + wholeMinutes + minutesFrac;
		
		if (sentence[signIndex] == 'S') {
			minutes *= -1.0;
		}
		
		// Final result expected in degrees
		return minutes / 60.0;
	}
	
	
	/**
	 * Parse two digit positive integer from char[] in a way that does not create any intermediate objects.
	 * 
	 * @param buf
	 * @param offset
	 * @return
	 */
	public static final int parsePositiveTwoDigitInt(char[] buf, int offset) {
	    return (buf[offset]-'0')*10 + (buf[offset+1]-'0');
	}
	
	/**
	 * Parse three digit positive integer form char[] in a way that does not create any intermediate objects.
	 * 
	 * @param buf
	 * @param offset
	 * @return
	 */
	public static final int parsePositiveThreeDigitInt(char[] buf, int offset) {
	    return (buf[offset]-'0')*100 + (buf[offset+1]-'0')*10 + buf[offset+2]-'0';
	}
	
	/**
	 * Parse variable digit positive integer form char[] in a way that does not create any intermediate objects.
	 * 
	 * @param buf
	 * @param offset
	 * @return
	 */
	public static final int parseVarDigitInt(final char[] buf, final int startIndex, final int endIndex) {
		int sum = 0;
		for (int i = startIndex; i <= endIndex; i++) {
			sum *= 10;
			sum += buf[i] - '0';
		}
		return sum;
	}
	
	/**
	 * Parse the fractional part (mantissa) of a floating point from char[] in a way that does not create any intermediate objects.
	 * 
	 * @param buf
	 * @param offset
	 * @return
	 */
	public static final double parseFractionalPart(final char[] buf, final int startIndex, final int endIndex) {
		int sum = 0;
		int div = 1;
		for (int i = startIndex; i < endIndex; i++) {
			div *= 10;
			sum *= 10;
			sum += buf[i] - '0';
		}
		return (double)sum / (double)div;
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
