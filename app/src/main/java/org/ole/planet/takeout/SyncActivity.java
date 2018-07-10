package org.ole.planet.takeout;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.lightcouch.CouchDbProperties;
import org.ole.planet.takeout.Data.realm_UserModel;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

public abstract class SyncActivity extends ProcessUserData {
    private TextView syncDate;
    private TextView intervalLabel;
    private Spinner spinner;
    private Switch syncSwitch;
    int convertedDate;
    public static final String PREFS_NAME = "OLE_PLANET";
    SharedPreferences settings;
    Realm mRealm;
    Context context;
    CouchDbProperties properties;
    MaterialDialog progress_dialog;


    public void sync(MaterialDialog dialog) {
        // Check Autosync switch (Toggler)
        syncSwitch = (Switch) dialog.findViewById(R.id.syncSwitch);
        intervalLabel = (TextView) dialog.findViewById(R.id.intervalLabel);
        syncSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.e("MD: ", "Autosync is On");
                    intervalLabel.setVisibility(View.VISIBLE);
                    spinner.setVisibility(View.VISIBLE);
                } else {
                    Log.e("MD: ", "Autosync is Off");
                    spinner.setVisibility(View.GONE);
                    intervalLabel.setVisibility(View.GONE);
                }
            }
        });
        dateCheck(dialog);
    }

    private void dateCheck(MaterialDialog dialog) {
        convertedDate = convertDate();
        // Check if the user never synced
        if (convertedDate == 0) {
            syncDate = (TextView) dialog.findViewById(R.id.lastDateSynced);
            syncDate.setText("Last Sync Date: Never");
        } else {
            syncDate = (TextView) dialog.findViewById(R.id.lastDateSynced);
            syncDate.setText("Last Sync Date: " + convertedDate);
        }

        // Init spinner dropdown items
        spinner = (Spinner) dialog.findViewById(R.id.intervalDropper);
        syncDropdownAdd();
    }

    // Converts OS date to human date
    private int convertDate() {
        // Context goes here
        return 0; // <=== modify this when implementing this method
    }

    // Create items in the spinner
    public void syncDropdownAdd() {
        List<String> list = new ArrayList<>();
        list.add("15 Minutes");
        list.add("30 Minutes");
        list.add("1 Hour");
        list.add("3 Hours");
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, list);
        spinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinner.setAdapter(spinnerArrayAdapter);
    }


    public void syncDatabase() {
        Thread td = new Thread(new Runnable() {
            public void run() {
                try {
                    realmConfig("_users");
                    userTransactionSync(settings, mRealm, properties, progress_dialog);
                    myLibraryTransactionSync();
                } finally {
                    if (mRealm != null) {
                        mRealm.close();
                    }
                }
            }
        });
        td.start();
    }

    public void alertDialogOkay(String Message) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage(Message);
        builder1.setCancelable(true);
        builder1.setNegativeButton("Okay",
                (dialog, id) -> dialog.cancel());
        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

    public boolean authenticateUser(SharedPreferences settings, String username, String password, Context context) {
        this.settings = settings;
        this.context = context;
        AndroidDecrypter decrypt = new AndroidDecrypter();
        realmConfig("_users");
        if (mRealm.isEmpty()) {
            alertDialogOkay("Server not configured properly. Connect this device with Planet server");
            mRealm.close();
            return false;
        } else {
            return checkName(username, password, decrypt);
        }
    }

    @Nullable
    private Boolean checkName(String username, String password, AndroidDecrypter decrypt) {
        try {
            RealmResults<realm_UserModel> db_users = mRealm.where(realm_UserModel.class)
                    .equalTo("name", username)
                    .findAll();
            mRealm.beginTransaction();
            for (realm_UserModel user : db_users) {
                if (decrypt.AndroidDecrypter(username, password, user.getDerived_key(), user.getSalt())) {
                    saveUserInfoPref(settings, password, user);
                    mRealm.close();
                    return true;
                }
            }
        } catch (Exception err) {
            err.printStackTrace();
            mRealm.close();
            return false;
        }
        mRealm.close();
        return false;
    }


    public void realmConfig(String dbName) {
        Realm.init(context);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .name(Realm.DEFAULT_REALM_NAME)
                .deleteRealmIfMigrationNeeded()
                .schemaVersion(4)
                .build();
        Realm.setDefaultConfiguration(config);
        mRealm = Realm.getInstance(config);
        properties = new CouchDbProperties()
                .setDbName(dbName)
                .setCreateDbIfNotExist(false)
                .setProtocol(settings.getString("url_Scheme", "http"))
                .setHost(settings.getString("url_Host", "192.168.2.1"))
                .setPort(settings.getInt("url_Port", 3000))
                .setUsername(settings.getString("url_user", ""))
                .setPassword(settings.getString("url_pwd", ""))
                .setMaxConnections(100)
                .setConnectionTimeout(0);

    }


    public void setUrlParts(String url, String password, Context context) {
        this.context = context;
        URI uri = URI.create(url);
        String url_user = null, url_pwd = null;
        if (url.contains("@")) {
            String[] userinfo = uri.getUserInfo().split(":");
            url_user = userinfo[0];
            url_pwd = userinfo[1];
        } else {
            url_user = "";
            url_pwd = password;
        }
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("serverURL", url);
        editor.putString("url_Scheme", uri.getScheme());
        editor.putString("url_Host", uri.getHost());
        editor.putInt("url_Port", uri.getPort());
        editor.putString("url_user", url_user);
        editor.putString("url_pwd", url_pwd);
        editor.commit();
        progress_dialog = new MaterialDialog.Builder(this)
                .title("Syncing")
                .content("Please wait")
                .progress(true, 0)
                .show();
        syncDatabase();
    }


}
