package com.exclus.smsshortcut.app;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.*;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.*;


public class PreferencesActivity extends Activity {

    private Intent mainActivityIntent;
    private ArrayList<Map<String, String>> mPeopleList;
    SharedPreferences prefs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        mPeopleList = new ArrayList<Map<String, String>>();

        mainActivityIntent = new Intent(this, MainActivity.class);

        prefs = getSharedPreferences(this.getPackageName(), MODE_PRIVATE);

        catchShortcut(getIntent());
        PopulatePeopleList();
    }

    @Override
    protected void onNewIntent(Intent in)
    {
        super.onNewIntent(in);

        catchShortcut(in);
    }

    public void PopulatePeopleList()
    {
        mPeopleList.clear();
        Cursor people = getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        while (people.moveToNext()) {
            String contactName = people.getString(people
                    .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            String contactId = people.getString(people
                    .getColumnIndex(ContactsContract.Contacts._ID));
            String hasPhone = people
                    .getString(people
                            .getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

            if ((Integer.parseInt(hasPhone) > 0)){
                // You know have the number so now query it like this
                Cursor phones = getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ contactId,
                        null, null);
                while (phones.moveToNext()){
                    //store numbers and display a dialog letting the user select which.
                    String phoneNumber = phones.getString(
                            phones.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER));
                    String numberType = phones.getString(phones.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.TYPE));
                    Map<String, String> NamePhoneType = new HashMap<String, String>();
                    NamePhoneType.put("Name", contactName);
                    NamePhoneType.put("Phone", phoneNumber);
                    if(numberType.equals("0"))
                        NamePhoneType.put("Type", "Work");
                    else
                    if(numberType.equals("1"))
                        NamePhoneType.put("Type", "Home");
                    else if(numberType.equals("2"))
                        NamePhoneType.put("Type",  "Mobile");
                    else
                        NamePhoneType.put("Type", "Other");
                    //Then add this map to the list.
                    mPeopleList.add(NamePhoneType);
                }
                phones.close();
            }
        }
        people.close();
//        startManagingCursor(people);
        people = null;
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

            Toast.makeText(getApplicationContext(), "SMS message \"" + message + "\" was sent to " + lPhonesList.toString(),
                    Toast.LENGTH_LONG).show();


            // exit from app, but w/o killing it
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory( Intent.CATEGORY_HOME );
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(homeIntent);
        }
    }

    //---sends an SMS message to another device---
    private void sendSMS(String phoneNumber, String message)
    {
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS sent",
                                Toast.LENGTH_SHORT).show();
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

        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
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
//        Intent intent = new Intent(this, MainActivity.class);
//        mainActivityIntent.putExtra("mPeopleList", mPeopleList);
        Global.getInstance().mPeopleList = mPeopleList;
        startActivity(mainActivityIntent);
    }
}
