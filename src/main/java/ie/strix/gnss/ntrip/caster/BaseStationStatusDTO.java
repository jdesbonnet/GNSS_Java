package ie.strix.gnss.ntrip.caster;

import lombok.Value;

@Value
public class BaseStationStatusDTO {
	private String stationId;
	private long timestamp;
	private int battery;
	private int temperature;
	private String gsvSentences;
}
