package ie.strix.gnss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ie.strix.gnss.nmea.ChecksumFailException;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class TestTrack {

	@DisplayName("Track Interpolation")
	@Test
	public void testTrackInterpolation() throws ChecksumFailException {
		PVT pvt0 = new PVT(1000, 10,10,0);
		PVT pvt1 = new PVT(2000, 11,11,1);
		PVT pvt2 = new PVT(3000, 12,12,2);
		PVT pvt3 = new PVT(4000, 13,13,3);
		
		pvt0.setHeading(0.0);
		pvt1.setHeading(90.0);
		pvt2.setHeading(180.0);
		pvt3.setHeading(270.0);
		
		
		pvt0.setPitch(10.0);
		pvt1.setPitch(20.0);
		pvt2.setPitch(30.0);
		pvt3.setPitch(40.0);
		
		
		Track track = new Track();
		track.addPvt(pvt0);
		track.addPvt(pvt1);
		track.addPvt(pvt2);
		track.addPvt(pvt3);

		// This is beyond track timerange
		PVT pvt5000 = track.interpolate(5000);
		assertNull(pvt5000);
		
		PVT pvt1500 = track.interpolate(1500);
		assertNotNull(pvt1500);
		assertEquals(10.5,pvt1500.getLatitude());
		assertEquals(10.5,pvt1500.getLongitude());
		assertEquals(0.5,pvt1500.getAltitude());
		assertEquals(45, pvt1500.getHeading());
		assertEquals(15, pvt1500.getPitch());

		PVT pvt2500 = track.interpolate(2500);
		assertNotNull(pvt2500);
		assertEquals(11.5,pvt2500.getLatitude());
		assertEquals(11.5,pvt2500.getLongitude());
		assertEquals(1.5,pvt2500.getAltitude());
		assertEquals(135, pvt2500.getHeading());
		assertEquals(25, pvt2500.getPitch());


	}
}
