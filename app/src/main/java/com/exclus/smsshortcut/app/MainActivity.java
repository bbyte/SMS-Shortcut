package com.exclus.smsshortcut.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.*;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import java.util.*;

public class MainActivity extends Activity {

    public static final String SMS_CONFIRMATION = "confirmation_settings";
//    private ArrayList<Map<String, String>> mPeopleList;
    SharedPreferences prefs = null;
    Map<String, ?> templatesList;
    ListView templatesListView;
    List<Map<String, String>> templatesNames;
    SimpleAdapter templatesListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        templatesListView = (ListView) findViewById(R.id.templatesListView);
        templatesNames = new ArrayList<Map<String, String>>();

        new getContacts(this).execute();

//        mPeopleList = new ArrayList<Map<String, String>>();

        prefs = getSharedPreferences(this.getPackageName(), MODE_PRIVATE);

        getTemplateNames();

        templatesListAdapter = new SimpleAdapter(this, templatesNames, android.R.layout.simple_list_item_1, new String[]{"name"}, new int[]{android.R.id.text1});
        templatesListView.setAdapter(templatesListAdapter);

        templatesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete entry")
                        .setMessage("Are you sure you want to delete this entry?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // continue with delete
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });

        CheckBox confirmation = (CheckBox) findViewById(R.id.confirmationCheckbox);

        if (prefs.getBoolean(SMS_CONFIRMATION, true)) {

            confirmation.setChecked(true);
        } else {

            confirmation.setChecked(false);
        }

        catchShortcut(getIntent());
    }

    private void getTemplateNames()
    {
        templatesList = prefs.getAll();

        for (Map.Entry<String, ?> templateEntry : templatesList.entrySet()) {

            if (templateEntry.getKey().contentEquals(SMS_CONFIRMATION))
                continue;

            HashMap<String, String> tmp = new HashMap<String, String>();
            tmp.put("name", templateEntry.getKey());
            templatesNames.add(tmp);
        }
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
        final Set<String> lPhonesList;

        if (in.hasExtra("message") && in.hasExtra("templateName")) {


            final String message = in.getStringExtra("message");
            String templateName = in.getStringExtra("templateName");

            lPhonesList = prefs.getStringSet(templateName, new HashSet<String>());

            prefs = getSharedPreferences(this.getPackageName(), MODE_PRIVATE);

            if (prefs.getBoolean(SMS_CONFIRMATION, true)) {

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(message)
                        .setMessage(lPhonesList.toString())
                        .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // continue with delete

                                for (String phone : lPhonesList) {

                                    sendSMS(phone, message);
                                }


                                Toast.makeText(getApplicationContext(), "Sending \"" + message + "\" to " + lPhonesList.toString(),
                                        Toast.LENGTH_LONG).show();

                                exitFromApp();

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

                for (String phone : lPhonesList) {

                    sendSMS(phone, message);
                }

                Toast.makeText(getApplicationContext(), "Sending \"" + message + "\" to " + lPhonesList.toString(),
                        Toast.LENGTH_LONG).show();

                exitFromApp();
            }
        }
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

//        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
//                new Intent(SENT), 0);
//        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
//                new Intent(DELIVERED), 0);

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


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void addPhoneButtonClicked(View view)
    {
        Intent addActivityIntent = new Intent(this, AddActivity.class);
        startActivity(addActivityIntent);
    }

    public void confirmationClicked(View view)
    {
        CheckBox checkbox = (CheckBox) view;
        prefs.edit().putBoolean(SMS_CONFIRMATION, checkbox.isChecked());
        prefs.edit().commit();
    }

    @Override
    public void onRestart()
    {
        super.onRestart();
        Log.e("YEAH", "So resuming?!?");

        getTemplateNames();
        templatesListAdapter.notifyDataSetChanged();
    }
}
