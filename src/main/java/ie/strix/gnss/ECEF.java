package ie.strix.gnss;

public class ECEF {

	// WGS84 constants
	private static final double A = 6378137.0; // Semi-major axis (meters)
	private static final double F = 1 / 298.257223563; // Flattening
	private static final double E2 = F * (2 - F); // Square of eccentricity

	/**
	 * Converts ECEF coordinates (x, y, z) to geodetic coordinates (lat, lon, alt).
	 * 
	 * @param x ECEF X coordinate in meters
	 * @param y ECEF Y coordinate in meters
	 * @param z ECEF Z coordinate in meters
	 * @return double[] with [latitudeDegrees, longitudeDegrees, altitudeMeters]
	 */
	public static double[] ecefToLatLngAlt(double x, double y, double z) {
		double r = Math.sqrt(x * x + y * y);
		double lon = Math.atan2(y, x); // Radians

		double b = A * (1 - F); // Semi-minor axis
		double theta = Math.atan2(z * A, r * b);

		double sinTheta = Math.sin(theta);
		double cosTheta = Math.cos(theta);

		double lat = Math.atan2(z + E2 * b * sinTheta * sinTheta * sinTheta,
				r - E2 * A * cosTheta * cosTheta * cosTheta); // Radians

		double sinLat = Math.sin(lat);
		double N = A / Math.sqrt(1 - E2 * sinLat * sinLat);
		double alt = r / Math.cos(lat) - N;

		// Convert radians to degrees
		double latDeg = Math.toDegrees(lat);
		double lonDeg = Math.toDegrees(lon);

		return new double[] { latDeg, lonDeg, alt };
	}
}
