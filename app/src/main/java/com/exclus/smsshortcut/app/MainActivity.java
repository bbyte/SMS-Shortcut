package com.exclus.smsshortcut.app;

import android.app.*;
import android.content.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import retrofit.RestAdapter;
import retrofit.http.GET;
import retrofit.http.Path;

import java.util.*;

public class MainActivity extends Activity {

    public static final String SMS_CONFIRMATION = "confirmation_settings";
//    private ArrayList<Map<String, String>> mPeopleList;
    private SharedPreferences prefs = null;
    private Map<String, ?> templatesList;
    private ListView templatesListView;
    private List<Map<String, String>> templatesNames;
    private SimpleAdapter templatesListAdapter;



//    private Intent addActivityIntent; // = new Intent(this, AddActivity.class);


    private class retrofitTest extends AsyncTask<Void, Void, Void>
    {
        private testRepo repos;

        @Override
        protected Void doInBackground(Void... params)
        {
            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint("https://api.github.com")
                    .build();

            GitHubService service = restAdapter.create(GitHubService.class);

            repos = service.listRepos("stephanenicolas");


            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            Log.e("NETWORK", repos.name);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


//        new retrofitTest().execute();

//        addActivityIntent = new Intent(this, AddActivity.class);

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
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(templatesNames.get(position).get("name"))
                        .setMessage(templatesNames.get(position).get("phones"))
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // continue with delete

                                deleteShortCut(templatesNames.get(position).get("name"));
                                prefs.edit().remove(templatesNames.get(position).get("name")).commit();

                                templatesNames.remove(position);
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

        Boolean conf = prefs.getBoolean(SMS_CONFIRMATION, true);

        if (prefs.getBoolean(SMS_CONFIRMATION, true)) {

            confirmation.setChecked(true);
        } else {

            confirmation.setChecked(false);
        }

        catchShortcut(getIntent());

        /*
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("My notification")
                        .setContentText("Hello World!");
// Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);

// The stack builder object will contain an artificial back stack for the
// started Activity.
// This ensures that navigating backward from the Activity leads out of
// your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
// Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
// Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
// mId allows you to update the notification later on.
        mNotificationManager.notify(100, mBuilder.build());
        */
    }

    private void getTemplateNames()
    {
        templatesNames.clear();
        templatesList = prefs.getAll();

        for (Map.Entry<String, ?> templateEntry : templatesList.entrySet()) {

            if (templateEntry.getKey().contentEquals(SMS_CONFIRMATION))
                continue;

            HashMap<String, String> tmp = new HashMap<String, String>();
            tmp.put("name", templateEntry.getKey());
            tmp.put("phones", templateEntry.getValue().toString());
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
    }

    public void reinstallShortcutsButtonClicked(View view)
    {
    }

    public void confirmationClicked(View view)
    {
        CheckBox checkbox = (CheckBox) view;

        prefs.edit().putBoolean(SMS_CONFIRMATION, checkbox.isChecked()).commit();
    }

    @Override
    public void onRestart()
    {
        super.onRestart();

        getTemplateNames();
        templatesListAdapter.notifyDataSetChanged();
    }

    private void deleteShortCut(String templateName) {

        Intent shortcutIntent = new Intent(getApplicationContext(), MainActivity.class);
        shortcutIntent.setAction(Intent.ACTION_MAIN);

        Intent addIntent = new Intent();
        addIntent
                .putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, templateName);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(getApplicationContext(),
                        R.drawable.ic_launcher));

        addIntent
                .setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");

        getApplicationContext().sendBroadcast(addIntent);
    }
}
