package ie.strix.gnss.ntrip.client;

import java.util.concurrent.Callable;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


/**
 * Connect to NTRIP server and obtain correction stream.
 * 
 * @author Joe Desbonnet
 *
 */
@Slf4j
@Command(name = "CmdNtripClient", mixinStandardHelpOptions = true, version = "1.0", description = "Connect to NTRIP server and obtain correction stream.")
public class CmdNtripClient implements Callable<Integer> {

	@Parameters(index = "0", description = "address of ntrip server in form ntrip://user:pw@host:port/mountpoint")
	private String ntripAddress;


	//@Option(names="--ntrip-address", description = "address of ntrip server in form ntrip://user:pw@host:port/mountpoint")
	//private String ntripAddress;
	
	@Option(names="--gga", description = "GGA line to send (usually required by NTRIP server before it sends correction stream")
	private String ggaSentence;
	
	public static void main(String... args) {
		int exitCode = new CommandLine(new CmdNtripClient()).execute(args);
		System.exit(exitCode);
	}

	@Override
	public Integer call() throws Exception {
		return 1;
	}

}