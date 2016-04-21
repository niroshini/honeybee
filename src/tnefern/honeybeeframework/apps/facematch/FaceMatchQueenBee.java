package tnefern.honeybeeframework.apps.facematch;

import android.app.Activity;
import android.util.Log;
import tnefern.honeybeeframework.common.CommonConstants;
import tnefern.honeybeeframework.common.CompletedJob;
import tnefern.honeybeeframework.common.FileFactory;
import tnefern.honeybeeframework.common.Job;
import tnefern.honeybeeframework.common.JobInitializer;
import tnefern.honeybeeframework.common.JobPool;
import tnefern.honeybeeframework.delegator.QueenBee;
import tnefern.honeybeeframework.delegator.ResultFactory;

public class FaceMatchQueenBee extends QueenBee {
	private SearchImage imageSearch = null;

	public FaceMatchQueenBee(Activity pAct) {
		super(pAct);
		imageSearch = new SearchImage();
	}

	public void setStealMode() {
		JobInitializer.getInstance(getParentContext()).setStealMode(
				CommonConstants.READ_FILES_MODE);
		JobPool.getInstance().setStealMode(CommonConstants.READ_FILES_MODE);
	}

	@Override
	public CompletedJob doAppSpecificJob(Object pParam) {
		if (pParam != null) {
			if (pParam instanceof Job) {
				Job j = (Job) pParam;
				CompletedJob cj = null;
				String fileName = j.jobParams;
				
				if(j.status == CommonConstants.JOB_BEEN_STOLEN){
						// first get the image file name
						fileName = fileName.substring(
								fileName.lastIndexOf("/") + 1, fileName.length());
						j.id = fileName;
						Job newJob = JobPool.getInstance().isJobExistsinOriginal(j);
						fileName = newJob.jobParams;
				}
				String extension = FileFactory.getInstance().getFileExtension(
						fileName);
				if ((extension
						.equalsIgnoreCase(FaceConstants.FILE_EXTENSION_JPEG))
						|| (extension
								.equalsIgnoreCase(FaceConstants.FILE_EXTENSION_JPG))) {
					Integer res = Integer.valueOf(imageSearch.search(fileName));
					cj = new CompletedJob(CommonConstants.READ_STRING_MODE,
							FileFactory.getInstance().getFileNameFromFullPath(
									fileName), -1, null);
					cj.intValue = res.intValue();
					FaceResult.getInstance().addToMap(cj.stringValue,
							cj.intValue);
					JobPool.getInstance().incrementDoneJobCount();
					FaceResult.getInstance().incrementDeleDoneJobs();
					Log.d("FaceMatchQueenBee", cj.stringValue+" : "+cj.intValue);
				} else {
					Log.d("EXTENSION OTHER = ", extension);
				}
				return cj;
			}
		}
		return null;

	}

	@Override
	public ResultFactory getResultFactory() {
		return FaceResult.getInstance();
	}

}
