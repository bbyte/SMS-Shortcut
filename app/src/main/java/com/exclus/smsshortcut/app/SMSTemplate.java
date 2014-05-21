package com.exclus.smsshortcut.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by bbyte on 5/16/14.
 */
public class SMSTemplate {

    private int id;
    private String name;
    private String text;
    private List<SMSPhone> phones;

    public SMSTemplate(){}

    public SMSTemplate(String name, String text, List<SMSPhone> phones)
    {
        super();
        this.name = name;
        this.text = text;
        this.phones = phones;
    }

    public int getId()
    {
        return this.id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getText()
    {
        return this.text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public List<SMSPhone> getPhones()
    {
        return this.phones;
    }

    public void setPhones(List<SMSPhone> phones)
    {
        // TODO: implement real database save here!
        this.phones = phones;
    }

    public String getPhonesAsString()
    {
        return this.phones.toString();
    }

    @Override
    public String toString()
    {
        return this.getName();
    }

}
