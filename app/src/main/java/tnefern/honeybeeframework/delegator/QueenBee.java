package tnefern.honeybeeframework.delegator;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import tnefern.honeybeeframework.apps.facematch.FaceConstants;
import tnefern.honeybeeframework.common.CommonConstants;
import tnefern.honeybeeframework.common.CompletedJob;
import tnefern.honeybeeframework.common.ConnectionFactory;
import tnefern.honeybeeframework.common.FileFactory;
import tnefern.honeybeeframework.common.Job;
import tnefern.honeybeeframework.common.JobInitializer;
import tnefern.honeybeeframework.common.JobPool;
import tnefern.honeybeeframework.stats.TimeMeter;
import tnefern.honeybeeframework.worker.ResultTransmitObject;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

/**
 * The QueenBee represents the work the delegator has to do, if the delegator is
 * working.
 * 
 * @author tnfernando
 * 
 */
public abstract class QueenBee implements ResultsRead {
	private Activity parentContext = null;

	int mode = -1;

	private boolean isInit = false;

	static final int IS_DELEGATOR = 1;
	private boolean isDelegatorWorking = true;
	private ArrayList<CompletedJob> doneJobs = new ArrayList<CompletedJob>();

	public QueenBee(Activity pAct) {
		this.parentContext = pAct;
		init();
	}

	void init() {
		if (!isInit) {
			TimeMeter.getInstance().setDeleThreadGapTime(
					System.currentTimeMillis()
							- TimeMeter.getInstance().getInitJobsTime());

			mode = IS_DELEGATOR;
			ConnectionFactory.getInstance().setReadResults(this);
			setStealMode();
			isInit = true;
		}

	}

	public Activity getParentContext() {
		return this.parentContext;
	}

	/**
	 * default steal mode is String. This can be changed in the implementing
	 * class.
	 */
	public void setStealMode() {
		JobPool.getInstance().setStealMode(CommonConstants.READ_STRING_MODE);
		JobInitializer.getInstance(parentContext).setStealMode(
				CommonConstants.READ_FILES_MODE);
	}

	public void populateWithJobs() {
		if (!JobPool.getInstance().isJobListEmpty()) {
//			 JobPool.getInstance().setStealMode(CommonConstants.READ_FILES_MODE);
			if (isDelegatorWorking) {
				fetchJobsFromPool();

			}

		}
	}

	private void fetchJobsFromPool() {// this is delegator doing normal jobs
		long time = System.currentTimeMillis();
		Job job = JobPool.getInstance().getFirst();
		while (job != null) {
			if (job.jobParams != null) {
//				this.doneJobs.add(doAppSpecificJob(job.jobParams));
				CompletedJob cj = doAppSpecificJob(job);
				this.doneJobs.add(cj);
				if(job.status == CommonConstants.JOB_BEEN_STOLEN){
					JobPool.getInstance().removeGivenJobs(cj);
				}
			}
			job = JobPool.getInstance().getFirst();
		}

		if (JobPool.getInstance().isJobPoolDone(this.doneJobs)) {
			long t = System.currentTimeMillis();
			Log.d("QueenBee", "ALL Work done! YAY");// OK it comes here

			this.doJobFinished(t);
		} else {
			if (JobPool.getInstance().isJobListEmpty()) {
				if (ConnectionFactory.getInstance().isStealing) {
					this.steal();
				} else {
					System.exit(parentContext.RESULT_OK);
				}

			}

		}
		time = System.currentTimeMillis() - time;
		TimeMeter.getInstance().addToCalculateTime(time);
	}

	private void steal() {
		try {
			StealFromWorkersThread t = new StealFromWorkersThread(
					this.parentContext);
			t.start();//November
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public abstract CompletedJob doAppSpecificJob(Object pParam);

	public abstract ResultFactory getResultFactory();

	@Override
	public void onResultsRead(ReceivedResults pRes) {
//		Log.d("Results", "QueenBee: onResultsRead mode: "+pRes.resultMode);
		String[] results = null;
		String num[];
		// System.out.println("Results have been received!");
		if (pRes.resultMode == CommonConstants.READ_STRING_MODE) {// RESULTPfilename0:3:Pfilename1:0:#
			Log.d("Results", "CommonConstants.READ_STRING_MODE");
			pRes.stringResults = pRes.stringResults.substring(
					CommonConstants.RESULT_SYMBOL.length() + 1,
					pRes.stringResults.length());
			// filename0:3:Pfilename1:0
			results = pRes.stringResults
					.split(FaceConstants.FACE_RESULT_BREAKER);
			for (int i = 0; i < results.length; i++) {
				num = results[i].split(":");
				getResultFactory().addToMap(num[0], Integer.parseInt(num[1]));

				CompletedJob cj = new CompletedJob(
						CommonConstants.READ_STRING_MODE, num[0], -1, null);
				cj.intValue = Integer.parseInt(num[1]);
				this.doneJobs.add(cj);

				getResultFactory().addToDoneDobMap(pRes.fromWorker);
//				Log.d("Results", results[i]);
				JobPool.getInstance().incrementDoneJobCount();

			}
			JobPool.getInstance().removeGivenJobs(results, pRes.fromWorker);

		}else if (pRes.resultMode == CommonConstants.READ_COMPLETED_JOB_OBJECT_ARRAY_MODE) {
			Log.d("Results", "CommonConstants.READ_COMPLETED_JOB_OBJECT_ARRAY_MODE");
			ResultTransmitObject[] jobs = (ResultTransmitObject[]) pRes.resultData;
			for(ResultTransmitObject rto: jobs){
				int intval =-1;
				switch(rto.mode){
				case CommonConstants.READ_COMPLETED_JOB_OBJECT_ARRAY_MODE:
					if(rto.mixedModeArray!=null && rto.mixedModeArray.length>1){
						switch(rto.mixedModeArray[0]){
						case CommonConstants.READ_INT_MODE:
								intval = rto.intValue;
								switch(rto.mixedModeArray[1]){
								case CommonConstants.READ_INT_ARRAY_MODE:
									getResultFactory().addToMap(String.valueOf(intval), rto.intArrayValue);
									
									CompletedJob cj = new CompletedJob(
											CommonConstants.READ_INT_ARRAY_MODE, String.valueOf(intval), intval,  null);
									cj.intArrayValue = rto.intArrayValue;
									cj.id = String.valueOf(intval);
									this.doneJobs.add(cj);
									
									getResultFactory().addToDoneDobMap(pRes.fromWorker);
									Log.d("Results", "index: "+rto.intValue);
									JobPool.getInstance().incrementDoneJobCount();
									break;
								}
								
								break;
						}
							
							
					}
					break;
				}
			
				
			}
			JobPool.getInstance().removeGivenJobs(jobs, pRes.fromWorker);
		}

		Log.d("Jobs", "onResultsRead Done jobs : " + JobPool.getInstance().getDoneJobs()
				+ " recent from : " + pRes.fromWorker);
		if (JobPool.getInstance().isJobPoolDone(this.doneJobs)) {
			long t = System.currentTimeMillis();
			// long time = System.currentTimeMillis() -
			// TimeMeter.getInstance().getOverallTotalTime();
			// time+=TimeMeter.getInstance().getPiconetTime();
			Log.d("Results", "All Work done! YAY");

			// FileFactory.getInstance().writeFileWithDate("TIME : "+time);
			// FileFactory.getInstance().writeFileWithDate("*************************");
			this.doJobFinished(t);
		}

	}

	private void endReading() {
		Intent talkWithServiceIntent = new Intent(
				CommonConstants.BROADCAST_DELE_STOP_READING);
		this.parentContext.sendBroadcast(talkWithServiceIntent);
	}

	private void doJobFinished(long t) {
		JobPool.getInstance().shutDownExecutor();
//		boolean t2 = getResultFactory().checkResults(this.doneJobs);
//		Log.d("Results","getResultFactory().checkResults = "+t2);
		endReading();

		ConnectionFactory.getInstance().isJobDone = true;
		
		
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = this.parentContext.registerReceiver(null, ifilter);
		int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

		float batteryPct = level / (float)scale;
		
		this.testAndCompare(t, batteryPct);
	}

	private void testAndCompare(long pCurrent, float batteryPct) {

		System.out.println("ALL DONE!. ");
		long mandelTime = pCurrent - TimeMeter.getInstance().getInitJobsTime();

		boolean result = true;
		long ownCalcTime = CommonConstants.MONOTIME;
		float startbattery = TimeMeter.getInstance().getBatteryLevel();
		Log.d("Queenbee", "DONE - " + result);
		float speedup = (float) ownCalcTime / (float) mandelTime;
		StringBuffer s = new StringBuffer("\n"
				+ DateFormat.getDateTimeInstance().format(new Date())
				+ "  Result = " + result);
		s.append("ownCalcTime = " + ownCalcTime + "  mandelTime = "
				+ mandelTime + "  Speedup = " + speedup + "\n");
		s.append("PARAMS : WorkChunk = " + CommonConstants.WORK_CHUNK
				+ " COMPLETED_JOBS_BUFFER = "
				+ CommonConstants.COMPLETED_JOBS_BUFFER + "  STEAL_CHUNK = "
				+ CommonConstants.STEAL_CHUNK + " STEAL LIMIT = "
				+ CommonConstants.STEAL_LIM + "  nodes = "
				+ ConnectionFactory.getInstance().getNumberOfWorkers()
				+ " MAX_FILES_PER_MSG = " + CommonConstants.MAX_FILES_PER_MSG);

		/*s.append("\n Breakdown : "
				+ " set up job pool time = "
				+ TimeMeter.getInstance().getJobPoolSetTime()
				+ " Computation time = "
				+ TimeMeter.getInstance().getCalculateTime()
				+ " Time to read results = "
				+ TimeMeter.getInstance().getReadingResults()
				+ " Time to Steal and get stolen = "
				+ TimeMeter.getInstance().getTotalStealTime()
				+ " Time to Steal from Workers = "
				+ TimeMeter.getInstance().getStealFromWorkersTime()
				+ " Time to Zip = "
				+ TimeMeter.getInstance().getZipTime()
				+ " Time gap between Dele start and worker param receive = "
				+ JobInitializer.getInstance(parentContext).getTimeGap()
				+ " Time gap between QueenBee thread start and click Offload = "
				+ TimeMeter.getInstance().getDeleThreadGapTime()+"\n battery start= "+startbattery+" battery end= "+batteryPct+"  Battery Usage= "+(batteryPct-startbattery));*/

		
		s.append("battery start= "+startbattery+" battery end= "+batteryPct+"  Battery Usage= "+(batteryPct-startbattery));
		Log.d("Queenbee", s.toString());
		System.out.println("ALL DONE!.  S = " + speedup);
		getResultFactory().setSpeedup(speedup);

		StringBuffer s2 = new StringBuffer();
		s2.append("Jobs by Delegator: "
				+ getResultFactory().getJobsByDelegator() + "  \n");

//		HashMap<String, Integer> map = getResultFactory().getDoneJobMap();
		HashMap<String, Integer> map = getResultFactory().getDoneJobMap();
		Iterator<Entry<String, Integer>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Integer> pairs = it.next();
			String key = pairs.getKey();

			String name = null;

			Iterator<WorkerInfo> iter = ConnectionFactory.getInstance()
					.getConnectedWorkerList().iterator();
			while (iter.hasNext()) {
				WorkerInfo info = iter.next();
				if (info != null && info.getAddress().equals(key)) {
					name = info.toString();
				}
			}

			Log.d("Queenbee", "key = " + key + " name: " + name);
			s2.append(name + " = " + pairs.getValue() + "  ");
			it.remove(); // avoids a ConcurrentModificationException
		}

		Log.d("Queenbee", s2.toString());
		getResultFactory().setCompareString(s.toString() + " " + s2.toString());
		try {
			FileFactory.getInstance().writeFile(
					CommonConstants.DEBUG_FILE_PATH, s.toString());
			FileFactory.getInstance().writeFile(
					CommonConstants.DEBUG_FILE_PATH, s2.toString());
			FileFactory.getInstance().logCalcTimesToFile();
			FileFactory.getInstance().writeJobsDoneToFile();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
