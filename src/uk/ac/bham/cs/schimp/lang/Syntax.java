package uk.ac.bham.cs.schimp.lang;

import org.apache.commons.lang3.StringUtils;
import uk.ac.bham.cs.schimp.source.Source;
import uk.ac.bham.cs.schimp.source.SourceRange;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public abstract class Syntax {
	
	private Source source;
	private SourceRange sourceRange;
	
	public Syntax() { }
	
	public Syntax(Source source, SourceRange sourceRange) {
		setSource(source);
		setSourceRange(sourceRange);
	}
	
	public void setSource(Source source) {
		this.source = source;
	}
	
	public void setSourceRange(SourceRange sourceRange) {
		this.sourceRange = sourceRange;
	}
	
	public abstract void check(SyntaxCheckContext context) throws SyntaxException;
	
	public String toString() {
		return this.toString(0);
	}
	
	public String toSourceString() {
		return this.toSourceString(0);
	}
	
	protected String indentation(int indent) {
		return StringUtils.repeat("  ", indent);
	}
	
	public abstract String toString(int indent);
	
	public abstract String toSourceString(int indent);
	
}
