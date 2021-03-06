package tnefern.honeybeeframework.apps.facematch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import tnefern.honeybeeframework.common.AppInfo;
import tnefern.honeybeeframework.common.CommonConstants;
import tnefern.honeybeeframework.common.FileFactory;
import tnefern.honeybeeframework.common.JobPool;
import tnefern.honeybeeframework.delegator.AppRequest;
import tnefern.honeybeeframework.delegator.DelegatorActivity;

public class FaceMatchDelegatorActivity extends DelegatorActivity {

	private ArrayList<AppInfo> fileList = null;
	private long miscTime = System.currentTimeMillis();
	private int whenStealingFromDelegatorParamMode = CommonConstants.READ_FILES_MODE;
	private String TAG= "FaceMatchDelegatorActivity";
	private FaceRequest photo;

	public void initJobs() {
		initFiles();
		photo = new FaceRequest(fileList.size(), fileList);
		photo.setQueenBee(new FaceMatchQueenBee(this));
	}

	private void initFiles() {
		miscTime = System.currentTimeMillis();
		fileList = new ArrayList<AppInfo>();
		String folderSaved = "/storage/emulated/0/Android/data/tnefern.honeybeeframework/files/"+ FaceConstants.SAVE_PHOTO_PATH;
		//String folderSaved = Environment.getExternalStorageDirectory().getAbsolutePath()
		//		+ "/" + FaceConstants.SAVE_PHOTO_PATH;
//		AssetManager assetManager = getAssets();
//		String folderSaved = FileFactory.getInstance().copyAssets2(assetManager, FaceConstants.SAVE_PHOTO_PATH,this);
//		String folderSaved = FileFactory.getInstance().copyAssetFiles(assetManager, FaceConstants.SAVE_PHOTO_PATH,this);//folderSaved: /storage/emulated/0/Android/data/tnefern.honeybeeframework/files/samplePicsforFaces240
//		String folderSaved = getExternalFilesDir(FaceConstants.SAVE_PHOTO_PATH).getAbsolutePath();
		Log.d(TAG,"folderSaved: "+folderSaved);
		FaceResult.getInstance().setFileList(
				FileFactory.getInstance().listFiles(new File(folderSaved),
						new JpegFilter[] { new JpegFilter() }, 0));
//FileName:/storage/emulated/0/samplePicsforFaces240/2ppl copy 2.JPG

		for (File file : FaceResult.getInstance().getFilesInFolder()) {
			fileList.add(new FaceInfo(file.getAbsolutePath(), file.getName()));
		}
		Log.d(TAG,"fileList size: "+fileList.size());
		miscTime = System.currentTimeMillis() - miscTime;
		JobPool.getInstance().setStealMode(
				this.whenStealingFromDelegatorParamMode);
	}


	@Override
	public AppRequest getAppRequest() {
		return photo;
	}
	
	@Override
	public void onJobDone() {
		super.onJobDone();
		Intent deleIntent = new Intent(this, FinishedFaceMatchDelegatorActivity.class);
		this.startActivityForResult(deleIntent,
				FaceConstants.FINISHED_DELEGATOR);

	}

}
