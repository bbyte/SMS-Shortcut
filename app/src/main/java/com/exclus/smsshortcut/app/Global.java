package com.exclus.smsshortcut.app;

import android.os.Build;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by bbyte on 5/7/14.
 */
public class Global
{
    ArrayList<Map<String, String>> mPeopleList;
    Boolean loadingContacts = false;
    String androidId;

    private static class Holder
    {
        static final Global INSTANCE = new Global();
    }

    public static Global getInstance()
    {
        return Holder.INSTANCE;
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }
}
