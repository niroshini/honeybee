package tnefern.honeybeeframework.common;

import java.util.ArrayList;

public class JobsGiven {
	private String workerAddress = null;

	private ArrayList<Job> givenList = null;
	private long giventime = -1;

	public JobsGiven(String pAdr, ArrayList<Job> pJobs, long pgiventime) {
		this.workerAddress = pAdr;
		this.givenList = pJobs;
		this.giventime = pgiventime;
	}
	
	public JobsGiven(String pAdr, Job[] pJobs, long pgiventime) {
		this.workerAddress = pAdr;
		
		this.givenList = new ArrayList<Job>();
		for(Job j :pJobs){
			this.givenList.add(j);
		}
		this.giventime = pgiventime;
	}

	public String getWorkerAddress() {
		return workerAddress;
	}

	public void setWorkerAddress(String workerAddress) {
		this.workerAddress = workerAddress;
	}

	public ArrayList<Job> getGivenList() {
		return givenList;
	}

	public void setGivenList(ArrayList<Job> givenList) {
		this.givenList = givenList;
	}
	
	public long getWorkerGivenTime() {
		return giventime;
	}
}
