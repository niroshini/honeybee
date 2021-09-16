package tnefern.honeybeeframework.delegator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import io.socket.client.IO;
import io.socket.emitter.Emitter;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import tnefern.honeybeeframework.R;
import tnefern.honeybeeframework.cloud.API;
import tnefern.honeybeeframework.cloud.CloudServer;
import tnefern.honeybeeframework.cloud.FileUploadResult;
import tnefern.honeybeeframework.cloud.RetrofitClient;
import tnefern.honeybeeframework.cloud.WorkForCloud;
import tnefern.honeybeeframework.common.CommonConstants;
import tnefern.honeybeeframework.common.ConnectionFactory;
import tnefern.honeybeeframework.common.FileFactory;
import tnefern.honeybeeframework.common.JobInitializer;
import tnefern.honeybeeframework.common.JobParams;
import tnefern.honeybeeframework.common.JobPool;
import tnefern.honeybeeframework.common.WifiP2PdeviceWrapper;
import tnefern.honeybeeframework.stats.JobInfo;
import tnefern.honeybeeframework.stats.TimeMeter;
import tnefern.honeybeeframework.wifidirect.WifiDirectConstants;
import tnefern.honeybeeframework.worker.ResultTransmitObject;

import android.annotation.SuppressLint;
import android.Manifest;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class represents the generic delegator. Any application classes
 * representing the delegator view must extend this class.
 * <p>
 * DelegatorActivity searches for workers, connects to them, and handles job and
 * result transmission as well as checking for worker heart beats.
 *
 * @author tnefernando
 */
public abstract class DelegatorActivity extends AppCompatActivity {
    private String id = null;

    ProgressDialog progressDialog;
    private static final String TAG = "DelegatorActivity";
    private static final String CLOUD_TAG = "Cloud";
    private ArrayList<String> areyouthereList = null;
    private ArrayAdapter<WorkerInfo> connected = null;

    // to get data from our WiFiDirectService
    private static final String TAG2 = "FaceMatchActivity";
    private ArrayAdapter<CloudConnectionHelper> mCloudServersArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter = null;
    private ListView newDevicesListView = null;
    private ListView cloudServerListView;
    //    public static final String BROADCAST_FACEMATCHDELEGATOR_ACTION = "org.com.honeybeecrowdDemo.apps.facematch";
    boolean mBound = false;
    private Runnable deleThread = null;
    private Runnable deleStolenThread = null;

    private final IntentFilter intentFilter = new IntentFilter();

    private WifiP2pManager manager;
    private Channel channel;
    private BroadcastReceiver wifireceiver = null;
    private Vector<WifiP2pDevice> peersDiscovered = new Vector<WifiP2pDevice>();
    private ConcurrentHashMap<String, ClientSocketThread> peersConnected = new ConcurrentHashMap<String, ClientSocketThread>();
    private ConnectionListenerClass connectionListener = new ConnectionListenerClass();
    private WifiP2pInfo connectionInfo = null;
    private String currentWifiDMac = null;
    private ServerSocket ownerServerSocket = null;

    int readMode = -1;
    String status = "not initialized";
    private UITimer timer = null;
    private Hashtable<String, Long> heartbeatTimestamps = new Hashtable<String, Long>();
    private Handler discoveryHandler = new Handler();
    private int whenStealingFromDelegatorParamMode = CommonConstants.READ_FILES_MODE;
    protected AppRequest taskRequest = null;
    private BroadcastReceiver batteryInfoReceiver = null;
    private boolean isSet = false;
    private boolean isTerminated = false;

//    private android.app.Fragment customUIFragment = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delegator_layout);
//        this.customUIFragment = getFragmentManager().findFragmentById(R.id.frag_custom);
        TimeMeter.getInstance().setInitJobsTime(System.currentTimeMillis());

        batteryInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!isSet) {
                    int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,
                            -1);
                    int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE,
                            -1);

                    float batteryPct = level / (float) scale;
                    TimeMeter.getInstance().setBatteryLevel(batteryPct);
                    Log.d(TAG2, "Init time set! Battery: " + batteryPct);
                    isSet = true;
                    init();
                }

            }
        };
        // for the purpose of measuring battery
        this.registerReceiver(this.batteryInfoReceiver, new IntentFilter(
                Intent.ACTION_BATTERY_CHANGED));
    }

    private void init() {
        mCloudServersArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_checked);
        cloudServerListView = findViewById(R.id.cloudServerList);
        cloudServerListView.setOnItemClickListener((parent, view, position, id) -> {
            mCloudServersArrayAdapter.getItem(position).connectToCloudServer();
            view.setEnabled(false);
        });
        cloudServerListView.setAdapter(mCloudServersArrayAdapter );

        connected = new ArrayAdapter<WorkerInfo>(this,
                android.R.layout.simple_list_item_1);

        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1);

        // Find and set up the ListView for newly discovered devices
        newDevicesListView = (ListView) findViewById(R.id.peersFoundList);
        ListItemClicked listObj = new ListItemClicked();
        newDevicesListView.setOnItemClickListener(listObj);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);

        areyouthereList = new ArrayList<String>();

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter
                .addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter
                .addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
//        intentFilter.addAction(BROADCAST_FACEMATCHDELEGATOR_ACTION);// niro
        intentFilter
                .addAction(JobInitializer.BROADCAST_STEALER_JOBS_TO_TRANSMIT_READY_ACTION);
        intentFilter
                .addAction(JobInitializer.BROADCAST_DELEGATOR_BEING_A_VICTIM_ACTION);
        intentFilter
                .addAction(CommonConstants.BROADCAST_DELE_INIT_STEALING_ACTION);

        intentFilter
                .addAction(WifiDirectConstants.NOTIFY_ALL_WIFI_DIRECT_P2P_ARE_CONNECTED_AND_BT_MAPPED);

        intentFilter.addAction(CommonConstants.BROADCAST_DELE_STOP_READING);

        JobPool.getInstance().setStealMode(
                this.whenStealingFromDelegatorParamMode);
        initJobs();

        JobInitializer steal = JobInitializer.getInstance(0, this);
        steal.initJobPool(getAppRequest());
        try {
            this.deleThread = steal
                    .assignJobs(
                            true,
                            true,
                            "tnefern.honeybeeframework.apps.facematch.FaceMatchDelegatorActivity",
                            getAppRequest().getQueenBee());
        } catch (IOException e) {
            e.printStackTrace();
        }
        JobPool.getInstance().submitJobWorker(deleThread);
        this.initWiFiDirect();
        initCloudServerConnection();
        initCustomUI();
    }

    public abstract void initJobs();

    public void initCustomUI() {
        //nothing here
    }

    public abstract AppRequest getAppRequest();

    private String getSendString(int pMode) {
        String sendString = "";
        switch (pMode) {
            case CommonConstants.READ_FILES_MODE:
                sendString = CommonConstants.SEND_FILES;
                break;
            case CommonConstants.READ_INT_ARRAY_MODE:
                sendString = CommonConstants.SEND_INT_ARRAY;
                break;
            case CommonConstants.READ_STRING_MODE:
                sendString = CommonConstants.SEND_STRING;
                break;
        }
        return sendString;
    }

//    protected Fragment getCustomUIFragment() {
//        return this.customUIFragment;
//    }

    @Override
    public void onPause() {
        super.onPause();
        if (wifireceiver != null) {
            unregisterReceiver(wifireceiver);
        }

    }

    @Override
    public void onDestroy() {
        try {
            if (wifireceiver != null) {
                unregisterReceiver(wifireceiver);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ConnectionFactory.getInstance().getWifiDirectDeviceMap().clear();
        ConnectionFactory.getInstance().getWorkerDeviceMap().clear();
        this.closeAllWifiDirectSockets();
        this.closeAllCloudSockets();

        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (wifireceiver != null) {
            registerReceiver(wifireceiver, intentFilter);
        }

    }

    private void closeAllWifiDirectSockets() {

        Iterator<Entry<String, ClientSocketThread>> it = this.peersConnected
                .entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, ClientSocketThread> pairs = it.next();
            String key = pairs.getKey();
            ClientSocketThread wifiCon = peersConnected.get(key);
            try {
                wifiCon.closeSocket();
            } catch (IOException e) {
                e.printStackTrace();
            }
            it.remove();
        }
        try {
            if (this.ownerServerSocket != null) {
                this.ownerServerSocket.close();
            }

        } catch (IOException e1) {
            e1.printStackTrace();
        }

    }

    private void closeAllCloudSockets() {
        for (int index = 0; index < mCloudServersArrayAdapter.getCount(); index++) {
            mCloudServersArrayAdapter.getItem(index).closeCloudServerConnection();
        }
    }

    // ///////////////////////////////////////////////
    private class UITimer {
        private Handler handler;
        private Runnable runMethod;
        private int intervalMs;
        private boolean enabled = false;
        private boolean oneTime = false;

        public UITimer(Handler handler, Runnable runMethod, int intervalMs) {
            this.handler = handler;
            this.runMethod = runMethod;
            this.intervalMs = intervalMs;
        }

        public UITimer(Handler handler, Runnable runMethod, int intervalMs,
                       boolean oneTime) {
            this(handler, runMethod, intervalMs);
            this.oneTime = oneTime;
        }

        public void start() {
            if (enabled)
                return;

            if (intervalMs < 1) {
                Log.e("timer start", "Invalid interval:" + intervalMs);
                return;
            }

            enabled = true;
            handler.postDelayed(timer_tick, intervalMs);
        }

        public void stop() {
            if (!enabled)
                return;

            enabled = false;
            handler.removeCallbacks(runMethod);
            handler.removeCallbacks(timer_tick);
        }

        public boolean isEnabled() {
            return enabled;
        }

        private Runnable timer_tick = new Runnable() {
            public void run() {
                if (!enabled)
                    return;

                handler.post(runMethod);

                if (oneTime) {
                    enabled = false;
                    return;
                }

                handler.postDelayed(timer_tick, intervalMs);
            }
        };
    }

    // ////////////////////////////////////////END class UITimer

    private void updateUI() {
        if (peersDiscovered != null) {

            Iterator<WifiP2pDevice> iter = peersDiscovered.iterator();

            while (iter.hasNext()) {
                WifiP2pDevice device = iter.next();
                if (ConnectionFactory.getInstance().getWifiDirectDeviceMap()
                        .get(device.deviceAddress) != null) {
                    // the device is already in the map. Do nothing
                } else {
                    // Log.d(TAG2, "New Peers : " + peers.size());
                    mNewDevicesArrayAdapter.add(device.deviceName + "\n"
                            + device.deviceAddress);
                    Log.d(TAG2, "New Peer adr : " + device.deviceAddress);
                    ConnectionFactory
                            .getInstance()
                            .getWifiDirectDeviceMap()
                            .put(device.deviceAddress,
                                    new WifiP2PdeviceWrapper(device));

                    ConnectionFactory
                            .getInstance()
                            .getWorkerDeviceMap()
                            .put(device.deviceAddress,
                                    new WorkerInfo(device,
                                            ConnectionFactory.WIFI_MODE));
                }

            }

        } else {
            mNewDevicesArrayAdapter.clear();
            ConnectionFactory.getInstance().getWifiDirectDeviceMap().clear();
        }
    }

    // ///////////////////////////////////////////////////////////////////////////////

    // //////////////////////////////////////////////////////////////
    private class ListItemClicked implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            final String s = ((TextView) view).getText().toString();
            final TextView tv = (TextView) view;
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            connectToDevice(s, tv);

                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            // No button clicked

                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(
                    DelegatorActivity.this);
            builder.setMessage("Connect to this device?")
                    .setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();

        }

    }

    // ///////////////////////////////////////////////////////////////////////////////END
    // class ListItemClicked

    private void connectToDevice(String pStr, TextView pTv) {
        String adr = pStr.split("\n")[1];
        WifiP2PdeviceWrapper wrapper = ConnectionFactory.getInstance()
                .getWifiDirectDeviceMap().get(adr);
        if (wrapper != null) {
            SendPeersConnectInfo(wrapper);
            // now broadcast the address so that the service can hear it, and
            // would know that the activity wants to connect to the device
        }
        pTv.setBackgroundColor(Color.YELLOW);
    }

    private void SendPeersConnectInfo(WifiP2PdeviceWrapper pWrapper) {
        if (pWrapper != null && pWrapper.p2pdevice != null) {
            Log.d("WiFiBroadcastReceiver", "Device is not null!!"
                    + pWrapper.p2pdevice.deviceName + " "
                    + pWrapper.p2pdevice.deviceAddress);
            // Now connect to this device
            // obtain a peer from the WifiP2pDeviceList
            WifiP2pConfig config = new WifiP2pConfig();
            final WifiP2pDevice device = pWrapper.p2pdevice;
            final String workerIP = pWrapper.p2pdevice.deviceAddress;
            currentWifiDMac = pWrapper.p2pdevice.deviceAddress;
            config.deviceAddress = pWrapper.p2pdevice.deviceAddress;
            config.groupOwnerIntent = 15;
            config.wps = new WpsInfo();
            // config.wps.setup = WpsInfo.PBC;
            config.wps.setup = WpsInfo.KEYPAD;
            Log.d("DelegatorActivity", "before connect to "
                    + pWrapper.p2pdevice.deviceName);
            manager.connect(channel, config, new ActionListener() {
                @Override
                public void onSuccess() {
                    peersConnected.put(workerIP, new ClientSocketThread(
                            workerIP, device));
                    WorkerInfo w = new WorkerInfo(device,
                            ConnectionFactory.WIFI_MODE);
                    w.isConnected = true;
                    ConnectionFactory.getInstance().getConnectedWorkerList()
                            .add(w);
                    Log.d("WiFiBroadcastReceiver", "onSuccess!!");

                }

                @Override
                public void onFailure(int reason) {
                    // failure logic
                    currentWifiDMac = null;
                    Log.d("WiFiBroadcastReceiver", "onFailure!!");
                }
            });

        }
    }

    // ///////////////////////////////////////////////////////////////////////
    // WiFiDirect stuff
    private Runnable runMethod = new Runnable() {
        public void run() {
            if (!isTerminated) {
                discoverWiFiDirectPeers();
                checkHeartbeats();
            }

        }
    };

    private void checkHeartbeats() {
        synchronized (this.peersConnected) {
            Iterator<Entry<String, ClientSocketThread>> it = this.peersConnected
                    .entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, ClientSocketThread> pairs = it.next();
                ClientSocketThread valThread = pairs.getValue();
                if (valThread != null & valThread.isConnected) {
                    String key = pairs.getKey();

                    Long timestamp = this.heartbeatTimestamps.get(key);
                    Log.d("RANDOM", "checkHeartbeats() - timestamp null? "
                            + (timestamp == null) + "");
                    if (timestamp != null) {
                        long gap = System.currentTimeMillis()
                                - timestamp.longValue();
                        if (gap >= (WifiDirectConstants.WORKER_HEARTBEAT_TIMEOUT_MULTIPLE * WifiDirectConstants.WORKER_HEARTBEAT_SLEEP)) {
                            Log.d("RANDOM",
                                    "gap at "
                                            + gap
                                            + " is high. something could be wrong with "
                                            + ConnectionFactory.getInstance()
                                            .getWorkerDeviceMap()
                                            .get(key).toString());

                            // TEST
                            for (String wnames : areyouthereList) {
                                Log.d("RANDOM", "list : " + wnames);
                            }
                            // TEST
                            if (!areyouthereList.contains(key)) {
                                // assume this worker is lost. reassign its
                                // jobs.
                                // askAreYouThere(key);
                                areyouthereList.add(key);
                                // onWorkerSilent(key);
                            }

                        }
                    }
                }

            }
            for (String wnames : areyouthereList) {
                Log.d("RANDOM", "list : " + wnames);
                onWorkerSilent(wnames);
            }
            areyouthereList.clear();
        }
    }

    private void discoverWiFiDirectPeers() {
        Log.d("RANDOM", "discoverWiFiDirectPeers...");
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // ...
                status = "Peer found";
            }

            @Override
            public void onFailure(int reasonCode) {
                // ...
                status = "Peer discover failure!";
            }
        });
    }

    private void initWiFiDirect() {

        manager = (WifiP2pManager) this
                .getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, this.getMainLooper(),
                new WifiChannelListener());

        wifireceiver = new ClientWiFiBroadcastReceiver(manager, channel, this);
        registerReceiver(wifireceiver, intentFilter);

        // start a timer to search for resources periodically. Checking for
        // worker heart beats will also be done within the same timer.
        timer = new UITimer(discoveryHandler, runMethod, 5 * 1000);
        timer.start();

    }

    @SuppressLint("SetTextI18n")
    private void initCloudServerConnection() {
        wifireceiver = new ClientWiFiBroadcastReceiver(manager, channel, this);
        registerReceiver(wifireceiver, intentFilter);

        // add cloud servers to our list
        // TODO : This is my pc as local server. Please change it to match your local server IP or another cloud server IP
        CloudConnectionHelper helper = new CloudConnectionHelper(new CloudServer("10.0.0.53", 3000),
                cloudConnectionHelper -> {
            mCloudServersArrayAdapter.notifyDataSetChanged();
        });
        mCloudServersArrayAdapter.add(helper);
        WorkerInfo cloudWorkerInfo = new WorkerInfo(helper.cloudServer, ConnectionFactory.CLOUD_MODE, helper.socket);
        ConnectionFactory.getInstance().getWorkerDeviceMap().put(helper.cloudServer.getIpAddress(), cloudWorkerInfo);

        // TODO: This my cloud server IP. Change it to match the cloud server IP before running
        helper = new CloudConnectionHelper(new CloudServer("54.206.11.180", 3000),
                cloudConnectionHelper -> {
                    mCloudServersArrayAdapter.notifyDataSetChanged();
                });
        mCloudServersArrayAdapter.add(helper);
        cloudWorkerInfo = new WorkerInfo(helper.cloudServer, ConnectionFactory.CLOUD_MODE, helper.socket);
        ConnectionFactory.getInstance().getWorkerDeviceMap().put(helper.cloudServer.getIpAddress(), cloudWorkerInfo);
    }

    private interface CloudConnectionHelperInterface {
        void onConnected(CloudConnectionHelper cloudConnectionHelper);
    }

    private class CloudConnectionHelper {

        private CloudServer cloudServer;
        private CloudConnectionHelperInterface cloudConnectionHelperInterface;
        private io.socket.client.Socket socket;

        private String serverStatus;

        public CloudConnectionHelper(CloudServer cloudServer, CloudConnectionHelperInterface cloudConnectionHelperInterface) {
            this.cloudServer = cloudServer;
            this.cloudConnectionHelperInterface = cloudConnectionHelperInterface;
            try {
                this.socket = IO.socket(cloudServer.getUrl());
                this.serverStatus = cloudServer.getUrl() + " (Available)";
            } catch (URISyntaxException ex) {
                Log.e(CLOUD_TAG, "could not connect to cloud due to " + ex.getMessage());
            }
        }

        @NonNull
        @Override
        public String toString() {
            return serverStatus;
        }

        private final Emitter.Listener onConnected = args -> runOnUiThread(() -> {
            serverStatus = cloudServer.getUrl() + " (Connected)";
            cloudConnectionHelperInterface.onConnected(this);

            peersConnected.put(cloudServer.getIpAddress(), new ClientSocketThread(cloudServer.getIpAddress(), socket));
            WorkerInfo cloudWorkerInfo = new WorkerInfo(cloudServer, ConnectionFactory.CLOUD_MODE, socket);
            cloudWorkerInfo.isConnected = true;
            ConnectionFactory.getInstance().getConnectedWorkerList().add(cloudWorkerInfo);
            Log.d(CLOUD_TAG, "Connected");
            socket.emit("initSignal");
        });

        private final Emitter.Listener onDisconnected = args -> runOnUiThread(() -> {
            Log.d(CLOUD_TAG, "Disconnected");
        });

        private final Emitter.Listener onConnectionError = args -> runOnUiThread(() -> {
            Log.d(CLOUD_TAG, "Connection Error");
        });

        private final Emitter.Listener onPingReceived = args -> runOnUiThread(() -> {
            Log.d(CLOUD_TAG, "ping received from server");
            heartbeatTimestamps.put(cloudServer.getIpAddress(), System.currentTimeMillis());
        });

        private final Emitter.Listener onStealRequestReceived = args -> {
            Log.d(CLOUD_TAG, "Steal request from server");
            try {
                WorkerInfo worker = ConnectionFactory
                        .getInstance()
                        .getWorkerInfoFromAddress(cloudServer.getIpAddress());
                if (worker != null) {
                    JobInitializer.getInstance(
                            DelegatorActivity.this)
                            .startVictimizedForDelegator(worker, false);
                }
            } catch (IOException e) {
                e.printStackTrace();
                // comes here after disconnect
                Log.d(CLOUD_TAG, "IOException " + e.getMessage());

            }
        };

        private final Emitter.Listener onFileReceivedByWorker = args -> {
            Log.d(CLOUD_TAG, "File received by worker. Continue sending file if left");
            ClientSocketThread wifiCon;
            synchronized (peersConnected) {
                wifiCon = peersConnected.get(cloudServer.getIpAddress());

                SendDataToCloudThread sendDataToCloudThread;
                if (wifiCon != null) {
                    if (!wifiCon.isStolen) {
                        sendDataToCloudThread = new SendDataToCloudThread(
                                CommonConstants.SEND_FILES, wifiCon);
                    } else {
                        sendDataToCloudThread = new SendDataToCloudThread(
                                CommonConstants.SEND_STEAL_FILES, wifiCon);
                    }
                    sendDataToCloudThread.start();
                } else {
                    Log.d(CLOUD_TAG, "FILE_RECEIVED_FROM_DELEGATOR " + cloudServer.getIpAddress() + "wificon is NULL");
                }

            }
        };

        private final Emitter.Listener onResultsReceived = args -> {
            Log.d(CLOUD_TAG, "Result received from server");
            JSONObject data = (JSONObject) args[0];
            String result;
            try {
                result = data.getString("result");
                processStringRead(result, cloudServer.getIpAddress(), System.currentTimeMillis());
            } catch (JSONException ex) {
                ex.printStackTrace();
                Log.e(TAG, "Error in getting result due to " + ex.getMessage());
            }
            runOnUiThread(() -> {
                socket.emit("resultsReceived");
            });
        };

        private final Emitter.Listener onStolenJobsReceived = args -> runOnUiThread(() -> {
            Log.d(CLOUD_TAG, "Jobs stolen by server");
        });

        private final Emitter.Listener onNoJobsReceived = args -> runOnUiThread(() -> {
            Log.d(CLOUD_TAG, "Server has no jobs");
        });

        private void connectToCloudServer() {
            socket.on(io.socket.client.Socket.EVENT_CONNECT, onConnected);
            socket.on(io.socket.client.Socket.EVENT_DISCONNECT, onDisconnected);
            socket.on(io.socket.client.Socket.EVENT_CONNECT_ERROR, onConnectionError);
            socket.on("ping", onPingReceived);
            socket.on("StealRequest", onStealRequestReceived);
            socket.on("FileReceivedByWorker", onFileReceivedByWorker);
            socket.on("Results", onResultsReceived);
            socket.on("StolenJobs", onStolenJobsReceived);
            socket.on("NoJobs", onNoJobsReceived);
            socket.connect();
        }

        private void closeCloudServerConnection() {
            socket.off(io.socket.client.Socket.EVENT_CONNECT, onConnected);
            socket.off(io.socket.client.Socket.EVENT_DISCONNECT, onDisconnected);
            socket.off(io.socket.client.Socket.EVENT_CONNECT_ERROR, onConnectionError);
            socket.off("ping", onPingReceived);
            socket.off("StealRequest", onStealRequestReceived);
            socket.off("FileReceivedByWorker", onFileReceivedByWorker);
            socket.off("Results", onResultsReceived);
            socket.off("StolenJobs", onStolenJobsReceived);
            socket.off("NoJobs", onNoJobsReceived);
            socket.disconnect();
        }
    }

    private void stopReadingAllWorkers() {
        Iterator<String> keySet = peersConnected.keySet().iterator();
        while (keySet.hasNext()) {
            String wifiMac = keySet.next();
            ClientSocketThread wifiCon = peersConnected.get(wifiMac);
            if (wifiCon != null && wifiCon.dReader != null) {
                if (wifiCon.cloudSocket != null) {
                    wifiCon.cloudSocket.emit("terminationSignal");
                } else {
                    wifiCon.dReader.stopReading();
                    OwnerWriteThread termTask = new OwnerWriteThread(CommonConstants.SEND_TERMINATION, wifiCon);
                    termTask.start();
                }
            }

        }
    }

    /**
     * stops the periodic resource discovery and heart beat check once the task
     * is completed.
     */
    public void onJobDone() {
        if (timer != null) {
            timer.stop();
        }

        ConnectionFactory.getInstance().getWifiDirectDeviceMap().clear();
        ConnectionFactory.getInstance().getWorkerDeviceMap().clear();
        this.closeAllWifiDirectSockets();
        this.closeAllCloudSockets();
    }

    private void onWorkerTalkingAgain(String pAdr, long pTime) {
        this.heartbeatTimestamps.put(pAdr, Long.valueOf(pTime));
        this.peersConnected.get(pAdr).isConnected = true;
        ConnectionFactory.getInstance().onWorkerSilentAtConnectedWorkers(pAdr);
    }

    private void onWorkerSilent(String pAdr) {

        JobPool.getInstance().addLostWorkerJobsBack(pAdr);
        ConnectionFactory.getInstance().getWorkerDeviceMap().get(pAdr).isConnected = false;
        ConnectionFactory.getInstance().onWorkerSilentAtConnectedWorkers(pAdr);
        // areyouthereList.remove(pAdr);
        this.heartbeatTimestamps.remove(pAdr);
        // this.peersConnected.remove(pAdr);
        this.peersConnected.get(pAdr).isConnected = false;
        executeAddedBackJobs();

        /*
         * this is for connections lasting more than 30 minutes. because every
         * 30 minutes there is a dnsmasq(7837): DHCPACK(p2p-p2p0-3)
         * 192.168.49.58 0a:60:6e:a9:84:95 android-8e3f89745178b63f
         * dnsmasq(7837): DHCPREQUEST(p2p-p2p0-3) 192.168.49.37
         * 12:bf:48:f6:a1:ab thing happening that drops the live connections. we
         * dont know why this happens, but we need that for testing extreme
         * datasets (>1GB). so when that happens we connect from start again.
         */
        /*
         * JobPool.getInstance().addLostWorkerJobsBack(pAdr); try { WorkerInfo
         * deadWorker =
         * ConnectionFactory.getInstance().getWorkerDeviceMap().get(pAdr);
         * if(deadWorker!=null){ if(deadWorker.getSocket()!=null){
         * if(deadWorker.getSocket().getInputStream()!=null){
         * deadWorker.getSocket().getInputStream().close(); }
         * if(deadWorker.getSocket().getOutputStream()!=null){
         * deadWorker.getSocket().getOutputStream().close(); }
         * deadWorker.getSocket().close(); } } } catch (IOException e) {
         * e.printStackTrace(); }
         * ConnectionFactory.getInstance().getWorkerDeviceMap().remove(pAdr);
         * ConnectionFactory
         * .getInstance().onWorkerSilentAtConnectedWorkersRemove(pAdr);
         * this.heartbeatTimestamps.remove(pAdr);
         * this.peersConnected.remove(pAdr); executeAddedBackJobs();
         */

    }

    private void executeAddedBackJobs() {
        try {
            Runnable addedBackRun = JobInitializer.getInstance(this)
                    .assignAddebBackJobsForDelegator(true,
                            getAppRequest().getQueenBee());
            JobPool.getInstance().submitJobWorker(addedBackRun);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ///////////////////////////////////////////////////////
    private class WifiChannelListener implements ChannelListener {

        @Override
        public void onChannelDisconnected() {
            Log.d(TAG, "onChannelDisconnected");

        }

    }

    // ///////////////////////////////////////////////END class
    // WifiChannelListener

    // /////////////////////////////////////////////////////////////////////
    private class ClientWiFiBroadcastReceiver extends BroadcastReceiver {
        private WifiP2pManager manager;
        private Channel channel;
        private Context parentContext;
        private PeerListListener clientPeerListener;

        /**
         * @param manager  WifiP2pManager system service
         * @param channel  Wifi p2p channel
         * @param pContext activity associated with the receiver
         */
        public ClientWiFiBroadcastReceiver(WifiP2pManager manager,
                                           Channel channel, Context pContext) {

            super();
            this.manager = manager;
            this.channel = channel;
            this.parentContext = pContext;
            clientPeerListener = new ClientPeerListListener();
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
                    Log.d("WiFiBroadcastReceiver", "WIFI_P2P_STATE_ENABLED");
                } else {
                    Log.d("WiFiBroadcastReceiver", "WIFI_P2P_STATE_DISABLED");

                }
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION
                    .equals(action)) {
                // Call WifiP2pManager.requestPeers() to get a list of current
                // peers

                // request available peers from the wifi p2p manager. This is an
                // asynchronous call and the calling activity is notified with a
                // callback on PeerListListener.onPeersAvailable()

                Log.d("WiFiBroadcastReceiver", "WIFI_P2P_PEERS_CHANGED_ACTION");
                if (manager != null) {
                    manager.requestPeers(channel, clientPeerListener);
                    WifiP2pDevice device = intent
                            .getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                    if (device != null) {
                        Log.d("WiFiBroadcastReceiver", "deviceAddress= "
                                + device.deviceAddress + " deviceName= "
                                + device.deviceName);
                    }
                }
            } else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION
                    .equals(action)) {
                Log.d("WiFiBroadcastReceiver",
                        "WIFI_P2P_DISCOVERY_CHANGED_ACTION");
                int state = intent.getIntExtra(
                        WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED) {
                    Log.d("WiFiBroadcastReceiver", "WIFI_P2P_DISCOVERY_STOPPED");
                } else if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
                    Log.d("WiFiBroadcastReceiver", "WIFI_P2P_DISCOVERY_STARTED");
                }

            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
                    .equals(action)) {
                Log.d("WiFiBroadcastReceiver",
                        "WIFI_P2P_CONNECTION_CHANGED_ACTION");
                // // Respond to new connection or disconnections
                if (manager == null) {
                    return;
                }

                NetworkInfo networkInfo = (NetworkInfo) intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                WifiP2pInfo p2pInfo = (WifiP2pInfo) intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);

                if (networkInfo.isConnected()) {
                    // we are connected with the other device, request
                    // connection
                    // info to find group owner IP
                    // Log.d("WiFiBroadcastReceiver",
                    // "connected. requesting connection info");

                    manager.requestConnectionInfo(channel, connectionListener);
                } else {
                    // It's a disconnect
                    // activity.resetData();
                    Log.d("RANDOM", "It's a disconnect");// comes here after
                    // disconnect
                }

            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
                    .equals(action)) {
                Log.d("WiFiBroadcastReceiver",
                        "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
                // Respond to this device's wifi state changing

                WifiP2pDevice device = intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                if (device != null) {
                    Log.d("WiFiBroadcastReceiver", "deviceAddress= "
                            + device.deviceAddress + " deviceName= "
                            + device.deviceName);
                }

            } else if (JobInitializer.BROADCAST_DELEGATOR_BEING_A_VICTIM_ACTION
                    .equals(action)) {
                String modetype = intent.getExtras().getString(
                        CommonConstants.VICTIM_MODE_TYPE);
                String wifiAdr = (String) intent.getExtras().get(
                        CommonConstants.VICTIM_WIFIADDRESS_TYPE);// "org.com.honeybee.victim.string"
                int workerConnectionMode = intent.getExtras().getInt(
                        CommonConstants.VICTIM_WORKER_CONNECTION_MODE
                );

                ClientSocketThread wifiCon = peersConnected.get(wifiAdr);
                wifiCon.fileIndex = 0;
                wifiCon.isStolen = true;

                if (modetype.equals(CommonConstants.VICTIM_FILE_TYPE)) {
                    Object[] tosendobj = (Object[]) intent.getExtras().get(
                            CommonConstants.VICTIM_FILE_TYPE);// "org.com.honeybee.victim.file"
                    File[] tosend = null;
                    if (tosendobj != null && tosendobj.length > 0) {
                        if (tosendobj[0] instanceof File) {
                            tosend = new File[tosendobj.length];
                        }

                        for (int i = 0; i < tosendobj.length; i++) {
                            tosend[i] = (File) tosendobj[i];
                        }

                        wifiCon.dataTosend = tosend;
                        if (workerConnectionMode == ConnectionFactory.CLOUD_MODE) {
                            SendDataToCloudThread sendDataToCloudThread = new SendDataToCloudThread(
                                    CommonConstants.SEND_STEAL_FILES, wifiCon);
                            sendDataToCloudThread.start();
                        } else if (workerConnectionMode == ConnectionFactory.WIFI_MODE) {
                            OwnerWriteThread task = new OwnerWriteThread(
                                    CommonConstants.SEND_STEAL_FILES, wifiCon);
                            task.start();
                        }
                    }
                } else if (modetype.equals(CommonConstants.VICTIM_STRING_TYPE)) {
                    String tosendstr = intent.getExtras().getString(
                            CommonConstants.VICTIM_STRING_TYPE);
                    if (tosendstr != null && tosendstr.length() > 0) {
                        wifiCon.dataTosend = tosendstr;
                        OwnerWriteThread task = new OwnerWriteThread(
                                CommonConstants.SEND_STEAL_STRING, wifiCon);
                        task.start();
                    }
                }

            } else if (CommonConstants.BROADCAST_DELE_INIT_STEALING_ACTION
                    .equals(action)) {
                String wifiAdr = (String) intent.getExtras().get(
                        CommonConstants.STEAL_STRING_TYPE);
                Log.d("Delegator", "steal..thief  " + wifiAdr);
                if (wifiAdr != null && wifiAdr.length() > 0) {
                    ClientSocketThread wifiCon = peersConnected.get(wifiAdr);
                    OwnerWriteThread task = new OwnerWriteThread("thief",
                            wifiCon);
                    task.start();
                }

            } else if (CommonConstants.BROADCAST_DELE_STOP_READING
                    .equals(action)) {
                Log.d("Delegator",
                        "CommonConstants.BROADCAST_DELE_STOP_READING");
                isTerminated = true;
                stopReadingAllWorkers();
                onJobDone();
            }
        }

    }// //////////////////////////////////////////////////////END class
    // ClientWiFiBroadcastReceiver

    // /////////////////////////////////////////////////////////////////////

    private class ClientPeerListListener implements PeerListListener {

        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if (peerList != null) {
                // Log.d("ClientPeerListListener", "devices found :"
                // + peerList.getDeviceList().size());
            }

            Iterator<WifiP2pDevice> iter = peerList.getDeviceList().iterator();
            if (iter != null) {
                while (iter.hasNext()) {
                    WifiP2pDevice dev = iter.next();
                    peersDiscovered.add(dev);

                }
            }

            if (peersDiscovered.size() == 0
                    || peersConnected.size() > peersDiscovered.size()) {
                return;
            } else {
                updateUI();
            }

        }

    }

    // /////////////////////////////////////////////////////////////////////

    // //////////////////////////////////////////////////////
    private class ClientSocketThread extends Thread {
        private String wifiMACAddress = null;
        Socket cWorker = null;
        io.socket.client.Socket cloudSocket;
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;
        InputStream is = null;
        OutputStream os = null;
        private WifiP2pDevice wifiDirectClient = null;
        private boolean hasRunOnce = false;
        DelegatorReadThread dReader = null;
        Object dataTosend = null;
        int fileIndex = 0;
        boolean isStolen = false;
        boolean isConnected = true;

        public ClientSocketThread(String pAdr, WifiP2pDevice pDev) {
            this.wifiMACAddress = pAdr;
            this.wifiDirectClient = pDev;
        }

        public ClientSocketThread(String ipAddress, io.socket.client.Socket cloudSocket) {
            this.wifiMACAddress = ipAddress;
            this.cloudSocket = cloudSocket;
        }

        public void run() {
            try {
                hasRunOnce = true;

                String clientInet = cWorker.getInetAddress().getHostAddress();
                String localAdr = cWorker.getLocalAddress().getHostAddress();

                StringBuffer s = new StringBuffer();
                s.append(WifiDirectConstants.WIFIP2P_ADDRESS);
                s.append(wifiMACAddress);
                os = cWorker.getOutputStream();
                is = cWorker.getInputStream();
                oos = new ObjectOutputStream(os);
                oos.flush();
                ois = new ObjectInputStream(is);
                ConnectionFactory.getInstance().getWorkerDeviceMap()
                        .get(wifiMACAddress).setObjectOutputStream(oos);

                oos.writeInt(CommonConstants.READ_STRING_MODE);
                oos.flush();
                oos.writeUTF(s.toString());
                oos.flush();

                JobParams params = JobPool.getInstance()
                        .fetchJobsToTransmitToWorker(
                                CommonConstants.WORKER_INIT_JOBS,
                                wifiMACAddress, DelegatorActivity.this);
                this.dataTosend = params.paramObject;
                String sendString = getSendString(params.paramMode);
                Log.d(TAG, "sendString " + sendString);

                OwnerWriteThread task = new OwnerWriteThread(sendString, this);
                task.start();

                dReader = new DelegatorReadThread(ois, wifiMACAddress);
                dReader.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void closeSocket() throws IOException {
            if (this.dReader != null) {
                this.dReader.isReading = false;
            }
            if (this.oos != null) {
                this.oos.close();
                this.oos = null;
            }
            if (this.ois != null) {
                this.ois.close();
                this.ois = null;
            }

            if (this.ois != null) {
                this.ois.close();
                this.ois = null;
            }

            if (cWorker != null) {
                cWorker.close();
                cWorker = null;
            }
            this.wifiMACAddress = null;

        }

    }

    // /////////////////////////////////////////////// END class
    // ClientSocketThread

    // //////////////////////////////////////////////////////

    /**
     * This thread handles all incoming communication from workers. Each worker
     * has its own DelegatorReadThread.
     *
     * @author tnefernando
     */
    private class DelegatorReadThread extends Thread {
        ObjectInputStream ois = null;

        String wifiAdr = null;
        private boolean isReading = true;

        DelegatorReadThread(ObjectInputStream pIs, String pStr) {
            ois = pIs;
            wifiAdr = pStr;
        }

        synchronized void stopReading() {
            this.isReading = false;
        }

        synchronized boolean isReading() {
            return this.isReading;
        }

        public void run() {
            try {
                readLoop:
                while (isReading()) {
                    if (readMode <= 0 && ois != null) {
                        synchronized (ois) {

                            readMode = ois.readInt();

                            if (!peersConnected.get(wifiAdr).isConnected) {
                                onWorkerTalkingAgain(wifiAdr,
                                        System.currentTimeMillis());
                            }
                            switch (readMode) {
                                case CommonConstants.READ_INT_MODE:
                                    int readInt = ois.readInt();
                                    readMode = 0;

                                    if (readInt == WifiDirectConstants.FILE_RECEIVED_FROM_DELEGATOR) {
                                        heartbeatTimestamps.put(this.wifiAdr,
                                                System.currentTimeMillis());
                                        Log.d("DelegatorReadThread", "ReadINT "
                                                + "FILE_RECEIVED_FROM_DELEGATOR "
                                                + wifiAdr);
                                        ClientSocketThread wifiCon = null;
                                        synchronized (peersConnected) {
                                            wifiCon = peersConnected.get(wifiAdr);

                                            OwnerWriteThread task = null;
                                            if (wifiCon != null) {
                                                if (!wifiCon.isStolen) {
                                                    task = new OwnerWriteThread(
                                                            "file", wifiCon);
                                                } else {
                                                    task = new OwnerWriteThread(
                                                            "stealfile", wifiCon);
                                                }
                                                task.start();
                                            } else {
                                                Log.d("DelegatorReadThread",
                                                        "FILE_RECEIVED_FROM_DELEGATOR "
                                                                + wifiAdr
                                                                + "wificon is NULL");
                                            }

                                        }
                                    } else if (readInt == CommonConstants.INIT_STEALING) {
                                        heartbeatTimestamps.put(this.wifiAdr,
                                                System.currentTimeMillis());
                                        Log.d("DelegatorReadThread", "ReadINT "
                                                + "INIT_STEALING");

                                        WorkerInfo worker = ConnectionFactory
                                                .getInstance()
                                                .getWorkerInfoFromAddress(wifiAdr);
                                        if (worker != null) {
                                            FileFactory
                                                    .getInstance()
                                                    .logJobDoneWithDate(
                                                            worker.toString()
                                                                    + " is trying to steal from me");
                                            JobInitializer.getInstance(
                                                    DelegatorActivity.this)
                                                    .startVictimizedForDelegator(
                                                            worker, false);
                                        }

                                    } else if (readInt == CommonConstants.NO_JOBS_TO_STEAL) {
                                        heartbeatTimestamps.put(this.wifiAdr,
                                                System.currentTimeMillis());
                                        // this.worker.hasJobs = false;
                                        WorkerInfo wifo = ConnectionFactory
                                                .getInstance()
                                                .getWorkerInfoFromWifiDirectAddress(
                                                        this.wifiAdr);

                                        if (wifo != null) {
                                            wifo.hasJobs = false;
                                            Log.d("Reader",
                                                    "No jobs to steal from "
                                                            + wifo.toString());
                                        }

                                        readMode = 0;
                                        // now see if there are any expired jobs
                                        // (jobs that were given to workers long
                                        // ago. worker is still alive and sending
                                        // heartbeat, but has not sent the results
                                        // yet)
                                        String expiredWorker = JobPool
                                                .getInstance().hasJobsExpired();
                                        if (expiredWorker != null) {
                                            Log.d("Reader", "expiredWorker: "
                                                    + expiredWorker);
                                            JobPool.getInstance()
                                                    .addLostWorkerJobsBack(
                                                            expiredWorker);
                                            executeAddedBackJobs();
                                        } else {
                                            if (!JobPool.getInstance()
                                                    .isJobPoolDone()) {
                                                StealFromWorkersThread tfw = new StealFromWorkersThread(
                                                        DelegatorActivity.this);
                                                tfw.start();
                                            }
                                        }

                                    }

                                    break;
                                case CommonConstants.READ_STRING_MODE:
                                    long t2 = System.currentTimeMillis();
                                    String readString = ois.readUTF();
                                    Log.d("WifiDirectService",
                                            "ReadSTRING from "
                                                    + ConnectionFactory
                                                    .getInstance()
                                                    .getWorkerDeviceMap()
                                                    .get(this.wifiAdr)
                                                    .toString() + " "
                                                    + readString);

                                    processStringRead(readString, wifiAdr, t2);

                                    if (readString
                                            .contains(CommonConstants.RESULT_SYMBOL)) {
                                        // tell the worker you received all of it.
                                        // we do
                                        // this because this is a big write and we
                                        // need
                                        // the worker to hold writing us anything
                                        // else
                                        // until this is read completely.
                                        ClientSocketThread wifiCon = null;
                                        synchronized (peersConnected) {
                                            wifiCon = peersConnected
                                                    .get(this.wifiAdr);

                                            OwnerWriteThread task = null;
                                            if (wifiCon != null) {

                                                task = new OwnerWriteThread(
                                                        CommonConstants.SEND_RESULTS_RECEIVED,
                                                        wifiCon);
                                                task.start();
                                                // Log.d("DelegatorReadThread",
                                                // "SEND_RESULTS_RECEIVED");
                                            }

                                        }
                                    }

                                    readString = null;
                                    readMode = 0;
                                    heartbeatTimestamps.put(this.wifiAdr,
                                            System.currentTimeMillis());
                                    break;
                                case CommonConstants.READ_FILE_MODE:
                                    heartbeatTimestamps.put(this.wifiAdr,
                                            System.currentTimeMillis());
                                    readMode = 0;
                                    break;
                                case WifiDirectConstants.WORKER_HEARTBEAT:
                                    // Log.d("RANDOM", "WORKER_HEARTBEAT ");
                                    readMode = 0;
                                    heartbeatTimestamps.put(this.wifiAdr,
                                            System.currentTimeMillis());
                                    break;
                                case CommonConstants.READ_COMPLETED_JOB_OBJECT_ARRAY_MODE:
                                    long t3 = System.currentTimeMillis();
                                    try {
                                        Object[] cjobs = (Object[]) ois
                                                .readObject();
                                        if (cjobs != null && cjobs.length > 0) {
                                            Log.d("Results",
                                                    "ReadCompletedJob Array from "
                                                            + ConnectionFactory
                                                            .getInstance()
                                                            .getWorkerDeviceMap()
                                                            .get(this.wifiAdr)
                                                            .toString()
                                                            + " " + cjobs.length);

                                            ResultTransmitObject resObs[] = new ResultTransmitObject[cjobs.length];
                                            int k = 0;
                                            for (Object o : cjobs) {
                                                if (o instanceof ResultTransmitObject) {
                                                    resObs[k] = (ResultTransmitObject) o;
                                                }
                                                k++;
                                            }
                                            processObjectArrayRead(resObs, wifiAdr,
                                                    t3);

                                        }
                                    } catch (ClassNotFoundException e) {
                                        e.printStackTrace();
                                    }
                                    readMode = 0;
                                    heartbeatTimestamps.put(this.wifiAdr,
                                            System.currentTimeMillis());
                                    // tell the worker you received all of it. we do
                                    // this because this is a big write and we need
                                    // the worker to hold writing us anything else
                                    // until this is read completely.
                                    ClientSocketThread wifiCon = null;
                                    synchronized (peersConnected) {
                                        wifiCon = peersConnected.get(this.wifiAdr);

                                        OwnerWriteThread task = null;
                                        if (wifiCon != null) {

                                            task = new OwnerWriteThread(
                                                    CommonConstants.SEND_RESULTS_RECEIVED,
                                                    wifiCon);
                                            task.start();
                                            Log.d("DelegatorReadThread",
                                                    "SEND_RESULTS_RECEIVED");
                                        }

                                    }

                                    break;
                            }
                            ois.notify();
                        }
                    }
                }// end while
            } catch (IOException e) {
                e.printStackTrace();
                // comes here after disconnect
                Log.d("RANDOM", "IOException " + e.getMessage());

            }
        }
    }

    // //////////////////////////////////////////////////////////////////// END
    // class DelegatorReadThread
    private void processObjectArrayRead(ResultTransmitObject[] pObjs,
                                        String pWifi, long pT) {
        ReceivedResults resObj = new ReceivedResults(
                CommonConstants.READ_COMPLETED_JOB_OBJECT_ARRAY_MODE);
        resObj.resultData = pObjs;
        resObj.fromWorker = pWifi;
        if (ConnectionFactory.getInstance().getResultsReader() != null) {
            ConnectionFactory.getInstance().getResultsReader()
                    .onResultsRead(resObj);
        } else {
            ConnectionFactory.getInstance().addResult(resObj);
        }

        TimeMeter.getInstance().addToReadingResults(
                System.currentTimeMillis() - pT);
    }

    private void processStringRead(String pS, String pWifi, long pT) {
        if (pS != null && pS.length() > 0) {
            if (pS.contains(CommonConstants.MSG_BREAK)) {
                pS = pS.substring(0, pS.lastIndexOf(CommonConstants.MSG_BREAK));

                if (pS.startsWith(CommonConstants.RESULT_SYMBOL)) {// then
                    // these
                    // are
                    // not
                    // jobs.
                    // Possibly
                    // job
                    // results
                    // in
                    // String
                    // format.
                    Log.d("processStringRead",
                            "Received string results . pMsg.startsWith(CommonConstants.RESULT_SYMBOL) ");
                    ReceivedResults resObj = new ReceivedResults(
                            CommonConstants.READ_STRING_MODE);
                    resObj.stringResults = pS;
                    resObj.fromWorker = pWifi;

                    if (ConnectionFactory.getInstance().getResultsReader() != null) {
                        ConnectionFactory.getInstance().getResultsReader()
                                .onResultsRead(resObj);
                    } else {
                        ConnectionFactory.getInstance().addResult(resObj);
                    }

                    TimeMeter.getInstance().addToReadingResults(
                            System.currentTimeMillis() - pT);
                } else {
                    JobParams jp = new JobParams(
                            CommonConstants.READ_STRING_MODE);

                    ConnectionFactory.getInstance().getWorkerInfoFromAddress(
                            pWifi).hasJobs = true;

                    jp.paramsString = pS;
                    processReadsForJobParams(jp, pWifi);

                    TimeMeter.getInstance()
                            .addReadStolenParamTime(
                                    new JobInfo(pWifi, System
                                            .currentTimeMillis() - pT));
                }

            }
        }

    }

    private void processReadsForJobParams(JobParams pMsg, String pWifiAdr) {
        if (pMsg != null) {
            if (pMsg.paramMode == CommonConstants.READ_STRING_MODE) {

                if (pMsg.paramsString.startsWith(CommonConstants.PARAM_SYMBOL)) {
                    Log.d("Reader",
                            "Received stolen jobs . pMsg.startsWith(CommonConstants.PARAM_SYMBOL) ");

                    pMsg.paramsString = pMsg.paramsString.substring(
                            CommonConstants.PARAM_SYMBOL.length(),
                            pMsg.paramsString.length());
                    try {
                        this.deleStolenThread = JobInitializer
                                .getInstance(this)
                                .assignStolenJobsForDelegator(true, pMsg,
                                        getAppRequest().getQueenBee());
                        JobPool.getInstance().submitJobWorker(deleStolenThread);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else if (pMsg.paramMode == CommonConstants.READ_FILES_MODE) {
                if (pMsg.paramsString != null) {

                    if (ConnectionFactory.getInstance().getResultsReader() != null) {
                        ReceivedResults resObj = new ReceivedResults(
                                CommonConstants.READ_FILES_MODE,
                                ConnectionFactory.getInstance()
                                        .getWorkerInfoFromAddress(pWifiAdr)
                                        .toString());
                        resObj.stringResults = pMsg.paramsString;
                        ConnectionFactory.getInstance().getResultsReader()
                                .onResultsRead(resObj);
                    }
                } else {
                    try {
                        this.deleStolenThread = JobInitializer
                                .getInstance(this)
                                .assignStolenJobsForDelegator(true, pMsg,
                                        getAppRequest().getQueenBee());
                        JobPool.getInstance().submitJobWorker(deleStolenThread);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else if (pMsg.paramMode == CommonConstants.READ_FILE_MODE) {
                if (pMsg.paramsString != null) {

                    if (ConnectionFactory.getInstance().getResultsReader() != null) {
                        ReceivedResults resObj = new ReceivedResults(
                                CommonConstants.READ_FILE_MODE);
                        resObj.stringResults = pMsg.paramsString;
                        ConnectionFactory.getInstance().getResultsReader()
                                .onResultsRead(resObj);
                    }
                } else {
                    try {
                        this.deleStolenThread = JobInitializer
                                .getInstance(this)
                                .assignStolenJobsForDelegator(true, pMsg,
                                        getAppRequest().getQueenBee());
                        JobPool.getInstance().submitJobWorker(deleStolenThread);// November
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    // ///////////////////////////////////////////////////////////////////////////

    /**
     * The delegator needs to be the p2p group owner.
     *
     * @author tnefernando
     */
    private class ConnectionListenerClass implements ConnectionInfoListener {

        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo arg0) {
            connectionInfo = arg0;
            // The owner IP is now known.
            String hostIP = connectionInfo.groupOwnerAddress.getHostAddress();

            // After the group negotiation, we assign the group owner as the
            // file
            // server. The file server is single threaded, single connection
            // server
            // socket.
            if (connectionInfo.groupFormed && connectionInfo.isGroupOwner) {//

                Log.d("ConnectionListenerClass",
                        "Delegator I AM THE GROUP OWNER and my IP= "
                                + connectionInfo.groupOwnerAddress);// we come
                // here

                Thread t = new Thread(new ConnectAsOwner());
                t.start();
            } else if (connectionInfo.groupFormed) {

                Log.d("ConnectionListenerClass",
                        "Delegator I AM NOT the group owner");
            }

        }

    }// ///////////////////////////////////END class ConnectionListenerClass

    // /////////////////////////////////////////////////////

    public class OwnerWriteThread extends Thread {
        String mode = "";
        ClientSocketThread cst = null;

        OwnerWriteThread(String pMode, ClientSocketThread pThr) {
            this.mode = pMode;
            this.cst = pThr;
        }

        private void sendFile(ClientSocketThread pWifiCon) throws IOException {
            Log.d(TAG, "sendFile");
            if (pWifiCon.dataTosend != null
                    && pWifiCon.dataTosend instanceof File[]) {
                File[] filesToSend = (File[]) pWifiCon.dataTosend;
                if (pWifiCon.fileIndex < filesToSend.length) {
                    File zipF = filesToSend[pWifiCon.fileIndex]
                            .getAbsoluteFile();

                    Log.d(TAG, "zipF " + zipF.getAbsolutePath());

                    pWifiCon.oos.writeInt(CommonConstants.READ_FILE_MODE);
                    pWifiCon.oos.flush();

                    byte[] fileBytes = FileFactory.getInstance().getFileBytes(
                            zipF);

                    writeFileToStream(fileBytes, pWifiCon.os, pWifiCon.oos);
                    pWifiCon.fileIndex++;
                    // now delete the zip files
                    zipF.delete();

                } else {// all the files have been sent. tell the worker so.
                    Log.d("WifiDirectService", "ALL_INIT_JOBS_SENT to: "
                            + pWifiCon.wifiMACAddress);
                    JobPool.getInstance().finishedTransmittingParams(
                            pWifiCon.wifiMACAddress);

                    pWifiCon.oos.writeInt(CommonConstants.READ_INT_MODE);
                    pWifiCon.oos.writeInt(CommonConstants.ALL_INIT_JOBS_SENT);
                    pWifiCon.oos.flush();
                    pWifiCon.isStolen = false;
                }

            } else {
            }
        }

        private void sendStringParams(ClientSocketThread pWifiCon)
                throws IOException {
            if (pWifiCon.dataTosend != null
                    && pWifiCon.dataTosend instanceof String) {
                String strToSend = (String) pWifiCon.dataTosend;

                if (strToSend != null && !strToSend.isEmpty()) {
                    pWifiCon.oos.writeInt(CommonConstants.READ_STRING_MODE);
                    pWifiCon.oos.flush();
                    pWifiCon.oos.writeUTF(strToSend);
                    pWifiCon.oos.flush();

                    Log.d("WifiDirectService", "ALL_INIT_JOBS_SENT to: "
                            + pWifiCon.wifiMACAddress);
                    JobPool.getInstance().finishedTransmittingParams(
                            pWifiCon.wifiMACAddress);

                    pWifiCon.oos.writeInt(CommonConstants.READ_INT_MODE);
                    pWifiCon.oos.writeInt(CommonConstants.ALL_INIT_JOBS_SENT);
                    pWifiCon.oos.flush();
                    pWifiCon.isStolen = false;
                }
            }
        }

        public void run() {
            Log.d(TAG, "OwnerWriteThread run " + mode);
            if (mode.equals(CommonConstants.SEND_FILES)) {
                try {
                    sendFile(cst);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if (mode.equals(CommonConstants.SEND_STRING)) {
                try {
                    sendStringParams(cst);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (mode.equals(CommonConstants.SEND_STEAL_STRING)) {
                try {
                    cst.oos.writeInt(CommonConstants.VICTIM_MODE);
                    sendStringParams(cst);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (mode.equals(CommonConstants.SEND_STEAL_FILES)) {
                try {
                    cst.oos.writeInt(CommonConstants.VICTIM_MODE);
                    sendFile(cst);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (mode.equals(CommonConstants.SEND_THIEF)) {
                try {
                    Log.d("im a thief", "thief run");
                    ObjectOutputStream oout = cst.oos;

                    synchronized (oout) {
                        Log.d("im a thief", "synchronized thief run");
                        oout.flush();
                        Log.d("im a thief", "flush thief run");
                        oout.writeInt(CommonConstants.READ_INT_MODE);
                        Log.d("im a thief", "writeInt thief run");
                        oout.writeInt(CommonConstants.INIT_STEALING);
                        Log.d("im a thief", "INIT_STEALING thief run");
                        oout.flush();
                        oout.notify();
                    }
                    Log.d("im a thief", "synchronized OUT thief run");
                    // cst.oos.writeInt(CommonConstants.READ_INT_MODE);
                    // cst.oos.writeInt(CommonConstants.INIT_STEALING);
                    // cst.oos.flush();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (mode.equals(CommonConstants.SEND_ARE_YOU_THERE)) {
                try {
                    cst.oos.writeInt(WifiDirectConstants.WORKER_ARE_YOU_THERE);
                    cst.oos.flush();
                    Log.d("RANDOM",
                            cst.wifiMACAddress + " are you there?"
                                    + "cst.cWorker.isConnected()?"
                                    + cst.cWorker.isConnected()
                                    + "  cWorker.isClosed()?"
                                    + cst.cWorker.isClosed());
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("RANDOM", cst.wifiMACAddress + " worker's gone!");
                }
            } else if (mode.equals(CommonConstants.SEND_RESULTS_RECEIVED)) {
                try {
                    cst.oos.writeInt(CommonConstants.READ_STRING_MODE);
                    cst.oos.writeUTF(CommonConstants.SEND_RESULTS_RECEIVED);
                    cst.oos.flush();
                    Log.d("Results", "sent SEND_RESULTS_RECEIVED");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("RANDOM", cst.wifiMACAddress + " worker's gone!");
                }
            } else if (mode.equals(CommonConstants.SEND_TERMINATION)) {
                try {
                    if (cst != null) {
                        if (cst.oos != null) {
                            cst.oos.writeInt(CommonConstants.READ_INT_MODE);
                            cst.oos.writeInt(CommonConstants.TERM_STEALING);
                            cst.oos.flush();
                        }
                    }

                    Log.d("SEND_TERMINATION", "sent TERM_STEALING");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("SEND_TERMINATION", cst.wifiMACAddress + " ERROR");
                }
            }
        }

        private void writeFileToStream(byte[] fbytes, OutputStream pOs,
                                       ObjectOutputStream pObjectS) throws IOException {
            pObjectS.writeInt(fbytes.length);
            pObjectS.flush();
            int writtenBytes = 0;
            int fullLen = fbytes.length;
            Log.d("writeFileToStream: length = ", fullLen + "");
            while (writtenBytes < fullLen) {
                if (fullLen - writtenBytes >= CommonConstants.PACKET_SIZE) {
                    pOs.write(fbytes, writtenBytes, CommonConstants.PACKET_SIZE);
                    pOs.flush();
                    writtenBytes += CommonConstants.PACKET_SIZE;

                } else {
                    pOs.write(fbytes, writtenBytes, fullLen - writtenBytes);
                    pOs.flush();
                    writtenBytes += (fullLen - writtenBytes);
                }
            }

        }

    }

    // ///////////////////////////////////////////////////////////////////////////END
    // class OwnerWriteThread

    private class SendDataToCloudThread extends Thread {

        private final String mode;
        private final ClientSocketThread csThread;

        public SendDataToCloudThread(String mode, ClientSocketThread csThread) {
            this.mode = mode;
            this.csThread = csThread;
        }

        @Override
        public void run() {
            Log.d(CLOUD_TAG, "SendDataToCloudThread run " + mode);
            switch (mode) {
                case CommonConstants.SEND_STEAL_FILES:
                    sendFile();
                    break;
                default:

            }
        }

        private void sendFile() {
            Log.d(CLOUD_TAG, "sending file");
            Object dataToSend = csThread.dataTosend;
            if (dataToSend instanceof File[]) {
                File[] filesToSend = (File[]) dataToSend;
                int fileIndex = csThread.fileIndex;
                if (fileIndex < filesToSend.length) {
                    uploadFile(filesToSend[fileIndex]);
                } else {// all the files have been sent. tell the worker so.
                    Log.d(CLOUD_TAG, "ALL_INIT_JOBS_SENT to: "
                            + csThread.wifiMACAddress);
                    JobPool.getInstance().finishedTransmittingParams(
                            csThread.wifiMACAddress);

                    csThread.cloudSocket.emit("allInitJobsSent");
                    csThread.isStolen = false;
                }
            }
        }

        public void uploadFile(File fileToSend) {
            File zipF = fileToSend.getAbsoluteFile();

            Log.d(CLOUD_TAG, "zipF " + zipF.getAbsolutePath());

            RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-file"), zipF);
            MultipartBody.Part fileMultipart = MultipartBody.Part.createFormData("file", zipF.getName(), requestBody);
            // TODO : fetch ip and port from helper instead of passing static port
            API api = RetrofitClient.getInstance(csThread.wifiMACAddress + ":3000").getAPI();
            Call<FileUploadResult> uploadCall = api.uploadFile(fileMultipart);
            uploadCall.enqueue(new Callback<FileUploadResult>() {
                @Override
                public void onResponse(Call<FileUploadResult> call, Response<FileUploadResult> response) {
                    if (response.isSuccessful()) {
                        csThread.fileIndex++;
                        zipF.delete();
                        Log.d(CLOUD_TAG, "File uploaded");

                        String uploadedFilePath = response.body().data.filePath;
                        csThread.cloudSocket.emit("stolenJobs", new Gson().toJson(new WorkForCloud("faceDetect", uploadedFilePath)));
                    } else {
                        Log.d(CLOUD_TAG, "File upload error: " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<FileUploadResult> call, Throwable t) {
                    t.printStackTrace();
                    Log.d(CLOUD_TAG, "Error while uploading file : " + t.getMessage());
                }
            });
        }
    }

    // ///////////////////////////////////////////////////////////////////////
    private class ConnectAsOwner implements Runnable {

        @Override
        public void run() {
            connect_as_owner();

        }

    }

    // ///////////////////////////////////////////////////////////////////////
    // END class ConnectAsOwner

    private void connect_as_owner() {

        try {
            if (ownerServerSocket == null) {
                ownerServerSocket = new ServerSocket(
                        WifiDirectConstants.FILE_TRANSFER_PORT);
            }
            Socket workerSocket = ownerServerSocket.accept();
            ClientSocketThread ctr = this.peersConnected.get(currentWifiDMac);
            if (!ctr.hasRunOnce) {
                ctr.cWorker = workerSocket;
                ctr.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    protected void loadFragment(Fragment fragment) {
// create a FragmentManager
        FragmentManager fm = getFragmentManager();
// create a FragmentTransaction to begin the transaction and replace the Fragment
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
// replace the FrameLayout with new Fragment
        fragmentTransaction.replace(R.id.frameLayout, fragment);
        fragmentTransaction.commit(); // save the changes
    }
}
