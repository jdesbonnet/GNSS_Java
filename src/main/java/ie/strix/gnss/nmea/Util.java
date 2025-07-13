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

	
}
