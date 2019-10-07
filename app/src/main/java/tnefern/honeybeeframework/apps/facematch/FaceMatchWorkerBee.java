package tnefern.honeybeeframework.apps.facematch;

import android.content.Context;
import android.util.Log;
import tnefern.honeybeeframework.common.CommonConstants;
import tnefern.honeybeeframework.common.CompletedJob;
import tnefern.honeybeeframework.common.FileFactory;
import tnefern.honeybeeframework.common.JobParams;
import tnefern.honeybeeframework.worker.WorkerBee;

public class FaceMatchWorkerBee extends WorkerBee {
	private SearchImage imageSearch = new SearchImage();
	public FaceMatchWorkerBee(Context pAct, String pActivityClass,
			JobParams pMsg, int pIndex, boolean stolen) {
		super(pAct, pActivityClass, pMsg, pIndex, stolen);
	}
	@Override
	public CompletedJob doAppSpecificJob(Object pParam) {
		if (pParam != null) {
			if (pParam instanceof String) {
				CompletedJob cj = null;
				String pS = (String) pParam;
				
				String extension = FileFactory.getInstance().getFileExtension(pS);
				if ((extension.equalsIgnoreCase(FaceConstants.FILE_EXTENSION_JPEG))
						|| (extension
								.equalsIgnoreCase(FaceConstants.FILE_EXTENSION_JPG))) {
					Integer res = Integer.valueOf(imageSearch.search(pS));
					cj = new CompletedJob(
							CommonConstants.READ_STRING_MODE, FileFactory.getInstance()
									.getFileNameFromFullPath(pS), -1, null);
					cj.intValue = res.intValue();
					FaceResult.getInstance().addToMap(cj.stringValue, cj.intValue);
					
					
					return cj;
				} else {
					Log.d("EXTENSION OTHER = ", extension);
				}

			}
		}
		return null;
	}

}
