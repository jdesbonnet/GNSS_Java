package ie.strix.gnss.ntrip.caster;

import java.io.IOException;
import java.io.OutputStream;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * Handlers non-NTRIP (regular HTTP) requests.
 *
 */

@Slf4j
class GetStationsHandler implements HttpHandler {
	/**
	 * 
	 */
	private final NtripCaster ntripCaster;

	/**
	 * @param ntripCaster
	 */
	GetStationsHandler(NtripCaster ntripCaster) {
		this.ntripCaster = ntripCaster;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		
		log.info("handle():");
		
		String method = exchange.getRequestMethod();

		log.info("received request on HTTP port: {} {}", method, exchange.getRequestURI());
		
		// Any GET will return full status information, no need to check path
		if ("GET".equalsIgnoreCase(method)) {
			String response = buildStationsJson();
			log.info("response={}",response);
			byte[] bytes = response.getBytes();
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.getResponseHeaders().add("Access-Control-Allow-Origin","*");
			exchange.sendResponseHeaders(200, bytes.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(bytes);
				os.close();
			}
			return;
		}
		

	}

	private String buildStationsJson() {
		Gson gson = new Gson();
		return gson.toJson(this.ntripCaster.listStations());
	}
}