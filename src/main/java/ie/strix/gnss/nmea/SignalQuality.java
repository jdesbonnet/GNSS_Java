package ie.strix.gnss.nmea;

import lombok.Getter;

@Getter
public final class SignalQuality {
	private Signal signal;
	private int prn;
	private int elevation;
	private int azimuth;
	private int snr;
	private int signalId;
	public SignalQuality (Signal signal, int prn, int ele, int azi, int snr, int signalId) {
		this.signal = signal;
		this.prn = prn;
		this.elevation = ele;
		this.azimuth = azi;
		this.snr = snr;
		this.signalId = signalId;
	}
	public String toString() {
		return "sig=" + this.signal + " prn=" + this.prn + " (" + this.elevation+","+this.azimuth + ") snr=" + snr;
	}
}