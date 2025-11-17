package ie.strix.gnss.ntrip.client;

import java.util.concurrent.Callable;

import lombok.extern.slf4j.Slf4j;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Slf4j
@Command(name = "ntrip_client", 
mixinStandardHelpOptions = true, 
version = "0.1", 
description = "Connect to NTRIP service")

public class Main implements Callable<Integer> {
	
	@Option(names = {"--ntrip-address"},description = "address of NTRIP service: ntrip://username:password@host:port/mountpoint")
	private String ntripAddress;

	public static void main(String... args) {

		// Configure slf4j-simple logging programmatically
		// TODO: doesn't work.
		System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
		System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd'T'HH:mm:ss'Z'");
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
		System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd HH:mm:ss");

		
		int exitCode = new CommandLine(new Main()).execute(args);
		System.exit(exitCode);
	}

	@Override
	public Integer call() throws Exception {

		log.info("call()");	
	
		NtripClient client = new NtripClient(ntripAddress);
		
		client.connect();
		
		return 1;
	}


}
