package uk.ac.bham.cs.schimp.exec;

public class OptionValueException extends Exception {
	
	private static final long serialVersionUID = 961777125878748792L;

	public OptionValueException() {
		super();
	}

	public OptionValueException(String message) {
		super(message);
	}

	public OptionValueException(Throwable cause) {
		super(cause);
	}

	public OptionValueException(String message, Throwable cause) {
		super(message, cause);
	}

}