package tnefern.honeybeeframework.common;

import java.io.File;

public class Job {
	public String jobParams = null;//the 'i' in jobParam is the fromRow. we already know the partition size
	public int status = -1;
	public String id = "";
	public int index = -1;
	public File f = null;
	public Object o = null;
	public int mode = -1;
	public int stealMode = -1;
	
//	public int fromRow = -1;//only need fromRow since we already know the partition size
//	public int toRow = -1;//
	public Job(String pParam, int pStatus, int pMode, int pStealMode){
		this.jobParams = pParam;
		this.status = pStatus;
		this.mode = pMode;
		this.stealMode = pStealMode;
	}
	
	public Job(String pParam, int pStatus, int pMode){//facematch
		this.jobParams = pParam;
		this.status = pStatus;
		this.mode = pMode;
	}
	
	public Job(Object pParam, int pStatus, int pMode, int pStealMode){
		this.o = pParam;
		this.status = pStatus;
		this.mode = pMode;
		this.stealMode = pStealMode;
	}
	
	public boolean equals(Object pJ){
		if(pJ!=null){
			Job j = (Job)pJ;
//			if((j.index == this.index) && (j.jobParams.equals(this.jobParams))){
//				return true;
//			}
			if(j.id.equals(this.id)){
				return true;
			}
		}
		return false;
	}

}
