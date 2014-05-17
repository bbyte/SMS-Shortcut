package com.exclus.smsshortcut.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bbyte on 5/16/14.
 */
public class DatabaseHelper extends SQLiteOpenHelper{

    // Logcat tag
    private static final String LOG = "DatabaseHelper";



    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "SMSTemplatesDB";

    private static final String TABLE_SMSTEMPLATES = "SMSTemplates";
    private static final String TABLE_SMSPHONES = "SMSPhones";

    private static final String TABLE_PREFERENCES = "Preferences";

    // Common column names
    private static final String KEY_ID = "id";
    private static final String KEY_CREATED_AT = "created_at";

    private static final String KEY_NAME = "name";
    private static final String KEY_TEXT = "smsText";
    private static final String KEY_TEMPLATEID = "template_id";
    private static final String KEY_PHONE = "phone";

    private static final String KEY_KEY = "key";
    private static final String KEY_VALUE = "value";

    private static final String CREATE_SMSTEMPLATES_TABLE = "CREATE TABLE " + TABLE_SMSTEMPLATES + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + KEY_NAME + " TEXT NOT NULL UNIQUE, "
            + KEY_TEXT + " TEXT)";
    private static final String CREATE_SMSPHONES_TABLE = "CREATE TABLE " + TABLE_SMSPHONES + "("
            + KEY_ID + " INTEGER PRIMARY KEY, "
            + KEY_PHONE + " TEXT NOT NULL, "
            + KEY_NAME + " TEXT"
            + KEY_TEMPLATEID + " INTEGER NO NULL)";

    private static final String CREATE_PREFENCES_TABLE = "CREATE TABLE " + TABLE_PREFERENCES + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + KEY_KEY + " TEXT NOT NULL UNIQUE, "
            + KEY_VALUE + " TEXT)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(CREATE_SMSTEMPLATES_TABLE);
        db.execSQL(CREATE_SMSPHONES_TABLE);
        db.execSQL(CREATE_PREFENCES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SMSTEMPLATES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SMSPHONES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PREFERENCES);

        onCreate(db);
    }

    public long createSMSTemplate(SMSTemplate smsTemplate, List<SMSPhone> phones)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, smsTemplate.getName());
        values.put(KEY_TEXT, smsTemplate.getText());

        long smsTemplateId = db.insert(TABLE_SMSTEMPLATES,null,values);

        for (SMSPhone smsPhone : phones) {
            smsPhone.setSMSTemplateId(smsTemplateId);
            long phoneId = createPhone(smsPhone);
        }

        return smsTemplateId;
    }

    public long createPhone(SMSPhone phone)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_TEMPLATEID, phone.getSMSTemplateId());
        values.put(KEY_NAME, phone.getName());
        values.put(KEY_PHONE, phone.getPhoneNumber());

        long phoneId = db.insert(TABLE_SMSPHONES, null, values);

        return phoneId;
    }

    public long createPreference(Preference preference)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_KEY, preference.getKey());
        values.put(KEY_VALUE, preference.getValue());

        long preferenceId = db.insert(TABLE_PREFERENCES, null, values);

        return preferenceId;
    }

    public SMSTemplate getSMSTemplate(long smsTemplateId)
    {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT * FROM " + TABLE_SMSTEMPLATES + "WHERE " + KEY_ID + " = " + smsTemplateId;

        Log.e(LOG, selectQuery);

        Cursor c = db.rawQuery(selectQuery,null);

        if (c != null)
            c.moveToFirst();

        SMSTemplate smsTemplate = new SMSTemplate();
        smsTemplate.setId(c.getInt(c.getColumnIndex(KEY_ID)));
        smsTemplate.setName(c.getString(c.getColumnIndex(KEY_NAME)));
        smsTemplate.setText(c.getString(c.getColumnIndex(KEY_TEXT)));

        smsTemplate.setPhones(getAllSMSPhonesByTemplateId(smsTemplateId));

        return smsTemplate;
    }

    public List<SMSTemplate> getAllSMSTemplates()
    {
        List<SMSTemplate> smsTemplates = new ArrayList<SMSTemplate>();

        String selectQuery = "SELECT * FROM " + TABLE_SMSTEMPLATES;

        Log.e(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery,null);

        if (c.moveToFirst()) {
            do {
                SMSTemplate smsTemplate = new SMSTemplate();
                smsTemplate.setId(c.getInt(c.getColumnIndex(KEY_ID)));
                smsTemplate.setName(c.getString(c.getColumnIndex(KEY_NAME)));
                smsTemplate.setText(c.getString(c.getColumnIndex(KEY_TEXT)));

                // TODO: Implement phones fetching!

                smsTemplate.setPhones(getAllSMSPhonesByTemplateId(smsTemplate.getId()));

                smsTemplates.add(smsTemplate);
            } while (c.moveToNext());
        }

        return smsTemplates;
    }

    public SMSTemplate getSMSTemplateByName(String name)
    {
        SMSTemplate smsTemplate = new SMSTemplate();

        String selectQuery = "SELECT * FROM " + TABLE_SMSTEMPLATES + " WHERE " + KEY_NAME + " = " + name + " LIMIT 1";

        Log.e(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        if (c.moveToFirst()) {
            smsTemplate.setId(c.getInt(c.getColumnIndex(KEY_ID)));
            smsTemplate.setName(c.getString(c.getColumnIndex(KEY_NAME)));
            smsTemplate.setText(c.getString(c.getColumnIndex(KEY_TEXT)));

            // TODO: Implement phones fetching!

            smsTemplate.setPhones(getAllSMSPhonesByTemplateId(smsTemplate.getId()));
        }
        return smsTemplate;
    }

    public List<SMSPhone> getAllSMSPhonesByTemplateId(long smsTemplateId)
    {
        List<SMSPhone> phones = new ArrayList<SMSPhone>();

        String selectQuery = "SELECT * FROM " + TABLE_SMSPHONES + " WHERE " + KEY_TEMPLATEID + " = " + smsTemplateId;

        Log.e(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        if (c.moveToFirst()) {
            do {
                SMSPhone smsPhone = new SMSPhone();
                smsPhone.setId(c.getInt(c.getColumnIndex(KEY_ID)));
                smsPhone.setPhoneNumber(c.getString(c.getColumnIndex(KEY_PHONE)));
                smsPhone.setName(c.getString(c.getColumnIndex(KEY_NAME)));
                smsPhone.setSMSTemplateId(smsTemplateId);
            } while (c.moveToNext());
        }

        return phones;
    }

    public List<Preference> getAllPreferences()
    {
        List<Preference> preferences = new ArrayList<Preference>();

        String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;

        Log.e(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery,null);

        if (c.moveToFirst()) {
            do {

                Preference preference = new Preference();
                preference.setId(c.getInt(c.getColumnIndex(KEY_ID)));
                preference.setKey(c.getString(c.getColumnIndex(KEY_KEY)));
                preference.setValue(c.getString(c.getColumnIndex(KEY_VALUE)));

                preferences.add(preference);
            } while (c.moveToNext());
        }

        return preferences;
    }

    public Preference getPreference(String preferenceKey)
    {
        String preferenceValue = "";
        String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES + " WHERE " + KEY_KEY + " = " + preferenceKey;

        Log.e(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        Preference preference = new Preference();

        if (c.moveToFirst()) {

            preference.setId(c.getInt(c.getColumnIndex(KEY_ID)));
            preference.setKey(c.getString(c.getColumnIndex(KEY_KEY)));
            preference.setValue(c.getString(c.getColumnIndex(KEY_VALUE)));
        }

        return preference;
    }


    public String getPreferenceValue(String preferenceKey)
    {
        String preferenceValue = "";
        String selectQuery = "SELECT value FROM " + TABLE_PREFERENCES + " WHERE " + KEY_KEY + " = " + preferenceKey;

        Log.e(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        if (c.moveToFirst()) {
            preferenceValue = c.getString(c.getColumnIndex(KEY_VALUE));
        }

        return preferenceValue;
    }

    public void setPreference(Preference preference)
    {
        if (isExistsPreference(preference.getKey())) {
            updatePreference(preference);
        } else {
            createPreference(preference);
        }
    }

    public int updatePreference(Preference preference)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
//        values.put(KEY_KEY, preferenceKey);
        values.put(KEY_VALUE, preference.getValue());

        return db.update(TABLE_PREFERENCES, values, KEY_KEY + " = ?", new String[]{ String.valueOf(preference.getKey())});

    }

    public Boolean isExistsPreference(String preferenceKey)
    {
        Preference prefence = getPreference(preferenceKey);

        if (prefence.getId() > 0)
            return true;
        else
            return false;

        /*
        String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES + " WHERE " + KEY_KEY + " = " + preferenceKey;

        Log.e(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        if (c != null)
            return true;
        else
            return false;
            */
    }

    public int updateSMSTemplate(SMSTemplate smsTemplate)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, smsTemplate.getName());
        values.put(KEY_TEXT, smsTemplate.getText());

        // as we don't need to check the phones, we just delete them

        deletePhonesByTemplateId(smsTemplate.getId());

        // then create them from zero

        for (SMSPhone phone : smsTemplate.getPhones()) {

            createPhone(phone);
        }

        return db.update(TABLE_SMSTEMPLATES, values, KEY_ID + " = ?", new String[] { String.valueOf(smsTemplate.getId())});
    }

    // very unusable

    public int updateSMSPhone(SMSPhone phone)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, phone.getName());
        values.put(KEY_PHONE, phone.getPhoneNumber());

        return db.update(TABLE_SMSPHONES, values, KEY_ID + " = ?", new String[] { String.valueOf(phone.getId())});
    }

    public void deleteSMSTemplate(long smsTemplateId)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        // first delete phones

        deletePhonesByTemplateId(smsTemplateId);

        // second the template

        db.delete(TABLE_SMSTEMPLATES, KEY_ID + " = ?", new String[] { String.valueOf(smsTemplateId)});
    }

    public void deletePhonesByTemplateId(long smsTemplateId)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(TABLE_SMSPHONES, KEY_TEMPLATEID + " = ?", new String[] { String.valueOf(smsTemplateId)});
    }

    public void deletePhone(long phoneId)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(TABLE_SMSPHONES, KEY_ID + " = ?", new String[] { String.valueOf(phoneId)});
    }
}
