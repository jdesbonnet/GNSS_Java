package ie.strix.gnss.nmea;

public class ChecksumFailException extends Exception {

	public ChecksumFailException() {
		
	}
	public ChecksumFailException (String message) {
		super(message);
	}
}
