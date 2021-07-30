package tnefern.honeybeeframework.wifidirect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tnefern.honeybeeframework.common.CommonConstants;
import tnefern.honeybeeframework.common.CompletedJob;
import tnefern.honeybeeframework.common.ConnectionFactory;
import tnefern.honeybeeframework.common.FileFactory;
import tnefern.honeybeeframework.common.JobInitializer;
import tnefern.honeybeeframework.common.JobParams;
import tnefern.honeybeeframework.common.JobPool;
import tnefern.honeybeeframework.stats.TimeMeter;
import tnefern.honeybeeframework.worker.FinishedWorkerActivity;
import tnefern.honeybeeframework.worker.ResultTransmitObject;
import tnefern.honeybeeframework.worker.WorkerNotify;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Environment;
import android.util.JsonReader;
import android.util.Log;

/**
 * For the Worker
 * 
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file/ This
 * is used to SEND the file.
 */
public class WiFiDirectWorkerNonOwnerService extends IntentService {

	private static final int SOCKET_TIMEOUT = 100000;
	public static final String ACTION_INIT_WIFID_CONNECTION = "com.example.android.wifidirect.INITCONNECTION";
	public static final String ACTION_SEND_FILE_WIFID = "com.example.android.wifidirect.SEND_FILE";
	public static final String ACTION_STOP_READING = "com.example.android.wifidirect.STOP_READING";
	public static final String EXTRAS_FILE_PATH = "file_url";
	public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
	public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";
	private int readMode = -1;
	private int readInt = 0;
	private boolean keepReading = true;
	private OutputStream stream = null;
	private InputStream is = null;
	private OutputStream out = null;
	private ObjectOutputStream oos = null;
	private ObjectInputStream ois = null;
	private Socket socket = null;
	private WorkerReadWriteThread wrwt;
	private WorkerHeartBeat heartbeat;
	private String workerClass = null;
	private final IntentFilter intentFilter = new IntentFilter();
	private boolean stolenIncoming = false;
	private WorkerBroadcastReceiver receiver = null;
	private boolean iswriting = false;
	private ConcurrentLinkedQueue<Writer> wrtingThreads = new ConcurrentLinkedQueue<Writer>();
	private ConcurrentLinkedQueue<Writer> resultsThreads = new ConcurrentLinkedQueue<Writer>();
	private ConcurrentLinkedQueue<Writer> stolenThreads = new ConcurrentLinkedQueue<Writer>();
	private int writecount = 0;
	private int writeResultscount = 0;
	private static final String STEAL_FROM_DELEGATOR = "steal";
	private static final String NOTHING_TO_GIVE_DELEGATOR = "nothingtogive";
	private static final String SEND_RESULTS_TO_DELEGATOR = "sendresults";
	private static final String SEND_STOLEN_JOBS_TO_DELEGATOR = "sendstolen";

	public WiFiDirectWorkerNonOwnerService(String name) {
		super(name);
		intentFilter
				.addAction(WiFiDirectWorkerNonOwnerService.ACTION_STOP_READING);
		intentFilter
				.addAction(CommonConstants.BROADCAST_WORKER_INIT_STEALING_ACTION);
		intentFilter
				.addAction(CommonConstants.BROADCAST_WORKER_SEND_RESULTS_ACTION);
		intentFilter
				.addAction(CommonConstants.BROADCAST_WORKER_NO_JOBS_TO_STEAL_ACTION);
		intentFilter
				.addAction(CommonConstants.BROADCAST_WORKER_HAVE_JOBS_STEAL_ACTION);
	}

	public WiFiDirectWorkerNonOwnerService() {
		super("MyFileTransferService");

		intentFilter
				.addAction(WiFiDirectWorkerNonOwnerService.ACTION_STOP_READING);
		intentFilter
				.addAction(CommonConstants.BROADCAST_WORKER_INIT_STEALING_ACTION);
		intentFilter
				.addAction(CommonConstants.BROADCAST_WORKER_SEND_RESULTS_ACTION);
		intentFilter
				.addAction(CommonConstants.BROADCAST_WORKER_NO_JOBS_TO_STEAL_ACTION);
		intentFilter
				.addAction(CommonConstants.BROADCAST_WORKER_HAVE_JOBS_STEAL_ACTION);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		WorkerNotify.getInstance(null).deleteJobData();

	}

	private void closeStreams() throws IOException {

		if (this.oos != null) {
			this.oos.close();
			this.oos = null;
		}
		if (this.ois != null) {
			this.ois.close();
			this.ois = null;
		}
		if (stream != null) {
			stream.close();
			stream = null;
		}

		if (socket != null) {
			if (socket.isConnected()) {
				try {
					socket.close();
					socket = null;
					Log.d("WiFiDirectFileTransferService", "Socket closed");
				} catch (IOException e) {
					// Give up
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onStart(Intent intent, int startId) {
		String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
		socket = new Socket();
		int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);
		workerClass = intent.getExtras().getString(
				CommonConstants.ACTIVITY_CLASS_NAME);
		Context context = getApplicationContext();
		Log.d("WiFiDirectWorkerNonOwnerService",
				"Worker Opening client socket - port:" + port + " at host: "
						+ host);
		heartbeat = new WorkerHeartBeat();
		heartbeat.start();
		wrwt = new WorkerReadWriteThread(context, host, port);
		wrwt.start();

		// Log.d("MyFileTransferService", "Client: connections established");
	}

	@Override
	public void onCreate() {
		super.onCreate();

		receiver = new WorkerBroadcastReceiver();
		registerReceiver(receiver, intentFilter);

		// intent = new Intent(BROADCAST_WIFISERVICE_ACTION);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Context context = getApplicationContext();

		if (intent.getAction().equals(ACTION_STOP_READING)) {
			// Log.d("MyFileTransferService", "onHandleIntent: closeStreams");
			try {
				closeStreams();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private void sendResultsAsObjects(Object pObjt) {
		Log.d("sendResultsAsObjects", "before send");
		if (writeResultscount <= 0) {
			Log.d("sendResultsAsObjects", "writeResultscount<=0");
			Writer w = new Writer(SEND_RESULTS_TO_DELEGATOR, pObjt);
			w.start();
		} else {
			Log.d("sendResultsAsObjects", "resultsThreads.offer(w)");
			Writer w = new Writer(SEND_RESULTS_TO_DELEGATOR, pObjt);
			resultsThreads.offer(w);
		}
	}

	private void sendStolenJobs(Object o) {
		Log.d("sendStolenJobs", "before sendStolenJobs");
		if (writecount <= 0) {
			Log.d("sendStolenJobs", "writecount<=0");
			Writer w = new Writer(SEND_STOLEN_JOBS_TO_DELEGATOR, o);
			w.start();
		} else {
			Log.d("sendStolenJobs", "wrtingThreads.offer(w)");
			Writer w = new Writer(SEND_STOLEN_JOBS_TO_DELEGATOR, o);
			stolenThreads.offer(w);
		}
	}

	private void stealFromDelegator() {
		Log.d("stealFromDelegator", "before steal");
		if (writecount <= 0) {
			Log.d("stealFromDelegator", "writecount<=0");
			Writer w = new Writer(STEAL_FROM_DELEGATOR);
			w.start();
		} else {
			Log.d("stealFromDelegator", "wrtingThreads.offer(w)");
			Writer w = new Writer(STEAL_FROM_DELEGATOR);
			wrtingThreads.offer(w);
		}

		// if (oos != null) {
		// synchronized (this) {
		// try {
		// Log.d("WorkerBroadcastReceiver",
		// "before BROADCAST_WORKER_INIT_STEALING_ACTION");
		// while (this.iswriting) {
		// this.wait();
		// Log.d("WorkerBroadcastReceiver",
		// "wait BROADCAST_WORKER_INIT_STEALING_ACTION");
		// }
		//
		// iswriting = true;
		// oos.writeInt(CommonConstants.READ_INT_MODE);
		// oos.writeInt(CommonConstants.INIT_STEALING);
		// oos.flush();
		// Log.d("WorkerBroadcastReceiver",
		// "after BROADCAST_WORKER_INIT_STEALING_ACTION");
		// } catch (IOException e) {
		// e.printStackTrace();
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		// iswriting = false;
		// this.notifyAll();
		// }
		//
		// }

	}

	private void startFinishedActivty() {
		Intent deleIntent = new Intent(this, FinishedWorkerActivity.class);
		deleIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		this.startActivity(deleIntent);
	}

	private class Writer extends Thread {
		Intent resultIntent = null;
		String runmode = null;
		// private boolean isthreadwriting = false;
		Object objs;

		Writer(String pMode) {
			this.runmode = pMode;
		}

		Writer(String pMode, Object pObjs) {
			this.runmode = pMode;
			this.objs = pObjs;
		}

		Writer(String pMode, Intent pIntent) {
			resultIntent = pIntent;
			this.runmode = pMode;
		}

		public void run() {
			if (this.runmode != null) {
				if (this.runmode.equals(SEND_RESULTS_TO_DELEGATOR)) {
					this.sendResultsArray();
				} else if (this.runmode.equals(NOTHING_TO_GIVE_DELEGATOR)) {
					this.nojobstoSteal();
				} else if (this.runmode.equals(STEAL_FROM_DELEGATOR)) {
					this.steal();
				} else if (this.runmode.equals(SEND_STOLEN_JOBS_TO_DELEGATOR)) {
					this.sendStolenJobs();
				}
			}
		}

		private void sendResultsArray() {
			Object[] cjobs = (Object[]) objs;
			Log.d("WorkerBroadcastReceiver",
					"before sending results CompletedJob[] cjobs= "
							+ cjobs.length);
			if (oos != null && cjobs.length > 0) {
				synchronized (oos) {
					// for (CompletedJob cj : cjobs) {
					while (iswriting) {
						try {
							oos.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					iswriting = true;
					try {
						oos.writeInt(CommonConstants.READ_COMPLETED_JOB_OBJECT_ARRAY_MODE);
						oos.writeObject(cjobs);
						oos.flush();

						// JobPool.getInstance().setSentResults(true);

					} catch (IOException e) {
						e.printStackTrace();
					}

					// }

					Log.d("WorkerBroadcastReceiver",
							"after sending results CompletedJob[] cjobs");
					JobPool.getInstance().setSentResults(true);
					iswriting = false;
					oos.notifyAll();
				}
			}
			writecount++;
			writeResultscount++;

			if (JobPool.getInstance().isJobListEmpty()) {

				if (JobPool.getInstance().isAllInitialJobsReceived()) {
					if (ConnectionFactory.getInstance().isStealing) {
						Log.d("WorkerBroadcastReceiver",
								"Ive done and sent my jobs. going to STEAL!");
						// steal();
						stealFromDelegator();

					}
				}

			}
		}

		private void sendResults_() {
			Log.d("WorkerBroadcastReceiver",
					"before sending results INIT stage");
			String results = resultIntent
					.getStringExtra(CommonConstants.RESULT_STRING_TYPE);

			if (oos != null && results != null && results.length() > 0) {
				synchronized (oos) {
					try {
						while (iswriting) {
							try {
								this.wait();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						iswriting = true;
						Log.d("WorkerBroadcastReceiver",
								"before sending results " + results);
						oos.writeInt(CommonConstants.READ_STRING_MODE);
						oos.writeUTF(results);
						oos.flush();

						Log.d("WorkerBroadcastReceiver",
								"after sending results");
						JobPool.getInstance().setSentResults(true);

					} catch (IOException e) {
						e.printStackTrace();
					}
					iswriting = false;
					this.notifyAll();
				}

			} else {
				Object objs = resultIntent.getExtras().getSerializable(
						CommonConstants.RESULT_COMPLETED_JOB_ARRAY_TYPE);
				if (objs != null && objs instanceof Object[]) {
					Object[] cjobs = (Object[]) objs;
					// ResultTransmitObject[] cjobs =
					// (ResultTransmitObject[]) objs;
					Log.d("WorkerBroadcastReceiver",
							"before sending results CompletedJob[] cjobs= "
									+ cjobs.length);
					if (oos != null && cjobs.length > 0) {
						synchronized (this) {
							// for (CompletedJob cj : cjobs) {
							while (iswriting) {
								try {
									this.wait();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							iswriting = true;
							try {
								oos.writeInt(CommonConstants.READ_COMPLETED_JOB_OBJECT_ARRAY_MODE);
								oos.writeObject(cjobs);
								oos.flush();

								// JobPool.getInstance().setSentResults(true);

							} catch (IOException e) {
								e.printStackTrace();
							}

							// }

							Log.d("WorkerBroadcastReceiver",
									"after sending results CompletedJob[] cjobs");
							JobPool.getInstance().setSentResults(true);
							iswriting = false;
							this.notifyAll();
						}
					}
				} else {
					Log.d("WorkerBroadcastReceiver",
							"cjobs was not serialized properly");
				}
			}

			//
			if (JobPool.getInstance().isJobListEmpty()) {

				if (JobPool.getInstance().isAllInitialJobsReceived()) {
					if (ConnectionFactory.getInstance().isStealing) {
						Log.d("WorkerBroadcastReceiver",
								"Ive done and sent my jobs. going to STEAL!");
						steal();

					}
				}

			}
		}

		private void steal() {
			if (oos != null) {
				synchronized (oos) {
					try {
						Log.d("WorkerBroadcastReceiver",
								"before BROADCAST_WORKER_INIT_STEALING_ACTION");
						iswriting = true;
						oos.writeInt(CommonConstants.READ_INT_MODE);
						oos.writeInt(CommonConstants.INIT_STEALING);
						oos.flush();
						Log.d("WorkerBroadcastReceiver",
								"after BROADCAST_WORKER_INIT_STEALING_ACTION");
					} catch (IOException e) {
						e.printStackTrace();
					}
					iswriting = false;
					oos.notifyAll();
				}

			}

		}

		private void nojobstoSteal() {
			Log.d("WorkerBroadcastReceiver",
					"BROADCAST_WORKER_NO_JOBS_TO_STEAL_ACTION");
			try {
				synchronized (oos) {
					while (iswriting) {
						oos.wait();
					}

					oos.writeInt(CommonConstants.READ_INT_MODE);
					oos.writeInt(CommonConstants.NO_JOBS_TO_STEAL);
					oos.flush();
					iswriting = false;
					oos.notifyAll();
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private void sendStolenJobs() {
			Log.d("WorkerBroadcastReceiver",
					"BROADCAST_WORKER_HAVE_JOBS_STEAL_ACTION");
			try {
				synchronized (oos) {
					while (iswriting) {
						oos.wait();
					}
					if (this.objs != null && this.objs instanceof String) {
						String s = (String) objs;
						oos.writeInt(CommonConstants.READ_STRING_MODE);
						oos.writeUTF(s);
						oos.flush();
					}
					
					iswriting = false;
					oos.notifyAll();
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// /////////////////////////////////////////////////////////////////////
	private class WorkerBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context arg0, Intent intent) {
			String action = intent.getAction();
			if (CommonConstants.BROADCAST_WORKER_INIT_STEALING_ACTION
					.equals(action)) {
				// steal();
				stealFromDelegator();

			} else if (intent.getAction().equals(ACTION_STOP_READING)) {
				try {
					closeStreams();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if (intent.getAction().equals(
					CommonConstants.BROADCAST_WORKER_SEND_RESULTS_ACTION)) {
				Log.d("WorkerBroadcastReceiver",
						"before sending results INIT stage");
				String results = intent
						.getStringExtra(CommonConstants.RESULT_STRING_TYPE);

				if (oos != null && results != null && results.length() > 0) {
					synchronized (this) {
						try {
							while (iswriting) {
								try {
									this.wait();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							iswriting = true;
							Log.d("WorkerBroadcastReceiver",
									"before sending results " + results);
							oos.writeInt(CommonConstants.READ_STRING_MODE);
							oos.writeUTF(results);
							oos.flush();

							Log.d("WorkerBroadcastReceiver",
									"after sending results");
							JobPool.getInstance().setSentResults(true);

						} catch (IOException e) {
							e.printStackTrace();
						}
						iswriting = false;
						this.notifyAll();
					}
					writecount++;

					if (JobPool.getInstance().isJobListEmpty()) {

						if (JobPool.getInstance().isAllInitialJobsReceived()) {
							if (ConnectionFactory.getInstance().isStealing) {
								Log.d("WorkerBroadcastReceiver",
										"Ive done and sent my jobs. going to STEAL!"); // steal();
								stealFromDelegator();

							}
						}

					}

				} else {
					Object objs = intent.getExtras().getSerializable(
							CommonConstants.RESULT_COMPLETED_JOB_ARRAY_TYPE);
					/*
					 * if (objs != null && objs instanceof Object[]) { Object[]
					 * cjobs = (Object[]) objs; Log.d("WorkerBroadcastReceiver",
					 * "before sending results CompletedJob[] cjobs= " +
					 * cjobs.length); if (oos != null && cjobs.length > 0) {
					 * synchronized (this) { while (iswriting) { try {
					 * this.wait(); } catch (InterruptedException e) {
					 * e.printStackTrace(); } } iswriting = true; try {
					 * oos.writeInt
					 * (CommonConstants.READ_COMPLETED_JOB_OBJECT_ARRAY_MODE);
					 * oos.writeObject(cjobs); oos.flush(); } catch (IOException
					 * e) { e.printStackTrace(); }
					 * 
					 * Log.d("WorkerBroadcastReceiver",
					 * "after sending results CompletedJob[] cjobs");
					 * JobPool.getInstance().setSentResults(true); iswriting =
					 * false; this.notifyAll(); } } } else {
					 * Log.d("WorkerBroadcastReceiver",
					 * "cjobs was not serialized properly"); }
					 */
					sendResultsAsObjects(objs);
				}
				// writecount++;

				/*
				 * if (JobPool.getInstance().isJobListEmpty()) {
				 * 
				 * if (JobPool.getInstance().isAllInitialJobsReceived()) { if
				 * (ConnectionFactory.getInstance().isStealing) {
				 * Log.d("WorkerBroadcastReceiver",
				 * "Ive done and sent my jobs. going to STEAL!"); // steal();
				 * stealFromDelegator();
				 * 
				 * } }
				 * 
				 * }
				 */

			} else if (intent.getAction().equals(
					CommonConstants.BROADCAST_WORKER_NO_JOBS_TO_STEAL_ACTION)) {
				Log.d("WorkerBroadcastReceiver",
						"BROADCAST_WORKER_NO_JOBS_TO_STEAL_ACTION");
				try {
					synchronized (this) {
						while (iswriting) {
							this.wait();
						}

						oos.writeInt(CommonConstants.READ_INT_MODE);
						oos.writeInt(CommonConstants.NO_JOBS_TO_STEAL);
						oos.flush();
						iswriting = false;
						this.notifyAll();
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else if (intent.getAction().equals(
					CommonConstants.BROADCAST_WORKER_HAVE_JOBS_STEAL_ACTION)) {
				int mode = intent.getIntExtra(CommonConstants.RESULT_INT_TYPE,
						0);

				// String results = intent
				// .getStringExtra(CommonConstants.RESULT_STRING_TYPE);
				switch (mode) {
				case CommonConstants.READ_STRING_MODE:
					String jobs = intent
							.getStringExtra(CommonConstants.RESULT_STRING_TYPE);
					sendStolenJobs(jobs);
					break;
				}
			}

		}

	}

	// //////////////////////////////////////////////////////
	private class WorkerReadWriteThread extends Thread {
		Context context = null;
		String host = null;
		int port = 0;

		WorkerReadWriteThread(Context pContext, String pHost, int pPort) {
			this.context = pContext;
			this.host = pHost;
			this.port = pPort;
		}

		public void run() {
			try {
				socket.bind(null);

				// socket.connect((new InetSocketAddress(host, port)),
				// SOCKET_TIMEOUT);

				// socket.connect((new InetSocketAddress(host, port)), 0);//0 is
				// infinite timeout
				socket.connect((new InetSocketAddress(host, port)),
						WifiDirectConstants.SOCKET_TIMEOUT);

				// Log.d("MyFileTransferService", "Worker Client socket - "
				// + socket.isConnected());
				stream = socket.getOutputStream();
				ContentResolver cr = context.getContentResolver();
				is = socket.getInputStream();
				out = socket.getOutputStream();
				oos = new ObjectOutputStream(out);
				oos.flush();
				ois = new ObjectInputStream(is);

				mainloop: while (true) {
					// try {
					if (readMode <= 0) {
						readMode = ois.readInt();
						// Log.d("WorkerReadWriteThread", "ReadINT " +
						// readMode);
					}
					switch (readMode) {
					case CommonConstants.READ_INT_MODE:
						int readint = ois.readInt();
						if (readint == CommonConstants.ALL_INIT_JOBS_SENT) {
							// Log.d("MyFileTransferService",
							// "CommonConstants.ALL_INIT_JOBS_SENT");
							JobPool.getInstance().setIsAllInitialJobsReceived();
						} else if (readint == CommonConstants.INIT_STEALING) {
							JobInitializer.getInstance(this.context)
									.startVictimized(out, true);
							Log.d("WorkerReadWriteThread",
									"Delegator trying to steal from me");
						} else if (readint == CommonConstants.TERM_STEALING) {

							Log.d("WorkerReadWriteThread",
									"CommonConstants.TERM_STEALING");
							ConnectionFactory.getInstance().isStealing = false;
							TimeMeter.getInstance().setTotalWorkerTime(
									System.currentTimeMillis()
											- TimeMeter.getInstance()
													.getTotalWorkerTime());
							FileFactory.getInstance().logJobDoneWithDate(
									"Total Worker time = "
											+ TimeMeter.getInstance()
													.getTotalWorkerTime());
							FileFactory.getInstance().logJobDone(
									"Calculation time = "
											+ TimeMeter.getInstance()
													.getCalculateTime());
							FileFactory
									.getInstance()
									.logJobDone(
											"Byte Conversion time = "
													+ TimeMeter
															.getInstance()
															.getTotalByteConversionTime());
							FileFactory.getInstance().logJobDone(
									"Transmit jobs time = "
											+ TimeMeter.getInstance()
													.getTransmitJobsTime());
							FileFactory.getInstance().writeJobsDoneToFile();
							FileFactory.getInstance().logCalcTimesToFile();

							// Intent deleIntent = new Intent(
							// WiFiDirectWorkerNonOwnerService.this,
							// FinishedWorkerActivity.class);
							// WiFiDirectWorkerNonOwnerService.this
							// .startActivity(deleIntent);

							startFinishedActivty();

							break mainloop;
						} else if (readint == CommonConstants.NO_JOBS_TO_STEAL) {
							Log.d("WorkerBroadcastReceiver",
									"dele says NO_JOBS_TO_STEAL but keep stealing");
							// steal();// keep trying to steal
							stealFromDelegator();
						}
						readMode = 0;
						break;
					case CommonConstants.READ_STRING_MODE:
						String readString = ois.readUTF();
						processString(readString);// niro TODO: handle what to
													// do on string read
						readString = null;
						readMode = 0;
						break;
					case CommonConstants.READ_FILE_MODE:
						final File f = new File(
								Environment.getExternalStorageDirectory() + "/"
										+ context.getPackageName() + "/"
										+ CommonConstants.RECEV_FILES_PATH
										+ "/" + System.currentTimeMillis()
										+ ".zip");

						File dirs = new File(f.getParent());
						if (!dirs.exists())
							dirs.mkdirs();
						f.createNewFile();
						byte[] buffer = new byte[CommonConstants.PACKET_SIZE];
						int bytesRead = 0;
						FileOutputStream fos = new FileOutputStream(f);
						int runs = 0;

						byte[] b = new byte[CommonConstants.PACKET_SIZE];
						int len = 0;
						int bytcount = CommonConstants.PACKET_SIZE;

						int lengthOfFile = ois.readInt();
						int readsofar = 0;
						Log.d("WorkerReadWriteThread", "File length = "
								+ lengthOfFile);
						whilelooplabel: while ((bytesRead = is.read(
								buffer,
								0,
								Math.min(buffer.length, lengthOfFile
										- readsofar))) != -1) {
							fos.write(buffer, 0, bytesRead); // write
							readsofar += bytesRead;

							if (readsofar >= lengthOfFile) {
								break whilelooplabel;
							}
						}

						fos.flush();
						fos.close();
						readMode = 0;
						synchronized (oos) {
							oos.writeInt(CommonConstants.READ_INT_MODE);
							oos.writeInt(WifiDirectConstants.FILE_RECEIVED_FROM_DELEGATOR);
							oos.flush();
							oos.notify();
						}

						// start workerbee
						JobParams jp = new JobParams(
								CommonConstants.READ_FILE_NAME_MODE);
						jp.paramsString = f.getAbsolutePath();
						jp.paramObject = f;

						WorkerNotify.getInstance(getApplicationContext())
								.assignJobsForWorker(jp, workerClass,
										stolenIncoming);
						stolenIncoming = false;
						// keepReading = false;//test
						break;
					case CommonConstants.VICTIM_MODE:// stolen params
						stolenIncoming = true;
						readMode = 0;
						break;
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					closeStreams();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void processString(String pRead) {
		if (pRead != null) {
			if (pRead.endsWith(CommonConstants.MSG_BREAK)) {
				pRead = pRead.substring(0, pRead.length() - 1);
				Log.d("processString", "msg = " + pRead);
				if (pRead.startsWith(CommonConstants.PARAM_SYMBOL)) {// then
																		// these
					// are job
					// params
					pRead = pRead.substring(
							CommonConstants.PARAM_SYMBOL.length(),
							pRead.length());

					// start workerbee
					JobParams jp = new JobParams(
							CommonConstants.READ_STRING_MODE);
					jp.paramsString = pRead;
					jp.paramObject = pRead;

					try {
						WorkerNotify.getInstance(getApplicationContext())
								.assignJobsForWorker(jp, workerClass,
										stolenIncoming);
					} catch (IOException e) {
						e.printStackTrace();
					}
					stolenIncoming = false;
				}
			} else if (pRead.equals(CommonConstants.SEND_RESULTS_RECEIVED)) {
				Log.d("processString", "SEND_RESULTS_RECEIVED");

				writeResultscount--;
				writecount--;
				if (!resultsThreads.isEmpty()) {
					Writer w = resultsThreads.poll();
					w.setPriority(Thread.MAX_PRIORITY);
					if (w != null) {
						w.start();
					}
				}

				
				if (!stolenThreads.isEmpty()) {
					Writer w = stolenThreads.poll();
					w.setPriority(Thread.MIN_PRIORITY);
					if (w != null) {
						w.start();
					}
				}
				
				if (!wrtingThreads.isEmpty()) {
					Writer w = wrtingThreads.poll();
					w.setPriority(Thread.MIN_PRIORITY);
					if (w != null) {
						w.start();
					}
				}

			}
		}
	}

	public static boolean copyFile(InputStream inputStream, OutputStream out) {
		byte buf[] = new byte[1024];
		int len;
		try {
			while ((len = inputStream.read(buf)) != -1) {
				out.write(buf, 0, len);

			}
			out.close();
			inputStream.close();
		} catch (IOException e) {
			Log.d("MyFileTransferService", e.toString());
			return false;
		}
		return true;
	}

	// ////////////////////////////////////////////////////////////////////
	private class WorkerHeartBeat extends Thread {

		// public WorkerHeartBeat(){
		//
		// }
		public void run() {
			while (true) {
				if (oos != null) {
					synchronized (oos) {
						try {
							oos.wait();
							oos.writeInt(WifiDirectConstants.WORKER_HEARTBEAT);

						} catch (IOException e1) {
							e1.printStackTrace();
						} catch (InterruptedException e2) {
							e2.printStackTrace();
						}

					}
					try {
						Thread.sleep(WifiDirectConstants.WORKER_HEARTBEAT_SLEEP);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			}
		}
	}

	// ////////////////////////////////////
	/**
	 * Class used for the client Binder. Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class WorkerBinder extends Binder {
		WiFiDirectWorkerNonOwnerService getService() {
			// Return this instance of LocalService so clients can call public
			// methods
			return WiFiDirectWorkerNonOwnerService.this;
		}
	}

}
