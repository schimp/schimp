package uk.ac.bham.cs.schimp.source;

import uk.ac.bham.cs.schimp.lang.Program;

public abstract class Source {
	
	public Source() { }
	
	public abstract Program parse();
	
}
