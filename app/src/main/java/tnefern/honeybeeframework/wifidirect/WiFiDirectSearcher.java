package tnefern.honeybeeframework.wifidirect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import tnefern.honeybeeframework.common.CommonConstants;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class WiFiDirectSearcher {
	private WifiP2pManager manager;
	private final IntentFilter intentFilter = new IntentFilter();
	private Channel channel;
	private BroadcastReceiver receiver = null;
	private Context parent = null;
	private String workerClass = null;
	// protected static final int FILE_TRANSFER_PORT = 8988;
	private ConnectionListenerClass connectionListener = new ConnectionListenerClass();
	private GroupInfoListener groupListener = new GroupInfoListener() {

		@Override
		public void onGroupInfoAvailable(WifiP2pGroup group) {
			// TODO Auto-generated method stub
			Collection<WifiP2pDevice> clients = group.getClientList();
			if (clients != null) {
				Iterator<WifiP2pDevice> it = clients.iterator();
				while (it.hasNext()) {
					WifiP2pDevice dev = it.next();
//					Log.d("WiFiDirectSearcher", dev.deviceAddress + "   Name:"
//							+ dev.deviceName);
				}
			}
			String inter = group.getInterface();
//			Log.d("WiFiDirectSearcher", " networkInterface :" + inter);
		}
	};

	public WiFiDirectSearcher(Context parentContext, String pClass) {
		this.parent = parentContext;
		this.workerClass = pClass;
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		intentFilter
				.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		intentFilter
				.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

		//
		manager = (WifiP2pManager) parent
				.getSystemService(Context.WIFI_P2P_SERVICE);
		channel = manager.initialize(parent, parent.getMainLooper(), null);
		receiver = new ServerWiFiBroadcastReceiver(manager, channel, parent);

	}

	public void unregisterReceivers() {
		parent.unregisterReceiver(receiver);
	}

	public void registerReceivers() {
		parent.registerReceiver(receiver, intentFilter);
	}

	public void disconnect() {
		closeStreams();
		manager.removeGroup(channel, new ActionListener() {

			@Override
			public void onFailure(int reasonCode) {
//				Log.d("WiFiDirectSearcher", "Disconnect failed. Reason :"
//						+ reasonCode);

			}

			@Override
			public void onSuccess() {
//				Log.d("WiFiDirectSearcher", "Disconnect success.!");
			}

		});
	}

	public void closeStreams() {
//		Intent serviceIntent = new Intent(parent,
//				WiFiDirectFileTransferService.class);
//		serviceIntent
//				.setAction(WiFiDirectFileTransferService.ACTION_STOP_READING);
//		parent.sendBroadcast(serviceIntent);
		
		Intent talkWithServiceIntent= new Intent(WiFiDirectWorkerNonOwnerService.ACTION_STOP_READING);;
		parent.sendBroadcast(talkWithServiceIntent);
	}

	public void discoverPeers() {

		manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

			@Override
			public void onFailure(int reasonCode) {
//				Log.d("WiFiDirectSearcher: Worker", "discover FAIL");
			}

			@Override
			public void onSuccess() {
//				Log.d("WiFiDirectSearcher: Worker", "discover SUCCESS");
			}

		});
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
			Log.d("WiFiDirectSearcher: Worker", e.toString());
			return false;
		}
		return true;
	}


	// /////////////////////////////////////////////////////////////////////
	private class ServerWiFiBroadcastReceiver extends BroadcastReceiver {
		private WifiP2pManager manager;
		private Channel channel;
		private Context parentContext;
		private PeerListListener serverPeerListener;

		public ServerWiFiBroadcastReceiver(WifiP2pManager manager,
				Channel channel, Context pContext) {
			super();
			this.manager = manager;
			this.channel = channel;
			this.parentContext = pContext;
			serverPeerListener = new ServerPeerListListener();
		}

		@Override
		public void onReceive(Context arg0, Intent intent) {
			String action = intent.getAction();
			if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
				// UI update to indicate wifi p2p status.
				int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,
						-1);
				if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
					// Wifi Direct mode is enabled
					// activity.setIsWifiP2pEnabled(true);
//					Log.d("ServerWiFiBroadcastReceiver",
//							"WIFI_P2P_STATE_ENABLED");
				} else {
					// activity.setIsWifiP2pEnabled(false);
					// activity.resetData();
//					Log.d("ServerWiFiBroadcastReceiver",
//							"WIFI_P2P_STATE_DISABLED");

				}
//				Log.d("ServerWiFiBroadcastReceiver", "P2P state changed - "
//						+ state);
			} else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION
					.equals(action)) {
				// Call WifiP2pManager.requestPeers() to get a list of current
				// peers

				// request available peers from the wifi p2p manager. This is an
				// asynchronous call and the calling activity is notified with a
				// callback on PeerListListener.onPeersAvailable()
//
//				Log.d("ServerWiFiBroadcastReceiver",
//						"WIFI_P2P_PEERS_CHANGED_ACTION");
				if (manager != null) {
					// manager.requestPeers(channel, (PeerListListener)
					// activity.getFragmentManager()
					// .findFragmentById(R.id.frag_devlist));
					// manager.requestPeers(channel, clientPeerListener);
				}
//				Log.d("ServerWiFiBroadcastReceiver", "P2P peers changed");
			} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
					.equals(action)) {
				// // Respond to new connection or disconnections
				if (manager == null) {
					return;
				}

				NetworkInfo networkInfo = (NetworkInfo) intent
						.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

				if (networkInfo.isConnected()) {
					// we are connected with the other device, request
					// connection
					// info to find group owner IP

					manager.requestConnectionInfo(channel, connectionListener);
					manager.requestGroupInfo(channel, groupListener);
				} else {
					// It's a disconnect
//					Log.d("ServerWiFiBroadcastReceiver", "Worker disconnect!!!");
				}

			} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
					.equals(action)) {
				// Respond to this device's wifi state changing

				// MyListFragment fragment = (MyListFragment)
				// activity.getFragmentManager()
				// .findFragmentById(R.id.frag_devlist);
				//
				// fragment.updateThisDevice((WifiP2pDevice)
				// intent.getParcelableExtra(
				// WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
			} 

		}

	}

	// /////////////////////////////////////////////////////////////////////END class ServerWiFiBroadcastReceiver

	private class ServerPeerListListener implements PeerListListener {

		@Override
		public void onPeersAvailable(WifiP2pDeviceList arg0) {

		}

	}

	// ///////////////////////////////////////////////////////////////////////////
	private class ConnectionListenerClass implements ConnectionInfoListener {

		@Override
		public void onConnectionInfoAvailable(WifiP2pInfo connectionInfo) {
			// TODO Auto-generated method stub
//			Log.d("ConnectionListenerClass", "onConnectionInfoAvailable");
			// TODO Auto-generated method stub
			if (connectionInfo.groupFormed && connectionInfo.isGroupOwner) {// comes
																			// here
				// new FileServerAsyncTask(getActivity(),
				// mContentView.findViewById(R.id.status_text))
				// .execute();

				Log.d("ConnectionListenerClass",
						"Worker I AM THE GROUP OWNER (CLIENT)");//
				// now send the files to the other device.
				
//				Intent serviceIntent = new Intent(parent,
//						WiFiDirectWorkerNonOwnerService.class);
//				serviceIntent
//						.setAction(WiFiDirectWorkerNonOwnerService.ACTION_INIT_WIFID_CONNECTION);
//				serviceIntent
//						.putExtra(
//								WiFiDirectWorkerNonOwnerService.EXTRAS_GROUP_OWNER_ADDRESS,
//								connectionInfo.groupOwnerAddress
//										.getHostAddress());
//				serviceIntent.putExtra(
//						WiFiDirectWorkerNonOwnerService.EXTRAS_GROUP_OWNER_PORT,
//						WifiDirectConstants.FILE_TRANSFER_PORT);
//				
//				serviceIntent.putExtra(CommonConstants.ACTIVITY_CLASS_NAME, workerClass);
//				parent.startService(serviceIntent);
			} else if (connectionInfo.groupFormed) {

				Log.d("ConnectionListenerClass",
						"Worker I AM THE NON OWNER");

				Intent uiintent= new Intent(WifiDirectConstants.NOTIFY_UI_UPON_CONNECTION);
				parent.sendBroadcast(uiintent);
				
				Intent serviceIntent = new Intent(parent,
						WiFiDirectWorkerNonOwnerService.class);
				serviceIntent
						.setAction(WiFiDirectWorkerNonOwnerService.ACTION_INIT_WIFID_CONNECTION);
				serviceIntent
						.putExtra(
								WiFiDirectWorkerNonOwnerService.EXTRAS_GROUP_OWNER_ADDRESS,
								connectionInfo.groupOwnerAddress
										.getHostAddress());
				serviceIntent.putExtra(
						WiFiDirectWorkerNonOwnerService.EXTRAS_GROUP_OWNER_PORT,
						WifiDirectConstants.FILE_TRANSFER_PORT);
				
				serviceIntent.putExtra(CommonConstants.ACTIVITY_CLASS_NAME, workerClass);
				parent.startService(serviceIntent);
			}

			if (connectionInfo.groupFormed) {// comes here
				// FileServerAsyncTask fileReader = new
				// FileServerAsyncTask(parent);
				// fileReader.execute();
			}

		}

	}////////////////////////////////////////END class ConnectionListenerClass


	public void unregisterAll() {
		parent.unregisterReceiver(receiver);
	}
}
