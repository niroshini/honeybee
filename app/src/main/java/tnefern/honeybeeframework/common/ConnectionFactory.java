package tnefern.honeybeeframework.common;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import tnefern.honeybeeframework.R;
import tnefern.honeybeeframework.delegator.ReceivedResults;
import tnefern.honeybeeframework.delegator.ResultsRead;
import tnefern.honeybeeframework.delegator.WorkerInfo;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

/**
 * This class holds all the common Bluetooth functions.
 * 
 * @author tnfernando
 * 
 */
public class ConnectionFactory {
	private static ConnectionFactory theInstance = null;
	private static BluetoothAdapter btInterface = null;
	private static ArrayAdapter<String> newDevicesArrayAdapter = null;
	/**
	 * workerMap contains the String address (whether BT or WifiDirect) and
	 * WorkerInfo
	 */
	private static HashMap<String, WorkerInfo> workerMap = new HashMap<String, WorkerInfo>();
	private static HashMap<String, WifiP2PdeviceWrapper> wifiDirectDevicesMap = new HashMap<String, WifiP2PdeviceWrapper>();;
	private static ArrayList<WorkerInfo> connectedWorkerList =  new ArrayList<WorkerInfo>();;
	private static ArrayList<String> silentWorkers =  new ArrayList<String>();;
	private static BluetoothSocket delegatingSocket = null;
	private BroadcastReceiver bcastReceiver = null;
	public static final int BT_ENABLE_SUCCESS = 1;
	public static final int BT_ENABLE_NOT_SUCCESS = 0;
	public static final int BT_ALREADY_ENABLED = 2;
	private int numConnected = 0;
	private int connectionState = CommonConstants.STATE_NONE;
//	private AcceptThread mAcceptThread;
//	private ConnectThread mConnectThread;
//	private ConnectedThread mConnectedThread;
	private long picoNetTime = 0;
	public boolean isStealing = true;
	public boolean isJobDone = false;
	private ResultsRead callbackObj = null;
	private ArrayList<ReceivedResults> resultArr = new ArrayList<ReceivedResults>();
	// private int intReads = 0;

	private boolean booLock = false;
	// private boolean isReceiverInit = false;

	// public MyLock lock = new Peterson();
	public Lock relock = new ReentrantLock();
	public int connectionMode = -1;
	public static int BT_MODE = 0;
	public static int WIFI_MODE = 1;

	// ExecutorService threadPoolWriter = Executors.newFixedThreadPool(3);

	private ConnectionFactory() {
		btInterface = BluetoothAdapter.getDefaultAdapter();
	}

	public synchronized static ConnectionFactory getInstance() {
		if (theInstance == null) {
			theInstance = new ConnectionFactory();
			theInstance.connectionMode = WIFI_MODE;//for now
		}
		return theInstance;
	}

	public BluetoothAdapter getBluetoothAdapter() {
		return btInterface;
	}

	public synchronized void addConnection() {
		numConnected++;
		// System.out.println("BTFactory, connected " + numConnected);
	}

	public synchronized void removeConnection() {
		numConnected--;
	}

	public int getNumberOfWorkers() {
		return numConnected;
	}

	public void setReadResults(ResultsRead pI) {
		this.callbackObj = pI;
		// this.relock.lock();
		synchronized (this.resultArr) {
			if (this.resultArr.size() > 0) {
				Iterator<ReceivedResults> iter = this.resultArr.iterator();
				while (iter.hasNext()) {
					ReceivedResults rs = iter.next();
					this.callbackObj.onResultsRead(rs);
				}
				this.resultArr.clear();
			}
		}
		// this.relock.unlock();
	}

	public ResultsRead getResultsReader() {
		return this.callbackObj;
	}

	private void initReceiver(String pTag, Activity pAct) {
		final Activity act = pAct;
		final String TAG = pTag;
		bcastReceiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {

				String action = intent.getAction();
				if (BluetoothDevice.ACTION_FOUND.equals(action)) {
					BluetoothDevice device = intent
							.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					newDevicesArrayAdapter.add(device.getName() + "\n"
							+ device.getAddress());
					// workerList.add(device);
					workerMap.put(device.getAddress(), new WorkerInfo(device));
					// Log.d(TAG, " BTFactory--> " + device.getName() + "  : "
					// + device.getAddress());

				} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
						.equals(action)) {
					// Log.d(TAG, "Discovery finished!");
					act.setTitle("Discovery finished!");
					newDevicesArrayAdapter = null;
				}

				else if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED
						.equals(action)) {

					int mode = intent.getIntExtra(
							BluetoothAdapter.EXTRA_SCAN_MODE,
							BluetoothAdapter.ERROR);
					String strMode = "";

					switch (mode) {
					case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
						strMode = "mode changed: SCAN_MODE_CONNECTABLE_DISCOVERABLE";
						break;
					case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
						strMode = "mode changed: SCAN_MODE_CONNECTABLE";
						break;
					case BluetoothAdapter.SCAN_MODE_NONE:
						strMode = "mode changed: SCAN_MODE_NONE";
						break;
					}

					Toast.makeText(act, strMode, Toast.LENGTH_LONG).show();
					act.setTitle(strMode);
					// Log.d(TAG, strMode);
				}
			}
		};
	}

	private void initReceiver(String pTag) {
		// final Activity act = pAct;
		final String TAG = pTag;
		bcastReceiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {

				String action = intent.getAction();
				if (BluetoothDevice.ACTION_FOUND.equals(action)) {
					BluetoothDevice device = intent
							.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					newDevicesArrayAdapter.add(device.getName() + "\n"
							+ device.getAddress());
					// workerList.add(device);
					workerMap.put(device.getAddress(), new WorkerInfo(device));
					// Log.d(TAG, " BTFactory--> " + device.getName() + "  : "
					// + device.getAddress());

				} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
						.equals(action)) {
					// Log.d(TAG, "Discovery finished!");
					newDevicesArrayAdapter = null;
				}

				else if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED
						.equals(action)) {

					int mode = intent.getIntExtra(
							BluetoothAdapter.EXTRA_SCAN_MODE,
							BluetoothAdapter.ERROR);
					String strMode = "";

					switch (mode) {
					case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
						strMode = "mode changed: SCAN_MODE_CONNECTABLE_DISCOVERABLE";
						break;
					case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
						strMode = "mode changed: SCAN_MODE_CONNECTABLE";
						break;
					case BluetoothAdapter.SCAN_MODE_NONE:
						strMode = "mode changed: SCAN_MODE_NONE";
						break;
					}

					// Log.d(TAG, strMode);
				}
			}
		};
	}

	public int startBluetooth(Activity pAct, String pTag) {

		// If the adapter is null, then Bluetooth is not supported
		if (btInterface == null) {
			Toast.makeText(pAct, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			pAct.finish();
			return BT_ENABLE_NOT_SUCCESS;
		}

		initReceiver(pTag, pAct);
		if (!btInterface.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			pAct.startActivityForResult(enableBtIntent,
					CommonConstants.REQUEST_ENABLE_BT);
			pAct.setTitle("Bluetooth is now enabled");
			return BT_ENABLE_SUCCESS;
		} else {
			pAct.setTitle("Bluetooth is now enabled");
			return BT_ALREADY_ENABLED;
		}

	}

	public int startBluetooth(String pTag) {

		// If the adapter is null, then Bluetooth is not supported
		if (btInterface == null) {
			// Toast.makeText(pAct, "Bluetooth is not available",
			// Toast.LENGTH_LONG).show();
			// pAct.finish();
			return BT_ENABLE_NOT_SUCCESS;
		}

		initReceiver(pTag);
		if (!btInterface.isEnabled()) {
			// Intent enableBtIntent = new Intent(
			// BluetoothAdapter.ACTION_REQUEST_ENABLE);
			// pAct.startActivityForResult(enableBtIntent,
			// CommonConstants.REQUEST_ENABLE_BT);
			// TODO: Confirmation dialog here
			btInterface.enable();
			// pAct.setTitle("Bluetooth is now enabled");
			return BT_ENABLE_SUCCESS;
		} else {
			// pAct.setTitle("Bluetooth is now enabled");
			return BT_ALREADY_ENABLED;
		}

	}

	public void cancelDiscovery() {
		if (btInterface != null) {
			btInterface.cancelDiscovery();
		}
	}

	/**
	 * Need to call startBluetooth() before calling this.
	 * 
	 * @param pAct
	 * @param pTag
	 * @param pAdapter
	 * @param pD
	 */
	public void lookForDevices(Activity pAct, String pTag, boolean pD,
			ArrayAdapter<String> pArrayAdapter) {
		final String TAG = pTag;
		newDevicesArrayAdapter = pArrayAdapter;
		newDevicesArrayAdapter.clear();

		if (pD)
			pAct.setTitle(R.string.txtScanning);

		pAct.setProgressBarIndeterminateVisibility(true);
		if (btInterface.isDiscovering()) {
			btInterface.cancelDiscovery();
		}
		btInterface.startDiscovery();

		// Register the BroadcastReceiver
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		pAct.registerReceiver(bcastReceiver, filter); // Don't forget to
		// unregister
		// during onDestroy

		// Register for broadcasts when discovery has finished
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		pAct.registerReceiver(bcastReceiver, filter);
		// return workerList;
	}

	/**
	 * Need to call startBluetooth() before calling this.
	 * 
	 * @param pTag
	 * @param pAdapter
	 * @param pD
	 */
	/*
	 * public void lookForDevices(String pTag, boolean pD, ArrayAdapter<String>
	 * pArrayAdapter) { // workerList = new ArrayList<String>(); workerMap = new
	 * HashMap<String, WorkerInfo>(); connectedWorkerList = new
	 * ArrayList<WorkerInfo>(); final String TAG = pTag; newDevicesArrayAdapter
	 * = pArrayAdapter; newDevicesArrayAdapter.clear();
	 * 
	 * if (pD) Log.d(TAG, "doDiscovery()"); //
	 * pAct.setTitle(R.string.txtScanning);
	 * 
	 * // pAct.setProgressBarIndeterminateVisibility(true); if
	 * (btInterface.isDiscovering()) { btInterface.cancelDiscovery(); }
	 * btInterface.startDiscovery();
	 * 
	 * // Register the BroadcastReceiver IntentFilter filter = new
	 * IntentFilter(BluetoothDevice.ACTION_FOUND);
	 * pAct.registerReceiver(bcastReceiver, filter); // Don't forget to //
	 * unregister // during onDestroy
	 * 
	 * // Register for broadcasts when discovery has finished filter = new
	 * IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
	 * pAct.registerReceiver(bcastReceiver, filter); // return workerList; }
	 */
	public HashMap<String, WorkerInfo> getWorkerDeviceMap() {
		return workerMap;
	}

	public HashMap<String, WifiP2PdeviceWrapper> getWifiDirectDeviceMap() {
		return wifiDirectDevicesMap;
	}

	public void mapWifiDirectToBT(String pBTMac) {
		WorkerInfo w = this.workerMap.get(pBTMac);
		if (w != null) {

		}
	}

	/**
	 * Checks if all connected devices are mapped with BT and wifiDirect MACs.
	 * 
	 * @return
	 */
	public boolean checkWifiDiretBtMapping() {
		// this.workerMap.
		Iterator<Entry<String, WorkerInfo>> iter = this.getWorkerDeviceMap()
				.entrySet().iterator();

		while (iter.hasNext()) {

			Entry<String, WorkerInfo> entry = iter.next();

			// String btMac = (String)entry.getKey();

			WorkerInfo val = (WorkerInfo) entry.getValue();

			if (val != null && val.isConnected) {
				// Log.d("checkWifiDiretBtMapping","wifid : "+val.getWiFiDirectAddress()+" BtName: "+val.getDevice().getName());
				if (val.getWiFiDirectAddress() == null) {
					return false;
				} else {
					if (val.getWiFiDirectAddress().isEmpty()) {
						return false;
					}
				}
			}
			// System.out.println("key,val: " + key + "," + val);

		}
		return true;
	}

	public int getWifiDiretBtMapped() {
		// this.workerMap.
		int count = 0;
		Iterator<Entry<String, WorkerInfo>> iter = this.getWorkerDeviceMap()
				.entrySet().iterator();

		while (iter.hasNext()) {

			Entry<String, WorkerInfo> entry = iter.next();

			// String btMac = (String)entry.getKey();

			WorkerInfo val = (WorkerInfo) entry.getValue();

			if (val != null && val.isConnected) {
				// Log.d("checkWifiDiretBtMapping","wifid : "+val.getWiFiDirectAddress()+" BtName: "+val.getDevice().getName());
				if (val.getWiFiDirectAddress() != null) {
					if (!val.getWiFiDirectAddress().isEmpty()) {
						count++;
					}
				}

			}
		}
		// System.out.println("key,val: " + key + "," + val);

		// }
		return count;
	}

	public String getBTAddrressFromWifiDirectAddress(String pWifi) {
		Iterator<Entry<String, WorkerInfo>> iter = this.getWorkerDeviceMap()
				.entrySet().iterator();

		while (iter.hasNext()) {
			Entry<String, WorkerInfo> entry = iter.next();
			WorkerInfo val = (WorkerInfo) entry.getValue();
			if (val != null && val.isConnected) {
				Log.d("getBTAddrressFromWifiDirectAddress",
						"wifid : " + val.getWiFiDirectAddress() + " BtName: "
								+ val.getBtDevice().getName() + " pWifi:"
								+ pWifi);
				if (val.getWiFiDirectAddress() != null) {
					if (val.getWiFiDirectAddress().equals(pWifi)) {
						return val.getBtDevice().getAddress();
					}
				}

			}
		}
		return null;

	}

	public WorkerInfo getWorkerInfoFromWifiDirectAddress(String pWifi) {
//		Iterator<Entry<String, WorkerInfo>> iter = this.getWorkerDeviceMap()
//				.entrySet().iterator();
//
//		while (iter.hasNext()) {
//			Entry<String, WorkerInfo> entry = iter.next();
//			WorkerInfo val = (WorkerInfo) entry.getValue();
//			if (val != null && val.isConnected) {
//				if (val.getWiFiDirectAddress() != null) {
//					if (val.getWiFiDirectAddress().equals(pWifi)) {
//						return val;
//					}
//				}
//
//			}
//		}
		
		WorkerInfo w = this.getWorkerDeviceMap().get(pWifi);
		if(w!=null){
			if(w.isConnected){
				return w;
			}
		}
		return null;

		
		
	}
	
	public WorkerInfo getWorkerInfoFromAddress(String pAdr) {
			return this.getWorkerDeviceMap().get(pAdr);
//		Iterator<Entry<String, WorkerInfo>> iter = this.getWorkerDeviceMap()
//				.entrySet().iterator();
//
//		while (iter.hasNext()) {
//			Entry<String, WorkerInfo> entry = iter.next();
//			WorkerInfo val = (WorkerInfo) entry.getValue();
//			if (val != null && val.isConnected) {
//				if (val.getWiFiDirectAddress() != null) {
//					if (val.getWiFiDirectAddress().equals(pWifi)) {
//						return val;
//					}
//				}
//
//			}
//		}
//		return null;

	}

	public ArrayList<WorkerInfo> getConnectedWorkerList() {
		return connectedWorkerList;
	}

	public void addToConnectedWorkers(WorkerInfo pInfo) {
		connectedWorkerList.add(pInfo);
	}

	public void onWorkerSilentAtConnectedWorkers(String pAdr) {
		Iterator<WorkerInfo>iter = connectedWorkerList.iterator();
		while(iter.hasNext()){
			WorkerInfo info = iter.next();
			if(info!=null && info.getAddress().equals(pAdr)){
				info.isConnected = false;
			}
		}
		
		if(!silentWorkers.contains(pAdr)){
			silentWorkers.add(pAdr);
		}
		
	}
	
	public void onWorkerSilentAtConnectedWorkersRemove(String pAdr) {
		int i = 0;
		int index = 0;
		Iterator<WorkerInfo>iter = connectedWorkerList.iterator();
		while(iter.hasNext()){
			WorkerInfo info = iter.next();
			if(info!=null && info.getAddress().equals(pAdr)){
				info.isConnected = false;
				index = i;
			}
			i++;
			
		}
		
		connectedWorkerList.remove(index);
		
		if(!silentWorkers.contains(pAdr)){
			silentWorkers.add(pAdr);
		}
		
	}
	public void onWorkerTalkingAgain(String pAdr) {
		if(silentWorkers.contains(pAdr)){
			silentWorkers.remove(pAdr);
		}
		getWorkerDeviceMap().get(pAdr).isConnected = true;
		Iterator<WorkerInfo>iter = connectedWorkerList.iterator();
		while(iter.hasNext()){
			WorkerInfo info = iter.next();
			if(info!=null && info.getAddress().equals(pAdr)){
				info.isConnected = true;
			}
		}
		
	}
	
	public boolean wasISilent(String pAdr){
		return silentWorkers.contains(pAdr);
	}
	public void unregisterReceivers(Activity pAct) {
		if (bcastReceiver != null) {
			pAct.unregisterReceiver(bcastReceiver);
		}
	}

	public void setDelegatingSocket(BluetoothSocket pSocket) {
		delegatingSocket = pSocket;
	}

	public BluetoothSocket getDelegatingSocket() {
		return delegatingSocket;
	}

	public void setAsDiscoverable(Activity pAct) {
		Intent discoverableIntent = new Intent(
				BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableIntent.putExtra(
				BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
				CommonConstants.BT_DISCOVERABLE_DURATION);
		// pAct.startActivity(discoverableIntent);
		pAct.startActivityForResult(discoverableIntent,
				CommonConstants.BT_SET_AS_DISCOVERABLE);
		// Log.d("WorkerActivity", "Device is now Discoverable1");
		pAct.setTitle("Device is now Discoverable");
	}

	public void setConnectionState(int pState) {
		this.connectionState = pState;
	}

	public int getConnectionState() {
		return connectionState;
	}


	public synchronized boolean getLock() {
		return this.booLock;

	}

	public synchronized void lock() {
		this.booLock = true;
	}

	public synchronized void unlock() {
		this.booLock = false;
	}


	public void addResult(ReceivedResults pStr) {
		synchronized (resultArr) {
			this.resultArr.add(pStr);
		}

	}


}
