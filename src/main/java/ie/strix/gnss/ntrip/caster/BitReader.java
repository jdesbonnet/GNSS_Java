package ie.strix.gnss.ntrip.caster;

// utility to read bits from byte array
class BitReader {
	private final byte[] data;
	private int bitPos;

	BitReader(byte[] data, int bitPos) {
		this.data = data;
		this.bitPos = bitPos;
	}

	long readBits(int n) {
		long val = 0;
		for (int i = 0; i < n; i++) {
			int byteIndex = bitPos / 8;
			int bitIndex = 7 - (bitPos % 8);
			val = (val << 1) | ((data[byteIndex] >> bitIndex) & 1);
			bitPos++;
		}
		return val;
	}
}