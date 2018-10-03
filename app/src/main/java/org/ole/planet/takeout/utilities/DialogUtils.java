package org.ole.planet.takeout.utilities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.WindowManager;

import org.ole.planet.takeout.MainApplication;
import org.ole.planet.takeout.datamanager.MyDownloadService;

import java.util.ArrayList;


public class DialogUtils {

    public static ProgressDialog getProgressDialog(final Context context) {
        final ProgressDialog prgDialog = new ProgressDialog(context);
        prgDialog.setTitle("Downloading file...");
        prgDialog.setMax(100);
        prgDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        prgDialog.setCancelable(false);
        prgDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Dismiss", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                prgDialog.dismiss();
            }
        });
        prgDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Stop Download", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                context.stopService(new Intent(context, MyDownloadService.class));
            }
        });
        return prgDialog;
    }

    public static void handleCheck(ArrayList<Integer> selectedItemsList, boolean b, Integer i) {
        if (b) {
            selectedItemsList.add(i);
        } else if (selectedItemsList.contains(i)) {
            selectedItemsList.remove(i);
        }
    }

    public static void showError(ProgressDialog prgDialog, String message) {
        prgDialog.setTitle(message);
        if (prgDialog.getButton(ProgressDialog.BUTTON_NEGATIVE) != null)
            prgDialog.getButton(ProgressDialog.BUTTON_NEGATIVE).setEnabled(false);
    }

    public static void showWifiSettingDialog(final Context context) {
        if (MainApplication.syncFailedCount > 3) {
            AlertDialog.Builder pd = new AlertDialog.Builder(context);
            String message = NetworkUtils.isBluetoothEnabled() ? "Bluetooth " : "";
            message += NetworkUtils.isWifiEnabled() ? "Wifi " : "";
            message += " is on please turn of to save battery";
            pd.setMessage(message);
            pd.setPositiveButton("Go to settings", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    context.startActivity(intent);
                }
            }).setNegativeButton("Cancel", null);
            pd.setCancelable(false);
            AlertDialog d = pd.create();
//            d.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            d.show();
        }
    }

    public static void showSnack(View v, String s) {
        if (v != null)
            Snackbar.make(v, s, Snackbar.LENGTH_LONG).show();
    }
}
