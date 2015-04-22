package com.exclus.smsshortcut;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.*;
import android.os.Bundle;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import java.util.*;
import java.util.regex.Pattern;


public class AddActivity extends Activity
{
    private AutoCompleteTextView phoneInputBox;
    private List<Map<String, String>> phonesList = new ArrayList<Map<String,String>>();
    private SimpleAdapter phonesListAdapter, phoneInputBoxAdapter, emptyListAdapter;
    private List<Map<String, String>> emptyPhoneList = new ArrayList<Map<String, String>>();

    private ListView phonesListView;

    private ProgressDialog progressDialog;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            Log.d("receiver", "Got message: " + message);

            setAutocompleteAdapter(context);

            hideProgressDialog();
        }
    };

    private void setAutocompleteAdapter(Context context)
    {
        phoneInputBoxAdapter = new SimpleAdapter(context, Global.getInstance().mPeopleList, R.layout.autocomplete_item,
                new String[]{"Name", "Phone", "Type"}, new int[]{
                R.id.contactName, R.id.contactNumber, R.id.contactType}
        );
        phoneInputBox.setAdapter(phoneInputBoxAdapter);
    }

    private String getNameFromPhonenumber(String phone)
    {
        // defaults to "Unknown string"

        String name = "Unknown";

        for (Map<String, String> personDetails : Global.getInstance().mPeopleList) {

            if (personDetails.get("Phone").contentEquals(phone)) {

                name = personDetails.get("Name");
                break;
            }
        }

        return name;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        HashMap<String, String> mp = new HashMap<String, String>();
        mp.put("table", "activities");
        mp.put("event", "TEMPLATE_ADD");
        new submitStatistics().execute(mp);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("loadingContactCompleted"));

        if (Global.getInstance().loadingContacts)
            progressDialog = ProgressDialog.show(AddActivity.this, "", "Loading contacts...");


        phoneInputBox = (AutoCompleteTextView) findViewById(R.id.phoneInputBox);
        phonesListView = (ListView) findViewById(R.id.phonesListView);

        if (! Global.getInstance().loadingContacts) {

            setAutocompleteAdapter(this);
        }


        phoneInputBox.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> av, View arg1, int index,
                                    long arg3) {

                Map<String, String> map = (Map<String, String>) av.getItemAtPosition(index);

                phonesList.add(createPhone("phone", map.get("Phone")));
//                phonesListAdapter = new SimpleAdapter(getApplicationContext(), phonesList, android.R.layout.simple_list_item_1, new String[]{"phone"}, new int[]{android.R.id.text1});
                phonesListAdapter.notifyDataSetChanged();

                phonesListView.setAdapter(phonesListAdapter);

                phoneInputBox.setText("");
                hideKeyboard();
            }
        });


//        emptyPhoneList.add(createPhone("phone", "Please add a phone by entering it in \"To...\" field"));
        emptyPhoneList.add(createPhone("phone", "Please add a phone"));
        emptyPhoneList.add(createPhone("phone", "You can do that by start typing in \"To...\" field"));


        phonesListAdapter = new SimpleAdapter(this, phonesList, android.R.layout.simple_list_item_1, new String[]{"phone"}, new int[]{android.R.id.text1});
        emptyListAdapter = new SimpleAdapter(this, emptyPhoneList, R.layout.itallic_list, new String[]{"phone"}, new int[]{android.R.id.text1});

//        phonesListView.setAdapter(phonesListAdapter);

        phonesListView.setAdapter(emptyListAdapter);

        phonesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (! phonesList.isEmpty()) {

                    Toast.makeText(getApplicationContext(), "Use long click to delete phone from template", Toast.LENGTH_LONG).show();
                    hideKeyboard();
                }
            }
        });

        phonesListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {

                if (! phonesList.isEmpty()) {

                    phonesList.remove(pos);
                    phonesListAdapter.notifyDataSetChanged();
                    Toast.makeText(getApplicationContext(), "Removed", Toast.LENGTH_LONG).show();
                }

                if (phonesList.isEmpty()) {

//                    phonesListAdapter = new SimpleAdapter(getApplicationContext(), emptyPhoneList, R.layout.itallic_list, new String[]{"phone"}, new int[]{android.R.id.text1});
                    phonesListView.setAdapter(emptyListAdapter);
                }


                return true;
            }
        });
    }

    public void addTemplateButtonClicked(View view)
    {
        if (isEmpty(phoneInputBox) || ! isValidNumber(phoneInputBox.getText().toString())) {

            Toast.makeText(getApplicationContext(), "To field must be a valid phone number", Toast.LENGTH_LONG).show();
        } else {

            if (phonesList.contains(createPhone("phone", phoneInputBox.getText().toString()))) {

                Toast.makeText(getApplicationContext(), "Number already added", Toast.LENGTH_LONG).show();
            } else {



                phonesList.add(createPhone("phone", phoneInputBox.getText().toString()));

                phonesListAdapter = new SimpleAdapter(this, phonesList, android.R.layout.simple_list_item_1, new String[]{"phone"}, new int[]{android.R.id.text1});


                phonesListAdapter.notifyDataSetChanged();
                phoneInputBox.setText("");
                hideKeyboard();
            }
        }
    }

    public void saveSMSTemplateClicked(View view)
    {
        EditText templateName = (EditText) findViewById(R.id.templateName);
        EditText smsMessage = (EditText) findViewById(R.id.smsMessage);
        ListView phoneListView = (ListView) findViewById(R.id.phonesListView);

        Toast toast;

        if (phonesList.isEmpty()) {

            redBlinkView(phoneListView);

            toast = Toast.makeText(getApplicationContext(), "At least one number have to be added", Toast.LENGTH_LONG);
//            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();

            hideKeyboard();

            return;
        }

        // TODO: Toast must be on top

        if (isEmpty(smsMessage)) {

            redBlinkView(smsMessage);

            toast = Toast.makeText(getApplicationContext(), "SMS message can't be empty", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 30);
            toast.show();

            hideKeyboard();

            return;
        }


        if (isEmpty(templateName)) {

            redBlinkView(templateName);

            toast = Toast.makeText(getApplicationContext(), "Template name can't be empty", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 30);
            toast.show();

            hideKeyboard();

            return;
        }


        Shortcut shortcut = new Shortcut(getApplicationContext(), templateName.getText().toString());


        List<SMSPhone> SMSPhones = new ArrayList<SMSPhone>();

        for (Map<String, String> phoneNumber : phonesList) {

            SMSPhone smsPhone = new SMSPhone();
            smsPhone.setPhoneNumber(phoneNumber.get("phone"));
            smsPhone.setName(getNameFromPhonenumber(phoneNumber.get("phone")));
            SMSPhones.add(smsPhone);

            Log.e("phone", smsPhone.toString());
        }

        DatabaseHelper db = DatabaseHelper.getHelper(getApplicationContext());

        SMSTemplate smsTemplate = new SMSTemplate();

        smsTemplate.setName(templateName.getText().toString());
        smsTemplate.setText(smsMessage.getText().toString());
        smsTemplate.setPhones(SMSPhones);

        db.createSMSTemplate(smsTemplate, SMSPhones);

        Log.e("Phones", SMSPhones.toString());

        shortcut.create();

        Toast.makeText(getApplicationContext(), "Template '" + templateName.getText().toString() + "' saved on home screen",
                Toast.LENGTH_LONG).show();

        templateName.setText("");
        smsMessage.setText("");
        phonesList.clear();
        phonesListAdapter.notifyDataSetChanged();
        hideKeyboard();
        HashMap<String, String> mp = new HashMap<String, String>();
        mp.put("table", "activities");
        mp.put("event", "TEMPLATE_SAVE");
        new submitStatistics().execute(mp);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        hideKeyboard();
        return true;
    }

    private HashMap<String, String> createPhone(String key, String name)
    {
        HashMap<String, String> phone = new HashMap<String, String>();
        phone.put(key, name);

        return phone;
    }

    private void hideKeyboard()
    {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.
                INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }

    private boolean isEmpty(EditText etText)
    {
        return etText.getText().toString().trim().length() == 0;
    }

    private boolean isValidNumber(String number)
    {

        Pattern sPattern = Pattern.compile("^[0-9\\s\\+\\-\\(\\)]+$");

        return sPattern.matcher(number).matches();
    }

    public void hideProgressDialog()
    {
        if (progressDialog.isShowing())
            progressDialog.dismiss();
    }

    private void redBlinkView(View view)
    {
        int RED = 0xffFF8080;
        int WHITE = 0xffffffff;


        ValueAnimator colorAnim = ObjectAnimator.ofInt(view, "backgroundColor", WHITE, RED);
        colorAnim.setDuration(1000);
        colorAnim.setEvaluator(new ArgbEvaluator());
        colorAnim.setRepeatCount(1);
        colorAnim.setRepeatMode(ValueAnimator.REVERSE);
        colorAnim.start();
    }


    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        // This is somewhat like [[NSNotificationCenter defaultCenter] removeObserver:name:object:]
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

}
