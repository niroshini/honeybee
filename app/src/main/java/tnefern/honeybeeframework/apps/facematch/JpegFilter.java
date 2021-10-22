package tnefern.honeybeeframework.apps.facematch;

import java.io.File;
import java.io.FilenameFilter;

public class JpegFilter implements FilenameFilter {

    @Override
    public boolean accept(File arg0, String name) {
        String lowerCaseName = name.toLowerCase();
        return (lowerCaseName.endsWith(FaceConstants.FILE_EXTENSION_JPG) || lowerCaseName.endsWith(FaceConstants.FILE_EXTENSION_JPEG));
    }

}
