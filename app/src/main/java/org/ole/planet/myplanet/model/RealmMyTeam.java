package org.ole.planet.myplanet.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.ole.planet.myplanet.MainApplication;
import org.ole.planet.myplanet.ui.sync.SyncActivity;
import org.ole.planet.myplanet.utilities.JsonUtils;
import org.ole.planet.myplanet.utilities.Utilities;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.PrimaryKey;

public class RealmMyTeam extends RealmObject {
    @PrimaryKey
    private String id;
    private String _id;
    private RealmList<String> userIds;
    private RealmList<String> courses;
    private String teamId;
    private String name;
    private String userId;
    private String description;
    private String requests;
    private String limit;
    private long createdDate;
    private String status;
    private String teamType;
    private String teamPlanetCode;
    private String docType;

    public static void insertMyTeams(String userId, JsonObject doc, Realm mRealm) {
        String teamId = JsonUtils.getString("_id", doc);
        RealmMyTeam myTeams = mRealm.where(RealmMyTeam.class).equalTo("id", teamId).findFirst();
        if (myTeams == null) {
            myTeams = mRealm.createObject(RealmMyTeam.class, teamId);
        }
        myTeams.setUserId(userId);
        myTeams.setUser_id(JsonUtils.getString("userId",doc));
        myTeams.setTeamId(JsonUtils.getString("teamId", doc));
        myTeams.set_id("_id");
        myTeams.setName(JsonUtils.getString("name", doc));
        myTeams.setDescription(JsonUtils.getString("description", doc));
        myTeams.setLimit(JsonUtils.getString("limit", doc));
        myTeams.setStatus(JsonUtils.getString("status", doc));
        myTeams.setTeamPlanetCode(JsonUtils.getString("teamPlanetCode", doc));
        myTeams.setCreatedDate(JsonUtils.getLong("createdDate", doc));
        myTeams.setTeamType(JsonUtils.getString("teamType", doc));
        myTeams.setRequests(JsonUtils.getJsonArray("requests", doc).toString());
        myTeams.setDocType(JsonUtils.getString("docType", doc).toString());
        JsonArray coursesArray = JsonUtils.getJsonArray("courses", doc);
        myTeams.courses = new RealmList<>();
        for (JsonElement e : coursesArray) {
            String id = e.getAsJsonObject().get("_id").getAsString();
            if (!myTeams.courses.contains(id))
                myTeams.courses.add(id);
        }
    }

    public static void insert(Realm mRealm, JsonObject doc) {
        insertMyTeams("", doc, mRealm);
    }

    public static void requestToJoin(String teamId, RealmUserModel userModel, Realm mRealm) {
        if (!mRealm.isInTransaction())
            mRealm.beginTransaction();
        RealmMyTeam team = mRealm.createObject(RealmMyTeam.class, UUID.randomUUID().toString());
        team.setDocType("request");
        team.setCreatedDate(new Date().getTime());
        team.setTeamType("sync");
        team.setUser_id(userModel.getId());
        team.setTeamId(teamId);
        team.setTeamPlanetCode(userModel.getPlanetCode());
        mRealm.commitTransaction();
    }

    public void leave(RealmUserModel user) {
            this.userIds.remove(user.getId());
    }

    public void setUser_id(String user_id) {
        this.userId = user_id;
    }

    public String getUser_id() {
        return this.userId;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(long createdDate) {
        this.createdDate = createdDate;
    }

    public String getTeamType() {
        return teamType;
    }

    public void setTeamType(String teamType) {
        this.teamType = teamType;
    }

    public String getTeamPlanetCode() {
        return teamPlanetCode;
    }

    public void setTeamPlanetCode(String teamPlanetCode) {
        this.teamPlanetCode = teamPlanetCode;
    }

    public static List<RealmObject> getMyTeamsByUserId(Realm mRealm, SharedPreferences settings) {
        RealmResults<RealmMyTeam> libs = mRealm.where(RealmMyTeam.class).findAll();
        return getMyTeamByUserId(settings.getString("userId", "--"), libs);
    }


    public static List<RealmObject> getMyTeamByUserId(String userId, List<RealmMyTeam> tm) {
        List<RealmObject> teams = new ArrayList<>();
        for (RealmMyTeam item : tm) {
            if (item.getUserId().contains(userId)) {
                teams.add(item);
            }
        }
        return teams;
    }

    public RealmList<String> getUserId() {
        return userIds;
    }

    public RealmList<String> getCourses() {
        return courses;
    }

    public void setCourses(RealmList<String> courses) {
        this.courses = courses;
    }

    public void setUserId(RealmList<String> userId) {
        this.userIds = userId;
    }

    public static JsonArray getMyTeamIds(Realm realm, String userId) {
        List<RealmObject> myLibraries = getMyTeamByUserId(userId, realm.where(RealmMyTeam.class).findAll());
        JsonArray ids = new JsonArray();
        for (RealmObject lib : myLibraries
        ) {
            ids.add(((RealmMyTeam) lib).getId());
        }
        return ids;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequests() {
        return requests;
    }

    public void setRequests(String requests) {
        this.requests = requests;
    }

    public String getLimit() {
        return limit;
    }

    public void setLimit(String limit) {
        this.limit = limit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setUserId(String userId) {
        if (this.userIds == null) {
            this.userIds = new RealmList<>();
        }

        if (!this.userIds.contains(userId) && !TextUtils.isEmpty(userId))
            this.userIds.add(userId);
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public boolean isMyTeam(String id) {
        Utilities.log(new Gson().toJson(this.userIds));
        return this.userIds != null && this.userIds.contains(id);
    }

    public boolean requested(String userId, Realm mRealm) {
        return mRealm.where(RealmMyTeam.class).equalTo("docType", "request").equalTo("teamId", this._id).equalTo("userId", userId).count() > 0;
    }
}
