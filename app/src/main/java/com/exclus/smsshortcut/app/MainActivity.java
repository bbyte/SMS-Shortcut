package com.exclus.smsshortcut.app;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.*;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {

//    private Intent mainActivityIntent;
//    private ArrayList<Map<String, String>> mPeopleList;
    SharedPreferences prefs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

//        mPeopleList = new ArrayList<Map<String, String>>();



        prefs = getSharedPreferences(this.getPackageName(), MODE_PRIVATE);

        catchShortcut(getIntent());


        new getContacts(this).execute();
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
        Set<String> lPhonesList;

        if (in.hasExtra("message") && in.hasExtra("templateName")) {

            String message = in.getStringExtra("message");
            String templateName = in.getStringExtra("templateName");

            lPhonesList = prefs.getStringSet(templateName, new HashSet<String>());

            for (String phone : lPhonesList) {

                Log.e("catchShortcut", phone);

                SmsManager smsMgr = SmsManager.getDefault();

                ArrayList<String> parts = smsMgr.divideMessage(message);

//                smsMgr.sendMultipartTextMessage(phone, null, parts, null,null);

//                smsMgr.sendTextMessage(phone, null, in.getStringExtra("message"), null, null);

                sendSMS(phone, message);
            }

            Toast.makeText(getApplicationContext(), "Sending \"" + message + "\" to " + lPhonesList.toString(),
                    Toast.LENGTH_LONG).show();


            // exit from app, but w/o killing it
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory( Intent.CATEGORY_HOME );
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(homeIntent);
        }
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
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
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
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.preferences, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
}
