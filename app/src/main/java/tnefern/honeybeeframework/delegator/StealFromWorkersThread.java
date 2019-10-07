package tnefern.honeybeeframework.delegator;

import java.io.IOException;
import java.util.ArrayList;

import tnefern.honeybeeframework.common.CommonConstants;
import tnefern.honeybeeframework.common.ConnectionFactory;
import tnefern.honeybeeframework.common.JobInitializer;
import tnefern.honeybeeframework.common.JobPool;
import tnefern.honeybeeframework.stats.TimeMeter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StealFromWorkersThread extends Thread {
	Context parent = null;
	public StealFromWorkersThread(Context pC) {
		this.parent = pC;
	}

	public void run() {
		long time = System.currentTimeMillis();
		boolean noonehasjobs = true;
		outerForLoop: for (int i = 0; i < ConnectionFactory.getInstance()
				.getConnectedWorkerList().size(); i++) {
			WorkerInfo value = ConnectionFactory.getInstance().getConnectedWorkerList()
					.get(i);
			if (value != null && value.isConnected && hasJobs(value) && JobPool.getInstance().hasJobsBeenTransmitted(
					 value.getAddress())) {
				try {
					Log.d("Delegator", "stealing now.. from " + value);
					steal(value);
					if (JobInitializer.getInstance(null).getStealList()
							.contains(value.getAddress())) {
						JobInitializer.getInstance(null).getStealList()
								.remove(value.getAddress());
					}
					JobInitializer.getInstance(null).getStealList()
							.add(value.getAddress());
					noonehasjobs = false;
					break outerForLoop;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		if(noonehasjobs){//all the workers had hasJobs(value) = false.
			//that means either all of them have been attempted before and they didnt have any jobs, or delegator hasnt finished transmitting jobs yet. So just get the least recent victim.
			ArrayList<String> lst = JobInitializer.getInstance(null).getStealList();
			if(lst.size()>0){
				String macAddress = JobInitializer.getInstance(null).getStealList().get(0);
				
				secondForLoop: for (WorkerInfo value :ConnectionFactory.getInstance()
						.getConnectedWorkerList()) {
					if (value != null && value.isConnected && value.getAddress().equals(macAddress)){
						try {
							steal(value);
							
							
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						if (JobInitializer.getInstance(null).getStealList()
								.contains(value.getAddress())) {
							JobInitializer.getInstance(null).getStealList()
									.remove(value.getAddress());
						}
						JobInitializer.getInstance(null).getStealList()
								.add(value.getAddress());
						
						noonehasjobs = false;
						break secondForLoop;
					}
				}
			}

			
		}
		TimeMeter.getInstance().logStealFromWorkersTime(
				System.currentTimeMillis() - time);
		if(noonehasjobs){
			
		}
	}

	private boolean hasJobs(WorkerInfo pInfo) {
		return JobInitializer.getInstance(this.parent).hasJobs(pInfo);
	}

	private void steal(WorkerInfo pInfo) throws IOException {
		
		Intent stealIntent = new Intent(CommonConstants.BROADCAST_DELE_INIT_STEALING_ACTION);
		stealIntent.putExtra(CommonConstants.STEAL_STRING_TYPE, pInfo.getWiFiDirectAddress());
		this.parent.sendBroadcast(stealIntent);
	}

}
