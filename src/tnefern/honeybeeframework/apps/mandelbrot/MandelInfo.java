package tnefern.honeybeeframework.apps.mandelbrot;

import tnefern.honeybeeframework.common.AppInfo;
import tnefern.honeybeeframework.common.CommonConstants;

public class MandelInfo extends AppInfo {

	public MandelInfo(String pString, String pId) {
		super(CommonConstants.READ_STRING_MODE, pString, pId);
	}

	public String toString() {
		return this.getStringInfo();
	}
}
