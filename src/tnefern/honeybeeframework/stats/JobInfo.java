package tnefern.honeybeeframework.stats;

public class JobInfo {

	public String deviceName = null;
	public long sendTime = 0;

	public JobInfo(String pName, long pTime) {
		this.deviceName = pName;
		this.sendTime = pTime;
	}
}
