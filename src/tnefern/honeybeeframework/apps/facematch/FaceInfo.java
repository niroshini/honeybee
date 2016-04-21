package tnefern.honeybeeframework.apps.facematch;

import tnefern.honeybeeframework.common.AppInfo;
import tnefern.honeybeeframework.common.CommonConstants;

public class FaceInfo extends AppInfo {

	/**
	 * 
	 * @param pMode denotes that a file will be stored
	 * @param pString file path
	 */
	public FaceInfo(String pString, String pId) {
		super(CommonConstants.FILE, pString, pId);
		// TODO Auto-generated constructor stub
	}

	public String toString() {
		return this.getStringInfo();
	}
}
