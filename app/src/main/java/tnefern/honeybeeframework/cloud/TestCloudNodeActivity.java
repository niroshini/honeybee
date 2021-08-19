package tnefern.honeybeeframework.cloud;

import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import android.util.Log;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import tnefern.honeybeeframework.R;

public class TestCloudNodeActivity extends AppCompatActivity {

    private static final String CLOUD_URL = "http://10.0.0.53:3000/";
    private static final String TAG = "Test Cloud";

    private LinearLayoutCompat testCloudParentLayout;

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket(CLOUD_URL);
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
            Log.e(TAG, "could not connect to cloud due to " + ex.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_cloud_node);
        testCloudParentLayout = findViewById(R.id.test_cloud_parent_layout);

        mSocket.on(Socket.EVENT_CONNECT, onConnected);
        mSocket.on(Socket.EVENT_DISCONNECT, onDisconnected);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectionError);
        mSocket.on("results", onResultReceived);
        addTextView("Connecting to the cloud ...");
        mSocket.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mSocket.disconnect();
        mSocket.off(Socket.EVENT_CONNECT, onConnected);
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnected);
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectionError);
        mSocket.off("results", onResultReceived);
    }

    private void addTextView(String text) {
        LayoutParams lparams = new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        TextView tv=new TextView(this);
        tv.setLayoutParams(lparams);
        tv.setText(text);
        testCloudParentLayout.addView(tv);
    }

    private final Emitter.Listener onConnected = args -> runOnUiThread(() -> {
        addTextView("Connected to the cloud...");
        addTextView("Uploading file");
        if (mSocket.connected()) {
            File externalStorage = ContextCompat.getExternalFilesDirs(getApplicationContext(), null)[0];
            File fileToUpload = new File(externalStorage, "work/work.zip");

            RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-file"), fileToUpload);
            MultipartBody.Part fileMultipart = MultipartBody.Part.createFormData("file", fileToUpload.getName(), requestBody);
            API api = RetrofitClient.getInstance().getAPI();
            Call<FileUploadResult> uploadCall = api.uploadFile(fileMultipart);
            uploadCall.enqueue(new Callback<FileUploadResult>() {
                @Override
                public void onResponse(Call<FileUploadResult> call, Response<FileUploadResult> response) {
                    if (response.isSuccessful()) {
                        addTextView("File upload successful");
                        String uploadedFilePath = response.body().data.filePath;
//                        mSocket.emit("work to do", "{\"method\":\"add\", \"numbers\":[1,2]}");

                        mSocket.emit("work to do", new Gson().toJson(new WorkForCloud("faceDetect", uploadedFilePath)));
                    } else {
                        addTextView("File upload error: " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<FileUploadResult> call, Throwable t) {
                    t.printStackTrace();
                    addTextView("Error while uploading file : " + t.getMessage());
                }
            });
        }
    });

    private final Emitter.Listener onDisconnected = args -> runOnUiThread(() -> {
        addTextView("Disconnected");
    });

    private final Emitter.Listener onConnectionError = args -> runOnUiThread(() -> {
        addTextView("Could not connect to the cloud...");
    });

    private final Emitter.Listener onResultReceived = args -> runOnUiThread(() -> {
        Log.d(TAG, "Result received");
        JSONObject data = (JSONObject) args[0];
        String result;
        try {
            result = data.getString("result");
            addTextView(result);
        } catch (JSONException ex) {
            ex.printStackTrace();
            Log.e(TAG, "Error in getting result duet to " + ex.getMessage());
        }
    });
}