package com.exclus.smsshortcut;

/**
 * Created by bbyte on 5/17/14.
 */
public class SMSPhone {
    private int id;
    private long SMSTemplateId;
    private String phoneNumber;
    private String name;

    public SMSPhone(){}

    public SMSPhone(String phoneNumber, String name, int SMSTemplateId)
    {
        super();
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.SMSTemplateId = SMSTemplateId;
    }

    public int getId()
    {
        return this.id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public long getSMSTemplateId()
    {
        return this.SMSTemplateId;
    }

    public void setSMSTemplateId(long smsTemplateId)
    {
        this.SMSTemplateId = smsTemplateId;
    }

    public String getPhoneNumber()
    {
        return this.phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber)
    {
        this.phoneNumber = phoneNumber;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return this.getPhoneNumber() + "(" + (this.getName() != null ? this.getName() : "") + ")";
    }
}
