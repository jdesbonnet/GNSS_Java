package ie.strix.gnss.ntrip;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class NtripUri {
	private static final int DEFAULT_PORT = 2101;

	private final String scheme; // always "ntrip"
	private final String username; // decoded
	private final String password; // decoded
	private final String host; // as-is (lowercased by URI)
	private final int port; // -1 if absent, else 1..65535
	private final String mountpoint; // decoded, without leading '/'

	private NtripUri(String scheme, String username, String password, String host, int port, String mountpoint) {
		this.scheme = scheme;
		this.username = username;
		this.password = password;
		this.host = host;
		this.port = port;
		this.mountpoint = mountpoint;
	}

	/** Parse an NTRIP URI. Throws IllegalArgumentException on invalid input. */
	public static NtripUri parse(String input) {
		Objects.requireNonNull(input, "input");
		final URI uri;
		try {
			uri = new URI(input);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid NTRIP URI syntax: " + e.getMessage(), e);
		}

		// Scheme
		String scheme = uri.getScheme();
		if (scheme == null || !scheme.equalsIgnoreCase("ntrip")) {
			throw new IllegalArgumentException("Expected scheme 'ntrip', got: " + scheme);
		}
		scheme = "ntrip";

		// Host
		String host = uri.getHost();
		if (host == null || host.isEmpty()) {
			// If parser couldn’t find host (e.g., raw authority), try to extract it
			// manually
			String authority = uri.getRawAuthority();
			if (authority != null) {
				int at = authority.lastIndexOf('@');
				String hostPort = (at >= 0 ? authority.substring(at + 1) : authority);
				int colon = hostPort.lastIndexOf(':');
				host = (colon >= 0 ? hostPort.substring(0, colon) : hostPort);
			}
		}
		if (host == null || host.isEmpty()) {
			throw new IllegalArgumentException("Missing host in NTRIP URI.");
		}

		// Port
		int port = uri.getPort(); // -1 if absent
		if (port == -1) {
			port = DEFAULT_PORT; // Allow omission; NTRIP default is 2101
		} else if (port < 1 || port > 65535) {
			throw new IllegalArgumentException("Port out of range: " + port);
		}

		// Userinfo -> username/password
		String rawUserInfo = uri.getRawUserInfo();
		if (rawUserInfo == null || rawUserInfo.isEmpty()) {
			throw new IllegalArgumentException("Missing credentials (username:password) in NTRIP URI.");
		}
		String username;
		String password;
		int firstColon = rawUserInfo.indexOf(':');
		if (firstColon < 0) {
			throw new IllegalArgumentException("User info must be 'username:password'.");
		} else {
			String rawUser = rawUserInfo.substring(0, firstColon);
			String rawPass = rawUserInfo.substring(firstColon + 1); // keep remaining (password may contain ':', encoded
																	// as %3A)
			username = decodePct(rawUser);
			password = decodePct(rawPass);
			if (username.isEmpty())
				throw new IllegalArgumentException("Username is empty.");
			if (password.isEmpty())
				throw new IllegalArgumentException("Password is empty.");
		}

		// Path -> mountpoint (strip leading '/')
		String rawPath = uri.getRawPath();
		String mountpoint;
		if (rawPath == null || rawPath.isEmpty() || rawPath.equals("/")) {
			throw new IllegalArgumentException("Missing mountpoint path.");
		} else {
			if (rawPath.startsWith("/"))
				rawPath = rawPath.substring(1);
			// Per NTRIP, path is a single segment (the mountpoint)
			if (rawPath.contains("/")) {
				throw new IllegalArgumentException("Mountpoint must be a single path segment.");
			}
			mountpoint = decodePct(rawPath);
			if (mountpoint.isEmpty())
				throw new IllegalArgumentException("Mountpoint is empty.");
		}

		return new NtripUri(scheme, username, password, host, port, mountpoint);
	}

	/** Percent-decode (supports %XX), does NOT treat '+' specially. */
	private static String decodePct(String s) {
		// Minimal, safe percent-decoder for RFC 3986 without '+' as space semantics.
		byte[] bytes = new byte[s.length()];
		int bi = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '%' && i + 2 < s.length()) {
				int hi = hex(s.charAt(i + 1));
				int lo = hex(s.charAt(i + 2));
				if (hi >= 0 && lo >= 0) {
					bytes[bi++] = (byte) ((hi << 4) + lo);
					i += 2;
					continue;
				}
			}
			// ASCII char as bytes; for non-ASCII, keep as UTF-16 then re-encode properly:
			// simplest approach: build a StringBuilder then getBytes; but here we only pass
			// through ASCII or %XX
			// To keep it correct for non-ASCII literals, fall back to StringBuilder path:
			// We'll just append and at the end, return builder.
			// To avoid complexity, handle simple case:
			// We'll rebuild via String when non-ASCII appears.
			if (c > 0x7F) {
				// fallback to java.net URI decoder behavior by round-trip
				return URI.create("scheme://x@" + "h" + "/" + s).getPath().substring(1); // shouldn't happen for typical
																							// NTRIP
			}
			bytes[bi++] = (byte) c;
		}
		return new String(bytes, 0, bi, StandardCharsets.UTF_8);
	}

	private static int hex(char c) {
		if (c >= '0' && c <= '9')
			return c - '0';
		if (c >= 'A' && c <= 'F')
			return c - 'A' + 10;
		if (c >= 'a' && c <= 'f')
			return c - 'a' + 10;
		return -1;
	}

	/** Build a proper ntrip://… URI string with percent-encoding where needed. */
	public String toUriString() {
		String rawUser = pctEncodeUserInfo(username);
		String rawPass = pctEncodeUserInfo(password);
		String rawPath = "/" + pctEncodePathSegment(mountpoint);
		try {
			URI uri = new URI(scheme, rawUser + ":" + rawPass, host, port == -1 ? DEFAULT_PORT : port, rawPath, null,
					null);
			return uri.toASCIIString();
		} catch (URISyntaxException e) {
			// Should not happen if we encoded correctly
			throw new IllegalStateException("Failed to build URI: " + e.getMessage(), e);
		}
	}

	/** Masked, human-friendly string (password hidden). */
	@Override
	public String toString() {
		String userPart = (username == null ? "" : username) + ":****";
		return "ntrip://" + userPart + "@" + host + ":" + (port == -1 ? DEFAULT_PORT : port) + "/" + mountpoint;
	}

	// Percent-encode helpers (RFC 3986 unreserved = ALPHA / DIGIT / "-" / "." / "_"
	// / "~")
	private static String pctEncodeUserInfo(String s) {
		return pctEncode(s, true);
	}

	private static String pctEncodePathSegment(String s) {
		return pctEncode(s, false);
	}

	private static String pctEncode(String s, boolean userInfo) {
		StringBuilder out = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (isUnreserved(c)) {
				out.append(c);
			} else if (!userInfo && (c == '@')) {
				// '@' is allowed in path-segment, but not safe in userinfo
				out.append(c);
			} else {
				byte[] bytes = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
				for (byte b : bytes) {
					out.append('%');
					int v = b & 0xFF;
					String hex = Integer.toHexString(v).toUpperCase();
					if (hex.length() == 1)
						out.append('0');
					out.append(hex);
				}
			}
		}
		return out.toString();
	}

	private static boolean isUnreserved(char c) {
		return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '.'
				|| c == '_' || c == '~';
	}

	// Getters
	public String getScheme() {
		return scheme;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port == -1 ? DEFAULT_PORT : port;
	}

	public String getMountpoint() {
		return mountpoint;
	}

	// Value semantics
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof NtripUri))
			return false;
		NtripUri n = (NtripUri) o;
		return getPort() == n.getPort() && scheme.equals(n.scheme) && username.equals(n.username)
				&& password.equals(n.password) && host.equalsIgnoreCase(n.host) && mountpoint.equals(n.mountpoint);
	}

	@Override
	public int hashCode() {
		return Objects.hash(scheme, username, password, host.toLowerCase(), getPort(), mountpoint);
	}
}
