package tnefern.honeybeeframework.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import tnefern.honeybeeframework.delegator.QueenBee;
import tnefern.honeybeeframework.delegator.WorkerInfo;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;

public class Slave implements Runnable {
	/**
	 * isSelf - indicates if the delegating device is also doing part of the
	 * job. If true, then delegating device is doing part of the work. If false,
	 * total load is offloaded.
	 */
	private boolean isSelf = false;

	/**
	 * isWorker - indicates whether this thread is a delegating device or a
	 * worker device.
	 */
	private boolean isWorker = false;
	private WorkerInfo workerInfo = null;
	private ArrayList<CompletedJob> doneJobs = null;
	private LinkedList<Job> jobList = null;
	// private int index = 0;
	protected JobParams workerParams = null;
	private boolean isNoMoreWork = false;
	/**
	 * activityClass = is the class name of activity that does what the app has
	 * to do. Instantiated via reflections.
	 */
	private String activityClassForDelegator = null;
	private String activityClassForWorker = null;
	/**
	 * stores the time to send job params to ith slave
	 */
	public long distTime = 0;
	protected Context parentActivity = null;
	private QueenBee queenbee = null;

	/**
	 * To post messages to the Delegator or Worker about the job list. For
	 * example, when someone steals from it, or when jobs are stolen from
	 * others.
	 */
//	private Handler handler = null;
	
//	private JobParams jobParams = null;

	/**
	 * If isWorker = true, that means this is called by a Worker. Otherwise,
	 * called by a Delegating device. Initially does the part of job allocated,
	 * and then will start stealing from others.
	 * 
	 * @param pSelf
	 * @param pInd
	 * @param pWorker
	 */
	public Slave(boolean pSelf, boolean pWorker, Context pAct,
			String pActivityClass, JobParams pMsg) {//facematch worker
		this.isSelf = pSelf;
		this.isWorker = pWorker;
		jobList = new LinkedList<Job>();
		doneJobs = new ArrayList<CompletedJob>();
		parentActivity = pAct;
		if (this.isWorker) {
			this.activityClassForWorker = pActivityClass;
		} else {
			this.activityClassForDelegator = pActivityClass;
		}
		this.workerParams = pMsg;

	}

	public Slave(boolean pSelf, boolean pWorker, Context pAct, JobParams pMsg, QueenBee pBee) {// delegator stolen work
		this.isSelf = pSelf;
		this.isWorker = pWorker;
		jobList = new LinkedList<Job>();
		doneJobs = new ArrayList<CompletedJob>();
		parentActivity = pAct;
		queenbee = pBee;
		this.workerParams = pMsg;
	}
	
	public Slave(boolean pSelf, boolean pWorker, Context pAct, QueenBee pBee) {//facematch initial delegator work
		this.isSelf = pSelf;
		this.isWorker = pWorker;
		jobList = new LinkedList<Job>();
		doneJobs = new ArrayList<CompletedJob>();
		parentActivity = pAct;
		queenbee = pBee;

	}
	/**
	 * Called only by Delegating thread. This constructor runs a thread to
	 * transmit the job params to a worker device related to a pInfo object and
	 * runs the transmitParams() method.
	 * 
	 * @param pInfo
	 * @param pInd
	 */
	public Slave(WorkerInfo pInfo, Context pAct) {
		this.workerInfo = pInfo;
		jobList = new LinkedList<Job>();
		doneJobs = new ArrayList<CompletedJob>();
		parentActivity = pAct;
	}
	

	void doOwnWork() throws IOException {
//		QueenBee bee = new QueenBee((Activity)parentActivity);//niro
		queenbee.populateWithJobs();
	}
	
	protected void doOwnWorkForWorker() throws IOException {
		if (parentActivity != null) {

			try {
				Class<?> handlerClass = FileFactory.getInstance().getClassFromName(
						this.activityClassForWorker, parentActivity);
				Intent camIntent = new Intent(parentActivity, handlerClass);
				camIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				parentActivity.startActivity(camIntent);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (NameNotFoundException e1) {
				e1.printStackTrace();
			}
		}

	}

	public void setJobs(LinkedList<Job> pList) {
		this.jobList = pList;
	}

	private void onStringParamMode(int pWorkMode, int pStealMode, int pStatus){
		String[] sArr = this.workerParams.paramsString.split(CommonConstants.PARTITION_BREAK);
		ArrayList<Job> jobT = new ArrayList<Job>();
		if (sArr != null && sArr.length > 0) {
			for (int i = 0; i < sArr.length; i++) {
				Job job = new Job(sArr[i], pStatus,pWorkMode, pStealMode );
				job.stealMode = pStealMode;
				
//				if(pStatus == CommonConstants.JOB_BEEN_STOLEN){fix this
//					if (job.jobParams != null) {
//						// first get the image file name
//						String fileName = job.jobParams;
//						fileName = fileName.substring(
//								fileName.lastIndexOf("/") + 1, fileName.length());
//						Job j = new Job(fileName, -1,
//								CommonConstants.READ_FILES_MODE);
//						j.id = fileName;
//						Job newJob = JobPool.getInstance().isJobExistsinOriginal(j);
//						if (newJob != null) {
//							JobPool.getInstance().addJob(newJob);
//						}
//
//					}
//				}else{
//					jobList.add(job);
//					jobT.add(job);
//				}
				
				jobList.add(job);
				jobT.add(job);
				
//				Log.d("assembleJobList", " sArr[" + i + "] = " + sArr[i]);
			}

		}
		
		JobPool.getInstance().addJobs(jobT);
	}
	/**
	 * In a worker, the string params are used to construct the job list
	 * 
	 * @param pMsg
	 */
	public void assembleJobList(int pMode, int pStatus) {
//		int mode = this.workerParams.paramMode;
		int mode = pMode;
//		if(pMode > 0){
//			mode = pMode;
//		}
		if(mode == CommonConstants.READ_STRING_MODE){
			onStringParamMode(CommonConstants.READ_STRING_MODE, CommonConstants.JOB_NOT_GIVEN,pStatus);
		}else if(mode == CommonConstants.READ_FILE_MODE){
			//do what ever needs to be done
			//if zip file, unzip
//			String extName = this.workerParams.paramsString;
			
			Job job = new Job(this.workerParams.paramsString, CommonConstants.JOB_NOT_GIVEN, CommonConstants.READ_FILE_MODE);
			jobList.add(job);
			JobPool.getInstance().addJob(job);
		}else if(mode == CommonConstants.READ_FILES_MODE){
			if(this.workerParams.paramObject !=null){
				Job job = new Job(this.workerParams.paramObject, CommonConstants.JOB_NOT_GIVEN, CommonConstants.READ_FILES_MODE, CommonConstants.READ_STRING_MODE);
				jobList.add(job);
				JobPool.getInstance().addJob(job);
			}else if(this.workerParams.paramsString !=null){
				onStringParamMode(CommonConstants.READ_FILES_MODE, CommonConstants.READ_STRING_MODE,CommonConstants.JOB_BEEN_STOLEN);
			}
			
		}else if(mode == CommonConstants.READ_FILE_STRING_MODE){
			onStringParamMode(CommonConstants.READ_FILE_STRING_MODE, CommonConstants.JOB_NOT_GIVEN,-1);
		}else if(mode == CommonConstants.READ_FILE_NAME_MODE){
			Job job = new Job(this.workerParams.paramObject, CommonConstants.JOB_NOT_GIVEN, CommonConstants.READ_FILE_NAME_MODE, CommonConstants.READ_STRING_MODE);
			jobList.add(job);
			JobPool.getInstance().addJob(job);
		}
		
	}

	public void setJobParamsForTransmission(JobParams pStr) {
		this.workerParams = pStr;
	}
	
	public JobParams getWorkerParams(){
		return this.workerParams;
	}

	@Override
	public void run() {
		try {
			if (isSelf) {
				if (isWorker) {
					doOwnWorkForWorker();
				} else {
					doOwnWork();
				}
			} else {
				if(this.workerInfo == null){
					doOwnWork();
				}
//				else{
//					doOthersWork();// transmit jobs
//				}
				
				// this.workerInfo.isConnected = false;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public WorkerInfo getWorkerInfo() {
		return workerInfo;
	}

	public LinkedList<Job> getJobList() {
		return jobList;
	}

	public boolean isHaveJobs() {
		synchronized (jobList) {
			return jobList.size() > 0;
		}
	}

	public Job[] letThemSteal() {
		return JobPool.getInstance().letThemSteal();
	}
}
