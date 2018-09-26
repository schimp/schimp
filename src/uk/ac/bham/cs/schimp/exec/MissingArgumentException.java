package uk.ac.bham.cs.schimp.exec;

public class MissingArgumentException extends Exception {

	private static final long serialVersionUID = 4707549372408270384L;

	public MissingArgumentException() {
		super();
	}

	public MissingArgumentException(String message) {
		super(message);
	}

	public MissingArgumentException(Throwable cause) {
		super(cause);
	}

	public MissingArgumentException(String message, Throwable cause) {
		super(message, cause);
	}

}