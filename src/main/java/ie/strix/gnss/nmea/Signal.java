package ie.strix.gnss.nmea;

public enum Signal {

	GPS_L1_CA(1,Constellation.GPS,"L1 C/A"),
	GPS_L1_PY(2,Constellation.GPS,"L1 P(Y)"),
	GPS_L1_M(3,Constellation.GPS,"L1 M"),
	GPS_L2_PY(4,Constellation.GPS,"L2 P(Y)"),
	GPS_L2C_M(5,Constellation.GPS,"L2C-M"),
	GPS_L2C_L(6,Constellation.GPS,"L2C-L"),
	GPS_L5_I(7,Constellation.GPS,"L2C-M"),
	GPS_L5_Q(8,Constellation.GPS,"L2C-L"),
	
	GLONASS_G1_CA(1,Constellation.GLONASS,"G1 C/A"),
	GLONASS_G1_P(2,Constellation.GLONASS,"G1 P"),
	GLONASS_G2_CA(3,Constellation.GLONASS,"G2 C/A"),
	GLONASS_G2_P(4,Constellation.GLONASS,"G2 P"),
	
	GALILEO_E5A(1,Constellation.GALILEO, "E5a"),
	GALILEO_E5B(2,Constellation.GALILEO, "E5b"),
	GALILEO_E5AB(3,Constellation.GALILEO, "E5a+b"),
	GALILEO_E6A(4,Constellation.GALILEO, "E6-A"),
	GALILEO_E6BC(5,Constellation.GALILEO, "E6-BC"),
	GALILEO_L1A(6,Constellation.GALILEO, "L1-A"),
	GALILEO_L1BC(7,Constellation.GALILEO, "L1-BC"),
	
	BEIDOU_B1I(1,Constellation.BEIDOU,"B1I"),
	BEIDOU_B1Q(2,Constellation.BEIDOU,"B1Q"),
	BEIDOU_B1C(3,Constellation.BEIDOU,"B1C"),
	BEIDOU_B1A(4,Constellation.BEIDOU,"B1A"),
	BEIDOU_B2A(5,Constellation.BEIDOU,"B2a"),
	BEIDOU_B2B(6,Constellation.BEIDOU,"B2b"),
	BEIDOU_B2AB(7,Constellation.BEIDOU,"B2ab"),
	BEIDOU_B3I(8,Constellation.BEIDOU,"B3I"),
	BEIDOU_B3Q(9,Constellation.BEIDOU,"B3Q"),
	BEIDOU_B3A(10,Constellation.BEIDOU,"B3A"),
	BEIDOU_B2I(11,Constellation.BEIDOU,"B2I"),
	BEIDOU_B2Q(12,Constellation.BEIDOU,"B2Q"),
	;
	
	private int id;
	private Constellation constellation;
	private String name;

	private Signal (int id, Constellation constellation, String name) {
		this.id = id;
		this.constellation = constellation;
		this.name = name;
	}
	
}
