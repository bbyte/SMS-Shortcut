package com.exclus.smsshortcut.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.*;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import java.util.*;
import java.util.regex.Pattern;


public class AddActivity extends Activity
{
    private ArrayList<Map<String, String>> mPeopleList;
    private AutoCompleteTextView phoneInputBox;
    private List<Map<String, String>> phonesList = new ArrayList<Map<String,String>>();
    private SimpleAdapter phonesListAdapter, phoneInputBoxAdapter;

    private SharedPreferences prefs = null;

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

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("loadingContactCompleted"));

        if (Global.getInstance().loadingContacts)
            progressDialog = ProgressDialog.show(AddActivity.this, "", "Loading contacts...");

        prefs = getSharedPreferences(this.getPackageName(), MODE_PRIVATE);


        phoneInputBox = (AutoCompleteTextView) findViewById(R.id.phoneInputBox);

        if (! Global.getInstance().loadingContacts) {

            setAutocompleteAdapter(this);
        }

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

        ListView phonesListView = (ListView) findViewById(R.id.phonesListView);

        phonesListAdapter = new SimpleAdapter(this, phonesList, android.R.layout.simple_list_item_1, new String[]{"phone"}, new int[]{android.R.id.text1});

        phonesListView.setAdapter(phonesListAdapter);

        phonesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getApplicationContext(), "Use long click to delete phone from template", Toast.LENGTH_LONG).show();
                hideKeyboard();
            }
        });

        phonesListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {

                phonesList.remove(pos);
                phonesListAdapter.notifyDataSetChanged();
                Toast.makeText(getApplicationContext(), "Removed", Toast.LENGTH_LONG).show();

                return true;
            }
        });
    }

    public void addPhoneButtonClicked(View view)
    {
        if (isEmpty(phoneInputBox) || ! isValidNumber(phoneInputBox.getText().toString())) {

            Toast.makeText(getApplicationContext(), "To field must be a valid phone number", Toast.LENGTH_LONG).show();
        } else {

            if (phonesList.contains(createPhone("phone", phoneInputBox.getText().toString()))) {

                Toast.makeText(getApplicationContext(), "Number already added", Toast.LENGTH_LONG).show();
            } else {

                phonesList.add(createPhone("phone", phoneInputBox.getText().toString()));

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

        if (isEmpty(templateName) || isEmpty(smsMessage) || phonesList.isEmpty()) {

            Toast.makeText(getApplicationContext(), "All fields have to be filled", Toast.LENGTH_LONG).show();
            hideKeyboard();

            return;
        }

        //Adding shortcut for AddActivity
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
        shortcutIntent.putExtra("duplicate", false);

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
        phonesListAdapter.notifyDataSetChanged();
        hideKeyboard();
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


    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        // This is somewhat like [[NSNotificationCenter defaultCenter] removeObserver:name:object:]
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

}
