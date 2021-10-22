package tnefern.honeybeeframework.apps.facematch;

import java.io.File;
import java.io.FilenameFilter;

public class PngFilter implements FilenameFilter {

    @Override
    public boolean accept(File arg0, String name) {
        return (name.toLowerCase().endsWith(FaceConstants.FILE_EXTENSION_PNG));
    }
}
