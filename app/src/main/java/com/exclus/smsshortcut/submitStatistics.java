package com.exclus.smsshortcut;

import android.content.Context;
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
//        @POST("/activities")
        @POST("/{table}")
        eventResult postJSON(@Path("table") String table, @Body eventData body);

        @POST("/{table}")
        eventResult postJSON(@Path("table") String table, @Body deviceData body);
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

    class deviceData
    {
        final String deviceId;
        final String type;
        final String version;
        final String screenWidth;
        final String screenHeight;

        deviceData(String deviceId, String type, String version, String screenWidth, String screenHeight)
        {
            this.deviceId = deviceId;
            this.type = type;
            this.version = version;
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
        }
    }

    @Override
    protected String doInBackground(Map<String, String>... params) {

        String URL = "http://smsshortcut.exclus.org";
        String event = "DEFAULT_EVENT";
        String table = "activities";
        String deviceType = Global.getInstance().getDeviceName(), screenWidth = "", screenHeight = "";
        Integer osVersion = android.os.Build.VERSION.SDK_INT;

        for (Map<String, String> param : params) {

            if (param.get("URL") != null)
                URL = param.get("URL");

            if (param.get("event") != null)
                event = param.get("event");

            if (param.get("table") != null)
                table = param.get("table");

            if (param.get("screenWidth") != null)
                screenWidth = param.get("screenWidth");

            if (param.get("screenHeight") != null)
                screenHeight = param.get("screenHeight");
        }

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.NONE)
                .setEndpoint(URL)
                .build();

        postEvent service = restAdapter.create(postEvent.class);

        try {
            eventResult result;
            if (table.equals("activities")) {
                result = service.postJSON(table, new eventData(Global.getInstance().androidId, event));
            } else if (table.equals("devices")) {

                result = service.postJSON(table, new deviceData(Global.getInstance().androidId, deviceType, Integer.toString(osVersion), screenHeight, screenWidth));
            }
        } catch (Exception e) {

            Log.e("NETWORK", e.toString());
        }

        return null;
    }

    @Override
    protected void onPostExecute(String result)
    {

    }

//    @Override
//    protected void execute(Map<String, String>... params)
//    {
//
//    }
}

