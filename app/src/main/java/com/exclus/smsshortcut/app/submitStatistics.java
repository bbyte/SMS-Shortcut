package com.exclus.smsshortcut.app;

import android.os.AsyncTask;
import android.provider.Settings;
import android.util.Log;
import retrofit.RestAdapter;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

import java.util.Map;

/**
 * Created by bbyte on 5/22/14.
 */


public class submitStatistics extends AsyncTask<Map<String, String>, Void, String>
{

    interface postEvent
    {
        @POST("/activities")
        eventResult postJSON(@Body eventData body);

    }

    class eventResult
    {
        public String message;
    }

    class eventData
    {
        final String deviceId;
        final String event;

        eventData(String deviceId, String event)
        {
            this.deviceId = deviceId;
            this.event = event;
        }
    }

    @Override
    protected String doInBackground(Map<String, String>... params) {

        String URL = "http://192.168.1.65:8001";

        for (Map<String, String> param : params) {

            if (param.get("URL") != null)
                URL = param.get("URL");

            if (param.get)
        }

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint("http://192.168.1.65:8001")
                .build();

        postEvent service = restAdapter.create(postEvent.class);

        try {
            eventResult result = service.postJSON(new eventData(Settings.Secure.ANDROID_ID, "test2"));
        } catch (Exception e) {

            Log.e("NETWORK", e.toString());
        }

        return null;
    }

    @Override
    protected void onPostExecute(String result)
    {

    }

    @Override
    protected void execute(Map<String, String>... params)
    {

    }
}

