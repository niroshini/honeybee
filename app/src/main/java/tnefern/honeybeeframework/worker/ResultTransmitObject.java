package tnefern.honeybeeframework.worker;

import java.io.Serializable;

public class ResultTransmitObject implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1603614845072630999L;

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
	
	/**
	 * ID value
	 * 
	 * @serial
	 */
	public String identifier = null;
}
