package tnefern.honeybeeframework.worker;

import java.io.File;

import tnefern.honeybeeframework.HoneybeeCrowdActivity;
import tnefern.honeybeeframework.R;
import tnefern.honeybeeframework.apps.facematch.FaceConstants;
import tnefern.honeybeeframework.common.FileFactory;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class FinishedWorkerActivity extends Activity {
	
	Button exitBtn = null;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_face_finished_worker_layout);

		exitBtn = (Button) findViewById(R.id.btnbacktomainWorker);
		exitBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent deleIntent = new Intent(FinishedWorkerActivity.this, HoneybeeCrowdActivity.class);
				deleIntent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
				startActivityForResult(deleIntent, FaceConstants.FINISHED_DELEGATOR);
				
			}
		});
	}
	
	public void onDestroy(){
		super.onDestroy();
		File dir = new File(Environment.getExternalStorageDirectory() + "/"
				+ FaceConstants.FACE_MATCH_DIR);
		// delete the contents of the faceMatch folder
		FileFactory.getInstance().deleteFolderContents(dir);
	}
}
