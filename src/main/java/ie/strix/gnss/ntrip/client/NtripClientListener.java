package ie.strix.gnss.ntrip.client;

public interface NtripClientListener {
	public void onConnect ();
	public void onDisconnect();
	public void onRtcmReceived(long bytesReceived);
}
