package tnefern.honeybeeframework.delegator;

public class ReceivedResults {
	public String stringResults = "";
	public int intResults = -1;
	public Object resultData = null;
	public int resultMode = -1;
	public String fromWorker = "";

	public ReceivedResults() {

	}

	public ReceivedResults(int pMode) {
		this.resultMode = pMode;
	}
	
	public ReceivedResults(int pMode, String pFrom) {
		this.resultMode = pMode;
		this.fromWorker = pFrom;
	}
}
