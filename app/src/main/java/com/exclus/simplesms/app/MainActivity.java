package com.exclus.simplesms.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LauncherActivity;
import android.app.PendingIntent;
import android.content.*;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import java.util.*;
import java.util.regex.Pattern;


public class MainActivity extends Activity {

    private ArrayList<Map<String, String>> mPeopleList;
    private SimpleAdapter mAdapter;
    private AutoCompleteTextView phoneInputBox;
    private List<Map<String, String>> phonesList = new ArrayList<Map<String,String>>();
    private SimpleAdapter simpleAdpt;

    SharedPreferences prefs = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(this.getPackageName(), MODE_PRIVATE);

        catchShortcut(getIntent());

        mPeopleList = new ArrayList<Map<String, String>>();
        PopulatePeopleList();
        phoneInputBox = (AutoCompleteTextView) findViewById(R.id.phoneInputBox);
        mAdapter = new SimpleAdapter(this, mPeopleList, R.layout.custcontview,
                new String[] { "Name", "Phone", "Type" }, new int[] {
                R.id.ccontName, R.id.ccontNo, R.id.ccontType });
        phoneInputBox.setAdapter(mAdapter);

        phoneInputBox.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> av, View arg1, int index,
                                    long arg3) {
                Map<String, String> map = (Map<String, String>) av.getItemAtPosition(index);

                String name = map.get("Name");
                String number = map.get("Phone");
//                phoneInputBox.setText("" + name + "<" + number + ">");
                phoneInputBox.setText(number);

            }
        });

        ListView lv = (ListView) findViewById(R.id.phonesListView);

        // This is a simple adapter that accepts as parameter
        // Context
        // Data list
        // The row layout that is used during the row creation
        // The keys used to retrieve the data
        // The View id used to show the data. The key number and the view id must match
        simpleAdpt = new SimpleAdapter(this,
                phonesList,
                android.R.layout.simple_list_item_1,
                new String[]{"phone"},
                new int[]{android.R.id.text1});

        lv.setAdapter(simpleAdpt);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getApplicationContext(), "Use long click to delete phone from template", Toast.LENGTH_LONG).show();
                hideKeyboard();
            }
        });

        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {

                phonesList.remove(pos);
                simpleAdpt.notifyDataSetChanged();
                Toast.makeText(getApplicationContext(), "Removed", Toast.LENGTH_LONG).show();

                return true;
            }
        });
    }

    public void PopulatePeopleList() {
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

    public void addPhoneButtonClicked(View view)
    {

        if (isEmpty(phoneInputBox) || ! isValidNumber(phoneInputBox.getText().toString())) {

            Toast.makeText(getApplicationContext(), "To field must be a valid number", Toast.LENGTH_LONG).show();
        } else {

            if (phonesList.contains(createPhone("phone", phoneInputBox.getText().toString()))) {

                Toast.makeText(getApplicationContext(), "Number already added", Toast.LENGTH_LONG).show();
            } else {

                phonesList.add(createPhone("phone", phoneInputBox.getText().toString()));

                simpleAdpt.notifyDataSetChanged();
                phoneInputBox.setText("");
                hideKeyboard();
            }
        }
    }

    private HashMap<String, String> createPhone(String key, String name) {
        HashMap<String, String> phone = new HashMap<String, String>();
        phone.put(key, name);

        return phone;
    }


    @Override
    protected void onNewIntent(Intent in) {
        super.onNewIntent(in);

//        if(in.hasExtra("some_tag") && in.getExtra("some_tag") == true) {
//            //Do Something Special
//        }

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

            Toast.makeText(getApplicationContext(), "SMS message \"" + message + "\" was sent to " + lPhonesList.toString(),
                    Toast.LENGTH_LONG).show();


            // exit from app, but w/o killing it
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory( Intent.CATEGORY_HOME );
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(homeIntent);
        }
    }


    public void saveSMSTemplateClicked(View view)
    {
        EditText templateName = (EditText) findViewById(R.id.templateName);
        EditText smsMessage = (EditText) findViewById(R.id.smsMessage);

        if (isEmpty(templateName) || isEmpty(smsMessage) || phonesList.isEmpty()) {

            Toast.makeText(getApplicationContext(), "All fields have to be filled", Toast.LENGTH_LONG).show();
            hideKeyboard();

            return;
        }

        //Adding shortcut for MainActivity
        //on Home screen
        Intent shortcutIntent = new Intent(getApplicationContext(), MainActivity.class);

        Set lPhonesList = new HashSet();

        for (Map<String, String> phoneNumber : phonesList) {

            lPhonesList.add(phoneNumber.get("phone"));
            Log.e("phone", phoneNumber.get("phone"));
        }

        prefs.edit().putStringSet(templateName.getText().toString(), lPhonesList).commit();

        Log.e("Phones", lPhonesList.toString());

        shortcutIntent.putExtra("message", smsMessage.getText().toString());
        shortcutIntent.putExtra("templateName", templateName.getText().toString());
        shortcutIntent.setAction(Intent.ACTION_MAIN);

        Intent addIntent = new Intent();
        addIntent
                .putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, templateName.getText().toString());
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(getApplicationContext(),
                        R.drawable.ic_launcher));

        addIntent
                .setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        getApplicationContext().sendBroadcast(addIntent);

        Toast.makeText(getApplicationContext(), "Template '" + templateName.getText().toString() + "' saved on home screen",
                Toast.LENGTH_LONG).show();

        templateName.setText("");
        smsMessage.setText("");
        phonesList.clear();
        simpleAdpt.notifyDataSetChanged();
        hideKeyboard();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        hideKeyboard();
        return true;
    }

    private void hideKeyboard()
    {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.
                INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
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

    private boolean isEmpty(EditText etText) {
        return etText.getText().toString().trim().length() == 0;
    }

    private boolean isValidNumber(String number) {

        Pattern sPattern = Pattern.compile("^[0-9\\s\\+-]+$");

        return sPattern.matcher(number).matches();
    }

}
