package tnefern.honeybeeframework.apps.facematch;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import tnefern.honeybeeframework.HoneybeeCrowdActivity;
import tnefern.honeybeeframework.R;
import tnefern.honeybeeframework.common.ConnectionFactory;
import tnefern.honeybeeframework.common.FileFactory;
import tnefern.honeybeeframework.delegator.WorkerInfo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class FinishedFaceMatchDelegatorActivity extends Activity {
	Button exitBtn = null;
	private TextView numberFound = null;
	private ImageView imageView = null;
	private Button nextButton = null;
	private Iterator<File>iter = null;
	private TextView stats = null;
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_face_finished_dele_layout);
		numberFound = (TextView) findViewById(R.id.labeldeleDonefaces);
		stats = (TextView) findViewById(R.id.txtOnConnect);
		exitBtn = (Button) findViewById(R.id.btnFaceExitAll);
		exitBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try {
					disconnectAllworkers();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				Intent deleIntent = new Intent(FinishedFaceMatchDelegatorActivity.this, HoneybeeCrowdActivity.class);
				deleIntent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
				startActivityForResult(deleIntent, FaceConstants.FINISHED_DELEGATOR);
				
			}
		});
		imageView = (ImageView) findViewById(R.id.deledoneimageview);
		nextButton = (Button) findViewById(R.id.btnNextImage);
		nextButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				iterateOnce();
				
			}
		});
		
		stats.setText("Speedup = "+FaceResult.getInstance().getSpeedup()+" Stats: "+FaceResult.getInstance().getCompareString());
		loadFirstImage();

	}
	private void iterateOnce(){
		File f = null;
		if(iter.hasNext()){
			f = iter.next();
			if(f!=null){
				this.setImagetoView(f.getAbsolutePath());
				String s = FileFactory.getInstance().getFileNameFromFullPath(f.getAbsolutePath());
				int num = FaceResult.getInstance().getNumberOfFaces(s);
				numberFound.setText("Number of faces in "+s+" = "+num);
			}
		}else{
			if( !FaceResult.getInstance().getFilesInFolder().isEmpty()){
				iter = FaceResult.getInstance().getFilesInFolder().iterator();
			}
		}
	}
	private void loadFirstImage(){
		
		if( !FaceResult.getInstance().getFilesInFolder().isEmpty()){
			iter = FaceResult.getInstance().getFilesInFolder().iterator();
		}
		iterateOnce();
		
		
		
	}
	
	private void setImagetoView(String pImPath){
		File imgFile = new  File(pImPath);
		if(imgFile.exists()){

		    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
		    imageView.setImageBitmap(myBitmap);

		}

	}
	
	void disconnectAllworkers() throws IOException {
		for (int i = 0; i < ConnectionFactory.getInstance().getConnectedWorkerList()
				.size(); i++) {
			WorkerInfo value = ConnectionFactory.getInstance().getConnectedWorkerList()
					.get(i);
			value.disconnectAsDelegator();
		}
	}

}
