package tnefern.honeybeeframework.common;

import android.text.TextUtils;

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

	/******************* FIELDS FOR STATS **********************/
	private long jobStartTime;
	private long jobEndTime;
	/**
	 * Can be either null when completed by delegator or
	 * the id/address of the worker who completed the job
	 */
	private String completedBy;
	private long zipStartTime;
	private long zipEndTime;
	private long zipTime;
	private long transmitStartTime;
	private long transmitEndTime;
	private long transmissionTime;
	private long resultReceivedTime;
	private long resultProcessedTime;
	private long computationTime;
	/******************* FIELDS FOR STATS **********************/

	public CompletedJob(){
		
	}

	
	public CompletedJob(int pMode, String pString, int pInd, Object pData){
		this.mode = pMode;
		this.stringValue  = pString;
		this.intValue = pInd;
		this.data = pData;
	}

	private boolean completedByDelegator() {
		return TextUtils.isEmpty(completedBy);
	}

	/**
	 * When done by Delegator jobStartTime is the time job started and
	 * when done by workers, zipStartTime is the time job started (from Delegator's perspective)
	 * @return job start time in millis
	 */
	public long getJobStartTime() {
		if (completedByDelegator()) {
			return jobStartTime;
		} else {
			return zipStartTime;
		}
	}

	public void setJobStartTime(long jobStartTime) {
		this.jobStartTime = jobStartTime;
	}

	public long getJobEndTime() {
		return jobEndTime;
	}

	public void setJobEndTime(long jobEndTime) {
		this.jobEndTime = jobEndTime;
	}

	public void setCompletedBy(String workerId) {
		this.completedBy = workerId;
	}

	public long getJobDuration() {
		return jobEndTime - jobStartTime;
	}

	public String getCompletedBy() {
		if (completedByDelegator()) {
			return "Delegator";
		} else {
			return completedBy;
		}
	}

	public long getZipStartTime() {
		return zipStartTime;
	}

	public void setZipStartTime(long zipStartTime) {
		this.zipStartTime = zipStartTime;
	}

	public long getZipEndTime() {
		return zipEndTime;
	}

	public void setZipEndTime(long zipEndTime) {
		this.zipEndTime = zipEndTime;
	}

	public long getZipTime() {
		return zipTime;
	}

	public void setZipTime(long zipTime) {
		this.zipTime = zipTime;
	}

	public long getTransmitStartTime() {
		return transmitStartTime;
	}

	public void setTransmitStartTime(long transmitStartTime) {
		this.transmitStartTime = transmitStartTime;
	}

	public long getTransmitEndTime() {
		return transmitEndTime;
	}

	public void setTransmitEndTime(long transmitEndTime) {
		this.transmitEndTime = transmitEndTime;
	}

	public long getTransmissionTime() {
		return transmissionTime;
	}

	public void setTransmissionTime(long transmissionTime) {
		this.transmissionTime = transmissionTime;
	}

	public long getResultReceivedTime() {
		return resultReceivedTime;
	}

	public void setResultReceivedTime(long resultReceivedTime) {
		this.resultReceivedTime = resultReceivedTime;
	}

	public long getResultProcessedTime() {
		return resultProcessedTime;
	}

	public void setResultProcessedTime(long resultProcessedTime) {
		this.resultProcessedTime = resultProcessedTime;
	}

	public long getComputationTime() {
		return computationTime;
	}

	public void setComputationTime(long computationTime) {
		this.computationTime = computationTime;
	}

	public static String getStatsTitle() {
		return "Job_id" + "," +
				"Start_time" + "," +
				"End_time" + "," +
				"Job_duration" + "," +
				"Completed_by" + "," +
				"Zip_start_time" + "," +
				"Zip_end_time" + "," +
				"Zip_time(Avg)" + ","+
				"Transmission_start_time" + "," +
				"Transmission_end_time" + "," +
				"Transmission_time(Avg)" + "," +
				"Result_received_time" + "," +
				"Result_processed_time" + "," +
				"Computation_time";
	}
	public String getStats() {
		return stringValue + "," +
				getJobStartTime() + "," +
				getJobEndTime() + "," +
				getJobDuration() + "," +
				getCompletedBy() + "," +
				getZipStartTime() + "," +
				getZipEndTime() + "," +
				getZipTime() + "," +
				getTransmitStartTime() + "," +
				getTransmitEndTime() + "," +
				getTransmissionTime() + "," +
				getResultReceivedTime() + "," +
				getResultProcessedTime() + "," +
				getComputationTime();
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
