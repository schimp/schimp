package uk.ac.bham.cs.schimp.source;

public class SourceRange {
	
	private int startLine;
	private int startColumn;
	private int endLine = -1;
	private int endColumn = -1;
	
	public SourceRange(int startLine, int startColumn) {
		this.startLine = startLine;
		this.startColumn = startColumn;
	}
	
	public SourceRange(int startLine, int startColumn, int endLine, int endColumn) {
		this.startLine = startLine;
		this.startColumn = startColumn;
		this.endLine = endLine;
		this.endColumn = endColumn;
	}
	
	public String toString() {
		return startLine + ":" + startColumn + "-" + (endLine != -1 && endColumn != -1 ? endLine + ":" + endColumn : "");
	}
	
}
