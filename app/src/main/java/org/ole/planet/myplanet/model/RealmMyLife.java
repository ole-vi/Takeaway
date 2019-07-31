package org.ole.planet.myplanet.model;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.ole.planet.myplanet.utilities.JsonUtils;
import org.ole.planet.myplanet.utilities.Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.PrimaryKey;

public class RealmMyLife extends RealmObject {
    private int weight;
    @PrimaryKey
    private String _id;
    private int imageId;
    private String userId;
    private String title;
    private int isVisible;

    public static List<RealmMyLife> getMyLifeByUserId(Realm mRealm, SharedPreferences settings) {
        String userId = settings.getString("userId", "--");
        return getMyLifeByUserId(mRealm, userId);
    }

    public static List<RealmMyLife> getMyLifeByUserId(Realm mRealm, String userId) {
        return mRealm.where(RealmMyLife.class).equalTo("userId", userId).findAll().sort("weight");
    }

    public static void createMyLife(RealmMyLife myLife, Realm mRealm, String _id) {
        if (!mRealm.isInTransaction())
            mRealm.beginTransaction();
        myLife.set_id(_id);
        mRealm.commitTransaction();
    }

    public RealmMyLife(int imageId, String userId, String title) {
        this.imageId = imageId;
        this.userId = userId;
        this.title = title;
        this.isVisible = 1;
    }

    public RealmMyLife() {
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public int getImageId() {
        return imageId;
    }

    public void setImageId(int imageId) {
        this.imageId = imageId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getIsVisible() {
        return isVisible;
    }

    public void setIsVisible(int isVisible) {
        this.isVisible = isVisible;
    }

    public static void updateWeight(int weight, String title, Realm realm, String userId) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                int currentWeight = -1;
                List<RealmMyLife> myLifeList = realm.where(RealmMyLife.class).findAll();
                for (RealmMyLife item : myLifeList) {
                    if (item.getUserId().contains(userId)) {
                        if (item.getTitle().contains(title)) {
                            currentWeight = item.getWeight();
                            item.setWeight(weight);
                        }
                    }
                }
                for (RealmMyLife item : myLifeList) {
                    if (item.getUserId().contains(userId)) {
                        if (currentWeight != -1 && item.getWeight() == weight && !item.getTitle().contains(title)) {
                            item.setWeight(currentWeight);
                        }
                    }
                }
            }
        });

    }
}
