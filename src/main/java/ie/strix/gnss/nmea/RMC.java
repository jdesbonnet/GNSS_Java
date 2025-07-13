package ie.strix.gnss.nmea;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class RMC extends Sentence {

	/** Milliseconds since midnight UTC */
	private int timeInDay;

	@Getter
	private Double latitude;
	
	@Getter
	private Double longitude;

	//private String date;
	private String dateIso;

	public RMC(String sentence) {
		super(sentence);
		parse();
	}

	private void parse() {
		final String timeStr = parts[1];
		final String statusStr = parts[2];
		final String latStr = parts[3];
		final String latSign = parts[4];
		final String lngStr = parts[5];
		final String lngSign = parts[6];
		
		final String speedOverGroundKtStr = parts[7];
		final String trackAngleStr        = parts[8];
		final String dateStr              = parts[9];
		final String magvarStr            = parts[9];

		// If there is no time we have nothing, ignore
		if (timeStr.length() == 0 || latStr.length() == 0 || lngStr.length() == 0) {
			return;
		}

		this.timeInDay = Util.parseNmeaTimestamp(timeStr);
		
		// TODO: is the *= -1 going to cause threading issues?
		if (latStr.length() > 0 && lngStr.length() > 0) {

			double latDeg = Double.valueOf(latStr.substring(0, 2));
			double latMin = Double.valueOf(latStr.substring(2));
			latitude = latDeg * 1.0 + latMin / 60.0;
			if ("S".equals(latSign)) {
				latitude *= -1;
			}

			double lngDeg = Double.valueOf(lngStr.substring(0, 3));
			double lngMin = Double.valueOf(lngStr.substring(3));
			longitude = lngDeg * 1.0 + lngMin / 60.0;
			if ("W".equals(lngSign)) {
				longitude *= -1;
			}

		} else {
			latitude = null;
			longitude = null;
		}

		dateIso = Util.parseNmeaDate(dateStr);
	}
}
