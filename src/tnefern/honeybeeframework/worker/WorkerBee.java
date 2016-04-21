package tnefern.honeybeeframework.worker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import tnefern.honeybeeframework.apps.facematch.FaceConstants;
import tnefern.honeybeeframework.apps.facematch.FaceResult;
import tnefern.honeybeeframework.apps.facematch.SearchImage;
import tnefern.honeybeeframework.common.CommonConstants;
import tnefern.honeybeeframework.common.CompletedJob;
import tnefern.honeybeeframework.common.FileFactory;
import tnefern.honeybeeframework.common.Job;
import tnefern.honeybeeframework.common.JobParams;
import tnefern.honeybeeframework.common.JobPool;
import tnefern.honeybeeframework.common.Slave;
import tnefern.honeybeeframework.stats.TimeMeter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

/**
 * A WorkerBee represents a Worker device.
 * 
 * @author tnfernando
 * 
 */
public abstract class WorkerBee extends Slave {

	private ArrayList<CompletedJob> doneJobs = new ArrayList<CompletedJob>();

	private int index = -1;
	private boolean isStolen = false;

	public WorkerBee(Context pAct, String pActivityClass, JobParams pMsg,
			int pIndex, boolean stolen) {
		super(true, true, pAct, pActivityClass, pMsg);
		this.index = pIndex;
		this.isStolen = stolen;
	}

	public abstract CompletedJob doAppSpecificJob(Object pParam);

	// private void runFaceMatch(String pS) {
	//
	// String extension = FileFactory.getInstance().getFileExtension(pS);
	// if ((extension.equalsIgnoreCase(FaceConstants.FILE_EXTENSION_JPEG))
	// || (extension
	// .equalsIgnoreCase(FaceConstants.FILE_EXTENSION_JPG))) {
	// Integer res = Integer.valueOf(imageSearch.search(pS));
	// // results.add(res);
	// CompletedJob cj = new CompletedJob(
	// CommonConstants.READ_STRING_MODE, FileFactory.getInstance()
	// .getFileNameFromFullPath(pS), -1, null);
	// cj.intValue = res.intValue();
	// // cj.index = i;
	// this.doneJobs.add(cj);
	// FaceResult.getInstance().addToMap(cj.stringValue, cj.intValue);
	// } else {
	// Log.d("EXTENSION OTHER = ", extension);
	// }
	//
	// }

	@Override
	protected void doOwnWorkForWorker() throws IOException {
		long time = System.currentTimeMillis();

		Job job = JobPool.getInstance().getFirst();
		if (job != null) {
			while (job != null) {
				if (job.jobParams != null) {
					// runFaceMatch(job.jobParams);
					this.doneJobs.add(this.doAppSpecificJob(job.jobParams));
					/*
					 * the following code is for testing purposes ONLY. It aims
					 * to simulate a 'weak' worker device by sleeping the
					 * thread. Should be commented out for release.
					 */
					// try {
					// Thread.sleep(2000);
					// } catch (InterruptedException e) {
					// e.printStackTrace();
					// }

//					for (int i = 0; i < 50000000; i++) {
//						for (int j = 0; j < 50000000; j++) {
//
//						}
//					}
//					for (int i = 0; i < 50000000; i++) {
//						for (int j = 0; j < 50000000; j++) {
//
//						}
//					}
//					for (int i = 0; i < 50000000; i++) {
//						for (int j = 0; j < 50000000; j++) {
//
//						}
//					}
//					for (int i = 0; i < 50000000; i++) {
//						for (int j = 0; j < 50000000; j++) {
//
//						}
//					}
//					for (int i = 0; i < 50000000; i++) {
//						for (int j = 0; j < 50000000; j++) {
//
//						}
//					}
//					for (int i = 0; i < 50000000; i++) {
//						for (int j = 0; j < 50000000; j++) {
//
//						}
//					}
//					for (int i = 0; i < 50000000; i++) {
//						for (int j = 0; j < 50000000; j++) {
//
//						}
//					}
//					for (int i = 0; i < 50000000; i++) {
//						for (int j = 0; j < 50000000; j++) {
//
//						}
//					}
//					for (int i = 0; i < 50000000; i++) {
//						for (int j = 0; j < 50000000; j++) {
//
//						}
//					}
//					for (int i = 0; i < 50000000; i++) {
//						for (int j = 0; j < 50000000; j++) {
//
//						}
//					}
				}
				job = JobPool.getInstance().getFirst();

				if (this.doneJobs.size() >= CommonConstants.COMPLETED_JOBS_BUFFER) {
					this.sendResults(false);
				}
			}

			time = System.currentTimeMillis() - time;
			TimeMeter.getInstance().addToCalculateTime(time);

			if (JobPool.getInstance().isJobListEmpty()) {
				this.sendResults(true);
			}
		}

	}

	private void sendResults(boolean isSteal) {
		CompletedJob[] cjobs = null;
		synchronized (this.doneJobs) {
			cjobs = new CompletedJob[this.doneJobs.size()];
			cjobs = this.doneJobs.toArray(cjobs);
			this.doneJobs.clear();
		}
		if (cjobs != null && cjobs.length > 0 && cjobs[0] != null) {
			switch (cjobs[0].mode) {
			case CommonConstants.READ_STRING_MODE:
				try {
					StringBuffer res = new StringBuffer();
					res.append(CommonConstants.RESULT_SYMBOL);
					for (CompletedJob cj : cjobs) {
						res.append(FaceConstants.FACE_RESULT_BREAKER);
						res.append(cj.stringValue);
						res.append(CommonConstants.APP_REQUEST_SEPERATOR);
						res.append(cj.intValue);

					}
					res.append(CommonConstants.MSG_BREAK);

					Intent resultIntent = new Intent(
							CommonConstants.BROADCAST_WORKER_SEND_RESULTS_ACTION);
					resultIntent.putExtra(CommonConstants.RESULT_STRING_TYPE,
							res.toString());
					this.parentActivity.sendBroadcast(resultIntent);

				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case CommonConstants.READ_COMPLETED_JOB_OBJECT_MODE:
				Intent resultIntent = new Intent(
						CommonConstants.BROADCAST_WORKER_SEND_RESULTS_ACTION);
				resultIntent
						.putExtra(CommonConstants.RESULT_OBJECT_TYPE, cjobs);
				this.parentActivity.sendBroadcast(resultIntent);
				break;
			case CommonConstants.READ_COMPLETED_JOB_OBJECT_ARRAY_MODE:

				try {
					// StringBuffer res = new StringBuffer();
					// res.append(CommonConstants.RESULT_SYMBOL);
					// for (CompletedJob cj : cjobs) {
					// res.append(FaceConstants.FACE_RESULT_BREAKER);
					// res.append(cj.stringValue);
					// res.append(CommonConstants.APP_REQUEST_SEPERATOR);
					// res.append(cj.intValue);
					//
					// }
					// res.append(CommonConstants.MSG_BREAK);
					ResultTransmitObject[] resObjs = new ResultTransmitObject[cjobs.length];
					int i = 0;

					for (CompletedJob cj : cjobs) {
						resObjs[i] = new ResultTransmitObject();
						resObjs[i].mode = cj.mode;
						resObjs[i].mixedModeArray = cj.mixedModeArray;
						resObjs[i].intValue = cj.intValue;
						resObjs[i].intArrayValue = cj.intArrayValue;
						resObjs[i].stringValue = cj.stringValue;
						resObjs[i].identifier = cj.id;
						i++;
					}

					// test
					// try
					// {
					// final File f = new File(
					// Environment.getExternalStorageDirectory()+"/testSER/resobj.ser");
					// File dirs = new File(f.getParent());
					// if (!dirs.exists())
					// dirs.mkdirs();
					// f.createNewFile();
					// FileOutputStream fileOut =
					// new FileOutputStream(f);
					// ObjectOutputStream out = new ObjectOutputStream(fileOut);
					// out.writeObject(resObjs);
					// out.close();
					// fileOut.close();
					// Log.d("sendResults","Serialized data is saved in /testSER/resobj.ser");
					//
					// FileInputStream fileIn = new FileInputStream(f);
					// ObjectInputStream in = new ObjectInputStream(fileIn);
					// ResultTransmitObject[] resObjs2 =
					// (ResultTransmitObject[]) in.readObject();
					// in.close();
					// fileIn.close();
					// if(resObjs2!=null){
					// Log.d("sendResults","DESerialized data");
					// }
					//
					// }catch(IOException eee)
					// {
					// eee.printStackTrace();
					// return;
					// }catch(ClassNotFoundException c)
					// {
					// System.out.println("Employee class not found");
					// c.printStackTrace();
					// return;
					// }
					// test

					Intent resultIntent2 = new Intent(
							CommonConstants.BROADCAST_WORKER_SEND_RESULTS_ACTION);

					Bundle bundle = new Bundle();
					bundle.putSerializable(
							CommonConstants.RESULT_COMPLETED_JOB_ARRAY_TYPE,
							resObjs);

					resultIntent2.putExtras(bundle);

					// resultIntent2.putExtra(CommonConstants.RESULT_COMPLETED_JOB_ARRAY_TYPE,
					// resObjs);
					this.parentActivity.sendBroadcast(resultIntent2);

				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}

		}

	}

	@Override
	public void run() {
		try {
			this.doOwnWorkForWorker();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
