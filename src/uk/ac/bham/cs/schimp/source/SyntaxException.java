package uk.ac.bham.cs.schimp.source;

public class SyntaxException extends Exception {
	
	private static final long serialVersionUID = 2017_11_26_001L;
	
	private SourceRange errorPosition;
	
	public SyntaxException(SourceRange errorPosition, String reason) {
		super(reason);
		this.errorPosition = errorPosition;
	}
	
	public SyntaxException(String reason) {
		super(reason);
	}
	
}
