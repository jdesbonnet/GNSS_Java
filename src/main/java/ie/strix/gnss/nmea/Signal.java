package ie.strix.gnss.nmea;

public enum Signal {

	GPS_L1_CA(1,"GPS","L1 C/A"),
	GPS_L1_PY(2,"GPS","L1 P(Y)"),
	GPS_L1_M(3,"GPS","L1 M"),
	GPS_L2_PY(4,"GPS","L2 P(Y)"),
	GPS_L2C_M(5,"GPS","L2C-M"),
	GPS_L2C_L(6,"GPS","L2C-L"),
	GPS_L5_I(7,"GPS","L2C-M"),
	GPS_L5_Q(8,"GPS","L2C-L"),
	
	GLONASS_G1_CA(1,"GLONASS","G1 C/A"),
	GLONASS_G1_P(2,"GLONASS","G1 P"),
	GLONASS_G2_CA(3,"GLONASS","G2 C/A"),
	GLONASS_G2_P(4,"GLONASS","G2 P"),
	
	GALILEO_E5A(1,"GALILEO", "E5a"),
	GALILEO_E5B(2,"GALILEO", "E5b"),
	GALILEO_E5AB(3,"GALILEO", "E5a+b"),
	GALILEO_E6A(4,"GALILEO", "E6-A"),
	GALILEO_E6BC(5,"GALILEO", "E6-BC"),
	GALILEO_L1A(6,"GALILEO", "L1-A"),
	GALILEO_L1BC(7,"GALILEO", "L1-BC"),
	
	BEIDOU_B1I(1,"BEIDOU","B1I"),
	BEIDOU_B1Q(2,"BEIDOU","B1Q"),
	BEIDOU_B1C(3,"BEIDOU","B1C"),
	BEIDOU_B1A(4,"BEIDOU","B1A"),
	BEIDOU_B2A(5,"BEIDOU","B2a"),
	BEIDOU_B2B(6,"BEIDOU","B2b"),
	BEIDOU_B2AB(7,"BEIDOU","B2ab"),
	BEIDOU_B3I(8,"BEIDOU","B3I"),
	BEIDOU_B3Q(9,"BEIDOU","B3Q"),
	BEIDOU_B3A(10,"BEIDOU","B3A"),
	BEIDOU_B2I(11,"BEIDOU","B2I"),
	BEIDOU_B2Q(12,"BEIDOU","B2Q"),
	;
	
	private int id;
	private String constellation;
	private String name;
	private Signal (int id, String constellation, String name) {
		this.name = name;
	}
	
}
