package tnefern.honeybeeframework.worker;


import java.io.File;


import tnefern.honeybeeframework.R;
import tnefern.honeybeeframework.common.CommonConstants;
import tnefern.honeybeeframework.common.FileFactory;
import tnefern.honeybeeframework.wifidirect.WiFiDirectSearcher;
import tnefern.honeybeeframework.wifidirect.WifiDirectConstants;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public abstract class WorkerActivity extends Activity {

	Button btnDeleteJobs = null;
	private WiFiDirectSearcher wifiDSearcher = null;
	private final IntentFilter intentFilter = new IntentFilter();
	private BroadcastReceiver workerReceiver = null;
	private TextView connectLabel = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_worker_layout);
//		Bundle bundle = getIntent().getExtras();
//		String workerActivityClass = bundle
//				.getString(CommonConstants.ACTIVITY_CLASS_NAME);
		
		btnDeleteJobs = (Button) findViewById(R.id.btnDeleteJobData);
		connectLabel = (TextView) findViewById(R.id.txtOnConnect);
		btnDeleteJobs.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				WorkerNotify.getInstance(WorkerActivity.this).deleteJobData();
				
			}
		});
		workerReceiver = new WorkerBroadcastReceiver();
		intentFilter.addAction(WifiDirectConstants.NOTIFY_UI_UPON_CONNECTION);
		registerReceiver(workerReceiver, intentFilter);
		WorkerNotify.getInstance().setWorkerBee(getWorkerBee());
		this.initWifiD("");
	}

	private void initWifiD(String pClass) {
		wifiDSearcher = new WiFiDirectSearcher(this, pClass);
		wifiDSearcher.discoverPeers();
	}
	
	public abstract WorkerBee getWorkerBee();

	@Override
	public void onResume() {
		super.onResume();
		wifiDSearcher.registerReceivers();
		if (this.workerReceiver != null) {
			registerReceiver(workerReceiver, intentFilter);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		wifiDSearcher.unregisterReceivers();
		if (this.workerReceiver != null) {
			unregisterReceiver(workerReceiver);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		wifiDSearcher.disconnect();

		File f = new File(Environment.getExternalStorageDirectory() + "/"
				+ getApplicationContext().getPackageName() + "/"
				+ CommonConstants.RECEV_FILES_PATH);
		 FileFactory.getInstance().deleteFolderContents(f);//TEST o6th june
	}
	//////////////////////////////////////////////////////
	private class WorkerBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context arg0, Intent intent) {
			String action = intent.getAction();
			if (WifiDirectConstants.NOTIFY_UI_UPON_CONNECTION.equals(action)) {
				connectLabel.setText("Working...");
			}
			
		}
		
	}
}
