package tnefern.honeybeeframework.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.util.Log;

/**
 * Records the time durations spent on establishing the piconet, setting up the
 * job pool, transmitting job params to worker nodes, time spent on stealing,
 * and time spent receiving the finished product.
 * 
 * @author tnfernando
 * 
 */
public class TimeMeter {
	private static TimeMeter theInstance = null;

	/**
	 * should contain one entry per each worker, giving the time to transmit
	 * initial job parameters.
	 */
	private List<JobInfo> sendJobTimes = null;

	/**
	 * time to turn bluetooth on, search for devices, and establish connections
	 */
	private long piconetTime = 0;

	/**
	 * time to read an INIT_STEAL signal from a worker and run the victim thread
	 */
	private List<JobInfo> victimTimes = null;

	/**
	 * contain one entry per each time the delegator read stolen job params from
	 * a worker
	 */
	private List<JobInfo> readStolenParams = null;

	private long jobPoolSetUpTime = 0;

	/**
	 * time to do own jobs
	 */
	private long calculateTime = 0;

	/**
	 * time to send the INIT_STEAL signal to all eligible workers
	 */
	private long stealFromWorkersTime = 0;

	/**
	 * time to read the results back from workers
	 */
	private long readingResults = 0;

	private long overallTotalTime = 0;

	private long timegapfromdelethreadStart = 0;
	// From this point on variables applicable to Workers only

	/**
	 * time to send the results to the delegator (applicable to Worker only)
	 */
	private long transmitJobs = 0;

	private long totalWorkerTime = 0;

	private long totalByteConversionTime = 0;

	private long zipTime = 0;

	/**
	 * this is the time that interface Delegate::initJobs() is called. In
	 * FaceMatch, this is called in FaceMatchActivity::initJobs().
	 */
	private long initJobsTime = 0;

	private ArrayList<JobInfo> jobCalTimes = null;
	
	private float batteryLevel = -1.0f;
	

	

	private TimeMeter() {
		sendJobTimes = Collections.synchronizedList(new ArrayList<JobInfo>());
		victimTimes = Collections.synchronizedList(new ArrayList<JobInfo>());
		readStolenParams = Collections
				.synchronizedList(new ArrayList<JobInfo>());
		jobCalTimes = new ArrayList<JobInfo>();
	}

	public static TimeMeter getInstance() {
		if (theInstance == null) {
			theInstance = new TimeMeter();
		}
		return theInstance;
	}

	public void setPiconetTime(long pT) {
		this.piconetTime = pT;
	}

	public void setDeleThreadGapTime(long pT) {
		this.timegapfromdelethreadStart = pT;
	}

	public long getDeleThreadGapTime() {
		return this.timegapfromdelethreadStart;
	}

	public long getPiconetTime() {
		return this.piconetTime;
	}

	public void addSendJobTime(JobInfo pSJ) {
		sendJobTimes.add(pSJ);
		// Log.d("Time meter", "added send job time "+pSJ.sendTime);
	}

	public long getTotalOfSendJobTime() {
		Iterator<JobInfo> iter = this.sendJobTimes.iterator();
		long time = 0;

		while (iter.hasNext()) {
			JobInfo s = iter.next();
			time += s.sendTime;
			// Log.d("Time meter",
			// "getTotalOfJobInfoTime() "+s.sendTime+"  total "+time);
		}
		return time;
	}

	public void addToVictimTime(JobInfo pSJ) {
		victimTimes.add(pSJ);
	}

	public long getTotalOfVictimTime() {
		Iterator<JobInfo> iter = this.victimTimes.iterator();
		long time = 0;

		while (iter.hasNext()) {
			JobInfo s = iter.next();
			time += s.sendTime;
		}
		return time;
	}

	public long getTotalOfReadStolenParamTime() {
		Iterator<JobInfo> iter = this.readStolenParams.iterator();
		long time = 0;

		while (iter.hasNext()) {
			JobInfo s = iter.next();
			time += s.sendTime;
		}
		return time;
	}

	public void setJobPoolSetTime(long pT) {
		this.jobPoolSetUpTime = pT;
		// Log.d("TimeMeter", "setJobPoolSetTime() "+pT);
	}

	public long getJobPoolSetTime() {
		return this.jobPoolSetUpTime;
	}

	public void addToCalculateTime(long pT) {
		this.calculateTime += pT;
//		Log.d("calculateTime", " calculateTime = " + calculateTime);
	}

	public long getCalculateTime() {
		return this.calculateTime;
	}

	public void logStealFromWorkersTime(long pT) {
		stealFromWorkersTime += pT;
	}

	public long getStealFromWorkersTime() {
		return stealFromWorkersTime + getTotalOfReadStolenParamTime();
	}

	public long getTotalStealTime() {
		return stealFromWorkersTime + getTotalOfReadStolenParamTime()
				+ getTotalOfVictimTime();
	}

	public void addReadStolenParamTime(JobInfo pSJ) {
		readStolenParams.add(pSJ);
	}

	public void addToReadingResults(long readingResults) {
		this.readingResults += readingResults;
	}

	public long getReadingResults() {
		return readingResults;
	}

	public void addToTransmitJobTime(long pTime) {
		this.transmitJobs += pTime;
	}

	public long getTransmitJobsTime() {
		return this.transmitJobs;
	}

	public void setTotalWorkerTime(long pT) {
		this.totalWorkerTime = pT;
	}

	public long getTotalWorkerTime() {
		return this.totalWorkerTime;
	}

	public void addToTotalByteConversionTime(long pT) {
		this.totalByteConversionTime += pT;
	}

	public long getTotalByteConversionTime() {
		return this.totalByteConversionTime;
	}

	public void addToJobCalTimes(String pS, Long pT) {
		this.jobCalTimes.add(new JobInfo(pS, pT));
	}

	public void addToJobCalTimes(int pN, Long pT) {
		this.jobCalTimes.add(new JobInfo(pN + "", pT));
	}

	public ArrayList<JobInfo> getJobCalTimes() {
		return this.jobCalTimes;
	}

	public void addToOverallTotalTime(long readingResults) {
		this.overallTotalTime += readingResults;
	}

	public long getOverallTotalTime() {
		return this.overallTotalTime;
	}

	public void setOverallInitialTime(long pTime) {
		this.overallTotalTime = pTime;
	}

	public void addToZipTime(long readingResults) {
		this.zipTime += readingResults;
	}

	public long getZipTime() {
		return this.zipTime;
	}

	public void setZipTime(long pTime) {
		this.zipTime = pTime;
	}

	public long getInitJobsTime() {
		return initJobsTime;
	}

	public void setInitJobsTime(long initJobsTime) {
		this.initJobsTime = initJobsTime;
	}
	
	public float getBatteryLevel() {
		return batteryLevel;
	}

	public void setBatteryLevel(float batteryLevel) {
		this.batteryLevel = batteryLevel;
	}
}
