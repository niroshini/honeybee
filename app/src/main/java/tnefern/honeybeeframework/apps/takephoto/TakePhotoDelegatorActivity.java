package tnefern.honeybeeframework.apps.takephoto;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import tnefern.honeybeeframework.R;
import tnefern.honeybeeframework.common.AppInfo;
import tnefern.honeybeeframework.common.CommonConstants;
import tnefern.honeybeeframework.common.FileFactory;
import tnefern.honeybeeframework.common.JobPool;
import tnefern.honeybeeframework.delegator.AppRequest;
import tnefern.honeybeeframework.delegator.DelegatorActivity;

public class TakePhotoDelegatorActivity extends DelegatorActivity {

	private ArrayList<AppInfo> fileList = null;
	private long miscTime = System.currentTimeMillis();
	private int whenStealingFromDelegatorParamMode = CommonConstants.READ_FILES_MODE;
	private String TAG= "TakePhotoDelegatorActivity";
	private TakePhotoRequest photo;
	static final int REQUEST_IMAGE_CAPTURE = 1;
//	ImageView im;
	private ShowPhotoFragment frag;

	static final int REQUEST_TAKE_PHOTO = 1;
	String currentPhotoPath;

	private Camera mCamera;
	private CameraPreview mCameraPreview;

	public void initJobs() {
		initFiles();
		photo = new TakePhotoRequest(fileList.size(), fileList);
		photo.setQueenBee(new TakePhotoQueenBee(this));
	}

	@Override
	public void initCustomUI() {


		mCamera = getCameraInstance();
		mCameraPreview = new CameraPreview(this, mCamera);

		ShowPhotoFragment frag = new ShowPhotoFragment();
		frag.setCameraPreview(mCameraPreview);
		frag.setCamera(mCamera);
		this.loadFragment(frag);

	}

//	private void initPhotoTaking(){
//		mCamera = getCameraInstance();
//		mCameraPreview = new CameraPreview(this, mCamera);
//	}



	private File createImageFile() throws IOException {
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String imageFileName = "JPEG_" + timeStamp + "_";
		File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		File image = File.createTempFile(
				imageFileName,  /* prefix */
				".jpg",         /* suffix */
				storageDir      /* directory */
		);

		// Save a file: path for use with ACTION_VIEW intents
		currentPhotoPath = image.getAbsolutePath();
		return image;
	}

	/**
	 * Helper method to access the camera returns null if it cannot get the
	 * camera or does not exist
	 *
	 * @return
	 */
	private Camera getCameraInstance() {
		Camera camera = null;
		try {
			camera = Camera.open();
		} catch (Exception e) {
			// cannot get camera or does not exist
			e.printStackTrace();
		}
		return camera;
	}

//	Camera.PictureCallback mPicture = new Camera.PictureCallback() {
//		@Override
//		public void onPictureTaken(byte[] data, Camera camera) {
//			File pictureFile = getOutputMediaFile();
//			if (pictureFile == null) {
//				return;
//			}
//			try {
//				FileOutputStream fos = new FileOutputStream(pictureFile);
//				fos.write(data);
//				fos.close();
//			} catch (FileNotFoundException e) {
//
//			} catch (IOException e) {
//			}
//		}
//
//	};
//
//	private static File getOutputMediaFile() {
//		File mediaStorageDir = new File(
//				Environment
//						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
//				"MyCameraApp");
//		if (!mediaStorageDir.exists()) {
//			if (!mediaStorageDir.mkdirs()) {
//				Log.d("MyCameraApp", "failed to create directory");
//				return null;
//			}
//		}
//		// Create a media file name
//		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
//				.format(new Date());
//		File mediaFile;
//		mediaFile = new File(mediaStorageDir.getPath() + File.separator
//				+ "IMG_" + timeStamp + ".jpg");
//
//		return mediaFile;
//	}
	private void initFiles() {
		miscTime = System.currentTimeMillis();
		fileList = new ArrayList<AppInfo>();
		String folderSaved = "/storage/emulated/0/Android/data/tnefern.honeybeeframework/files/"+ TakePhotoConstants.SAVE_PHOTO_PATH;
		//String folderSaved = Environment.getExternalStorageDirectory().getAbsolutePath()
		//		+ "/" + FaceConstants.SAVE_PHOTO_PATH;
//		AssetManager assetManager = getAssets();
//		String folderSaved = FileFactory.getInstance().copyAssets2(assetManager, FaceConstants.SAVE_PHOTO_PATH,this);
//		String folderSaved = FileFactory.getInstance().copyAssetFiles(assetManager, FaceConstants.SAVE_PHOTO_PATH,this);//folderSaved: /storage/emulated/0/Android/data/tnefern.honeybeeframework/files/samplePicsforFaces240
//		String folderSaved = getExternalFilesDir(FaceConstants.SAVE_PHOTO_PATH).getAbsolutePath();
		Log.d(TAG,"folderSaved: "+folderSaved);
		TakePhotoResult.getInstance().setFileList(
				FileFactory.getInstance().listFiles(new File(folderSaved),
						new JpegFilter[] { new JpegFilter() }, 0));
//FileName:/storage/emulated/0/samplePicsforFaces240/2ppl copy 2.JPG

		for (File file : TakePhotoResult.getInstance().getFilesInFolder()) {
			fileList.add(new TakePhotoInfo(file.getAbsolutePath(), file.getName()));
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
		Intent deleIntent = new Intent(this, FinishedTakePhotoDelegatorActivity.class);
		this.startActivityForResult(deleIntent,
				TakePhotoConstants.FINISHED_DELEGATOR);

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
//		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
			Bundle extras = data.getExtras();
			Bitmap imageBitmap = (Bitmap) extras.get("data");
			Log.e(TAG,"activity PRE setImageBitmap");
			frag.setImageBitmap(imageBitmap);
		}
		Log.e(TAG,"activity setImageBitmap");
//        super.onActivityResult(requestCode, resultCode, data);
	}

	public void dispatchTakePictureIntent() {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
		startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
	}

}
