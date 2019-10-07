package tnefern.honeybeeframework.apps.facematch;

import java.io.File;
import java.io.FilenameFilter;

public class JpegFilter implements FilenameFilter{

	@Override
	public boolean accept(File arg0, String name) {
		  return ( name.endsWith(".jpg")|| name.endsWith(".JPG"));
	}

}
