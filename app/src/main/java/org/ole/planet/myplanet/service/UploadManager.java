package org.ole.planet.myplanet.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.github.kittinunf.fuel.android.core.Json;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONObject;
import org.ole.planet.myplanet.MainApplication;
import org.ole.planet.myplanet.callback.SuccessListener;
import org.ole.planet.myplanet.datamanager.ApiClient;
import org.ole.planet.myplanet.datamanager.ApiInterface;
import org.ole.planet.myplanet.datamanager.DatabaseService;
import org.ole.planet.myplanet.datamanager.FileUploadService;
import org.ole.planet.myplanet.datamanager.ManagerSync;
import org.ole.planet.myplanet.model.MyPlanet;
import org.ole.planet.myplanet.model.RealmAchievement;
import org.ole.planet.myplanet.model.RealmApkLog;
import org.ole.planet.myplanet.model.RealmCourseProgress;
import org.ole.planet.myplanet.model.RealmFeedback;
import org.ole.planet.myplanet.model.RealmMyPersonal;
import org.ole.planet.myplanet.model.RealmMyTeam;
import org.ole.planet.myplanet.model.RealmNews;
import org.ole.planet.myplanet.model.RealmOfflineActivity;
import org.ole.planet.myplanet.model.RealmRating;
import org.ole.planet.myplanet.model.RealmResourceActivity;
import org.ole.planet.myplanet.model.RealmSubmission;
import org.ole.planet.myplanet.model.RealmTeamLog;
import org.ole.planet.myplanet.model.RealmUserModel;
import org.ole.planet.myplanet.ui.sync.SyncActivity;
import org.ole.planet.myplanet.utilities.FileUtils;
import org.ole.planet.myplanet.utilities.JsonUtils;
import org.ole.planet.myplanet.utilities.NetworkUtils;
import org.ole.planet.myplanet.utilities.Utilities;
import org.ole.planet.myplanet.utilities.VersionUtils;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadManager extends FileUploadService {
    private DatabaseService dbService;
    private Realm mRealm;
    private static UploadManager instance;
    Context context;
    SharedPreferences pref;

    public static UploadManager getInstance() {
        if (instance == null) {
            instance = new UploadManager(MainApplication.context);
        }
        return instance;
    }

    public UploadManager(Context context) {
        dbService = new DatabaseService(context);
        this.context = context;
        pref = context.getSharedPreferences(SyncActivity.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void uploadActivities(SuccessListener listener) {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        RealmUserModel model = new UserProfileDbHandler(MainApplication.context).getUserModel();
        if (model.isManager())
            return;
        try {
            apiInterface.postDoc(Utilities.getHeader(), "application/json", Utilities.getUrl() + "/myplanet_activities", MyPlanet.getMyPlanetActivities(context, pref, model)).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (listener != null) {
                        listener.onSuccess("My planet activities uploaded successfully");
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                }
            });
        } catch (Exception e) {
        }
    }


    public void uploadExamResult(final SuccessListener listener) {
        mRealm = dbService.getRealmInstance();
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        mRealm.executeTransactionAsync(realm -> {
            List<RealmSubmission> submissions = realm.where(RealmSubmission.class).equalTo("status", "graded").equalTo("uploaded", false).findAll();
            for (RealmSubmission sub : submissions) {
                try {
                    RealmSubmission.continueResultUpload(sub, apiInterface, realm);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, () -> listener.onSuccess("Result sync completed successfully"));
        uploadCourseProgress();
    }

    public void uploadAchievement() {
        mRealm = dbService.getRealmInstance();
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        mRealm.executeTransactionAsync(realm -> {
            List<RealmAchievement> list = realm.where(RealmAchievement.class).findAll();
            for (RealmAchievement sub : list) {
                try {
                    if (sub.get_id().startsWith("guest"))
                        continue;
                    JsonObject ob = apiInterface.putDoc(Utilities.getHeader(), "application/json", Utilities.getUrl() + "/achievements/" + sub.get_id(), RealmAchievement.serialize(sub)).execute().body();
                    if (ob == null) {
                        ResponseBody re = apiInterface.postDoc(Utilities.getHeader(), "application/json", Utilities.getUrl() + "/achievements", RealmAchievement.serialize(sub)).execute().errorBody();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public void uploadCourseProgress() {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        mRealm = dbService.getRealmInstance();
        mRealm.executeTransactionAsync(realm -> {
            List<RealmCourseProgress> data = realm.where(RealmCourseProgress.class)
                    .isNull("_id").findAll();
            for (RealmCourseProgress sub : data) {
                try {
                    if (sub.getUserId().startsWith("guest"))
                        continue;
                    JsonObject object = apiInterface.postDoc(Utilities.getHeader(), "application/json", Utilities.getUrl() + "/courses_progress", RealmCourseProgress.serializeProgress(sub)).execute().body();
                    if (object != null) {
                        sub.set_id(JsonUtils.getString("id", object));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public void uploadFeedback(final SuccessListener listener) {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        mRealm = dbService.getRealmInstance();
        mRealm.executeTransactionAsync(realm -> {
            List<RealmFeedback> feedbacks = realm.where(RealmFeedback.class).equalTo("uploaded", false).findAll();
            for (RealmFeedback feedback : feedbacks) {
                try {
                    JsonObject object = apiInterface.postDoc(Utilities.getHeader(), "application/json", Utilities.getUrl() + "/feedback", RealmFeedback.serializeFeedback(feedback)).execute().body();
                    if (object != null) {
                        feedback.setUploaded(true);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }, () -> listener.onSuccess("Feedback sync completed successfully"));
    }


    public void uploadMyPersonal(RealmMyPersonal personal, SuccessListener listener) {
        mRealm = new DatabaseService(context).getRealmInstance();
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        if (!personal.isUploaded()) {
            apiInterface.postDoc(Utilities.getHeader(), "application/json", Utilities.getUrl() + "/resources", RealmMyPersonal.serialize(personal)).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    JsonObject object = response.body();
                    if (object != null) {
                        if (!mRealm.isInTransaction())
                            mRealm.beginTransaction();
                        String _rev = JsonUtils.getString("rev", object);
                        String _id = JsonUtils.getString("id", object);
                        personal.setUploaded(true);
                        personal.set_rev(_rev);
                        personal.set_id(_id);
                        mRealm.commitTransaction();
                        uploadAttachment(_id, _rev, personal, listener);
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    listener.onSuccess("Unable to upload resource");
                }
            });

        }
    }


    public void uploadTeams() {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        mRealm = dbService.getRealmInstance();
        mRealm.executeTransactionAsync(realm -> {
            List<RealmMyTeam> teams = realm.where(RealmMyTeam.class).isEmpty("_id").isNotEmpty("teamId").findAll();
            for (RealmMyTeam team : teams) {
                try {
                    JsonObject object = apiInterface.postDoc(Utilities.getHeader(), "application/json", Utilities.getUrl() + "/teams", RealmMyTeam.serialize(team)).execute().body();
                    if (object != null) {
                        team.set_id(JsonUtils.getString("id", object));
                        team.set_rev(JsonUtils.getString("rev", object));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }


    public void uploadUserActivities(final SuccessListener listener) {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        mRealm = dbService.getRealmInstance();
        RealmUserModel model = new UserProfileDbHandler(MainApplication.context).getUserModel();
        if (model.isManager())
            return;
        mRealm.executeTransactionAsync(realm -> {
            final RealmResults<RealmOfflineActivity> activities = realm.where(RealmOfflineActivity.class)
                    .isNull("_rev").equalTo("type", "login").findAll();
            for (RealmOfflineActivity act : activities) {
                try {
                    if (act.getUserId().startsWith("guest"))
                        continue;
                    JsonObject object = apiInterface.postDoc(Utilities.getHeader(), "application/json", Utilities.getUrl() + "/login_activities", RealmOfflineActivity.serializeLoginActivities(act)).execute().body();
                    act.changeRev(object);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            uploadTeamActivities(realm, apiInterface);
        }, () -> listener.onSuccess("Sync with server completed successfully"));
    }

    private void uploadTeamActivities(Realm realm, ApiInterface apiInterface) {
        final RealmResults<RealmTeamLog> logs = realm.where(RealmTeamLog.class)
                .equalTo("uploaded", false).findAll();
        for (RealmTeamLog log : logs) {
            try {
                JsonObject object = apiInterface.postDoc(Utilities.getHeader(), "application/json", Utilities.getUrl() + "/team_activities", RealmTeamLog.serializeTeamActivities(log)).execute().body();
                if (object != null)
                    log.setUploaded(true);
            } catch (IOException e) {
            }

        }
    }

    public void uploadRating(final SuccessListener listener) {
        mRealm = dbService.getRealmInstance();
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        mRealm.executeTransactionAsync(realm -> {
            final RealmResults<RealmRating> activities = realm.where(RealmRating.class).equalTo("isUpdated", true).findAll();
            for (RealmRating act : activities) {
                try {
                    if (act.getUserId().startsWith("guest"))
                        continue;
                    Response<JsonObject> object;
                    if (TextUtils.isEmpty(act.get_id())) {
                        object = apiInterface.postDoc(Utilities.getHeader(), "application/json", Utilities.getUrl() + "/ratings", RealmRating.serializeRating(act)).execute();
                    } else {
                        object = apiInterface.putDoc(Utilities.getHeader(), "application/json", Utilities.getUrl() + "/ratings/" + act.get_id(), RealmRating.serializeRating(act)).execute();
                    }
                    if (object.body() != null) {
                        act.set_id(JsonUtils.getString("id", object.body()));
                        act.set_rev(JsonUtils.getString("rev", object.body()));
                        act.setUpdated(false);
                    }
                } catch (Exception e) {
                }
            }
        });
    }

    public void uploadNews() {
        mRealm = dbService.getRealmInstance();
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        RealmUserModel userModel = new UserProfileDbHandler(context).getUserModel();
        mRealm.executeTransactionAsync(realm -> {
            final RealmResults<RealmNews> activities = realm.where(RealmNews.class).isNull("_id").or().isEmpty("_id").findAll();
            for (RealmNews act : activities) {
                try {
                    if (act.getUserId().startsWith("guest"))
                        continue;
                    Response<JsonObject> object;
                    Utilities.log(RealmNews.serializeNews(act, userModel).toString());
                    if (TextUtils.isEmpty(act.get_id())) {
                        object = apiInterface.postDoc(Utilities.getHeader(), "application/json", Utilities.getUrl() + "/news", RealmNews.serializeNews(act, userModel)).execute();
                    } else {
                        object = apiInterface.putDoc(Utilities.getHeader(), "application/json", Utilities.getUrl() + "/news/" + act.get_id(), RealmNews.serializeNews(act, userModel)).execute();
                    }
                    if (object.body() != null) {
                        act.set_id(JsonUtils.getString("id", object.body()));
                        act.set_rev(JsonUtils.getString("rev", object.body()));
                    }
                } catch (Exception e) {
                }
            }
        });
    }

    public void uploadCrashLog(final SuccessListener listener) {
        mRealm = dbService.getRealmInstance();
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        mRealm.executeTransactionAsync(realm -> {
            RealmResults<RealmApkLog> logs;
            logs = realm.where(RealmApkLog.class).isNull("_rev").findAll();
            for (RealmApkLog act : logs) {
                try {
                    JsonObject o = apiInterface.postDoc(Utilities.getHeader(), "application/json", Utilities.getUrl() + "/apk_logs", RealmApkLog.serialize(act)).execute().body();
                    if (o != null) act.set_rev(JsonUtils.getString("rev", o));
                } catch (IOException e) {
                }
            }
        }, () -> listener.onSuccess("Crash log uploaded."));
    }

    public void uploadResourceActivities(String type) {
        mRealm = dbService.getRealmInstance();
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);

        String db = type.equals("sync") ? "admin_activities" : "resource_activities";
        mRealm.executeTransactionAsync(realm -> {
            RealmResults<RealmResourceActivity> activities;
            if (type.equals("sync")) {
                activities = realm.where(RealmResourceActivity.class).isNull("_rev").equalTo("type", "sync").findAll();
            } else {
                activities = realm.where(RealmResourceActivity.class).isNull("_rev").notEqualTo("type", "sync").findAll();
            }
            for (RealmResourceActivity act : activities) {
                try {
                    JsonObject object = apiInterface.postDoc(Utilities.getHeader(), "application/json", Utilities.getUrl() + "/" + db, RealmResourceActivity.serializeResourceActivities(act)).execute().body();
                    if (object != null) {
                        act.set_rev(JsonUtils.getString("rev", object));
                        act.set_id(JsonUtils.getString("id", object));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

}
