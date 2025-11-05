package ie.strix.gnss.rtcm;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * TODO: validate
 * 
 * Minimal RTCM 3.x parser + CRC-24Q validator. - Finds frames in a byte stream
 * - Validates CRC-24Q (poly 0x1864CFB, init 0, no refin/out, no xorout) -
 * Parses message number (first 12 bits of payload) and station ID (next 12
 * bits)
 */
public final class Rtcm3Parser {

	public static final int PREAMBLE = 0xD3;
	private static final int CRC24Q_POLY = 0x01864CFB;
	private static final int[] CRC24Q_TABLE = new int[256];

	static {
		// Build CRC-24Q lookup table
		for (int i = 0; i < 256; i++) {
			int crc = i << 16;
			for (int b = 0; b < 8; b++) {
				crc <<= 1;
				if ((crc & 0x1000000) != 0)
					crc ^= CRC24Q_POLY;
				crc &= 0xFFFFFF;
			}
			CRC24Q_TABLE[i] = crc;
		}
	}

	/** One parsed RTCM frame. */
	public static final class Frame {
		public final int startIndex; // index in source where preamble (0xD3) was found
		public final int lengthField; // 10-bit length value (payload size in bytes)
		public final byte[] payload; // L bytes
		public final int crcTx; // 24-bit transmitted CRC (big-endian)
		public final int crcCalc; // 24-bit calculated CRC over [len bytes + payload]
		public final boolean crcOk; // whether calc == tx

		// Convenience fields decoded from payload where available
		public final Integer messageNumber; // first 12 bits
		public final Integer stationId; // next 12 bits (if present)

		public Frame(int startIndex, int lengthField, byte[] payload, int crcTx, int crcCalc) {
			this.startIndex = startIndex;
			this.lengthField = lengthField;
			this.payload = payload;
			this.crcTx = crcTx;
			this.crcCalc = crcCalc;
			this.crcOk = (crcTx == crcCalc);

			// Decode first 24 bits if available:
			Integer msg = null, stn = null;
			if (payload.length >= 3) {
				// messageNumber = first 12 bits; stationId = next 12 bits (common for many
				// 1001+ etc.)
				int b0 = payload[0] & 0xFF;
				int b1 = payload[1] & 0xFF;
				int b2 = payload[2] & 0xFF;
				int first24 = (b0 << 16) | (b1 << 8) | b2;
				msg = (first24 >>> 12) & 0x0FFF;
				stn = first24 & 0x0FFF;
			}
			this.messageNumber = msg;
			this.stationId = stn;
		}

		@Override
		public String toString() {
			return "RTCM3 Frame@" + startIndex + " len=" + lengthField + " msg="
					+ (messageNumber == null ? "?" : messageNumber) + " stn=" + (stationId == null ? "?" : stationId)
					+ " crcOk=" + crcOk;
		}
	}

	/**
	 * Scan the given byte array for RTCM3 frames. Returns a list of frames in
	 * order; malformed/partial frames are skipped safely.
	 */
	public static List<Frame> parseAll(byte[] data) {
		List<Frame> out = new ArrayList<>();
		int i = 0;
		while (i + 6 <= data.length) { // minimal frame size
			if ((data[i] & 0xFF) != PREAMBLE) {
				i++;
				continue;
			}

			if (i + 3 > data.length)
				break; // need 2 length bytes
			int b1 = data[i + 1] & 0xFF;
			int b2 = data[i + 2] & 0xFF;

			// Extract 10-bit length
			int L = ((b1 & 0x03) << 8) | b2; // keep only low 2 bits of b1
			int frameLen = 1 + 2 + L + 3; // preamble + len + payload + crc

			if (L > 1023) { // not possible per spec, resync
				i++;
				continue;
			}
			if (i + frameLen > data.length) {
				// partial frame at end; stop scanning
				break;
			}

			// Slice payload
			byte[] payload = new byte[L];
			System.arraycopy(data, i + 3, payload, 0, L);

			// Transmitted CRC (big-endian) is last 3 bytes
			int c0 = data[i + 3 + L] & 0xFF;
			int c1 = data[i + 3 + L + 1] & 0xFF;
			int c2 = data[i + 3 + L + 2] & 0xFF;
			int crcTx = (c0 << 16) | (c1 << 8) | c2;

			// Calculate CRC over b1,b2,payload
			int crcCalc = crc24q(data, i + 1, 2 + L);

			out.add(new Frame(i, L, payload, crcTx, crcCalc));
			i += frameLen; // advance to next possible frame
		}
		return out;
	}

	/** Compute CRC-24Q over a slice (offset..offset+len-1). */
	public static int crc24q(byte[] data, int offset, int len) {
		int crc = 0;
		for (int k = 0; k < len; k++) {
			int b = data[offset + k] & 0xFF;
			int idx = ((crc >>> 16) ^ b) & 0xFF;
			crc = ((crc << 8) & 0xFFFFFF) ^ CRC24Q_TABLE[idx];
		}
		return crc & 0xFFFFFF;
	}

	// ----- Example usage -----
	public static void main(String[] args) {
		// Example: concatenate bytes from a stream and pass them here.
		// (Put your real RTCM byte stream in 'buf'.)
		byte[] buf = new byte[] {
				// 0xD3, <len1>, <len2>, <payload...>, <crc24q:3>
				// Provide real data to test.
		};

		List<Frame> frames = parseAll(buf);
		for (Frame f : frames) {
			System.out.println(f);
			if (!f.crcOk) {
				System.out.printf("  CRC mismatch: tx=%06X calc=%06X%n", f.crcTx, f.crcCalc);
			} else {
				System.out.printf("  CRC OK. Message %d, Station %d%n", f.messageNumber, f.stationId);
			}
		}
	}
}
