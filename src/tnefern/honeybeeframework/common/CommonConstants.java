package tnefern.honeybeeframework.common;

public class CommonConstants {
	// Intent request codes
	public static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	public static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	public static final int REQUEST_ENABLE_BT = 3;
	public static final int BT_SET_AS_DISCOVERABLE = 4;
	public static final int BT_DISCOVERABLE_DURATION = 100;// 300

	public static final String WORKER_UUID = "04c6093b-0000-1000-8000-00805f9b34fb";
	public static final String SERVICE_NAME = "NiroBtService";

	// Job Status
	public static final int JOB_DONE = 1;
	public static final int JOB_GIVEN = 2;
	public static final int JOB_NOT_GIVEN = 3;
	public static final int JOB_BEEN_STOLEN = 4;

	// Work stealing signals
	// public static final int READY_TO_STEAL_SIGNAL = 999;
	// public static final int TERMINATE_WORK_STEALING = 1000;

	// PARAM SYMBOLS
	public static final String PARTITION_BREAK = "PARTITION";
	public static final String MSG_BREAK = "#";// written at the end of every
	// message transmitted from
	// delegator to workers. This indicates end of message.
	public static final String PARAM_SYMBOL = "PARAM";
	public static final String INCOMING_SYMBOL = "INCOMING";
	public static final String TERM_SYMBOL = "TERMINATE";
	public static final String APP_REQUEST_SEPERATOR = ":";
	public static final String RESULT_SYMBOL = "RESULT";
	public static final String BROADCAST_WORKER_SEND_RESULTS_ACTION = "org.com.honeybeecrowdDemo.workersendresultaction";
	public static final String BROADCAST_WORKER_NO_JOBS_TO_STEAL_ACTION = "org.com.honeybeecrowdDemo.workernojabstostealaction";
	public static final String BROADCAST_WORKER_HAVE_JOBS_STEAL_ACTION = "org.com.honeybeecrowdDemo.workerhavejobstostealaction";

	// intent extra types
	public static final String RESULT_STRING_TYPE = "org.com.honeybee.result.string";
	public static final String RESULT_INT_TYPE = "org.com.honeybee.result.integer";
	public static final String RESULT_COMPLETED_JOB_ARRAY_TYPE = "tnefern.honeybeeframework.result.completedJobArray";
	public static final String RESULT_OBJECT_TYPE = "org.com.honeybee.resultObject";
	public static final int COMPLETED_JOBS_BUFFER = 10;
	public static final int WORK_CHUNK = 1;// 5//10;//15//for some unknown
											// reason, the work chunk must be a
											// multiple of the steal chunk (so
											// if WC =3, SC should also be 3)
	public static final int MAX_JOBS_STORE = 120;
	// BT Connection status
	public static final int STATE_NONE = 10; // we're doing nothing
	public static final int STATE_LISTEN = 11; // now listening for incoming
	// connections
	public static final int STATE_CONNECTING = 12; // now initiating an outgoing
	// connection
	public static final int STATE_CONNECTED = 13; // now connected to a remote
	// device

	// testing purposes
	public static final String TEMP_MSG1 = "Twinkle, twinkle, little bat - How I wonder what you are at! - Up above the world you fly -Like a tea-tray in the sky.";
	public static final String TEMP_MSG2 = "The reason birds can fly and we can't is simply because they have perfect faith, for to have faith is to have wings.";
	public static final String TEMP_MSG3 = "All that is gold";
	// public static final String DEBUG_FILE_PATH = "debugText.txt";
	// public static final String DEBUG_FILE_PATH = "chunkText.txt";
	public static final String DEBUG_FILE_PATH = "faceDebugDemoI7.txt";
	public static final String ZIP_FILE_PATH = "faceMatch.zip";
	public static final String COPY_FILE_PATH = "hBeeTemp";
	public static final String ZIPSTORE_FILE_PATH = "honeyZipStore";
	public static final String RECEV_FILES_PATH = "honeyBRec";

	public static final int PACKET_SIZE = 8192;// 8192;
	public static final int PACKET_SIZE_FOR_STRING = 100;
	public static final int BYTE_SIZE = 4;
	public static final int MAX_FILES_PER_MSG = 5;
	public static final int WORKER_INIT_JOBS = 5;

	public static final int READ_INT_MODE = 1;
	public static final int READ_STRING_MODE = 2;
	public static final int READ_INT_ARRAY_MODE = 3;
	public static final int READ_PHOTO_MODE = 4;
	public static final int READ_FILE_MODE = 5;
	public static final int READ_FILES_MODE = 6;
	public static final int READ_FILE_MODE2 = 7;
	public static final int READ_FILE_STRING_MODE = 8;
	public static final int READ_FILE_NAME_MODE = 9;
	public static final int ALL_INIT_JOBS_SENT = 10;
	public static final int READ_MIXED_MODE = 11;
	public static final int READ_COMPLETED_JOB_OBJECT_MODE = 12;
	public static final int READ_COMPLETED_JOB_OBJECT_ARRAY_MODE = 13;
	public static final int VICTIM_MODE = 90;

	// delegator's send signals
	public static final String SEND_FILES = "file";
	public static final String SEND_INT_ARRAY = "intarray";
	public static final String SEND_STEAL_STRING = "stealstring";
	public static final String SEND_STRING = "string";
	public static final String SEND_STEAL_FILES = "stealfile";
	public static final String SEND_THIEF = "thief";
	public static final String SEND_ARE_YOU_THERE = "areyouthere";
	public static final String SEND_RESULTS_RECEIVED = "resultswerereceived";
	public static final String SEND_TERMINATION = "terminatestealing";

	// stealing
	public static final String BROADCAST_WORKER_INIT_STEALING_ACTION = "org.com.honeybeecrowdDemo.workerinitstealingaction";
	public static final String BROADCAST_DELE_INIT_STEALING_ACTION = "org.com.honeybeecrowdDemo.delegatorinitstealingaction";
	public static final String BROADCAST_DELE_STOP_READING = "org.com.honeybeecrowdDemo.delegatorstopreading";

	// on finished
	public static final String BROADCAST_DELE_COMPLETED_RESULTS = "tnefern.honeybeecrowd.delegatorsendresults";

	public static final String STEAL_STRING_TYPE = "org.com.honeybee.steal.string";

	public static final String VICTIM_WIFIADDRESS_TYPE = "org.com.honeybee.victim.wifiAddress";
	public static final String VICTIM_MODE_TYPE = "org.com.honeybee.victim.modetype";
	public static final String VICTIM_STRING_TYPE = "org.com.honeybee.victim.string";
	public static final String VICTIM_FILE_TYPE = "org.com.honeybee.victim.file";

	public static final int INIT_STEALING = 1;
	public static final int STEAL_CHUNK = 5;// 3
	public static final int STEAL_LIM = 5;
	public static final int NO_JOBS_TO_STEAL = 4;
	public static final int TERM_STEALING = 5;

	// thread Synch. these are used for the peterson lock to ensure that result
	// transmission and steal param transmission do not occur simultaneously.
	public static final int TRANSMIT_RESULTS_ID = 1;
	public static final int TRANSMIT_STEAL_PRAMS_ID = 0;

	// Activity Ids
	public static final int LOOK_FOR_WORK_ACTIVITY_ID = 99;

	// Message IDS
	public static String LOOK_FOR_WORK_ACTIVITY_KEY = "LookForWorkActivity";
	public static String APP_WORKER_ACTIVITY_KEY = "ApplicationWorkerActivity";
	public static String ACTIVITY_CLASS_NAME = "Activity class name";
	public static String JOBS_UPDATED = "JobsUpdated";
	public static int ADD_JOB = 1;
	public static int REMOVE_JOB = -1;
	// testing only
	// Ideos i =200
	// public static final long MONOTIME = 208438;
	// Ideos i =500
	// public static final long MONOTIME = 540866;

	// public static final long MONOTIME = 77910;//faceMatch, Nexus S, 30 photos
	// public static final long MONOTIME = 253708;//faceMatch, Nexus 7, 240
	// photos
	// public static final long MONOTIME = 62903;//faceMatch, Nexus 7, 60 photos
	// public static final long MONOTIME = 126389;// faceMatch, Nexus 7, 120
	// photos
	// public static final long MONOTIME = 253708;//faceMatch, Nexus 7, 240
	// photos
	// public static final long MONOTIME = 511046;//faceMatch, Nexus 7, 480
	// photos
	// public static final long MONOTIME = 1046070;//faceMatch, Nexus 7, 960
	// photos
	// public static final long MONOTIME = 2112755;//faceMatch, Nexus 7, 1920
	// photos

	// mandelbrot
	// public static final long MONOTIME = 27301;//26322;//numberOfRows =
	// 300;,iter = 200;
	//public static final long MONOTIME = 185910;//iter=1500
	public static final long MONOTIME = 372954;//iter=3000

//64398;
//33584;// 26322;//numberOfRows =
												// 300;,iter = 250;
	public static final int TAKE_PHOTO_CODE = 6666;

	// App Info modes
	public static final int TEXT = 1;
	public static final int GPS = 2;
	public static final int FILE = 3;

	public static final String RESULTS_RECEIVED_CALLBACK = "ResultsReceivedCallback";

	public static final int PRIORITY_DO_OWN_JOBS = 10;
	public static final int PRIORITY_LET_OTHER_STEAL = 1;

	public static final int CONNECTION_MODE_WIFIDIRECT = 0;
	public static final int CONNECTION_MODE_BLUETOOTH = 1;

}
