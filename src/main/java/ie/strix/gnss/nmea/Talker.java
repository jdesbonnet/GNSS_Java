package ie.strix.gnss.nmea;

public enum Talker {
GPS("GP"), GLONASS("GL"),GALILEO("GA"),BEIDOU ("BD");
	
	private String talkerCode;
	private Talker (String talkerCode) {
		this.talkerCode = talkerCode;
	}
}
