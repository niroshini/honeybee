package tnefern.honeybeeframework.cloud;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private String BASE_URL = "http://%s/api/";
    private static Map<String,RetrofitClient> mInstances = new HashMap<>();
    private Retrofit retrofit;

    private RetrofitClient(String ipAndPort) {
        retrofit = new Retrofit.Builder()
                .baseUrl(String.format(Locale.ENGLISH, BASE_URL, ipAndPort))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static synchronized RetrofitClient getInstance(String ipAndPort) {
        if (!mInstances.containsKey(ipAndPort)) {
            mInstances.put(ipAndPort, new RetrofitClient(ipAndPort));
        }
        return mInstances.get(ipAndPort);
    }

    public API getAPI() {
        return retrofit.create(API.class);
    }
}
