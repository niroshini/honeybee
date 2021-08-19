package tnefern.honeybeeframework.cloud;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class FileUploadResult {
    @SerializedName("status")
    @Expose
    public String status;
    @SerializedName("data")
    @Expose
    public Data data;
    @SerializedName("message")
    @Expose
    public String message;

    public class Data {

        @SerializedName("file_path")
        @Expose
        public String filePath;

    }
}
