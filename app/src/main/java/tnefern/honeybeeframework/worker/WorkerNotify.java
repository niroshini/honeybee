package tnefern.honeybeeframework.worker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;

import tnefern.honeybeeframework.apps.facematch.FaceConstants;
import tnefern.honeybeeframework.apps.facematch.JpegFilter;
import tnefern.honeybeeframework.common.CommonConstants;
import tnefern.honeybeeframework.common.FileFactory;
import tnefern.honeybeeframework.common.Job;
import tnefern.honeybeeframework.common.JobParams;
import tnefern.honeybeeframework.common.JobPool;
import tnefern.honeybeeframework.common.Slave;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.util.Log;

/**
 * This class is used by the Worker side. When the jobs are received by the
 * WifiD/BT service, the service calls this class. This class stores an instance
 * of WorkerBee thread.
 * 
 * Previously assignJobsForWorker() was in class Stealer. It has been moved to
 * here.
 * 
 * @author tnfernando
 * 
 */
public class WorkerNotify {
	private static WorkerNotify theInstance = null;
	// private String workerAppActivity = null;
	private Context parentActivity = null;
	private WorkerBee ownWorker = null;
	private boolean isInit = false;
	private int index = 2;
	boolean isStolen = false;
	private ArrayBlockingQueue<Job> tempJobStore = new ArrayBlockingQueue<Job>(
			10);// for worker

	private WorkerNotify() {
	}

	public static WorkerNotify getInstance(Context pCont) {
		if (theInstance == null) {
			theInstance = new WorkerNotify();
			theInstance.parentActivity = pCont;
		}
		return theInstance;
	}

	public static WorkerNotify getInstance() {
		if (theInstance == null) {
			theInstance = new WorkerNotify();
		}
		return theInstance;
	}

	/**
	 * Called when ever files are received by the Worker
	 * 
	 * @param pMsg
	 *            Jobs received
	 * @param pClass
	 *            worker activity class
	 * @param isStolen
	 *            whether these are stolen or regular jobs.
	 * @throws IOException
	 */
	public void assignJobsForWorker(JobParams pMsg, String pClass,
			boolean isStolen) throws IOException {

		this.isStolen = isStolen;
		// Slave ownSlave = new WorkerBee(theInstance.parentActivity,
		// pClass, pMsg,index, isStolen);

		this.populateWithJob(pMsg);
		// JobPool.getInstance().submitJobWorker(ownSlave);
		JobPool.getInstance().submitJobWorker(this.ownWorker);

	}

	public void setWorkerBee(WorkerBee pWBee) {
		this.ownWorker = pWBee;
	}

	public void deleteJobData() {
		File dir = new File(Environment.getExternalStorageDirectory() + "/"
				+ FaceConstants.FACE_MATCH_DIR);
		// delete the contents of the faceMatch folder
		FileFactory.getInstance().deleteFolderContents(dir);
	}

	private void populateWithJob(JobParams workerParams) {
		if (workerParams != null) {
			// Log.d("WorkerNotify", workerParams.paramMode + "");
			if (workerParams.paramMode == CommonConstants.READ_FILE_NAME_MODE) {
				Job job = new Job(workerParams.paramObject,
						CommonConstants.JOB_NOT_GIVEN,
						CommonConstants.READ_FILE_NAME_MODE,
						CommonConstants.READ_STRING_MODE);
				job.f = (File) workerParams.paramObject;
				job.jobParams = workerParams.paramsString;

				if (job.o instanceof File) {// facematch
					File unFile = (File) job.o;
					String extension = FileFactory.getInstance()
							.getFileExtension(unFile.getAbsolutePath());
					if (extension
							.equalsIgnoreCase(FaceConstants.FILE_EXTENSION_ZIP)) {
						try {
							File sdDir = Environment
									.getExternalStorageDirectory();
							File dir = new File(sdDir,
									FaceConstants.UNZIP_FILE_PATH + index);
							dir.mkdir();
							FileFactory.getInstance().unzip(unFile, dir);
							unFile.delete();

						} catch (IOException e) {
							e.printStackTrace();
						}
						// indexes.add(new Integer(i));
					}
				}
				Collection<File> filesInfolder = FileFactory.getInstance()
						.listFiles(new File(getUnzipDirectoryPath(index)),
								new JpegFilter[] { new JpegFilter() }, 0);
				// Log.d("WorkerNotify", "files logged");
				if (this.isStolen) {
					if (filesInfolder != null && !filesInfolder.isEmpty()) {
						Job[] jobsStolen = new Job[filesInfolder.size()];
						int i = 0;
						for (File file : filesInfolder) {
							jobsStolen[i] = new Job(file.getAbsolutePath(), -1,
									CommonConstants.READ_FILES_MODE,
									CommonConstants.READ_STRING_MODE);
							// Log.d("WorkerNotify", "stolen files logged");
							i++;
						}

						// jobsStolen = filesInfolder.toArray(jobsStolen);
						JobPool.getInstance().addJobs(jobsStolen, isStolen);
					}

				} else {
					for (File file : filesInfolder) {
						JobPool.getInstance().addJob(
								new Job(file.getAbsolutePath(), -1,
										CommonConstants.READ_FILES_MODE,
										CommonConstants.READ_STRING_MODE));
						// Log.d("WorkerNotify", file.getAbsolutePath());
					}
				}

				// }
				index++;

			} else if (workerParams.paramMode == CommonConstants.READ_STRING_MODE) {
				Job job = new Job(workerParams.paramObject,
						CommonConstants.JOB_NOT_GIVEN,
						CommonConstants.READ_STRING_MODE,
						CommonConstants.READ_STRING_MODE);
				job.jobParams = workerParams.paramsString;

				String[] sArr = workerParams.paramsString
						.split(CommonConstants.PARTITION_BREAK);
				Job[] jobList = new Job[sArr.length];
				if (sArr != null && sArr.length > 0) {
					for (int i = 0; i < sArr.length; i++) {
						jobList[i] = new Job(sArr[i],
								CommonConstants.JOB_NOT_GIVEN,
								CommonConstants.READ_STRING_MODE,
								CommonConstants.READ_STRING_MODE);
						Log.d("assembleJobList", " sArr[" + i + "] = "
								+ sArr[i]);
					}

				}
				JobPool.getInstance().addJobs(jobList, isStolen);
			}
		}

	}

	public void updateIndex(int ind) {
		this.index = ind;
	}

	private void assembleJobList(JobParams pworkerParams) {
		// Log.d("WorkerNotify",
		// "INIT:assembleJobList assembleJobList mode="+pworkerParams.paramMode);
		int mode = pworkerParams.paramMode;

		if (mode == CommonConstants.READ_STRING_MODE) {
			onStringParamMode(CommonConstants.READ_STRING_MODE,
					CommonConstants.JOB_NOT_GIVEN, -1, pworkerParams);
		} else if (mode == CommonConstants.READ_FILE_MODE) {
			// do what ever needs to be done
			// if zip file, unzip
			// String extName = this.workerParams.paramsString;

			Job job = new Job(pworkerParams.paramsString,
					CommonConstants.JOB_NOT_GIVEN,
					CommonConstants.READ_FILE_MODE);
			JobPool.getInstance().addJob(job);
		} else if (mode == CommonConstants.READ_FILES_MODE) {
			if (pworkerParams.paramObject != null) {
				Job job = new Job(pworkerParams.paramObject,
						CommonConstants.JOB_NOT_GIVEN,
						CommonConstants.READ_FILES_MODE,
						CommonConstants.READ_STRING_MODE);
				JobPool.getInstance().addJob(job);
			} else if (pworkerParams.paramsString != null) {
				onStringParamMode(CommonConstants.READ_FILES_MODE,
						CommonConstants.READ_STRING_MODE,
						CommonConstants.JOB_BEEN_STOLEN, pworkerParams);
			}

		} else if (mode == CommonConstants.READ_FILE_STRING_MODE) {
			onStringParamMode(CommonConstants.READ_FILE_STRING_MODE,
					CommonConstants.JOB_NOT_GIVEN, -1, pworkerParams);
		} else if (mode == CommonConstants.READ_FILE_NAME_MODE) {
			Job job = new Job(pworkerParams.paramObject,
					CommonConstants.JOB_NOT_GIVEN,
					CommonConstants.READ_FILE_NAME_MODE,
					CommonConstants.READ_STRING_MODE);
			job.f = (File) pworkerParams.paramObject;
			job.jobParams = pworkerParams.paramsString;
			JobPool.getInstance().addJob(job);
		}

	}

	private void onStringParamMode(int pWorkMode, int pStealMode, int pStatus,
			JobParams pworkerParams) {
		String[] sArr = pworkerParams.paramsString
				.split(CommonConstants.PARTITION_BREAK);
		ArrayList<Job> jobT = new ArrayList<Job>();
		if (sArr != null && sArr.length > 0) {
			for (int i = 0; i < sArr.length; i++) {
				Job job = new Job(sArr[i], pStatus, pWorkMode);
				job.stealMode = pStealMode;
				jobT.add(job);
				// Log.d("assembleJobList", " sArr[" + i + "] = " + sArr[i]);
			}

		}
		JobPool.getInstance().addJobs(jobT);
	}

	private void addJobsToTempStorage(JobParams pMsg, boolean isStolen) {
		if (pMsg != null) {
			// Log.d("WorkerNotify", pMsg.paramMode + "");
			if (pMsg.paramMode == CommonConstants.READ_FILE_NAME_MODE) {
				Job job = new Job(pMsg.paramObject,
						CommonConstants.JOB_NOT_GIVEN,
						CommonConstants.READ_FILE_NAME_MODE,
						CommonConstants.READ_STRING_MODE);
				job.f = (File) pMsg.paramObject;
				job.jobParams = pMsg.paramsString;
				// this.tempJobStore.add(job);
				AddToJobPool atjp = new AddToJobPool(job, isStolen);
				JobPool.getInstance().executeRunnable(atjp);

			}
		}

	}

	public Job[] retreiveAllJobs() {
		return this.tempJobStore.toArray(new Job[0]);
	}

	public synchronized Job[] retrieveAndRemoveAllJobs() {
		Job[] jobs = new Job[tempJobStore.size()];
		jobs = this.tempJobStore.toArray(jobs);
		this.tempJobStore.clear();
		return jobs;
	}

	private class AddToJobPool implements Runnable {
		Job toAddd = null;
		boolean isStolen = false;

		AddToJobPool(Job j, boolean pStolen) {
			this.toAddd = j;
			this.isStolen = pStolen;
		}

		@Override
		public void run() {
			if (this.toAddd != null) {
				// JobPool.getInstance().addJob(toAddd);
				populateWithJob(toAddd);
			}

		}

	}

	private String getUnzipDirectoryPath(int i) {
		return Environment.getExternalStorageDirectory().getAbsolutePath()
				+ "/" + FaceConstants.UNZIP_FILE_PATH + i;
	}

	public void populateWithJob(Job pToAdd) {
		if (pToAdd.o instanceof String[]) {
			String[] fileNames = (String[]) pToAdd.o;
			for (int x = 0; x < fileNames.length; x++) {
				String extension = FileFactory.getInstance().getFileExtension(
						fileNames[x]);
				if (extension
						.equalsIgnoreCase(FaceConstants.FILE_EXTENSION_ZIP)) {

					try {
						File sdDir = Environment.getExternalStorageDirectory();
						File dir = new File(sdDir,
								FaceConstants.UNZIP_FILE_PATH + index);
						dir.mkdir();
						File unFile = new File(fileNames[x]);
						FileFactory.getInstance().unzip(unFile, dir);
						unFile.delete();

					} catch (IOException e) {
						e.printStackTrace();
					}
					// indexes.add(new Integer(i));
				} else {
					// indexes.add(new Integer(i));
				}
			}

		} else if (pToAdd.o instanceof File) {// facematch
			File unFile = (File) pToAdd.o;
			String extension = FileFactory.getInstance().getFileExtension(
					unFile.getAbsolutePath());
			if (extension.equalsIgnoreCase(FaceConstants.FILE_EXTENSION_ZIP)) {
				try {
					File sdDir = Environment.getExternalStorageDirectory();
					File dir = new File(sdDir, FaceConstants.UNZIP_FILE_PATH
							+ index);
					dir.mkdir();
					FileFactory.getInstance().unzip(unFile, dir);
					unFile.delete();

				} catch (IOException e) {
					e.printStackTrace();
				}
				// indexes.add(new Integer(i));
			}
		}
		Collection<File> filesInfolder = FileFactory.getInstance().listFiles(
				new File(getUnzipDirectoryPath(index)),
				new JpegFilter[] { new JpegFilter() }, 0);
		// Log.d("WorkerNotify", "files logged");
		if (this.isStolen) {
			if (filesInfolder != null && !filesInfolder.isEmpty()) {
				Job[] jobsStolen = new Job[filesInfolder.size()];
				int i = 0;
				for (File file : filesInfolder) {
					jobsStolen[i] = new Job(file.getAbsolutePath(), -1,
							CommonConstants.READ_FILES_MODE);
					// Log.d("WorkerNotify", "stolen files logged");
					i++;
				}

				// jobsStolen = filesInfolder.toArray(jobsStolen);
				JobPool.getInstance().addJobs(jobsStolen, isStolen);
			}

		} else {
			for (File file : filesInfolder) {
				JobPool.getInstance().addJob(
						new Job(file.getAbsolutePath(), -1,
								CommonConstants.READ_FILES_MODE));
				// Log.d("WorkerNotify", file.getAbsolutePath());
			}
		}

		// }
		index++;

	}
}
