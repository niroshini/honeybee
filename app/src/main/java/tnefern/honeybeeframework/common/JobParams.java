package tnefern.honeybeeframework.common;

/**
 * paramMode will denote if the params are delivered in String or File format.
 * if String, only a String message will be sent to the workers. if File, first
 * a String message will be sent
 * 
 * @author tnfernando
 * 
 */
public class JobParams {
	public int paramMode = -1;
	public String paramsString = null;
//	public File paramFile = null;
	public Object paramObject = null;

	public JobParams(int pMode) {
		this.paramMode = pMode;
	}
	
}
