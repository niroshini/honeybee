package tnefern.honeybeeframework.wifidirect;

public class WifiDirectConstants {
	public final static String NOTIFY_WIFI_DIRECT_SERVICE_FROM_DELEGATOR_READER = "NOTIFY_WIFI_DIRECT_SERVICE_FROM_DELEGATOR_READER";
	public final static String NOTIFY_ALL_WIFI_DIRECT_P2P_ARE_CONNECTED_AND_BT_MAPPED = "NOTIFY_ALL_WIFI_DIRECT_P2P_ARE_CONNECTED_AND_BT_MAPPED";
	public final static String NOTIFY_UI_UPON_CONNECTION = "NOTIFY_UI_UPON_CONNECTION";
	public final static String WIFIP2P_ADDRESS = "WIFIP2PADDRESS";
	public static final int SOCKET_TIMEOUT = 100000;
	public static final int FILE_TRANSFER_PORT = 8988;
	
	public static final int FILE_RECEIVED_FROM_DELEGATOR = 198192;
	
	//heartbeat related constants
	public static final int WORKER_HEARTBEAT = 222;
	public static final int WORKER_HEARTBEAT_SLEEP = 2500;//milliseconds
	public static final int WORKER_HEARTBEAT_TIMEOUT_MULTIPLE = 12;
	public static final int WORKER_ARE_YOU_THERE = 333;
	
	//job expiry
	public static final int WORKER_JOB_EXPIRY = 120000;//milliseconds. job dependant
}
