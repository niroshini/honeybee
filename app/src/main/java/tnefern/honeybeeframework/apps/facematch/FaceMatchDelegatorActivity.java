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
		String f = Environment.getExternalStorageDirectory().getAbsolutePath()
				+ "/" + FaceConstants.SAVE_PHOTO_PATH;
//		String f = this.getExternalFilesDir(FaceConstants.SAVE_PHOTO_PATH).getAbsolutePath();
		AssetManager assetManager = getAssets();

		/*
		test code begin
		 */
//		try {
//
//
//			File saveDirectory = this.getExternalFilesDir(FaceConstants.SAVE_PHOTO_PATH);
//			if (!saveDirectory.mkdirs()) {
//				Log.e(TAG, "Directory not created");
//			}
//			Log.d(TAG, "saveDirectory: "+saveDirectory.getPath() );
//			String[] files = assetManager.list(FaceConstants.SAVE_PHOTO_PATH);
//			if(files!=null){
//				Log.d(TAG,"TEST "+ files[0]);
//				InputStream in= assetManager.open(FaceConstants.SAVE_PHOTO_PATH+"/"+files[0]);
//				File file = new File(saveDirectory,files[0]);
//				Log.d(TAG,"TEST file to be "+ file.getAbsolutePath());
//				file.createNewFile();
//
//				FileOutputStream out = new FileOutputStream( file);
//			}
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		/*
		test code end
		 */

		String folderSaved = FileFactory.getInstance().copyAssetFiles(assetManager, FaceConstants.SAVE_PHOTO_PATH,this);

		FaceResult.getInstance().setFileList(
				FileFactory.getInstance().listFiles(new File(folderSaved),
						new JpegFilter[] { new JpegFilter() }, 0));


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
