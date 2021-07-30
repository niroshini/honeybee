package tnefern.honeybeeframework.common;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import tnefern.honeybeeframework.apps.facematch.FaceResult;
import tnefern.honeybeeframework.delegator.AppRequest;
import tnefern.honeybeeframework.delegator.QueenBee;
import tnefern.honeybeeframework.delegator.ResultFactory;
import tnefern.honeybeeframework.delegator.WorkerInfo;
import tnefern.honeybeeframework.stats.JobInfo;
import tnefern.honeybeeframework.stats.TimeMeter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class JobInitializer {
	// private Mandelbrot mandelObj = null;
	private static int iterations = -1;
	private static int N = -1;

	public static int[][] fullSet = null;
	private static Object appInfo = null;
	private static int nodes = 0;
	// private static int partition = CommonConstants.WORK_CHUNK;
	private static int NperNode = 0;// how many partitions per node
	private static int NperDele = 0;
	private static int remPerLastWorker = 0;

	private static final String TAG = "Stealer";
	private long distributeTime = 0;

	private long mandelTime = 0;
	private long ownCalcTime = 0;
	private long readTimes = 0;
	private long startTime = 0;

	private long picoNetTime = 0;
	private float speedup = 0.0f;
	private VictimThread victim = null;
	// private Slave ownSlave = null;

	public int testRead = 0;// TEST
	private long timeGap = 0;

	private static JobInitializer theInstance = null;
	public static boolean isJobFinished = false;
	public static boolean isJobsInitialized = false;

	private Context parentActivity = null;
	private String delegatorAppActivity = null;
	private String workerAppActivity = null;
	private int stealMode = -1;
	// private WorkerInfo prev = null;
	private int prevIndex = -1;

	public static final String BROADCAST_STEALER_JOBS_TO_TRANSMIT_READY_ACTION = "org.com.honeybeecrowdDemo.common.jobsreadytotransmit";
	public static final String BROADCAST_DELEGATOR_BEING_A_VICTIM_ACTION = "org.com.honeybeecrowdDemo.common.delegatorbeingavictim";

	/**
	 * contains BT addresses of workers who were victims
	 */
	private ArrayList<String> stealList = new ArrayList<String>();
	private Slave ownSlave;

	// private ProgressDialog pd;
	private JobInitializer() {

	}

	/**
	 * Initializes the Stealer instance from app parameters
	 * 
	 * @param pNodes
	 *            number of workers (not including delegator)
	 * @param pAct
	 *            Activity which called this
	 * @return
	 */
	public static JobInitializer getInstance(int pNodes, Activity pAct) {// should
		// only
		// be
		// called
		// in
		// the
		// Delegating
		// class
		if (theInstance == null) {
			theInstance = new JobInitializer();
			// iterations = pIter;
			// this.startTime = pStart;
			nodes = pNodes;

		}
		theInstance.parentActivity = pAct;
		return theInstance;
	}

	/**
	 * Initializes the Stealer instance from app parameters
	 * 
	 * @param pObj
	 *            contains application specific information
	 * @param pN
	 *            number of job partitions
	 * @param pNodes
	 *            number of workers (not including delegator)
	 * @param pAct
	 *            Activity which called this
	 * @return
	 */
	public static JobInitializer getInstance(Object pObj, int pN, int pNodes,
			Activity pAct) {// should
		// only
		// be
		// called
		// in
		// the
		// Delegating
		// class
		if (theInstance == null) {
			theInstance = new JobInitializer();
			// iterations = pIter;
			N = pN;
			// this.startTime = pStart;
			nodes = pNodes;
			appInfo = pObj;

		}
		theInstance.parentActivity = pAct;
		return theInstance;
	}

	public static JobInitializer getInstance(int pIter, int pN, int pNodes,
			Activity pAct) {// should
		// only
		// be
		// called
		// in
		// the
		// Delegating
		// class
		if (theInstance == null) {
			theInstance = new JobInitializer();
			iterations = pIter;
			N = pN;
			// this.startTime = pStart;
			nodes = pNodes;
			// fullSet = new int[N][N];

		}
		theInstance.parentActivity = pAct;
		return theInstance;
	}

	public static JobInitializer getInstance(Context pCont) {
		if (theInstance == null) {
			// throw new IllegalArgumentException();
			theInstance = new JobInitializer();
			theInstance.parentActivity = pCont;
		}
		return theInstance;
	}

	public void initJobPool(AppRequest pReqObj) {

		startTime = System.currentTimeMillis();// should change this to include
		N = pReqObj.getNumberOfJobs();
		JobPool.getInstance().initJobPool(pReqObj);

		TimeMeter.getInstance().setJobPoolSetTime(
				System.currentTimeMillis() - startTime);

	}

	public ArrayList<String> getStealList() {
		return this.stealList;
	}

	// public void initJobPool() {
	//
	// startTime = System.currentTimeMillis();// should change this to include
	//
	// // mandelObj = new Mandelbrot(iterations, N / partition, N);
	// // PhotoRequest photo = new PhotoRequest(N , 0, 0);
	// // JobPool.getInstance().initJobPool(pReqObj, N);//temp
	//
	// TimeMeter.getInstance().setJobPoolSetTime(
	// System.currentTimeMillis() - startTime);
	// Log.d("Stealer", "set up job time = "
	// + (System.currentTimeMillis() - startTime));
	//
	// }
	public long getPicoNetTime() {
		return picoNetTime;
	}

	/**
	 * 
	 * @param isOwnWork
	 *            indicates if Delegating device also work
	 * @throws IOException
	 */
	public Runnable assignStolenJobsForDelegator(boolean isDeleWork,
			JobParams pMsg, QueenBee pBee) throws IOException {
		// Log.d("Delegator - assignStolenJobsForDelegator",
		// "delagator doing stolen work");
		pMsg.paramMode = JobPool.getInstance().getMode();
		// ownSlave = new Slave(isDeleWork, false, theInstance.parentActivity,
		// this.delegatorAppActivity, pMsg);//November

		Slave stealSlave = new Slave(isDeleWork, false,
				theInstance.parentActivity, pMsg, pBee);
		stealSlave.assembleJobList(this.stealMode,
				CommonConstants.JOB_BEEN_STOLEN);
		// Thread selfT = new Thread(ownSlave);
		// selfT.start();
		return stealSlave;

	}

	public Runnable assignAddebBackJobsForDelegator(boolean isDeleWork,
			QueenBee pBee) throws IOException {
		// Log.d("Delegator - assignStolenJobsForDelegator",
		// "delagator doing stolen work");
		// ownSlave = new Slave(isDeleWork, false, theInstance.parentActivity,
		// this.delegatorAppActivity, pMsg);//November

		Slave addedBackSlave = new Slave(isDeleWork, false,
				theInstance.parentActivity, pBee);
		return addedBackSlave;

	}

	private Runnable assignJobsForDelegator(boolean isDeleWork,
			String pActClass, QueenBee pBee) throws IOException {

		this.delegatorAppActivity = pActClass;
		// ownSlave = new Slave(isDeleWork, false, theInstance.parentActivity,
		// this.delegatorAppActivity, null);//niro

		ownSlave = new Slave(isDeleWork, false, theInstance.parentActivity,
				pBee);
		timeGap = System.currentTimeMillis();
		return ownSlave;
	}

	public int getStealMode() {
		return this.stealMode;
	}

	public void setStealMode(int pSteaLMode) {
		this.stealMode = pSteaLMode;
	}

	public Object fetchJobsToTransmitToWorker(int pNumJobs) {
		ArrayList<Job> list = JobPool.getInstance().fetchFilesToWorkerTransmit(
				pNumJobs);
		return null;
	}

	// public Runnable assignJobs(boolean isOwnWork, boolean isDelegator,
	// JobParams pMsg, String pActClassDelegator, String pActClassWorker,
	// QueenBee pBee)
	// throws IOException {//niro

	public Runnable assignJobs(boolean isOwnWork, boolean isDelegator,
			String pActClassDelegator, QueenBee pBee) throws IOException {
		Runnable delegatorR = null;
		if (isDelegator) {
			if (isOwnWork) {// delegator also works
			// if (pMsg == null) {// delagator doing initially allocated work

				// delegatorR = assignJobsForDelegator(isOwnWork,
				// pActClassDelegator);//niro

				delegatorR = assignJobsForDelegator(isOwnWork,
						pActClassDelegator, pBee);
				isJobsInitialized = true;
				// }

			}

			return delegatorR;
		}

		return null;
	}

	private void signalWifiDirectService() {
		Intent talkWithServiceIntent = new Intent(
				BROADCAST_STEALER_JOBS_TO_TRANSMIT_READY_ACTION);
		parentActivity.sendBroadcast(talkWithServiceIntent);
	}

	// public void assignJobsForWorker(JobParams pMsg, String pClass)
	// throws IOException {// this is not called in facematch
	// timeGap = System.currentTimeMillis();
	// this.workerAppActivity = pClass;
	// ownSlave = new WorkerBee(theInstance.parentActivity,
	// this.workerAppActivity, pMsg, -1, false);
	// ownSlave.assembleJobList(-1, -1);
	// // Thread t = new Thread(ownSlave);
	// JobPool.getInstance().submitJobWorker(ownSlave);
	// timeGap = System.currentTimeMillis() - timeGap;
	// FileFactory.getInstance().logJobDoneWithDate(
	// "Time gap to start job = " + timeGap + " ms");
	// }

	public static int getN() {
		return N;
	}

	private class GetJobsForStealing implements Callable<Job[]> {

		@Override
		public Job[] call() throws Exception {
			return JobPool.getInstance().letThemSteal();
		}

	}

	private void stealFromMe(OutputStream pOut) throws IOException {
		Log.d("stealFromMe",
				"stealFromMe");
		Victim v = null;
		if (!JobPool.getInstance().isJobListEmpty()) {
			GetJobsForStealing gjfs = new GetJobsForStealing();
			Future<Job[]> stolenJ = JobPool.getInstance()
					.submitCallableforJobs(gjfs);
			Job[] stolen = null;
			try {
				stolen = stolenJ.get();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			} catch (ExecutionException e1) {
				e1.printStackTrace();
			}
			String[] sNames = null;
			if (stolen != null) {
				if (stolen.length > 0) {
					 Log.d("stealFromMe", "stolen.length = " + stolen.length);
					int mode = stolen[0].stealMode;
					// Log.d("stealFromMe", "mode" + mode);
					switch (mode) {
					case CommonConstants.READ_STRING_MODE:
						// Log.d("stealFromMe", "stolen.length = " +
						// stolen.length
						// + " mode = CommonConstants.READ_STRING_MODE");
						// construct param list in String form.
						StringBuffer sBuf = new StringBuffer();
						sBuf.append(CommonConstants.PARAM_SYMBOL);
						for (int i = 0; i < stolen.length; i++) {
							sBuf.append(stolen[i].jobParams);
							sBuf.append(CommonConstants.PARTITION_BREAK);
						}
						sBuf.append(CommonConstants.MSG_BREAK);
//						this.victim = new VictimThread(pOut, sBuf.toString()
//								.getBytes(), CommonConstants.READ_STRING_MODE);
//						this.victim.start();
						
						try {
							Intent havejobstostealIntent = new Intent(
									CommonConstants.BROADCAST_WORKER_HAVE_JOBS_STEAL_ACTION);
							havejobstostealIntent.putExtra(CommonConstants.RESULT_STRING_TYPE, sBuf.toString());
							havejobstostealIntent.putExtra(CommonConstants.RESULT_INT_TYPE,  CommonConstants.READ_STRING_MODE);
							this.parentActivity.sendBroadcast(havejobstostealIntent);

						} catch (Exception e) {
							e.printStackTrace();
						}

						break;

					case CommonConstants.READ_FILE_MODE:
						sNames = new String[stolen.length];
						for (int i = 0; i < sNames.length; i++) {
							sNames[i] = stolen[i].jobParams;
							// get the file names
							// get the File objects corresponding to those file
							// names
							// zip all of them into one File
							// return the *.zip file object
						}

						try {

							String s = FileFactory.getInstance()
									.zipFilesIntoDirectory(sNames,
											CommonConstants.ZIP_FILE_PATH);
							File file = FileFactory.getInstance().getFile(s);

							this.victim = new VictimThread(pOut, FileFactory
									.getInstance().getFileBytes(file),
									CommonConstants.READ_FILE_MODE,
									CommonConstants.PACKET_SIZE, s);
							this.victim.start();

							break;

						} catch (IOException e) {
							e.printStackTrace();
						}

					case CommonConstants.READ_FILES_MODE:
						// Log.d("stealFromMe", "stolen.length = " +
						// stolen.length
						// + " mode = CommonConstants.READ_FILES_MODE");
						sNames = new String[stolen.length];
						for (int i = 0; i < sNames.length; i++) {
							sNames[i] = stolen[i].jobParams;
						}

						try {

							String s = FileFactory.getInstance()
									.zipFilesIntoDirectory(sNames,
											CommonConstants.ZIP_FILE_PATH);
							File file = FileFactory.getInstance().getFile(s);
							break;

						} catch (IOException e) {
							e.printStackTrace();
						}

					case CommonConstants.READ_FILE_NAME_MODE:
						break;
					}
				}else{
					// no more jobs. send a signal saying will not be victimised.
					// StringBuffer sBuf = new StringBuffer();
					// sBuf.append(CommonConstants.NO_JOBS_TO_STEAL);
					 Log.d("stealFromMe", "NO_JOBS_TO_STEAL");
					try {
						Intent nojobstostealIntent = new Intent(
								CommonConstants.BROADCAST_WORKER_NO_JOBS_TO_STEAL_ACTION);
						this.parentActivity.sendBroadcast(nojobstostealIntent);

					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}else{
				// no more jobs. send a signal saying will not be victimised.
				// StringBuffer sBuf = new StringBuffer();
				// sBuf.append(CommonConstants.NO_JOBS_TO_STEAL);
				 Log.d("stealFromMe", "NO_JOBS_TO_STEAL");
				try {
					Intent nojobstostealIntent = new Intent(
							CommonConstants.BROADCAST_WORKER_NO_JOBS_TO_STEAL_ACTION);
					this.parentActivity.sendBroadcast(nojobstostealIntent);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			// no more jobs. send a signal saying will not be victimised.
			// StringBuffer sBuf = new StringBuffer();
			// sBuf.append(CommonConstants.NO_JOBS_TO_STEAL);
			 Log.d("stealFromMe", "NO_JOBS_TO_STEAL");
			try {
				Intent nojobstostealIntent = new Intent(
						CommonConstants.BROADCAST_WORKER_NO_JOBS_TO_STEAL_ACTION);
				this.parentActivity.sendBroadcast(nojobstostealIntent);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private void stealFromDelegator(WorkerInfo pInfo) throws IOException {
		if (!JobPool.getInstance().isJobListEmpty()) {
			GetJobsForStealing gjfs = new GetJobsForStealing();
			Future<Job[]> stolenJ = JobPool.getInstance()
					.submitCallableforJobs(gjfs);
			Job[] stolen = null;
			try {
				stolen = stolenJ.get();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			} catch (ExecutionException e1) {
				e1.printStackTrace();
			}
			String[] sNames = null;
			if (stolen != null) {
				if (stolen.length > 0) {
					JobPool.getInstance().addToGivenJobs(
							new JobsGiven(pInfo.getAddress(), stolen, System.currentTimeMillis()));
					// Log.d("stealFromMe", "stolen.length = " + stolen.length);
					int mode = stolen[0].stealMode;
					// Log.d("stealFromMe", "mode" + mode);
					switch (mode) {
					case CommonConstants.READ_STRING_MODE:
						// Log.d("stealFromMe", "stolen.length = " +
						// stolen.length
						// + " mode = CommonConstants.READ_STRING_MODE");
						// construct param list in String form.
						StringBuffer sBuf = new StringBuffer();
						sBuf.append(CommonConstants.PARAM_SYMBOL);
						for (int i = 0; i < stolen.length; i++) {
							sBuf.append(stolen[i].jobParams);
							sBuf.append(CommonConstants.PARTITION_BREAK);
						}
						sBuf.append(CommonConstants.MSG_BREAK);

						Intent victimStringIntent = new Intent(
								BROADCAST_DELEGATOR_BEING_A_VICTIM_ACTION);

						victimStringIntent.putExtra(
								CommonConstants.VICTIM_MODE_TYPE,
								CommonConstants.VICTIM_STRING_TYPE);
						victimStringIntent.putExtra(
								CommonConstants.VICTIM_STRING_TYPE,
								sBuf.toString());
						victimStringIntent.putExtra(
								CommonConstants.VICTIM_WIFIADDRESS_TYPE,
								pInfo.getWiFiDirectAddress());
						theInstance.parentActivity
								.sendBroadcast(victimStringIntent);

						// this.victim = new VictimThread(pInfo.getBTSocket()
						// .getOutputStream(), sBuf.toString().getBytes(),
						// CommonConstants.READ_STRING_MODE);
						// this.victim.start();

						break;

					case CommonConstants.READ_FILE_MODE:
						sNames = new String[stolen.length];
						for (int i = 0; i < sNames.length; i++) {
							sNames[i] = stolen[i].jobParams;
							// get the file names
							// get the File objects corresponding to those file
							// names
							// zip all of them into one File
							// return the *.zip file object
						}

						try {

							String s = FileFactory.getInstance()
									.zipFilesIntoDirectory(sNames,
											CommonConstants.ZIP_FILE_PATH);
							File file = FileFactory.getInstance().getFile(s);

							// this.victim = new
							// VictimThread(pInfo.getBTSocket()
							// .getOutputStream(), FileFactory
							// .getInstance().getFileBytes(file),
							// CommonConstants.READ_FILE_MODE,
							// CommonConstants.PACKET_SIZE, s);
							// this.victim.start();

							break;

						} catch (IOException e) {
							e.printStackTrace();
						}

					case CommonConstants.READ_FILES_MODE:
						// Log.d("stealFromMe", "stolen.length = " +
						// stolen.length
						// + " mode = CommonConstants.READ_FILES_MODE");
						sNames = new String[stolen.length];
						// int j = 0;
						for (int i = 0; i < sNames.length; i++) {
							sNames[i] = stolen[i].jobParams;
							// Log.d("stealFromMe : READ_FILES_MODE",
							// sNames[i]);
						}

						String[][] sarr = null;
						ArrayList<String[]> sList = null;

						if (sNames.length > CommonConstants.MAX_FILES_PER_MSG) {
							int times = sNames.length
									/ CommonConstants.MAX_FILES_PER_MSG;
							int rem = sNames.length
									% CommonConstants.MAX_FILES_PER_MSG;
							;
							if (rem > 0) {
								sList = new ArrayList<String[]>();
								// sarr = new String[times +
								// 1][CommonConstants.MAX_FILES_PER_MSG];
								// Log.d("stealFromMe : times ", times +
								// " rem: "
								// + rem);

								int x = 0;
								for (int k = 0; k < times; k++) {
									String[] sEle = new String[CommonConstants.MAX_FILES_PER_MSG];
									for (int l = 0; l < CommonConstants.MAX_FILES_PER_MSG; l++) {
										sEle[l] = sNames[x];
										x++;
									}
									sList.add(sEle);
								}
								String[] sEle = new String[rem];
								for (int k = 0; k < rem; k++) {
									sEle[k] = sNames[x];
									x++;
								}
								sList.add(sEle);

							} else {

								sarr = new String[times][CommonConstants.MAX_FILES_PER_MSG];
								// Log.d("stealFromMe : times ", times + " ");

								int x = 0;
								for (int k = 0; k < times; k++) {
									for (int l = 0; l < CommonConstants.MAX_FILES_PER_MSG; l++) {
										sarr[k][l] = sNames[x];
										// if (sNames[x] == null) {
										// Log.d("stealFromMe",
										// "sNames[x] is null : " + x);
										// }
										x++;
									}

								}
							}

						} else {
							sarr = new String[1][sNames.length];
							for (int k = 0; k < sNames.length; k++) {
								sarr[0][k] = sNames[k];
							}
						}

						File[] filesToSend = null;
						if (sList != null && sList.size() > 0) {
							filesToSend = new File[sList.size()];

							int si = 0;
							Iterator<String[]> iter = sList.iterator();

							while (iter.hasNext()) {
								String zipfile;
								try {
									zipfile = FileFactory
											.getInstance()
											.zipFilesIntoDirectory(
													iter.next(),
													CommonConstants.ZIP_FILE_PATH);
									// fileNames.add(zipfile);
									filesToSend[si] = FileFactory.getInstance()
											.getFile(zipfile);
									si++;
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						} else {
							filesToSend = new File[sarr.length];
							int si = 0;
							for (String[] arr : sarr) {
								String zipfile;
								try {
									zipfile = FileFactory
											.getInstance()
											.zipFilesIntoDirectory(
													arr,
													CommonConstants.ZIP_FILE_PATH);
									// fileNames.add(zipfile);
									filesToSend[si] = FileFactory.getInstance()
											.getFile(zipfile);
									si++;
								} catch (IOException e) {
									e.printStackTrace();
								}

							}
						}

						Intent victimIntent = new Intent(
								BROADCAST_DELEGATOR_BEING_A_VICTIM_ACTION)
								.putExtra(CommonConstants.VICTIM_FILE_TYPE,
										filesToSend);
						victimIntent.putExtra(
								CommonConstants.VICTIM_WIFIADDRESS_TYPE,
								pInfo.getWiFiDirectAddress());
						victimIntent.putExtra(CommonConstants.VICTIM_MODE_TYPE,
								CommonConstants.VICTIM_FILE_TYPE);
						theInstance.parentActivity.sendBroadcast(victimIntent);

						break;

					}
				}

			}
		} else {
			try {
				pInfo.sayNoJobsToSteal();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	public void startVictimized(OutputStream pOut, boolean isSlave)
			throws IOException {
		long t = System.currentTimeMillis();
		
		if (isSlave) {// I am a worker
			Log.d("WorkerReadWriteThread",
					"I am a worker: Delegator trying to steal from me");
			stealFromMe(pOut);

		} else {// I am a delegating device
			if (JobInitializer.isJobFinished) {

				try {
					ConnectionFactory.getInstance().relock.lock();
					pOut.write(ByteBuffer.allocate(4)
							.putInt(CommonConstants.READ_INT_MODE).array());
					pOut.write(ByteBuffer.allocate(4)
							.putInt(CommonConstants.TERM_STEALING).array());
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					ConnectionFactory.getInstance().relock.unlock();
				}

			} else {
				 Log.d("Delegator", "I am being a Victim");
				stealFromMe(pOut);
				TimeMeter.getInstance().addToVictimTime(
						new JobInfo(null, System.currentTimeMillis() - t));
			}

		}

	}

	public void startVictimizedForDelegator(WorkerInfo pWorker, boolean isSlave)
			throws IOException {
		long t = System.currentTimeMillis();
		{// I am a delegating device
			if (JobInitializer.isJobFinished) {

				try {
					ConnectionFactory.getInstance().relock.lock();
					pWorker.terminateStealing();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					ConnectionFactory.getInstance().relock.unlock();
				}

			} else {
				stealFromDelegator(pWorker);
				TimeMeter.getInstance().addToVictimTime(
						new JobInfo(null, System.currentTimeMillis() - t));
			}

		}

	}

	public void setPicoNetTime(long picoNetTime) {
		this.picoNetTime = picoNetTime;
	}

	/**
	 * Send a signal to a worker signaling that job is completed and stealing
	 * has stopped. The worker will not try to steal thereon.
	 * 
	 * @throws IOException
	 */
	public void terminateStealing() throws IOException {
		isJobFinished = true;
		for (WorkerInfo value : ConnectionFactory.getInstance()
				.getConnectedWorkerList()) {
			if (value != null) {
				try {
					ConnectionFactory.getInstance().relock.lock();
					value.terminateStealing();

					break;
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					ConnectionFactory.getInstance().relock.unlock();

				}

			}
		}
	}

	private void steal(WorkerInfo pInfo, int pInd) throws IOException {
		this.prevIndex = pInd;
		// Log.d("Delegator", "OK to steal from value.names = "
		// + pInfo.getDevice().getName());
		pInfo.getBTSocket()
				.getOutputStream()
				.write(ByteBuffer.allocate(4)
						.putInt(CommonConstants.READ_INT_MODE).array());
		pInfo.getBTSocket()
				.getOutputStream()
				.write(ByteBuffer.allocate(4)
						.putInt(CommonConstants.INIT_STEALING).array());
		FileFactory.getInstance().logJobDone(
				"trying to steal from " + pInfo.getBtDevice().getName());
	}

	public boolean hasJobs(WorkerInfo pInfo) {
		if (pInfo.connection_mode == ConnectionFactory.BT_MODE) {
			if (pInfo.getBtDevice() != null) {
				return ConnectionFactory.getInstance().getWorkerDeviceMap()
						.get(pInfo.getBtDevice().getAddress()).hasJobs;
			}
		} else if (pInfo.connection_mode == ConnectionFactory.WIFI_MODE) {
			if (pInfo.getWiFiDirectAddress() != null) {
				return ConnectionFactory.getInstance().getWorkerDeviceMap()
						.get(pInfo.getWiFiDirectAddress()).hasJobs;
			}
		}
		return false;

	}

	/**
	 * Select a worker device and steal from one of them.
	 * 
	 * @throws IOException
	 * 
	 */
	public void stealFromWorkers() throws IOException {
		// for (WorkerInfo value : BTFactory.getInstance().getWorkerDeviceMap()
		// .values()) {
		long t = System.currentTimeMillis();
		int sleep = 0;

		outerLoop: while (true) {
			// int i = 0;
			int j = 0;
			for (int i = 0; i < ConnectionFactory.getInstance()
					.getConnectedWorkerList().size(); i++) {
				WorkerInfo value = ConnectionFactory.getInstance()
						.getConnectedWorkerList().get(i);
				if (value != null
						&& value.isConnected
						&& JobPool.getInstance().hasJobsBeenTransmitted(
								value.getAddress()) && hasJobs(value)) {
					if (this.prevIndex == -1) {// we are at the very first time
												// of
												// stealing
						steal(value, i);

						break outerLoop;
					} else if (this.prevIndex == ConnectionFactory
							.getInstance().getConnectedWorkerList().size() - 1) {
						// start again from beginning
						steal(value, 0);

						break outerLoop;
					} else if (this.prevIndex <= i) {
						// do nothing
					} else {
						steal(value, i);

						break outerLoop;
					}

				} else {
					// Log.d("Delegator", "Going to considering STOP stealing");
					if (sleep >= 5) {
						if (JobPool.getInstance().hasAllJobsBeenTransmitted()) {
							sleep = 0;
							// Log.d("Delegator",
							// "Going to STOP stealing from Workers");
							break outerLoop;
						}
					}
					try {
						Thread.sleep(1000);
						sleep++;
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

		TimeMeter.getInstance().logStealFromWorkersTime(
				System.currentTimeMillis() - t);
	}

	public long getTimeGap() {
		return timeGap;
	}

	public void setTimeGap(long timeGap) {
		this.timeGap = timeGap;
	}

	// private class ProgressThread implements Runnable{
	// @Override
	// public void run() {
	// pd = ProgressDialog.show(parentActivity, "Working..", "Calculating Pi",
	// true,
	// false);
	//
	// }
	// }

	// private Handler handler = new Handler() {
	// @Override
	// public void handleMessage(Message msg) {
	// // Looper.prepare();
	// // Looper.loop();
	// pd.dismiss();
	//
	//
	// }
	// };

}
