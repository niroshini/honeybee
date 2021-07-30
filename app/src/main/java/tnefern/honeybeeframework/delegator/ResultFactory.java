package tnefern.honeybeeframework.delegator;

import java.util.ArrayList;
import java.util.HashMap;

import tnefern.honeybeeframework.common.CompletedJob;

public abstract class ResultFactory {
	private int jobsByDelegator = 0;
	private String compareString = "";
	private float speedup = 0.0f;

	private HashMap<String, Integer> doneJobsMap = new HashMap<String, Integer>();// how
																					// many
																					// jobs
																					// each
																					// worker
																					// has
																					// done

	public abstract boolean checkResults(ArrayList<CompletedJob> pdone);

	public abstract void addToMap(String pId, Object pResult);

	public float getSpeedup() {
		return speedup;
	}

	public void setSpeedup(float speedup) {
		this.speedup = speedup;
	}

	public void incrementDeleDoneJobs() {
		this.jobsByDelegator++;
	}

	public int getJobsByDelegator() {
		return this.jobsByDelegator;
	}

	public String getCompareString() {
		return compareString;
	}

	public void setCompareString(String compareString) {
		this.compareString = compareString;
	}

	/**
	 * String name of the worker device Integer how many jobs it did
	 * 
	 * @return
	 */
	public HashMap<String, Integer> getDoneJobMap() {
		return this.doneJobsMap;
	}

	public void addToDoneDobMap(String pWorkerName) {
		if (pWorkerName != null) {
			Integer val = (Integer) this.doneJobsMap.get(pWorkerName);
			if (val == null) {
				this.doneJobsMap.put(pWorkerName, Integer.valueOf(1));
			} else {
				int intval = val.intValue();
				intval++;
				this.doneJobsMap.put(pWorkerName, Integer.valueOf(intval));
			}
		}
	}

}
