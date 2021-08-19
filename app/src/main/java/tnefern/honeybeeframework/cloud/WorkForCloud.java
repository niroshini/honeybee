package tnefern.honeybeeframework.cloud;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class WorkForCloud {
    public WorkForCloud(String method, String filePath) {
        this.method = method;
        this.filePath = filePath;
    }

    @SerializedName("method")
    @Expose
    public String method;
    @SerializedName("filePath")
    @Expose
    public String filePath;
}
