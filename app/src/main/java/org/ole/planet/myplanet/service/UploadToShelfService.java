package org.ole.planet.myplanet.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.ole.planet.myplanet.MainApplication;
import org.ole.planet.myplanet.callback.SuccessListener;
import org.ole.planet.myplanet.datamanager.ApiClient;
import org.ole.planet.myplanet.datamanager.ApiInterface;
import org.ole.planet.myplanet.datamanager.DatabaseService;
import org.ole.planet.myplanet.datamanager.Service;
import org.ole.planet.myplanet.model.RealmMeetup;
import org.ole.planet.myplanet.model.RealmMyCourse;
import org.ole.planet.myplanet.model.RealmMyHealthPojo;
import org.ole.planet.myplanet.model.RealmMyLibrary;
import org.ole.planet.myplanet.model.RealmRemovedLog;
import org.ole.planet.myplanet.model.RealmUserModel;
import org.ole.planet.myplanet.ui.sync.SyncActivity;
import org.ole.planet.myplanet.utilities.AndroidDecrypter;
import org.ole.planet.myplanet.utilities.JsonUtils;
import org.ole.planet.myplanet.utilities.Utilities;

import java.io.IOException;
import java.security.Key;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import retrofit2.Response;

public class UploadToShelfService {

    private static UploadToShelfService instance;
    private DatabaseService dbService;
    private SharedPreferences sharedPreferences;
    private Realm mRealm;


    public UploadToShelfService(Context context) {
        sharedPreferences = context.getSharedPreferences(SyncActivity.PREFS_NAME, Context.MODE_PRIVATE);
        dbService = new DatabaseService(context);
    }

    public static UploadToShelfService getInstance() {
        if (instance == null) {
            instance = new UploadToShelfService(MainApplication.context);
        }
        return instance;
    }

    public void uploadUserData(SuccessListener listener) {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        mRealm = dbService.getRealmInstance();
        mRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                List<RealmUserModel> userModels = realm.where(RealmUserModel.class).isEmpty("_id").findAll();
                for (RealmUserModel model : userModels) {
                    try {
                        Response<JsonObject> res = apiInterface.getJsonObject(Utilities.getHeader(), Utilities.getUrl() + "/_users/org.couchdb.user:" + model.getName()).execute();
                        if (res.body() == null) {
                            res = apiInterface.putDoc(null, "application/json", Utilities.getUrl() + "/_users/org.couchdb.user:" + model.getName(), model.serialize()).execute();
                            if (res.body() != null) {
                                String id = res.body().get("id").getAsString();
                                String rev = res.body().get("rev").getAsString();
                                res = apiInterface.getJsonObject(Utilities.getHeader(), Utilities.getUrl() + "/_users/" + id).execute();
                                if (res.body() != null) {
                                    model.set_id(id);
                                    model.set_rev(rev);
                                    model.setPassword_scheme(JsonUtils.getString("password_scheme", res.body()));
                                    model.setDerived_key(JsonUtils.getString("derived_key", res.body()));
                                    model.setSalt(JsonUtils.getString("salt", res.body()));
                                    model.setIterations(JsonUtils.getString("iterations", res.body()));
                                    saveKeyIv(apiInterface, model);
                                }
                            }
                        } else {
                            Utilities.log("User " + model.getName() + " already exist");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, () -> {
            uploadToshelf(listener);
        }, (err) -> {
            uploadToshelf(listener);
        });

    }

    public void saveKeyIv(ApiInterface apiInterface, RealmUserModel model) throws IOException {
        String table = "userdb-" + Utilities.toHex(model.getPlanetCode()) + "-" + Utilities.toHex(model.getName());

        JsonObject ob = new JsonObject();
        Key key = AndroidDecrypter.generateKey();
        String keyString = new String(key.getEncoded());
        String iv = AndroidDecrypter.generateIv(key);
        ob.addProperty("key", keyString);
        ob.addProperty("iv", iv);
        ob.addProperty("createdOn", new Date().getTime());
        Response response = apiInterface.postDoc(Utilities.getHeader(), "application/jsonn", Utilities.getUrl() + "/" + table, ob).execute();
        Utilities.log(new Gson().toJson(ob));
        if (response.body() != null) {
            Utilities.log(new Gson().toJson(response.body()));
            model.setKey(keyString);
            model.setIv(iv);
        } else {
            Utilities.log(new Gson().toJson(response.errorBody()));
        }
    }


    public void uploadHealth() {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        mRealm = dbService.getRealmInstance();
        mRealm.executeTransactionAsync(realm -> {
            List<RealmMyHealthPojo> myHealths = realm.where(RealmMyHealthPojo.class).findAll();
            for (RealmMyHealthPojo pojo : myHealths) {
                try {
                    if (pojo.get_id().isEmpty()) {
                        RealmUserModel user = realm.where(RealmUserModel.class).equalTo("_id", pojo.getUserId()).findFirst();
                        pojo.setData(AndroidDecrypter.encrypt(pojo.getData(), user.getKey(), user.getIv()));
                    }
                    Response res = apiInterface.postDoc(Utilities.getHeader(), "application/json", Utilities.getUrl() + "/health", RealmMyHealthPojo.serialize(pojo)).execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });
    }


    public void uploadToshelf(final SuccessListener listener) {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        mRealm = dbService.getRealmInstance();
        mRealm.executeTransactionAsync(realm -> {
            RealmResults<RealmUserModel> users = realm.where(RealmUserModel.class).isNotEmpty("_id").findAll();
            for (RealmUserModel model : users) {
                try {
                    if (model.getId().startsWith("guest"))
                        continue;
                    JsonObject jsonDoc = apiInterface.getJsonObject(Utilities.getHeader(), Utilities.getUrl() + "/shelf/" + model.get_id()).execute().body();
                    JsonObject object = getShelfData(realm, model.getId(), jsonDoc);
                    Utilities.log("JSON " + new Gson().toJson(jsonDoc));
                    JsonObject d = apiInterface.getJsonObject(Utilities.getHeader(), Utilities.getUrl() + "/shelf/" + model.getId()).execute().body();
                    object.addProperty("_rev", JsonUtils.getString("_rev", d));
                    apiInterface.putDoc(Utilities.getHeader(), "application/json", Utilities.getUrl() + "/shelf/" + sharedPreferences.getString("userId", ""), object).execute().body();
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onSuccess("Unable to update documents.");
                }
            }
        }, () -> listener.onSuccess("Sync with server completed successfully"));
    }


    public JsonObject getShelfData(Realm realm, String userId, JsonObject jsonDoc) {
        JsonArray myLibs = RealmMyLibrary.getMyLibIds(realm, userId);
        JsonArray myCourses = RealmMyCourse.getMyCourseIds(realm, userId);
//        JsonArray myTeams = RealmMyTeam.getMyTeamIds(realm, userId);
        JsonArray myMeetups = RealmMeetup.getMyMeetUpIds(realm, userId);

        List<String> removedResources = Arrays.asList(RealmRemovedLog.removedIds(realm, "resources", userId));
        List<String> removedCourses = Arrays.asList(RealmRemovedLog.removedIds(realm, "courses", userId));

        JsonArray mergedResourceIds = mergeJsonArray(myLibs, JsonUtils.getJsonArray("resourceIds", jsonDoc), removedResources);
        JsonArray mergedCoueseIds = mergeJsonArray(myCourses, JsonUtils.getJsonArray("courseIds", jsonDoc), removedCourses);

        JsonObject object = new JsonObject();


        object.addProperty("_id", sharedPreferences.getString("userId", ""));
        object.add("meetupIds", mergeJsonArray(myMeetups, JsonUtils.getJsonArray("meetupIds", jsonDoc), removedResources));
        object.add("resourceIds", mergedResourceIds);
        object.add("courseIds", mergedCoueseIds);
//        object.add("myTeamIds", mergeJsonArray(myTeams, JsonUtils.getJsonArray("myTeamIds", jsonDoc), removedResources));
        return object;
    }


    public JsonArray mergeJsonArray(JsonArray array1, JsonArray array2, List<String> removedIds) {
        JsonArray array = new JsonArray();
        array.addAll(array1);
        for (JsonElement e : array2) {
            if (!array.contains(e) && !removedIds.contains(e.getAsString())) {
                array.add(e);
            }
        }
        return array;
    }
}
