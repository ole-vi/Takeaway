package org.ole.planet.takeout.Data;

import android.text.TextUtils;

import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.ole.planet.takeout.utilities.TimeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class realm_meetups extends RealmObject {
    @PrimaryKey
    private String id;
    private String userId;
    private String meetupId;
    private String meetupId_rev;
    private String title;
    private String description;
    private String startDate;
    private String endDate;
    private String recurring;
    private String Day;
    private String startTime;
    private String endTime;
    private String category;
    private String meetupLocation;
    private String creator;

    public static void insertMyMeetups(String userId, String meetupID, JsonObject meetupDoc, Realm mRealm) {

        realm_meetups myMeetupsDB = mRealm.createObject(realm_meetups.class, UUID.randomUUID().toString());
        myMeetupsDB.setUserId(userId);
        myMeetupsDB.setMeetupId(meetupID);
        myMeetupsDB.setMeetupId_rev(meetupDoc.get("_rev").getAsString());
        myMeetupsDB.setTitle(meetupDoc.get("title").getAsString());
        myMeetupsDB.setDescription(meetupDoc.get("description").getAsString());
        myMeetupsDB.setStartDate(meetupDoc.get("startDate").getAsString());
        myMeetupsDB.setEndDate(meetupDoc.get("endDate").getAsString());
        myMeetupsDB.setRecurring(meetupDoc.get("recurring").getAsString());
        if (meetupDoc.has("day"))
            myMeetupsDB.setDay(meetupDoc.get("day").getAsJsonArray().toString());
        myMeetupsDB.setStartTime(meetupDoc.get("startTime").getAsString());
        if (meetupDoc.has("endTime"))
            myMeetupsDB.setStartTime(meetupDoc.get("endTime").getAsString());
        myMeetupsDB.setCategory(meetupDoc.get("category").getAsString());
        myMeetupsDB.setMeetupLocation(meetupDoc.get("meetupLocation").getAsString());
        myMeetupsDB.setCreator(meetupDoc.get("creator").getAsString());
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMeetupId() {
        return meetupId;
    }

    public void setMeetupId(String meetupId) {
        this.meetupId = meetupId;
    }

    public String getMeetupId_rev() {
        return meetupId_rev;
    }

    public void setMeetupId_rev(String meetupId_rev) {
        this.meetupId_rev = meetupId_rev;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getRecurring() {
        return recurring;
    }

    public void setRecurring(String recurring) {
        this.recurring = recurring;
    }

    public String getDay() {
        return Day;
    }

    public void setDay(String day) {
        Day = day;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getMeetupLocation() {
        return meetupLocation;
    }

    public void setMeetupLocation(String meetupLocation) {
        this.meetupLocation = meetupLocation;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public static HashMap<String, String> getHashMap(realm_meetups meetups) {
        HashMap<String, String> map = new HashMap<>();
        map.put("Meetup Title", checkNull(meetups.getTitle()));
        map.put("Created By", checkNull(meetups.getCreator()));
        map.put("Category", checkNull(meetups.getCategory()));
        map.put("Meetup Date", TimeUtils.getFormatedDate(Long.parseLong(meetups.getStartDate())) + " - " + TimeUtils.getFormatedDate(Long.parseLong(meetups.getEndDate())));
        map.put("Meetup Time", checkNull(meetups.getStartTime()) + " - " + checkNull(meetups.getEndTime()));
        map.put("Recurring", checkNull(meetups.getRecurring()));

        String recurringDays = "";
        try {
            JSONArray ar = new JSONArray(meetups.getDay());
            for (int i = 0; i < ar.length(); i++) {
                recurringDays += ar.get(i).toString() + ", ";
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        map.put("Recurring Days", checkNull(recurringDays));
        map.put("Location", checkNull(meetups.getMeetupLocation()));
        map.put("Description", checkNull(meetups.getDescription()));
        return map;
    }

    public static String[] getJoinedUserIds(Realm mRealm) {
        List<realm_meetups> list = mRealm.where(realm_meetups.class).isNotEmpty("userId").findAll();
        String[] myIds = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            myIds[i] = list.get(i).getUserId();
        }
        return myIds;
    }


    public static String checkNull(String s) {
        return TextUtils.isEmpty(s) ? "" : s;
    }
}
