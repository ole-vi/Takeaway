package org.ole.planet.myplanet.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import org.lightcouch.CouchDbClientAndroid;
import org.lightcouch.CouchDbException;
import org.lightcouch.CouchDbProperties;
import org.lightcouch.Document;
import org.lightcouch.NoDocumentException;
import org.ole.planet.myplanet.Data.realm_meetups;
import org.ole.planet.myplanet.Data.realm_myCourses;
import org.ole.planet.myplanet.Data.realm_myLibrary;
import org.ole.planet.myplanet.Data.realm_myTeams;
import org.ole.planet.myplanet.Data.realm_resourceActivities;
import org.ole.planet.myplanet.LoginActivity;
import org.ole.planet.myplanet.MainApplication;
import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.callback.SyncListener;
import org.ole.planet.myplanet.datamanager.DatabaseService;
import org.ole.planet.myplanet.userprofile.UserProfileDbHandler;
import org.ole.planet.myplanet.utilities.Constants;
import org.ole.planet.myplanet.utilities.NotificationUtil;
import org.ole.planet.myplanet.utilities.Utilities;

import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import io.realm.Realm;
import io.realm.RealmResults;
import okhttp3.internal.Util;

public class SyncManager {

    static final String PREFS_NAME = "OLE_PLANET";
    private static SyncManager ourInstance;
    private SharedPreferences settings;
    private Realm mRealm;
    private CouchDbProperties properties;
    private Context context;
    private boolean syncTimeout = false;
    private boolean isSyncing = false;
    private String[] stringArray = new String[4];
    private Document shelfDoc;
    private SyncListener listener;
    private DatabaseService dbService;
    private UserProfileDbHandler userProfileDbHandler;

    float timeElapsed;

    private SyncManager(Context context) {
        this.context = context;
        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        dbService = new DatabaseService(context);
        userProfileDbHandler = new UserProfileDbHandler(context);
    }

    public static SyncManager getInstance() {
        if (ourInstance == null) {
            ourInstance = new SyncManager(MainApplication.context);
        }
        return ourInstance;
    }

    public void start(SyncListener listener) {
        this.listener = listener;
        if (!isSyncing) {
            if (listener != null) {
                listener.onSyncStarted();
            }
            syncDatabase();
        } else {
            Utilities.log("Already Syncing...");
        }
    }

    public void destroy() {
        NotificationUtil.cancel(context, 111);
        if (mRealm != null) {
            mRealm.close();
        }
        isSyncing = false;
        ourInstance = null;
        settings.edit().putLong("LastSync", new Date().getTime()).commit();
        if (listener != null) {
            listener.onSyncComplete();
        }
    }

    private void syncDatabase() {
        Thread td = new Thread(new Runnable() {
            public void run() {
                try {
                    Date startDate = new Date();

                    isSyncing = true;
                    syncTimeoutValidation(startDate);
                    NotificationUtil.create(context, R.mipmap.ic_launcher, " Syncing data", "Please wait...");
                    syncTimeoutValidation(startDate);
                    mRealm = dbService.getRealmInstance();
                    syncTimeoutValidation(startDate);
                    properties = dbService.getClouchDbProperties("tablet_users", settings);
                    syncTimeoutValidation(startDate);
                    TransactionSyncManager.syncDb(mRealm, properties, "users");
                    syncTimeoutValidation(startDate);
                    TransactionSyncManager.syncDb(mRealm, dbService.getClouchDbProperties("courses", settings), "course");
                    syncTimeoutValidation(startDate);
                    TransactionSyncManager.syncDb(mRealm, dbService.getClouchDbProperties("exams", settings), "exams");
                    syncTimeoutValidation(startDate);
                    resourceTransactionSync();
                    syncTimeoutValidation(startDate);
                    TransactionSyncManager.syncDb(mRealm, dbService.getClouchDbProperties("ratings", settings), "rating");
                    syncTimeoutValidation(startDate);
                    TransactionSyncManager.syncDb(mRealm, dbService.getClouchDbProperties("submissions", settings), "submissions");
                    syncTimeoutValidation(startDate);
                    myLibraryTransactionSync();
                    syncTimeoutValidation(startDate);
                    TransactionSyncManager.syncDb(mRealm, dbService.getClouchDbProperties("login_activities", settings), "login");
                    syncTimeoutValidation(startDate);
                    realm_resourceActivities.onSynced(mRealm, settings);
                    syncTimeoutValidation(startDate);

                } catch (Exception err) {
                    if (!syncTimeout) {
                        handleException(err.getMessage());
                    } else {
                        syncTimeout = false;
                        handleException("Sync took to long to respond");
                    }
                } finally {
                    destroy();
                }
            }
        });
        td.start();
    }

    private void syncTimeoutValidation(Date startDate) throws Exception {
        Date endDate = new Date();
        timeElapsed = (float)((endDate.getTime() - startDate.getTime()) / 1000.0);

        if (timeElapsed > 20) {
            syncTimeout = true;
            throw new Exception();
        }
    }

    private void handleException(String message) {
        if (listener != null) {
            isSyncing = false;
            MainApplication.syncFailedCount++;
            listener.onSyncFailed(message);
        }
    }


    public void resourceTransactionSync() {
        final CouchDbProperties properties = dbService.getClouchDbProperties("resources", settings);
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                final CouchDbClientAndroid dbClient = new CouchDbClientAndroid(properties);
                syncResource(dbClient);
            }
        });
    }

    private void syncResource(CouchDbClientAndroid dbClient) {
        int skip = 0;
        int limit = 1000;
        while (true) {
            final List<JsonObject> allDocs = dbClient.findDocs("\n" +
                    "{\n" +
                    "    \"selector\": {\n" +
                    "    },\n" +
                    "    \"limit\":" + limit + " ,\n" +
                    "    \"skip\": " + skip + "\n" +
                    "}", JsonObject.class);
            realm_myLibrary.save(allDocs, mRealm);
            if (allDocs.size() < limit) {
                break;
            } else {
                skip = skip + limit;
            }

        }
    }


    private void myLibraryTransactionSync() {
        properties.setDbName("shelf");
        properties.setUsername(settings.getString("url_user", ""));
        properties.setPassword(settings.getString("url_pwd", ""));
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                CouchDbClientAndroid dbShelfClient = new CouchDbClientAndroid(properties);
                List<Document> allShelfDocs = dbShelfClient.view("_all_docs").includeDocs(true).query(Document.class);
                for (int i = 0; i < allShelfDocs.size(); i++) {
                    shelfDoc = allShelfDocs.get(i);
                    populateShelfItems(settings, realm);
                }
            }
        });
    }


    private void populateShelfItems(SharedPreferences settings, Realm mRealm) {
        properties.setDbName("shelf");
        properties.setUsername(settings.getString("url_user", ""));
        properties.setPassword(settings.getString("url_pwd", ""));
        CouchDbClientAndroid dbShelfClient = new CouchDbClientAndroid(properties);
        try {
            this.mRealm = mRealm;
            JsonObject jsonDoc = dbShelfClient.find(JsonObject.class, shelfDoc.getId());
            if (jsonDoc.getAsJsonArray("resourceIds") != null) {
                for (int i = 0; i < Constants.shelfDataList.size(); i++) {
                    Constants.ShelfData shelfData = Constants.shelfDataList.get(i);
                    JsonArray array = jsonDoc.getAsJsonArray(shelfData.key);
                    memberShelfData(array, shelfData);
                }
            } else {
                Log.e("DB", " BAD Metadata -- Shelf Doc ID " + shelfDoc.getId());
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    private void memberShelfData(JsonArray array, Constants.ShelfData shelfData) {
        Utilities.log("Array " + array);
        if (array.size() > 0) {
            triggerInsert(shelfData.categoryKey, shelfData.type);
            check(stringArray, array, shelfData.aClass);
        }
    }

    private void triggerInsert(String categroryId, String categoryDBName) {
        stringArray[0] = shelfDoc.getId();
        stringArray[1] = categroryId;
        stringArray[2] = categoryDBName;
    }


    private void check(String[] stringArray, JsonArray array_categoryIds, Class aClass) {
        for (int x = 0; x < array_categoryIds.size(); x++) {
            if (array_categoryIds.get(x) instanceof JsonNull) {
                continue;
            }
            RealmResults db_Categrory = mRealm.where(aClass)
                    .equalTo("userId", stringArray[0])
                    .equalTo(stringArray[1], array_categoryIds.get(x).getAsString())
                    .findAll();
            checkEmptyAndSave(db_Categrory, x, array_categoryIds);
        }
    }

    private void checkEmptyAndSave(RealmResults db_Categrory, int x, JsonArray array_categoryIds) {
        if (db_Categrory.isEmpty()) {
            setRealmProperties(stringArray[2]);
            validateDocument(array_categoryIds, x);
        } else {
            Log.e("DATA", " Data already saved for -- " + stringArray[0] + " " + array_categoryIds.get(x).getAsString());
        }
    }

    private void validateDocument(JsonArray array_categoryIds, int x) {
        CouchDbClientAndroid generaldb = new CouchDbClientAndroid(properties);
        if (generaldb.contains(array_categoryIds.get(x).getAsString())) {
            JsonObject resourceDoc = generaldb.find(JsonObject.class, array_categoryIds.get(x).getAsString());
            triggerInsert(stringArray, array_categoryIds, x, resourceDoc);
        }
    }

    private void triggerInsert(String[] stringArray, JsonArray array_categoryIds,
                               int x, JsonObject resourceDoc) {

        switch (stringArray[2]) {
            case "resources":
                Utilities.log("Resource  " + array_categoryIds);
                realm_myLibrary.insertMyLibrary(stringArray[0], resourceDoc, mRealm);
                break;
            case "meetups":
                realm_meetups.insertMyMeetups(stringArray[0], array_categoryIds.get(x).getAsString(), resourceDoc, mRealm);
                break;
            case "courses":
                realm_myCourses.insertMyCourses(stringArray[0], resourceDoc, mRealm);
                break;
            case "teams":
                realm_myTeams.insertMyTeams(stringArray[0], array_categoryIds.get(x).getAsString(), resourceDoc, mRealm);
                break;
        }
    }

    private void setRealmProperties(String dbName) {
        properties.setDbName(dbName);
        properties.setUsername(settings.getString("url_user", ""));
        properties.setPassword(settings.getString("url_pwd", ""));
    }
}
