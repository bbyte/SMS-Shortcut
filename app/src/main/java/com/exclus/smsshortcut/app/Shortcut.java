package com.exclus.smsshortcut.app;

import android.content.Context;
import android.content.Intent;

/**
 * Created by bbyte on 5/16/14.
 */
public class Shortcut {

    private String name;
    private String message;
    private Context context;
    private Intent shortcutIntent, addIntent;

    public Shortcut(Context context, String shortcutName)
    {
        this.context = context;
        this.name = shortcutName;

        this.shortcutIntent = new Intent(context, MainActivity.class);
        shortcutIntent.setAction(Intent.ACTION_MAIN);

        addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, this.name);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(context,
                        R.drawable.ic_launcher));
        addIntent.putExtra("duplicate", false); // Just create once
    }

    public String getShortName()
    {
        return this.name;
    }

    public void setShortcutName(String name)
    {
        this.name = name;
    }

//    public Boolean create(String message)
    public Boolean create()
    {
//        this.message = message;

//        Intent shortcutIntent = new Intent(context, MainActivity.class);
//        shortcutIntent.putExtra("message", this.message);
        shortcutIntent.putExtra("templateName", this.name);

//        shortcutIntent.setAction(Intent.ACTION_MAIN);

//        Intent addIntent = new Intent();
//        addIntent
//                .putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
//        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, this.name);
//        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
//                Intent.ShortcutIconResource.fromContext(context,
//                        R.drawable.ic_launcher));

        addIntent
                .setAction("com.android.launcher.action.INSTALL_SHORTCUT");

        context.sendBroadcast(addIntent);

        return true;
    }

    public Boolean remove()
    {
//        Intent shortcutIntent = new Intent(context, MainActivity.class);
//        shortcutIntent.setAction(Intent.ACTION_MAIN);
//
//        Intent addIntent = new Intent();
//        addIntent
//                .putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
//        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, this.name);
//        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
//                Intent.ShortcutIconResource.fromContext(context,
//                        R.drawable.ic_launcher));
        addIntent
                .setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");

        context.sendBroadcast(addIntent);

        return true;
    }
}
