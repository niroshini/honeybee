package tnefern.honeybeeframework.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import tnefern.honeybeeframework.delegator.AppRequest;
import tnefern.honeybeeframework.delegator.WorkerInfo;
import tnefern.honeybeeframework.stats.JobInfo;
import tnefern.honeybeeframework.stats.TimeMeter;
import tnefern.honeybeeframework.wifidirect.WifiDirectConstants;
import tnefern.honeybeeframework.worker.JobsReceived;
import tnefern.honeybeeframework.worker.ResultTransmitObject;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/**
 * Contains the jobs in a LinkedList.
 * 
 * @author tnfernando
 * 
 */
public class JobPool {
	private static JobPool theObject = null;
	private int partitionsPerNode = 0;
	private int partitionsForLastWorker = 0;
	private int partitionsPerDelegator = 0;
	private Job[] allJobs = null;// for delegator
	private ConcurrentLinkedQueue<Job> jobList = new ConcurrentLinkedQueue<Job>();// for
																					// worker
	private HashMap<String, JobsGiven> givenJobsList = new  HashMap<String, JobsGiven>();
	// private ArrayBlockingQueue<Job> tempjobList = null;// for worker
	private int completedJobs = 0;
	private Handler handler = null;
	private int jobParamMode = -1;
	private int stealMode = -1;
	private ArrayList<String> doneTransmitting = new ArrayList<String>();

	private boolean isSentResults = false;
	private JobsReceived callBackObj = null;
	private boolean isAllinitialJobsReceived = false;
	private CountDownLatch singlecountdown = null;
	
//	private boolean isStealRequestSent = false;
//	private boolean isStealReplyReceived = false;

	/**
	 * this contains the threads that will do the local processing, i.e., the jobs
	 * from the jobList.
	 */
	private ExecutorService singleWorkPool = Executors
			.newSingleThreadExecutor();

	/**
	 * the jobUpdateThreadPool has two threads. one to fetch jobs from the
	 * temporary job store and add them as they are received. The other thread
	 * is to deliver jobs already in the job pool to the main activity for
	 * processing, or for stealing.
	 */
	ExecutorService jobUpdateThreadPool = Executors.newFixedThreadPool(5);

	private JobPool() {

	}

	public static JobPool getInstance() {
		if (theObject == null) {
			theObject = new JobPool();
		}
		return theObject;
	}

	public void awaitCountDown(){
		try {
			this.singlecountdown.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void countdown(){
		this.singlecountdown.countDown();
	}
	public void setIsAllInitialJobsReceived() {
		this.isAllinitialJobsReceived = true;
	}

	public boolean isAllInitialJobsReceived() {
		return this.isAllinitialJobsReceived;
	}
	public void shutDownExecutor() {
		jobUpdateThreadPool.shutdown();
		singleWorkPool.shutdown();
		Log.d("JobPool", "shutDownExecutor");
	}

	public void executeRunnable(Runnable r) {
		jobUpdateThreadPool.execute(r);
	}

	public Future<Job> submitCallable(Callable<Job> c) {
		return jobUpdateThreadPool.submit(c);
	}

	public Future<Job[]> submitCallableforJobs(Callable<Job[]> c) {
		return jobUpdateThreadPool.submit(c);
	}

	public void submitJobWorker(Runnable task) {
		this.singleWorkPool.submit(task);
	}

	public void setJobsReceived(JobsReceived pCall) {
		this.callBackObj = pCall;
	}

	// public void setTempStore(ArrayBlockingQueue<Job> pStore){
	// this.tempjobList = pStore;
	// }

	public JobsReceived getJobsReceived() {
		return this.callBackObj;
	}

	public ArrayList<Job> fetchFilesToWorkerTransmit(int nJobs) {
		ArrayList<Job> fetchedJobs = new ArrayList<Job>();

		Job j = this.jobList.poll();
		int fetched = 0;
		fetchloop: while (j != null) {
			fetchedJobs.add(j);
			fetched++;
			if (fetched >= nJobs)
				break fetchloop;
			j = this.jobList.poll();
		}

		return fetchedJobs;
	}

	public void addToGivenJobs(JobsGiven pGiven){
		this.givenJobsList.put(pGiven.getWorkerAddress(), pGiven);
	}
	public void addLostWorkerJobsBack(String pLostWorker){
		if(pLostWorker!=null){
			JobsGiven given = this.givenJobsList.remove(pLostWorker);
			if(given!=null && given.getGivenList()!=null){
				for(Job j:given.getGivenList()){
					this.addJob(j);
					Log.d("RANDOM", "addLostWorkerJobsBack : " + j.jobParams);
				}
			}
		}
		
	}
	public boolean isJobListEmpty() {
		if (this.jobList == null) {
			return true;
		} else if (this.jobList.isEmpty()) {

			return true;

		} else {
			return false;
		}
	}

	public void setSentResults(boolean pF) {
		this.isSentResults = pF;
//		this.countdown();
	}

	public boolean hasSentResults() {
//		this.awaitCountDown();
		return this.isSentResults;
	}

	public int getStealMode() {
		return this.stealMode;
	}

	public void setStealMode(int pSteaLMode) {
		this.stealMode = pSteaLMode;
	}

	public int getMode() {
		return this.jobParamMode;
	}

	// public boolean isJobListNull
	public Iterator<Job> getJobIterator() {
		if (this.jobList == null)
			return null;
		Iterator<Job> iter = this.jobList.iterator();
		return iter;
	}

	public  void printAllJobs() {
		synchronized(this.jobList){
			if (this.jobList != null) {
				Iterator<Job> iter = this.jobList.iterator();
				Log.d("JobPool", "printAllJobs before fetch: ");
				while (iter.hasNext()) {
					Job jNxt = iter.next();
					Log.d("JobPool",jNxt.jobParams+"*");
				}
			}
		}
		
	}

	// public synchronized Job getFirst() {
	public Job getFirst() {
		// if (this.jobList == null || this.jobList.size() == 0)
		// return null;
		// BTFactory.getInstance().relock.lock();
		// Job j = this.jobList.removeFirst();

		Job j = this.jobList.poll();
		// BTFactory.getInstance().relock.unlock();
		return j;
	}

	public synchronized void clearAll() {
		// BTFactory.getInstance().relock.lock();
		if (this.jobList != null && !this.jobList.isEmpty()) {
			this.jobList.clear();

			if (this.handler != null) {
				Message msg = new Message();
				Bundle bundle = new Bundle();
				bundle.putInt(CommonConstants.JOBS_UPDATED, 0);
				msg.setData(bundle);

				this.handler.sendMessage(msg);
			}
		}
		// BTFactory.getInstance().relock.unlock();
	}

	// public Job remove_(int pIndex) {
	// return null;// this is a ghost method. remove later.
	// }

	/*
	 * public Job remove(int pIndex) { Job j = null; // if
	 * (!this.isJobListEmpty()) {
	 * 
	 * synchronized (jobList) { j = this.jobList.remove(pIndex); //
	 * jobList.notifyAll(); }
	 * 
	 * if (this.handler != null) { Message msg = new Message(); Bundle bundle =
	 * new Bundle(); bundle.putInt(CommonConstants.JOBS_UPDATED, 0);
	 * msg.setData(bundle);
	 * 
	 * this.handler.sendMessage(msg); } } // return j; }
	 */

	public void setHandler(Handler pHandler) {
		this.handler = pHandler;
	}

	public void setPartitionsPerNode(int pNper) {
		this.partitionsPerNode = pNper;
	}

	public void setPartitionsPerNode(int pNper, int pRemainder) {
		this.partitionsPerNode = pNper;
		this.partitionsForLastWorker = pRemainder;
	}

	public void setPartitionsPerDelegator(int pNper) {
		this.partitionsPerDelegator = pNper;
	}

	public synchronized void incrementDoneJobCount() {
		this.completedJobs++;
		
	}

	public synchronized boolean isJobPoolDone(ArrayList<CompletedJob> pDone) {
		Log.d("Jobs", " Done jobs : " + completedJobs + " Total : "
				+ this.allJobs.length);//17th june
		if (this.completedJobs == this.allJobs.length) {
			return true;
		}else if(this.completedJobs >this.allJobs.length){
			//this is weird. lets try to see duplicates
			Log.d("Jobs", " Done jobs > all jobs------- ");
			ArrayList<String>missingCompletedJobs = new ArrayList<String>();
			HashMap<String, Integer>jobMap = new HashMap<String, Integer>();
			for(CompletedJob cj:pDone){
				if(cj.stringValue!=null){
					Integer intO = jobMap.get(cj.stringValue);
					if(intO!=null){
						int intval = intO.intValue();
						jobMap.put(cj.stringValue, Integer.valueOf(++intval));
					}else{
						jobMap.put(cj.stringValue, Integer.valueOf(1));
					}
				}
				
			}
			
			//now see which jobs were duplicated
			Iterator<Entry<String, Integer>> it = jobMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, Integer> pairs =  it.next();
				String key = pairs.getKey();
				if(pairs.getValue()>1){
					Log.d("Jobs", "duplicated "+key+" = "+pairs.getValue());
				}
				
			}
			
			//now see which jobs were missed (if any)
			for(Job j:this.allJobs){
//				String jobstr= FileFactory.getInstance().getFileNameFromFullPath(j.jobParams);//december
				String jobstr= j.id;
				if(!jobMap.containsKey(jobstr)){
					missingCompletedJobs.add(jobstr);
				}
			}
			
			Log.d("Jobs", "****missingJobs****");
			for(String sj:missingCompletedJobs){
				Log.d("Jobs", "missing: "+sj);
			}
			if(missingCompletedJobs.size()>0){
				return false;
			}
			return true;
		}
		return false;
	}
	
	public synchronized boolean isJobPoolDone() {
//		Log.d("Jobs", " Done jobs : " + completedJobs + " Total : "
//				+ this.allJobs.length);//17th june
		if (this.completedJobs >= this.allJobs.length) {
			return true;
		}
		return false;
	}

	public int getDoneJobs() {
		return this.completedJobs;
	}

	public synchronized int getAllJobSize() {
		if (this.jobList == null)
			return -1;
		return this.jobList.size();
	}

	public Job isJobExistsinOriginal(Job pJob) {
		if (pJob != null) {

			// synchronized (this.jobList) {
			// if(this.jobList.contains(pJob)){
			// return this.jobList.
			// }
			// }
			if (this.allJobs != null && this.allJobs.length > 0) {
				for (int i = 0; i < allJobs.length; i++) {
					if (pJob.equals(allJobs[i])) {
						return allJobs[i];
					}
				}
				return null;
			} else {
				return null;
			}

		} else {
			return null;
		}
	}

	public Job isJobExists_old(Job pJob) {
		if (pJob != null) {
			if (this.allJobs != null && this.allJobs.length > 0) {
				for (int i = 0; i < allJobs.length; i++) {
					if (pJob.equals(allJobs[i])) {
						return allJobs[i];
					}
				}
				return null;
			} else {
				return null;
			}

		} else {
			return null;
		}
	}

	public void initJobPool(AppRequest pReq) {
		allJobs = new Job[pReq.getNumberOfJobs()];
		jobParamMode = pReq.getMode();
		switch (jobParamMode) {
		case CommonConstants.READ_STRING_MODE:
			String sArr[] = pReq.getDistributedString();
//			ArrayList<AppInfo> apJobs = pReq.getAppInfo();
			for (int i = 0; i < pReq.getNumberOfJobs(); i++) {
				Job j = new Job(sArr[i], CommonConstants.JOB_NOT_GIVEN,
						CommonConstants.READ_STRING_MODE, this.stealMode);
				j.index = i;
				j.id = String.valueOf(i);//TODO: Do this in a better way
				allJobs[i] = j;
				this.jobList.offer(j);
			}
			break;

		case CommonConstants.READ_FILE_MODE:
			// here apJobs.get(i).getStringInfo() gives the file name
			ArrayList<AppInfo> apJobs = pReq.getAppInfo();
			for (int i = 0; i < apJobs.size(); i++) {
				Job j = new Job(apJobs.get(i).getStringInfo(),
						CommonConstants.JOB_NOT_GIVEN,
						CommonConstants.READ_FILE_MODE);
				j.index = i;
				j.id = apJobs.get(i).getId();
				allJobs[i] = j;
				this.jobList.offer(j);
			}
			break;
		case CommonConstants.READ_FILES_MODE:// facematch
			ArrayList<AppInfo> apJobs2 = pReq.getAppInfo();
			for (int i = 0; i < apJobs2.size(); i++) {
				Job j = new Job(apJobs2.get(i).getStringInfo(),
						CommonConstants.JOB_NOT_GIVEN,
						CommonConstants.READ_FILES_MODE, this.stealMode);
				j.index = i;
				j.id = apJobs2.get(i).getId();
				allJobs[i] = j;
				this.jobList.offer(j);
			}
			break;

		case CommonConstants.READ_FILE_MODE2:
			ArrayList<AppInfo> apJobs3 = pReq.getAppInfo();
			for (int i = 0; i < apJobs3.size(); i++) {
				Job j = new Job(apJobs3.get(i).getStringInfo(),
						CommonConstants.JOB_NOT_GIVEN,
						CommonConstants.READ_FILE_MODE2);
				j.index = i;
				j.id = apJobs3.get(i).getId();
				allJobs[i] = j;
				this.jobList.offer(j);
			}
			break;
		}

	}

	// public ArrayBlockingQueue<Job> initJobsForOwn() {
	// jobList = new ArrayBlockingQueue<Job>(CommonConstants.MAX_JOBS_STORE);
	// for (int i = 0; i < partitionsPerDelegator; i++) {
	// jobList.add(allJobs[i]);// test
	// }
	// return jobList;
	// }

	public ConcurrentLinkedQueue<Job> initJobsForOwn() {
		// for (int i = 0; i < partitionsPerDelegator; i++) {
		for (int i = 0; i < allJobs.length; i++) {
			jobList.add(allJobs[i]);// test
		}
		return jobList;
	}

	public synchronized ConcurrentLinkedQueue<Job> getJobList() {
		// public ArrayBlockingQueue<Job> getJobList() {
		return this.jobList;
	}

	/**
	 * called in Worker only
	 * 
	 * @param pJobs
	 */
	// public void setJobs(LinkedList<Job> pJobs) {
	// this.jobList = pJobs;
	// }
	public void addJobs(Job[] pJobsArr, boolean isNotify) {
		for (Job j : pJobsArr) {
			// this.jobList.addLast(iterJobs.next());
			this.jobList.offer(j);
//			Log.d("JobPool : stolen jobs", j.jobParams);
		}

		if (this.handler != null) {
			Message msg = new Message();
			Bundle bundle = new Bundle();
			bundle.putInt(CommonConstants.JOBS_UPDATED, 0);
			msg.setData(bundle);

			this.handler.sendMessage(msg);
		}

		if (this.callBackObj != null && isNotify) {
//			Log.d("JobPool : stolen jobs", "this.callBackObj.onJobRecieved()");
			this.callBackObj.onJobRecieved();
		}
	}

	public void addJob(Job pJob) {
		// this.jobList.addLast(pJob);
		this.jobList.offer(pJob);
		// if (pJob != null) {
		// if (pJob.jobParams != null) {
		// Log.d("JobPool : addJob = ", pJob.jobParams);
		// } else {
		// Log.d("JobPool : addJob = ", " is file null? "
		// + (pJob.f == null));
		// }
		//
		// } else {
		// Log.d("JobPool : addJob = ", "job is NULL");
		// }

		if (this.handler != null) {
			Message msg = new Message();
			Bundle bundle = new Bundle();
			bundle.putInt(CommonConstants.JOBS_UPDATED, 0);
			msg.setData(bundle);

			this.handler.sendMessage(msg);
		}

		// if (this.callBackObj != null) {
		// this.callBackObj.onJobRecieved(pJob);
		// }

	}

	public void addJobs(ArrayList<Job> jobs) {
		// public void addJobs(ArrayList<Job> jobs) {
		Iterator<Job> iterJobs = jobs.iterator();
		while (iterJobs.hasNext()) {
			// this.jobList.addLast(iterJobs.next());
			this.jobList.offer(iterJobs.next());
		}

		if (this.handler != null) {
			Message msg = new Message();
			Bundle bundle = new Bundle();
			bundle.putInt(CommonConstants.JOBS_UPDATED, 0);
			msg.setData(bundle);

			this.handler.sendMessage(msg);
		}

	}

	/*
	 * public Job[] letThemSteal() { Job[] stolenGoods = null; if (jobList !=
	 * null && !jobList.isEmpty()) { if (jobList.size() >=
	 * CommonConstants.STEAL_LIM) { ArrayList<Job> stolen = new
	 * ArrayList<Job>();
	 * 
	 * //there are no inbuilt methods to remove the last object from the
	 * blockingQueue. So we do that by code.
	 * 
	 * //Since the iterator is not always trustworthy to relect or not to
	 * reflect the changes after modification Job[]jobArray =
	 * jobList.toArray(new Job[0]);
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * int len = 0; int y = 0; Job j = null; stealingLoop: while (true) { if (y
	 * >= CommonConstants.STEAL_CHUNK) break stealingLoop; synchronized
	 * (jobList) { if (jobList.isEmpty()) break stealingLoop; int ind =
	 * jobList.size() - 1; // j = jobList.get(ind); j = jobList. while (j.status
	 * == CommonConstants.JOB_BEEN_STOLEN && ind > 0) { ind--; // j =
	 * jobList.get(ind); j = jobList.poll(); } if (j.status !=
	 * CommonConstants.JOB_BEEN_STOLEN && ind >= 0) { j = jobList.remove(ind); }
	 * else { j = null; break stealingLoop; }
	 * 
	 * } // jobList.notifyAll(); if (j != null) { j.stealMode = this.stealMode;
	 * Log.d("Slave- letThemSteal", "stolen from : " + j.jobParams +
	 * " this.stealMode = " + this.stealMode);
	 * 
	 * stolen.add(j); y++; }
	 * 
	 * } // } Iterator<Job> iter = stolen.iterator(); stolenGoods = new
	 * Job[stolen.size()]; int x = 0; while (iter.hasNext()) { stolenGoods[x] =
	 * iter.next(); x++; } Log.d("Slave- letThemSteal", "stolenGoods.length = "
	 * + stolenGoods.length); if (this.handler != null) { Message msg = new
	 * Message(); Bundle bundle = new Bundle();
	 * bundle.putInt(CommonConstants.JOBS_UPDATED, 0); msg.setData(bundle);
	 * 
	 * this.handler.sendMessage(msg); } } } // } return stolenGoods; }
	 */

	/*
	 * public Job[] letThemSteal_Original() { Job[] stolenGoods = null; if
	 * (jobList != null && !jobList.isEmpty()) { if (jobList.size() >=
	 * CommonConstants.STEAL_LIM) { ArrayList<Job> stolen = new
	 * ArrayList<Job>(); int len = 0; int y = 0; Job j = null; stealingLoop:
	 * while (true) { if (y >= CommonConstants.STEAL_CHUNK) break stealingLoop;
	 * synchronized (jobList) { if (jobList.isEmpty()) break stealingLoop; int
	 * ind = jobList.size() - 1; j = jobList.get(ind); while (j.status ==
	 * CommonConstants.JOB_BEEN_STOLEN && ind > 0) { ind--; // j =
	 * jobList.get(ind); j = jobList.poll(); } if (j.status !=
	 * CommonConstants.JOB_BEEN_STOLEN && ind >= 0) { j = jobList.remove(ind); }
	 * else { j = null; break stealingLoop; }
	 * 
	 * } // jobList.notifyAll(); if (j != null) { j.stealMode = this.stealMode;
	 * Log.d("Slave- letThemSteal", "stolen from : " + j.jobParams +
	 * " this.stealMode = " + this.stealMode);
	 * 
	 * stolen.add(j); y++; }
	 * 
	 * } // } Iterator<Job> iter = stolen.iterator(); stolenGoods = new
	 * Job[stolen.size()]; int x = 0; while (iter.hasNext()) { stolenGoods[x] =
	 * iter.next(); x++; } Log.d("Slave- letThemSteal", "stolenGoods.length = "
	 * + stolenGoods.length); if (this.handler != null) { Message msg = new
	 * Message(); Bundle bundle = new Bundle();
	 * bundle.putInt(CommonConstants.JOBS_UPDATED, 0); msg.setData(bundle);
	 * 
	 * this.handler.sendMessage(msg); } } } // } return stolenGoods; }
	 */

	public Job[] letThemSteal() {
		Job[] stolenGoods = null;
		ArrayList<Job> stolen = new ArrayList<Job>();
		Job firstStolen = null;
		synchronized (this.jobList) {
			if (!jobList.isEmpty()) {
				if (jobList.size() > CommonConstants.STEAL_LIM) {
					firstStolen = jobList.poll();
				}
			}
		}

		stealingLoop: while (firstStolen != null) {
			stolen.add(firstStolen);
			synchronized (this.jobList) {
				if (jobList.size() <= CommonConstants.STEAL_LIM) {
					break stealingLoop;
				}
			}
			if (stolen.size() >= CommonConstants.STEAL_CHUNK) {
				break stealingLoop;
			}

			firstStolen = jobList.poll();
		}

		if (!stolen.isEmpty()) {
			Iterator<Job> iter = stolen.iterator();
			stolenGoods = new Job[stolen.size()];
			int x = 0;
			while (iter.hasNext()) {
				stolenGoods[x] = iter.next();
				x++;
			}
//			Log.d("Slave- letThemSteal", "stolenGoods.length = "
//					+ stolenGoods.length);
			if (this.handler != null) {
				Message msg = new Message();
				Bundle bundle = new Bundle();
				bundle.putInt(CommonConstants.JOBS_UPDATED, 0);
				msg.setData(bundle);

				this.handler.sendMessage(msg);
			}

		}
		return stolenGoods;
	}

	public String getJobsForTransmission_(int pInd) {
		int startInd = pInd * partitionsPerNode;
		StringBuffer sBuf = new StringBuffer();

		sBuf.append(CommonConstants.PARAM_SYMBOL);
		for (int i = startInd; i < startInd + partitionsPerNode; i++) {
			sBuf.append(allJobs[i].jobParams);
			sBuf.append(CommonConstants.PARTITION_BREAK);
		}

		sBuf.append(CommonConstants.MSG_BREAK);

		return sBuf.toString();
	}

	public JobParams fetchJobsToTransmitToWorker(int pNumJobs, String pAdr) {
//		StringBuffer sb = new StringBuffer();
		ArrayList<Job> list = fetchFilesToWorkerTransmit(pNumJobs);
		JobParams jParams = null;
		switch (jobParamMode) {
		case CommonConstants.READ_FILES_MODE:
			jParams = new JobParams(CommonConstants.READ_FILES_MODE);
			String[][] sarr = null;
			ArrayList<String> fileNames = new ArrayList<String>();
			if (list.size() > CommonConstants.MAX_FILES_PER_MSG) {
				int times = list.size() / CommonConstants.MAX_FILES_PER_MSG;
				int rem = list.size() % CommonConstants.MAX_FILES_PER_MSG;
				;
				if (rem > 0) {
					sarr = new String[times + 1][CommonConstants.MAX_FILES_PER_MSG];
				} else {
					sarr = new String[times][CommonConstants.MAX_FILES_PER_MSG];
				}

				int x = 0;
				for (int k = 0; k < times; k++) {
					for (int l = 0; l < CommonConstants.MAX_FILES_PER_MSG; l++) {
						sarr[k][l] = list.get(x).jobParams;
						x++;
					}

				}
				for (int k = 0; k < rem; k++) {
					sarr[sarr.length - 1][k] = list.get(x).jobParams;
					x++;
				}

			} else {
				sarr = new String[1][list.size()];
				for (int k = 0; k < list.size(); k++) {
					sarr[0][k] = list.get(k).jobParams;
				}
			}

			for (String[] arr : sarr) {
				String zipfile;
				try {
					zipfile = FileFactory.getInstance().zipFilesIntoDirectory(
							arr, CommonConstants.ZIP_FILE_PATH);
					fileNames.add(zipfile);
				} catch (IOException e) {
					e.printStackTrace();
				}

			}

			try {
				jParams.paramObject = FileFactory.getInstance().getFiles(
						fileNames);
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		case CommonConstants.READ_STRING_MODE:
			jParams = new JobParams(CommonConstants.READ_STRING_MODE);
			StringBuffer sBuf = new StringBuffer();
			Iterator<Job>iter = list.iterator();
			
			sBuf.append(CommonConstants.PARAM_SYMBOL);
			while(iter.hasNext()){
				sBuf.append(iter.next().jobParams);
				sBuf.append(CommonConstants.PARTITION_BREAK);
			}
			sBuf.append(CommonConstants.MSG_BREAK);
			
			jParams.paramObject = sBuf.toString();
			break;
		}
		
	
		JobPool.getInstance().addToGivenJobs(new JobsGiven(pAdr, list, System.currentTimeMillis()));
		
//		Log.d("Job Pool","fetchJobsToTransmitToWorker: "+sb.toString());
		return jParams;
	}

	/**
	 * Get the jobs for one worker
	 * 
	 * @param pInd
	 * @return
	 */
	public JobParams getJobsForTransmission(int pInd) {
		int startInd = pInd * partitionsPerNode;
		JobParams jParams;
		String[] sNames = null;
		int j = 0;
		switch (jobParamMode) {
		case CommonConstants.READ_STRING_MODE:

			StringBuffer sBuf = new StringBuffer();

			sBuf.append(CommonConstants.PARAM_SYMBOL);
			for (int i = startInd; i < startInd + partitionsPerNode; i++) {
				sBuf.append(allJobs[i].jobParams);
				sBuf.append(CommonConstants.PARTITION_BREAK);
			}

			sBuf.append(CommonConstants.MSG_BREAK);
			jParams = new JobParams(CommonConstants.READ_STRING_MODE);
			jParams.paramObject = sBuf.toString();
			return jParams;
			// return sBuf.toString();
		case CommonConstants.READ_FILE_MODE:
			sNames = new String[partitionsPerNode];
			j = 0;
			for (int i = startInd; i < startInd + partitionsPerNode; i++) {
				sNames[j] = allJobs[i].jobParams;
				j++;
			}

			try {
				String filename = FileFactory
						.getInstance()
						.zipFilesIntoDirectory(
								FileFactory
										.getInstance()
										.getDirectoryNameFromFullPath(sNames[0]),
								startInd, startInd + partitionsPerNode,
								CommonConstants.ZIP_FILE_PATH);

				jParams = new JobParams(CommonConstants.READ_FILE_MODE);
				jParams.paramObject = FileFactory.getInstance().getFile(
						filename);
				return jParams;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;

		case CommonConstants.READ_FILE_MODE2:
			sNames = new String[partitionsPerNode];
			j = 0;
			for (int i = startInd; i < startInd + partitionsPerNode; i++) {
				sNames[j] = allJobs[i].jobParams;
				j++;
				// get the file names
			}
			jParams = new JobParams(CommonConstants.READ_FILE_MODE2);
			jParams.paramObject = sNames;
			return jParams;

		case CommonConstants.READ_FILES_MODE:
			jParams = new JobParams(CommonConstants.READ_FILES_MODE);
			int partitions = 0;
			if (partitionsForLastWorker > 0) {
				if (startInd == 0) {
					partitions = partitionsPerNode + partitionsForLastWorker;
				}
			} else {
				partitions = partitionsPerNode;
			}

			if (partitions > CommonConstants.MAX_FILES_PER_MSG) {// file
																	// array
				int times = partitions / CommonConstants.MAX_FILES_PER_MSG;
				int rem = -1;
				if (times < 2) {
					rem = partitions - CommonConstants.MAX_FILES_PER_MSG;
				}

				sNames = new String[partitions];
				j = 0;
				for (int i = startInd; i < startInd + partitions; i++) {
					sNames[j] = allJobs[i].jobParams;
					j++;
				}

				int start = 0;
				ArrayList<String> fileNames = new ArrayList<String>();
				try {
					String filename = FileFactory
							.getInstance()
							.zipFilesIntoDirectory(
									FileFactory.getInstance()
											.getDirectoryNameFromFullPath(
													sNames[0]),
									startInd,
									startInd
											+ CommonConstants.MAX_FILES_PER_MSG,
									CommonConstants.ZIP_FILE_PATH);
					fileNames.add(filename);

					start = startInd + CommonConstants.MAX_FILES_PER_MSG;

					if (rem > 0) {
						// int temp = 0;
						// if(rem)
						filename = FileFactory.getInstance()
								.zipFilesIntoDirectory(
										FileFactory.getInstance()
												.getDirectoryNameFromFullPath(
														sNames[0]), start,
										start + rem,
										CommonConstants.ZIP_FILE_PATH);
						fileNames.add(filename);
					} else {
						for (int k = 0; k < times - 1; k++) {
							filename = FileFactory
									.getInstance()
									.zipFilesIntoDirectory(
											FileFactory
													.getInstance()
													.getDirectoryNameFromFullPath(
															sNames[0]),
											start,
											start
													+ CommonConstants.MAX_FILES_PER_MSG,
											CommonConstants.ZIP_FILE_PATH);
							fileNames.add(filename);
							start = start + CommonConstants.MAX_FILES_PER_MSG;
						}
					}

					jParams.paramObject = FileFactory.getInstance().getFiles(
							fileNames);
					// return jParams;
				} catch (IOException e) {
					e.printStackTrace();
				}
				return jParams;
				// break;
			} else {
				sNames = new String[partitionsPerNode];
				j = 0;
				for (int i = startInd; i < startInd + partitionsPerNode; i++) {
					sNames[j] = allJobs[i].jobParams;
					j++;
				}

				if (sNames.length > 0) {
					try {
						String filename = FileFactory.getInstance()
								.zipFilesIntoDirectory(
										FileFactory.getInstance()
												.getDirectoryNameFromFullPath(
														sNames[0]), startInd,
										startInd + partitionsPerNode,
										CommonConstants.ZIP_FILE_PATH);

						// jParams = new
						// JobParams(CommonConstants.READ_FILE_MODE);
						jParams.paramObject = new File[] { FileFactory
								.getInstance().getFile(filename) };

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				return jParams;
			}
		}

		return null;

	}

	public void transmitFileAsParams(byte[] arr, OutputStream pOut,
			int pPacketSize) throws IOException {
		// byte[] arr = FileFactory.getInstance().getFileBytes(pFile);
		pOut.write(ByteBuffer.allocate(4).putInt(CommonConstants.READ_INT_MODE)
				.array());
		pOut.flush();

		pOut.write(ByteBuffer.allocate(4).putInt(arr.length).array());
		pOut.flush();

		pOut.write(ByteBuffer.allocate(4)
				.putInt(CommonConstants.READ_FILE_MODE).array());
		pOut.flush();

		// int = 500;
		int fullLen = arr.length;
		int writtenBytes = 0;
		while (writtenBytes < fullLen) {
			if (fullLen - writtenBytes >= pPacketSize) {
				pOut.write(arr, writtenBytes, pPacketSize);
				pOut.flush();
				writtenBytes += pPacketSize;

			} else {
				pOut.write(arr, writtenBytes, fullLen - writtenBytes);
				pOut.flush();
				writtenBytes += (fullLen - writtenBytes);
			}
		}
	}

	public void transmitFileAsParams(File file, OutputStream pOut,
			int pPacketSize) throws IOException {
		pOut.write(ByteBuffer.allocate(4).putInt(CommonConstants.READ_INT_MODE)
				.array());
		pOut.flush();

		pOut.write(ByteBuffer.allocate(4).putInt((int) file.length()).array());
		pOut.flush();

		pOut.write(ByteBuffer.allocate(4)
				.putInt(CommonConstants.READ_FILE_MODE).array());
		pOut.flush();

		InputStream ios = null;
		try {
			byte[] buffer = new byte[pPacketSize];
			ios = new FileInputStream(file);
			int read = 0;
			while ((read = ios.read(buffer)) != -1) {
				pOut.write(buffer, 0, read);
				pOut.flush();
			}
		} finally {
			try {
				if (ios != null)
					ios.close();
			} catch (IOException e) {
				// swallow, since not that important
			}
		}
	}

	public void transmitFilesAsParams(byte[] arr, OutputStream pOut,
			int pPacketSize) throws IOException {
		pPacketSize = 4096;// test
		pOut.write(ByteBuffer.allocate(4)
				.putInt(CommonConstants.READ_FILES_MODE).array());
		pOut.flush();

		pOut.write(ByteBuffer.allocate(4).putInt(1).array());
		pOut.flush();

		pOut.write(ByteBuffer.allocate(4).putInt(arr.length).array());
		pOut.flush();

		int fullLen = arr.length;
		int writtenBytes = 0;
//		Log.d("transmitFilesAsParams", "fullLen = " + fullLen
//				+ " packetSize = " + pPacketSize);
		/*
		 * while (writtenBytes < fullLen) { if (fullLen - writtenBytes >=
		 * pPacketSize) { pOut.write(arr, writtenBytes, pPacketSize);
		 * pOut.flush(); writtenBytes += pPacketSize;
		 * Log.d("transmitFilesAsParams","written "+writtenBytes+
		 * " of "+fullLen);
		 * 
		 * } else { pOut.write(arr, writtenBytes, fullLen - writtenBytes);
		 * pOut.flush(); writtenBytes += (fullLen - writtenBytes);
		 * Log.d("transmitFilesAsParams","written "+writtenBytes+
		 * " of "+fullLen); } }
		 */

		pOut.write(arr);
//		Log.d("transmitFilesAsParams", "finished transmitting file over BT");
	}

	public void transmitFilesAsParams(File[] files, WorkerInfo pInfo,
			int pPacketSize, Activity c) throws IOException {
		long distTime = System.currentTimeMillis();
		OutputStream pOut = pInfo.getBTSocket().getOutputStream();
		final Context con = c.getApplicationContext();
		// synchronized(pOut){
		pOut.write(ByteBuffer.allocate(4)
				.putInt(CommonConstants.READ_FILES_MODE).array());
		pOut.flush();

		pOut.write(ByteBuffer.allocate(4).putInt((int) files.length).array());
		pOut.flush();
		byte[] arr = null;
		for (int i = 0; i < files.length; i++) {
			FileInputStream fis = new FileInputStream(files[i]);
			arr = new byte[(int) files[i].length()];
			fis.read(arr);

			pOut.write(ByteBuffer.allocate(4).putInt(arr.length).array());
			pOut.flush();
			// if (c != null) {
			// c.runOnUiThread(new Runnable() {
			// public void run() {
			// Toast.makeText(con, "Transmitting images",
			// Toast.LENGTH_SHORT).show();
			// }
			// });
			// }

			// try {
			// FileFactory
			// .getInstance()
			// .writeFileWithDate(
			// "###################transmitFilesAsParams##############");
			// FileFactory.getInstance().writeFileWithDate(
			// "FileName = " + files[i].getName() + "  File lenth = "
			// + arr.length);
			// } catch (IOException e1) {
			// e1.printStackTrace();
			// }

			int fullLen = arr.length;
			// pPacketSize = fullLen;

			// pOut.write(arr, 0, pPacketSize);

			int writtenBytes = 0;
			while (writtenBytes < fullLen) {
				if (fullLen - writtenBytes >= pPacketSize) {
					pOut.write(arr, writtenBytes, pPacketSize);
					pOut.flush();
					writtenBytes += pPacketSize;
					final int writ = writtenBytes;
					if (c != null) {
						c.runOnUiThread(new Runnable() {
							public void run() {
								Toast.makeText(con, "Sent " + writ + " bytes ",
										Toast.LENGTH_SHORT).show();
							}
						});
					}

				} else {
					pOut.write(arr, writtenBytes, fullLen - writtenBytes);
					pOut.flush();
					writtenBytes += (fullLen - writtenBytes);
				}
			}
			// final int ind = i;
			pOut.flush();
			// if (c != null) {
			// c.runOnUiThread(new Runnable() {
			// public void run() {
			// Toast.makeText(con, "Sent file " + ind,
			// Toast.LENGTH_SHORT).show();
			// }
			// });
			// }
			// pOut.notifyAll();
			files[i].delete();

		}
		// }
		finishedTransmittingParams(pInfo.getBtDevice().getAddress());

		distTime = System.currentTimeMillis() - distTime;
		TimeMeter.getInstance().addSendJobTime(new JobInfo("", distTime));

	}

	/**
	 * 
	 * @param workerAddress
	 *            the BT/WifiDirect address of worker
	 */
	public void finishedTransmittingParams(String workerAddress) {
		this.doneTransmitting.add(workerAddress);
	}

	public boolean hasJobsBeenTransmitted(String pWorker) {
		Iterator<String> iter = this.doneTransmitting.iterator();

		while (iter.hasNext()) {
			String s = iter.next();
			if (s != null && s.equals(pWorker)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasAllJobsBeenTransmitted() {
		if (this.doneTransmitting.size() == ConnectionFactory.getInstance()
				.getConnectedWorkerList().size()) {
//			Log.d("Delegator",
//					"this.doneTransmitting.size() = "
//							+ this.doneTransmitting.size()
//							+ "  BTFactory.getInstance().getConnectedWorkerList().size()= "
//							+ BTFactory.getInstance().getConnectedWorkerList()
//									.size());
			return true;
		}
		return false;
	}

	public int getJobsBeenTransmitted() {
		return this.doneTransmitting.size();
	}

	public void transmitFilesAsParams(String[] fileNames, OutputStream pOut,
			int pPacketSize) throws IOException {
		File file = null;
		for (int i = 0; i < fileNames.length; i++) {
			file = new File(fileNames[i]);
			long fullLen = file.length();
			byte[] arr = FileFactory.getInstance().getFileBytes(file);
			String s = (file.getName());
			s += CommonConstants.MSG_BREAK;
			byte[] nameArr = s.getBytes();

			pOut.write(ByteBuffer.allocate(4)
					.putInt(CommonConstants.READ_INT_MODE).array());
			pOut.flush();

			pOut.write(ByteBuffer.allocate(4).putInt(nameArr.length).array());
			pOut.flush();

			pOut.write(ByteBuffer.allocate(4)
					.putInt(CommonConstants.READ_STRING_MODE).array());
			pOut.flush();

			pOut.write(nameArr);
			pOut.flush();

			pOut.write(ByteBuffer.allocate(4)
					.putInt(CommonConstants.READ_INT_MODE).array());
			pOut.flush();

			pOut.write(ByteBuffer.allocate(4).putInt((int) fullLen).array());
			pOut.flush();

			pOut.write(ByteBuffer.allocate(4)
					.putInt(CommonConstants.READ_FILE_MODE2).array());
			pOut.flush();

			int writtenBytes = 0;
			while (writtenBytes < fullLen) {
				if (fullLen - writtenBytes >= pPacketSize) {
					pOut.write(arr, writtenBytes, pPacketSize);
					pOut.flush();
					writtenBytes += pPacketSize;

				} else {
					pOut.write(arr, writtenBytes,
							(int) (fullLen - writtenBytes));
					pOut.flush();
					writtenBytes += (fullLen - writtenBytes);
				}
			}
		}
	}
	
	//TODO//change this
	public boolean checkResults(HashMap<String, Object> presultMap, ArrayList<CompletedJob> pdone){
		ArrayList<String>wrongJobs = new ArrayList<String>();
		for(Job j:allJobs){
//			String jobstr= FileFactory.getInstance().getFileNameFromFullPath(j.jobParams);
			Object intRes = presultMap.get(j.id);
			if(intRes == null){
				wrongJobs.add(j.jobParams);
			}
		}
		if(wrongJobs.size()>0){
			Log.d("checkResults", "WRONG!! the following had no results----------");
			for(String wj:wrongJobs){
				Log.d("checkResults", "WRONG!! for the following ----------"+wj);
			}
			return false;
		}
		Log.d("checkResults", "CORRECT!! ");
		return true;
	}
	
	public void removeGivenJobs(Object pResObj){
		if(pResObj !=null){
			if(pResObj instanceof CompletedJob){
				CompletedJob cj = (CompletedJob) pResObj;
				Iterator<Entry<String, JobsGiven>> it = givenJobsList.entrySet().iterator();
			    while (it.hasNext()) {
			    	Entry<String, JobsGiven> pairs = it.next();
			    	JobsGiven givenjobs = pairs.getValue();
			    	if(givenjobs!=null){
			    		Iterator<Job> jobsIter =givenjobs.getGivenList().iterator();
			    		while(jobsIter.hasNext()){
			    			Job job = jobsIter.next();
			    			if(job.id.equals(cj.id)){
			    				jobsIter.remove();
			    			}
			    		}
			    	}
			    }
				
			}
		}
	}
	
	public void removeGivenJobs(Object pResObj, String pWorker){
		if(pResObj !=null){
			if(pResObj instanceof String[]){
				String[]resultArray = (String[])pResObj;
				if(resultArray!=null){
					String sjobs[];
					for(String jobName:resultArray){
						sjobs = jobName.split(":");
						if(sjobs!=null){
							JobsGiven given = givenJobsList.get(pWorker);
							if((given!=null) && (given.getGivenList()!=null)){
								String name;
								ArrayList<Job>givenlist = given.getGivenList();
								Iterator<Job>iter = givenlist.iterator();
								while(iter.hasNext()){
									Job j = iter.next();;
									if(j!=null){
										name =FileFactory.getInstance().getFileNameFromFullPath(j.jobParams);
										if(sjobs[0].equals(name)){
//											Log.d("RANDOM","removed "+name);
											iter.remove();
										}
									}
								}
							}
						}
					}
				}
				
			}else if(pResObj instanceof ResultTransmitObject[]){
				ResultTransmitObject[]resultArray = (ResultTransmitObject[])pResObj;
				if(resultArray!=null){
					for(ResultTransmitObject jobName:resultArray){
						if(jobName!=null){
							JobsGiven given = givenJobsList.get(pWorker);
							if((given!=null) && (given.getGivenList()!=null)){
								String jobId;
								ArrayList<Job>givenlist = given.getGivenList();
								Iterator<Job>iter = givenlist.iterator();
								while(iter.hasNext()){
									Job j = iter.next();;
									if(j!=null){
										jobId =j.id;
										if(jobName.identifier.equals(jobId)){
											Log.d("removeGivenJobs","removed "+jobId);
											iter.remove();
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	public String hasJobsExpired(){
		Iterator<Entry<String, JobsGiven>> it = givenJobsList.entrySet().iterator();
	    while (it.hasNext()) {
	    	Entry<String, JobsGiven> pairs = it.next();
	    	JobsGiven givenjobs = pairs.getValue();
	    	if(givenjobs!=null){
	    		if((System.currentTimeMillis()-givenjobs.getWorkerGivenTime())>=WifiDirectConstants.WORKER_JOB_EXPIRY){
	    			return pairs.getKey();
	    		}
	    	}
//	        it.remove(); 
	    }
		
		return null;
	}
	
}
