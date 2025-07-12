package ie.strix.gnss.rtcm;

/**
 * Related to RTCM v3 messages.
 * 
 * RTCM3 message:
 * 0xD3 <msglenlsb> <msglenmsb> [payload] <crcbyte0> <crcbyte1> <crcbyte2>
 *
 * msglen: length excluding 0xD3 preamble and excluding 3 bytes of CRC.
 * CRC is computed on bytes from and including preamble to up but excluding the CRC.
 * 
 * First 12 bits of payload define message type.
 *
 */
public class RTCM3 {
	private static final int[] CRC24Q_TABLE = new int[256];
	private static final int POLY = 0x1864CFB;

	static {
		for (int i = 0; i < 256; i++) {
			int crc = i << 16;
			for (int j = 0; j < 8; j++) {
				if ((crc & 0x800000) != 0) {
					crc = (crc << 1) ^ POLY;
				} else {
					crc <<= 1;
				}
			}
			CRC24Q_TABLE[i] = crc & 0xFFFFFF;
		}
	}

	/**
	 * Compute the CRC-24Q over a byte array.
	 *
	 * @param data   Byte array containing the message (starting from preamble 0xD3)
	 * @param length Number of bytes to include in the CRC (everything except final
	 *               3-byte CRC)
	 * @return 24-bit CRC as an int
	 */
	public static int computeCRC24Q(byte[] data, int length) {
		int crc = 0;

		for (int i = 0; i < length; i++) {
			int b = data[i] & 0xFF;
			int index = ((crc >> 16) ^ b) & 0xFF;
			crc = ((crc << 8) ^ CRC24Q_TABLE[index]) & 0xFFFFFF;
		}

		return crc;
	}
}
