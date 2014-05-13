package com.exclus.smsshortcut.app;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by bbyte on 5/7/14.
 */
public class Global
{
    ArrayList<Map<String, String>> mPeopleList;
    Boolean loadingContacts = false;

    private static class Holder
    {
        static final Global INSTANCE = new Global();
    }

    public static Global getInstance()
    {
        return Holder.INSTANCE;
    }
}
