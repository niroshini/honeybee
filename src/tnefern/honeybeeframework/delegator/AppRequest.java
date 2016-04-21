package tnefern.honeybeeframework.delegator;

import java.util.ArrayList;

import tnefern.honeybeeframework.common.AppInfo;



public interface AppRequest {

	public String[] getDistributedString();//remove this
	
	public int getNumberOfJobs();
	
	public ArrayList<AppInfo> getAppInfo();
	
//	public String getAppInfoString();
	
	public int getMode();
	
	public QueenBee getQueenBee();
	
	public void setQueenBee(QueenBee pBee);
//	public int getStealMode();
}
