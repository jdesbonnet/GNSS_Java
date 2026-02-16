package ie.strix.gnss.nmea;

import lombok.Getter;

/**
 * Placeholder sentence type for valid-but-unsupported NMEA sentence IDs.
 */
@Getter
public class UnknownSentence extends Sentence {

	private final String sentenceId;

	public UnknownSentence(String sentence) throws ChecksumFailException {
		super(sentence);
		sentenceId = sentence.substring(3, 6);
	}
}
