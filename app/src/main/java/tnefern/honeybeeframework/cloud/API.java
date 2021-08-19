package tnefern.honeybeeframework.cloud;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface API {
    @Multipart
    @POST("upload/")
    Call<FileUploadResult> uploadFile(@Part MultipartBody.Part file);
}
