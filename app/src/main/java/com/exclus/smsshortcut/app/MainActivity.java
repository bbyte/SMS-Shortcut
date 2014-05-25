package com.exclus.smsshortcut.app;

import android.app.*;
import android.content.*;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.*;
import android.widget.*;
import retrofit.RestAdapter;
import retrofit.http.GET;
import retrofit.http.Path;

import java.util.*;

public class MainActivity extends Activity {

    public static final String SMS_CONFIRMATION = "confirmation_settings";
    public static final String ALREADY_INSTALLED = "installed";

    private ListView templatesListView;
    private List<SMSTemplate> smsTemplates;
    private ArrayAdapter templatesListAdapter;

    private List<Map<String, String>> emptySMSTemplates;
    private SimpleAdapter emptySMSTemplatesAdapter;

    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = DatabaseHelper.getHelper(getApplicationContext());

        db.setPreference(ALREADY_INSTALLED, "false");

        Global.getInstance().androidId = Settings.Secure.getString(this.getContentResolver(),Settings.Secure.ANDROID_ID);

        HashMap<String, String> mp = new HashMap<String, String>();

        mp.put("event", "STARTED");

        new submitStatistics().execute(mp);

        templatesListView = (ListView) findViewById(R.id.templatesListView);

        new getContacts(this).execute();



        // TODO: need to find better way to display help on the screen

        emptySMSTemplates = new ArrayList<Map<String, String>>();
        emptySMSTemplates.add(new HashMap<String, String>(){{put("text", "No templates are created");}});
        emptySMSTemplates.add(new HashMap<String, String>(){{put("text", "To create a template, click on");}});
        emptySMSTemplates.add(new HashMap<String, String>(){{put("text", "\"Add template\" button");}});

        emptySMSTemplatesAdapter = new SimpleAdapter(this, emptySMSTemplates, R.layout.itallic_list, new String[]{"text"}, new int[]{android.R.id.text1});

        smsTemplates = db.getAllSMSTemplates();

        templatesListAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, smsTemplates);

        if (smsTemplates.isEmpty()) {

            templatesListView.setAdapter(emptySMSTemplatesAdapter);
        } else {

            templatesListView.setAdapter(templatesListAdapter);
        }

        templatesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                if (smsTemplates.isEmpty()) {

                    templatesListView.setAdapter(emptySMSTemplatesAdapter);

                    return;
                }

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(smsTemplates.get(position).getName())
                        .setMessage(smsTemplates.get(position).getPhonesAsString())
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // continue with delete

                                deleteShortCut(smsTemplates.get(position).toString());

                                db.deleteSMSTemplate(smsTemplates.get(position).getId());

                                smsTemplates.remove(position);

                                templatesListAdapter.notifyDataSetChanged();
                                Toast.makeText(MainActivity.this, "SMS template was deleted", Toast.LENGTH_LONG).show();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show();
            }
        });

        CheckBox confirmation = (CheckBox) findViewById(R.id.confirmationCheckbox);

        if (! db.getPreferenceBooleanValue(ALREADY_INSTALLED))
            firstRun();

        Boolean conf = db.getPreferenceBooleanValue(SMS_CONFIRMATION);

        if (conf) {

            confirmation.setChecked(true);
        } else {

            confirmation.setChecked(false);
        }

        catchShortcut(getIntent());
    }

    @Override
    protected void onNewIntent(Intent in)
    {
        super.onNewIntent(in);

        new getContacts(this).execute();

        catchShortcut(in);
    }

    private void catchShortcut(Intent in)
    {
        final SMSTemplate smsTemplate;

        if (in.hasExtra("templateName")) {

            HashMap<String, String> mp = new HashMap<String, String>();
            mp.put("table", "activities");
            mp.put("event", "SENDING_SMS");
            new submitStatistics().execute(mp);

            db = DatabaseHelper.getHelper(this);

            smsTemplate = db.getSMSTemplateByName(in.getStringExtra("templateName"));

            if (db.getPreferenceBooleanValue(SMS_CONFIRMATION)) {

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(smsTemplate.getText())
                        .setMessage(smsTemplate.getPhonesAsString())
                        .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // continue with delete

                                sendSMSFromTemplateAndExit(smsTemplate);

                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing

                                Toast.makeText(getApplicationContext(), "SMS canceled", Toast.LENGTH_LONG).show();
                                exitFromApp();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            } else {

                sendSMSFromTemplateAndExit(smsTemplate);
            }
        }
    }

    private void sendSMSFromTemplateAndExit(SMSTemplate smsTemplate)
    {
        for (SMSPhone phone : smsTemplate.getPhones()) {

            sendSMS(phone.getPhoneNumber(), smsTemplate.getText());
        }

        Toast.makeText(getApplicationContext(), "Sending \"" + smsTemplate.getText() + "\" to " + smsTemplate.getPhonesAsString(),
                Toast.LENGTH_LONG).show();

        exitFromApp();
    }

    private void exitFromApp()
    {
        // exit from app, but w/o killing it
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory( Intent.CATEGORY_HOME );
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);
    }

    private void sendSMS(String phoneNumber, String message)
    {
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        ArrayList<PendingIntent> sentPendingIntents = new ArrayList<PendingIntent>();
        ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<PendingIntent>();

        sentPendingIntents.add(PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0));
        deliveredPendingIntents.add(PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0));

        // when the SMS has been sent
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS sent", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(SENT));

        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "SMS not delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();
//        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);

        // convert message to multipart

        ArrayList<String> parts = sms.divideMessage(message);

        sms.sendMultipartTextMessage(phoneNumber, null, parts, sentPendingIntents, deliveredPendingIntents);
    }


    // Button functions

    public void addTemplateButtonClicked(View view)
    {
        Intent addActivityIntent = new Intent(this, AddActivity.class);
        startActivity(addActivityIntent);
    }

    public void aboutButtonClicked(View view)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();

        HashMap<String, String> mp = new HashMap<String, String>();
        mp.put("table", "activities");
        mp.put("event", "ABOUT_CLICKED");
        new submitStatistics().execute(mp);

        View dialogView = inflater.inflate(R.layout.dialog_about, null);


        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_about, null))
                // Add action buttons
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // sign in the user ...
                    }
                })
                .create().show();
    }

    public void helpButtonClicked(View view)
    {

        HashMap<String, String> mp = new HashMap<String, String>();
        mp.put("table", "activities");
        mp.put("event", "HELP_CLICKED");
        new submitStatistics().execute(mp);
    }

    public void reinstallShortcutsButtonClicked(View view)
    {
        HashMap<String, String> mp = new HashMap<String, String>();
        mp.put("table", "activities");
        mp.put("event", "REINSTALL_SHORTCUT_CLICKED");

        new submitStatistics().execute(mp);
        for (SMSTemplate smsTemplate : db.getAllSMSTemplates()) {

            Shortcut shortcut = new Shortcut(this, smsTemplate.getName());
            shortcut.create();
        }

        Toast.makeText(this, "All shortcuts are restored on home screen", Toast.LENGTH_LONG).show();
    }

    public void confirmationClicked(View view)
    {
        CheckBox checkbox = (CheckBox) view;

        if (checkbox.isChecked())
            db.setPreference(SMS_CONFIRMATION, "true");
        else
            db.setPreference(SMS_CONFIRMATION, "false");
    }

    @Override
    public void onRestart()
    {
        super.onRestart();

        HashMap<String, String> mp = new HashMap<String, String>();
        mp.put("table", "activities");
        mp.put("event", "MAIN");
        new submitStatistics().execute(mp);

//        smsTemplates = db.getAllSMSTemplates();

        // strange but token from here
        // http://stackoverflow.com/questions/16219732/refreshing-arrayadapter-onresume-notifydatasetchanged-not-working

        smsTemplates.clear();
        smsTemplates.addAll(db.getAllSMSTemplates());

        if (smsTemplates.isEmpty()) {

            templatesListView.setAdapter(emptySMSTemplatesAdapter);
        } else {

            templatesListView.setAdapter(templatesListAdapter);
            templatesListAdapter.notifyDataSetChanged();
        }

        Log.e("onRestart", smsTemplates.toString());
    }

    private void deleteShortCut(String templateName)
    {


        Shortcut shortcut = new Shortcut(this, templateName);
        shortcut.remove();

        HashMap<String, String> mp = new HashMap<String, String>();
        mp.put("table", "activities");
        mp.put("event", "SHORTCUT_DELETE");
        new submitStatistics().execute(mp);
    }

    private void firstRun()
    {
        db.setPreference(ALREADY_INSTALLED, "true");
        db.setPreference(SMS_CONFIRMATION, "true");

        HashMap<String, String> mp = new HashMap<String, String>();

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        mp.put("table", "devices");
        mp.put("screenWidth", Integer.toString(size.x));
        mp.put("screenHeight", Integer.toString(size.y));

        new submitStatistics().execute(mp);

        HashMap<String, String> event = new HashMap<String, String>();
        event.put("table", "activites");
        event.put("event", "SETUP");

        new submitStatistics().execute(event);
    }

    public void donateButtonClicked(View view)
    {
        Uri uri = Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=8698JGH7LAJFW");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }
}
