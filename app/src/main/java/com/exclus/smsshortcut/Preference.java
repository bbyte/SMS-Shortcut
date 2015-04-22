package com.exclus.smsshortcut;

import java.util.List;

/**
 * Created by bbyte on 5/17/14.
 */
public class Preference {

    private int id;
    private String value;
    private String key;

    public Preference(){}

    public Preference(String key, String value)
    {
        super();
        this.key = key;
        this.value = value;
    }

    public int getId()
    {
        return this.id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getKey()
    {
        return this.key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public String getValue()
    {
        return this.value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }
}
