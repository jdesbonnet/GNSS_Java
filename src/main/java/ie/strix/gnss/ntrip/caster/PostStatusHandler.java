package ie.strix.gnss.ntrip.caster;

import java.io.IOException;
import java.io.InputStream;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * Handlers non-NTRIP (regular HTTP) requests.
 *
 */

@Slf4j
class PostStatusHandler implements HttpHandler {
	/**
	 * 
	 */
	private final NtripCaster ntripCaster;

	/**
	 * @param ntripCaster
	 */
	PostStatusHandler(NtripCaster ntripCaster) {
		this.ntripCaster = ntripCaster;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		
		String method = exchange.getRequestMethod();

		log.info("received request on HTTP port: {} {}", method, exchange.getRequestURI());
		
		
		// Any POST is a status update from a station
		if ("POST".equalsIgnoreCase(method)) {
			InputStream in = exchange.getRequestBody();
			//String method3 = exchange.getReques
			String json = new String(in.readAllBytes());
			log.info("status={}",json);
			Gson gson = new Gson();
			BaseStationStatusDTO stationUpdate = gson.fromJson(json,BaseStationStatusDTO.class);
			BaseStation station = this.ntripCaster.stations.get(stationUpdate.getStationId());
			station.addUpdate(stationUpdate);
		
			
			// Find base station and attache it.
		}
		exchange.sendResponseHeaders(405, -1);
		return;
	}

}