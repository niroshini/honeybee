package tnefern.honeybeeframework.delegator;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

import tnefern.honeybeeframework.cloud.CloudServer;
import tnefern.honeybeeframework.common.CommonConstants;
import tnefern.honeybeeframework.common.ConnectionFactory;
import tnefern.honeybeeframework.common.FileFactory;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.net.wifi.p2p.WifiP2pDevice;

public class WorkerInfo {
	private BluetoothDevice btdevice = null;
	private BluetoothSocket btsocket = null;
	private Socket socket = null;
	private io.socket.client.Socket cloudSocket;
	private CloudServer cloudServer;
	private String wifiDirectAddress = null;
	private WifiP2pDevice wifip2pDevice = null;
	public boolean isConnected = false;
	public boolean hasJobs = true;
	public int jobsDone = 0;
	public int connection_mode = -1;
	private int stealChunk = CommonConstants.STEAL_CHUNK;
	private int maxFilesPerMsg = CommonConstants.MAX_FILES_PER_MSG;
	private ObjectOutputStream oos = null;

	public WorkerInfo(BluetoothDevice pDev, BluetoothSocket pSoc) {
		this.btdevice = pDev;
		this.btsocket = pSoc;
	}

	public WorkerInfo(String pMac) {
		this.wifiDirectAddress = pMac;
	}

	public WorkerInfo(Socket pSocket) {
		this.socket = pSocket;
	}

	public WorkerInfo(WifiP2pDevice pdev) {
		this.wifip2pDevice = pdev;
	}

	public WorkerInfo(WifiP2pDevice pdev, int pMode) {
		this.wifip2pDevice = pdev;
		this.connection_mode = pMode;
	}

	public WorkerInfo(Socket pSocket, String pWifiMac, int pMode) {
		this.socket = pSocket;
		this.wifiDirectAddress = pWifiMac;
		this.connection_mode = pMode;
	}

	public WorkerInfo(BluetoothDevice pDev) {
		this.btdevice = pDev;
	}

	public WorkerInfo(CloudServer cloudServer, int connectionMode, io.socket.client.Socket socket) {
		this.cloudServer = cloudServer;
		this.connection_mode = connectionMode;
		this.cloudSocket = socket;
	}

	public void setStealChunk(int stealChunk) {
		this.stealChunk = stealChunk;
	}

	public int getStealChunk() {
		return stealChunk;
	}

	public int getMaxFilesPerMsg() {
		return maxFilesPerMsg;
	}

	public void setMaxFilesPerMsg(int maxFilesPerMsg) {
		this.maxFilesPerMsg = maxFilesPerMsg;
	}

	public BluetoothDevice getBtDevice() {
		return btdevice;
	}

	public String getWiFiDirectAddress() {
		if (wifiDirectAddress == null) {
			if (wifip2pDevice != null) {
				return wifip2pDevice.deviceAddress;
			}
		}
		return wifiDirectAddress;
	}

	public String getAddress() {
		if (this.connection_mode == ConnectionFactory.BT_MODE
				&& this.btsocket != null) {
			this.btdevice.getAddress();
		} else if (this.connection_mode == ConnectionFactory.WIFI_MODE) {
			if (this.wifiDirectAddress != null)
				return this.wifiDirectAddress;
			if (this.wifip2pDevice != null)
				return this.wifip2pDevice.deviceAddress;
		} else if (this.connection_mode == ConnectionFactory.CLOUD_MODE) {
			if (this.cloudServer != null) {
				return cloudServer.getIpAddress();
			}
		}
		return "";

	}
	public void setObjectOutputStream(ObjectOutputStream pOos) {
		this.oos = pOos;
	}


	public BluetoothSocket getBTSocket() {
		return btsocket;
	}

	public String toString() {
		if (this.connection_mode == ConnectionFactory.BT_MODE
				&& this.btsocket != null) {
			return this.btsocket.getRemoteDevice().getName();
		} else if (this.connection_mode == ConnectionFactory.WIFI_MODE
				) {
			if(this.wifip2pDevice!=null){
				return this.wifip2pDevice.deviceName;
			}
			return "";
		} else if (this.connection_mode == ConnectionFactory.CLOUD_MODE) {
			if (cloudServer != null) {
				return cloudServer.getUrl();
			}
		}
		return "";
	}

	public void disconnectAsDelegator() throws IOException {
		if (this.connection_mode == ConnectionFactory.BT_MODE
				&& this.btsocket != null) {
			this.getBTSocket().getInputStream().close();
			this.getBTSocket().getOutputStream().close();
			this.getBTSocket().close();
		} else if (this.connection_mode == ConnectionFactory.WIFI_MODE
				&& this.socket != null) {
			this.socket.getInputStream().close();
			this.socket.getOutputStream().close();
			this.socket.close();
		} else if (this.connection_mode == ConnectionFactory.CLOUD_MODE
				&& this.cloudSocket != null) {
			this.cloudSocket.disconnect();
		}
	}

	public void terminateStealing() throws IOException {
		if (this.connection_mode == ConnectionFactory.BT_MODE
				&& this.btsocket != null) {
			FileFactory.getInstance().logJobDoneWithDate(
					"Sending Termination signal to " + getBtDevice().getName());
			this.getBTSocket()
					.getOutputStream()
					.write(ByteBuffer.allocate(4)
							.putInt(CommonConstants.READ_INT_MODE).array());
			this.getBTSocket()
					.getOutputStream()
					.write(ByteBuffer.allocate(4)
							.putInt(CommonConstants.TERM_STEALING).array());
		} else if (this.connection_mode == ConnectionFactory.WIFI_MODE) {
			if (this.oos != null) {
				this.oos.writeInt(CommonConstants.READ_INT_MODE);
				this.oos.writeInt(CommonConstants.TERM_STEALING);
			}
		} else if (this.connection_mode == ConnectionFactory.CLOUD_MODE
				&& this.cloudSocket != null) {
			this.cloudSocket.emit("terminationSignal");
		}
	}
	
	public void sayNoJobsToSteal() throws IOException {
		if (this.connection_mode == ConnectionFactory.BT_MODE
				&& this.btsocket != null) {
			FileFactory.getInstance().logJobDoneWithDate(
					"sayNoJobsToSteal signal to " + getBtDevice().getName());
			this.getBTSocket()
					.getOutputStream()
					.write(ByteBuffer.allocate(4)
							.putInt(CommonConstants.READ_INT_MODE).array());
			this.getBTSocket()
					.getOutputStream()
					.write(ByteBuffer.allocate(4)
							.putInt(CommonConstants.NO_JOBS_TO_STEAL).array());
		} else if (this.connection_mode == ConnectionFactory.WIFI_MODE) {
			if (this.oos != null) {
				this.oos.writeInt(CommonConstants.READ_INT_MODE);
				this.oos.writeInt(CommonConstants.NO_JOBS_TO_STEAL);
			}
		} else if (this.connection_mode == ConnectionFactory.CLOUD_MODE
		&& this.cloudSocket != null) {
			this.cloudSocket.emit("noJobsToSteal");
		}
	}

}
