package tnefern.honeybeeframework.common;

import java.io.Serializable;

public class CompletedJob implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8963635274308113567L;

	/**
	 * denotes the mode of the data stored
	 * 
	 * @serial
	 */
	public int mode = -1;

	/**
	 * String value
	 * 
	 * @serial
	 */
	public String stringValue = null;
	/**
	 * integer array value
	 * 
	 * @serial
	 */
	public int[] intArrayValue = null;
	/**
	 * integer value
	 * 
	 * @serial
	 */
	public int intValue = -1;
	/**
	 * If the results are in multiple modes, this array would contain which
	 * types of data will be transmitted, in which order. For example, if the
	 * results to be sent are a String followed by an int array, the first
	 * element will give the mode as String, and the second mode will give the
	 * mode as int array.
	 * 
	 * @serial
	 */
	public int[] mixedModeArray = null;
	public transient Object data = null;
	public String id = null;
	
	public CompletedJob(){
		
	}

	
	public CompletedJob(int pMode, String pString, int pInd, Object pData){
		this.mode = pMode;
		this.stringValue  = pString;
		this.intValue = pInd;
		this.data = pData;
	}

//	public int[][] getResultSet() {
//		return resultSet;
//	}
//
//	public void setResultSet(int[][] resultSet) {
//		this.resultSet = resultSet;
//	}

//	public Object clone(){
//		CompletedJob cj = new CompletedJob();
//		cj.stringValue = this.stringValue;
//		cj.intValue = this.intValue;
//		cj.mode = this.mode;
////		cj.data = this.data.;
////		if(this.results!=null){
////			cj.results = new byte[this.results.length];
////			System.arraycopy(this.results, 0, cj.results, 0, this.results.length);
////		}
//		
//		return cj;
//	}
}
