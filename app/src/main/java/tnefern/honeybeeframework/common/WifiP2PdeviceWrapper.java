package tnefern.honeybeeframework.common;

import android.net.wifi.p2p.WifiP2pDevice;

public class WifiP2PdeviceWrapper {

	public WifiP2pDevice p2pdevice = null;
	public String wifidirectAddress = null;
	public boolean isConnected = false;
	public Object dataObject = null;

	public WifiP2PdeviceWrapper(WifiP2pDevice pObj) {
		this.p2pdevice = pObj;
	}
	
	public WifiP2PdeviceWrapper(String pStr) {
		this.wifidirectAddress = pStr;
	}
	
	public String getName(){
		return this.p2pdevice.deviceName;
	}
	
	public String getAdress(){
		return this.p2pdevice.deviceAddress;
	}
}
