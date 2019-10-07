package tnefern.honeybeeframework.apps.facematch;

import java.io.File;
import java.util.ArrayList;

import android.content.Intent;
import android.os.Environment;
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
		FaceResult.getInstance().setFileList(
				FileFactory.getInstance().listFiles(new File(f),
						new JpegFilter[] { new JpegFilter() }, 0));
		for (File file : FaceResult.getInstance().getFilesInFolder()) {
			fileList.add(new FaceInfo(file.getAbsolutePath(), file.getName()));
		}
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
