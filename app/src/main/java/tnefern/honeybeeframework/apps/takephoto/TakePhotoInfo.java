package tnefern.honeybeeframework.apps.takephoto;

import tnefern.honeybeeframework.common.AppInfo;
import tnefern.honeybeeframework.common.CommonConstants;

public class TakePhotoInfo extends AppInfo {

	/**
	 * 
	 * @param pMode denotes that a file will be stored
	 * @param pString file path
	 */
	public TakePhotoInfo(String pString, String pId) {
		super(CommonConstants.FILE, pString, pId);
		// TODO Auto-generated constructor stub
	}

	public String toString() {
		return this.getStringInfo();
	}
}
