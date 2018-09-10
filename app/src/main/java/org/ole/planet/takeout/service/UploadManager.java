package org.ole.planet.takeout.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.lightcouch.CouchDbClientAndroid;
import org.lightcouch.CouchDbProperties;
import org.lightcouch.Response;
import org.ole.planet.takeout.Data.realm_feedback;
import org.ole.planet.takeout.Data.realm_offlineActivities;
import org.ole.planet.takeout.Data.realm_submissions;
import org.ole.planet.takeout.MainApplication;
import org.ole.planet.takeout.SyncActivity;
import org.ole.planet.takeout.callback.SuccessListener;
import org.ole.planet.takeout.datamanager.DatabaseService;
import org.ole.planet.takeout.utilities.Utilities;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

public class UploadManager {
    private DatabaseService dbService;
    private Context context;
    private CouchDbProperties properties;
    private SharedPreferences sharedPreferences;
    private Realm mRealm;
    private static UploadManager instance;

    public static UploadManager getInstance() {
        if (instance == null) {
            instance = new UploadManager(MainApplication.context);
        }
        return instance;
    }


    public UploadManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(SyncActivity.PREFS_NAME, Context.MODE_PRIVATE);
        dbService = new DatabaseService(context);

    }

    public void uploadExamResult(final SuccessListener listener) {
        mRealm = dbService.getRealmInstance();
        final CouchDbProperties properties = dbService.getClouchDbProperties("submissions", sharedPreferences);
        mRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(@NonNull Realm realm) {
                final CouchDbClientAndroid dbClient = new CouchDbClientAndroid(properties);
                List<realm_submissions> submissions = realm.where(realm_submissions.class).equalTo("status", "graded").equalTo("uploaded", false).findAll();
                for (realm_submissions sub : submissions) {
                    Response r = dbClient.post(realm_submissions.serializeExamResult(realm, sub));
                    if (!TextUtils.isEmpty(r.getId())) {
                        sub.setUploaded(true);
                        Utilities.log("ID " + r.getId());
                    }
                }
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                listener.onSuccess("Result sync completed successfully");
            }
        });
    }

    public void uploadFeedback(final SuccessListener listener) {
        mRealm = dbService.getRealmInstance();
        final CouchDbProperties properties = dbService.getClouchDbProperties("feedback", sharedPreferences);
        mRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(@NonNull Realm realm) {
                final CouchDbClientAndroid dbClient = new CouchDbClientAndroid(properties);
                List<realm_feedback> feedbacks = realm.where(realm_feedback.class).equalTo("uploaded", false).findAll();
                for (realm_feedback feedback : feedbacks) {
                    Response r = dbClient.post(realm_feedback.serializeFeedback(feedback));
                    if (!TextUtils.isEmpty(r.getId())) {
                        feedback.setUploaded(true);
                    }
                }
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                listener.onSuccess("Feedback sync completed successfully");
            }
        });
    }

    public void uploadUserActivities(final SuccessListener listener) {
        mRealm = dbService.getRealmInstance();
        final CouchDbProperties properties = dbService.getClouchDbProperties("login_activities", sharedPreferences);
        mRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(@NonNull Realm realm) {
                final RealmResults<realm_offlineActivities> activities = realm.where(realm_offlineActivities.class)
                        .isNull("_rev").findAll();
                Utilities.log("Size " + activities.size());
                final CouchDbClientAndroid dbClient = new CouchDbClientAndroid(properties);
                for (realm_offlineActivities act : activities) {
                    Response r = dbClient.post(realm_offlineActivities.serializeLoginActivities(act));
                    if (!TextUtils.isEmpty(r.getId())) {
                        act.set_rev(r.getRev());
                        act.set_id(r.getId());
                    }
                }
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                listener.onSuccess("Sync with server completed successfully");
            }
        });
    }
}
