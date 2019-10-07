package tnefern.honeybeeframework.apps.facematch;

import java.util.ArrayList;
import java.util.Iterator;

import tnefern.honeybeeframework.common.AppInfo;
import tnefern.honeybeeframework.common.CommonConstants;
import tnefern.honeybeeframework.delegator.AppRequest;
import tnefern.honeybeeframework.delegator.QueenBee;


public class FaceRequest implements AppRequest {
	private int numberOfPhotos = -1;
	private ArrayList<AppInfo> fileList = null;
	private int mode = -1;
	private QueenBee faceQueen = null;

	public FaceRequest(int pJobs, ArrayList<AppInfo> pList) {
		this.numberOfPhotos = pJobs;
		this.fileList = pList;
		this.mode = CommonConstants.READ_FILES_MODE;
	}

	@Override
	public String[] getDistributedString() {
		String[] strArr = new String[numberOfPhotos];
		int k = 0;

		for (int i = 0; i < this.fileList.size(); i++) {
			StringBuffer sBuf = new StringBuffer();
			sBuf.append(this.fileList.get(i).getStringInfo());
			sBuf.append(CommonConstants.APP_REQUEST_SEPERATOR);
			sBuf.append(numberOfPhotos);
			sBuf.append(CommonConstants.APP_REQUEST_SEPERATOR);
			sBuf.append(k);
			strArr[k] = sBuf.toString();
			k++;
		}

		return strArr;
	}

	@Override
	public int getNumberOfJobs() {
		return numberOfPhotos;
	}

	@Override
	public ArrayList<AppInfo> getAppInfo() {
		return fileList;
	}

//	@Override
	public String getAppInfoString() {
		StringBuffer str = new StringBuffer();
		if (fileList != null) {
			Iterator<AppInfo> iter = fileList.iterator();
			while (iter.hasNext()) {
				str.append(iter.next().toString());
				str.append(CommonConstants.APP_REQUEST_SEPERATOR);
			}
		}
		return str.substring(0, str.length() - 1);
	}

	@Override
	public int getMode() {
		return mode;
	}

	@Override
	public QueenBee getQueenBee() {
		return this.faceQueen;
	}

	@Override
	public void setQueenBee(QueenBee pBee) {
		this.faceQueen = pBee;
		
	}

	/**
	 * This is only used when Delegator steals from Workers. Because the
	 * delegator already has all the data, there is no point in workers sending
	 * the files again as jobs. Therefore, workers only need to specify which
	 * files in string mode.
	 */
//	@Override
//	public int getStealMode() {
//		// TODO Auto-generated method stub
//		return CommonConstants.READ_STRING_MODE;
//	}

}
