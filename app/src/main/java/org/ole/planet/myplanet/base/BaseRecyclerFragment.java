package org.ole.planet.myplanet.base;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.callback.OnRatingChangeListener;
import org.ole.planet.myplanet.datamanager.DatabaseService;
import org.ole.planet.myplanet.model.RealmMyCourse;
import org.ole.planet.myplanet.model.RealmMyLibrary;
import org.ole.planet.myplanet.model.RealmRemovedLog;
import org.ole.planet.myplanet.model.RealmStepExam;
import org.ole.planet.myplanet.model.RealmUserModel;
import org.ole.planet.myplanet.service.UserProfileDbHandler;
import org.ole.planet.myplanet.utilities.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;

import static android.content.Context.MODE_PRIVATE;

public abstract class BaseRecyclerFragment<LI> extends android.support.v4.app.Fragment implements OnRatingChangeListener {

    public static final String PREFS_NAME = "OLE_PLANET";
    public static SharedPreferences settings;
    public List<LI> selectedItems;
    public Realm mRealm;
    public DatabaseService realmService;
    public UserProfileDbHandler profileDbHandler;
    public RealmUserModel model;
    public RecyclerView recyclerView;
    TextView tvMessage;
    List<LI> list;

    public BaseRecyclerFragment() {
    }

    public abstract int getLayout();

    public abstract RecyclerView.Adapter getAdapter();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(getLayout(), container, false);
        settings = getActivity().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        recyclerView = v.findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        tvMessage = v.findViewById(R.id.tv_message);
        selectedItems = new ArrayList<>();
        list = new ArrayList<>();
        realmService = new DatabaseService(getActivity());
        mRealm = realmService.getRealmInstance();
        profileDbHandler = new UserProfileDbHandler(getActivity());
        model = mRealm.copyToRealmOrUpdate(profileDbHandler.getUserModel());
        recyclerView.setAdapter(getAdapter());
        return v;
    }

    @Override
    public void onRatingChanged() {
        recyclerView.setAdapter(getAdapter());
    }

    public void addToMyList() {
        for (int i = 0; i < selectedItems.size(); i++) {
            RealmObject object = (RealmObject) selectedItems.get(i);
            if (object instanceof RealmMyLibrary) {
                RealmMyLibrary myObject = mRealm.where(RealmMyLibrary.class).equalTo("resourceId", ((RealmMyLibrary) object).getResource_id()).findFirst();
                RealmMyLibrary.createFromResource(myObject, mRealm, model.getId());
                RealmRemovedLog.onAdd(mRealm, "resources", profileDbHandler.getUserModel().getId(), myObject.getResourceId());
                Utilities.toast(getActivity(), "Added to my library");
            } else {
                RealmMyCourse myObject = RealmMyCourse.getMyCourse(mRealm, ((RealmMyCourse) object).getCourseId());
                RealmMyCourse.createMyCourse(myObject, mRealm, model.getId());
                RealmRemovedLog.onAdd(mRealm, "courses", profileDbHandler.getUserModel().getId(), myObject.getCourseId());
                Utilities.toast(getActivity(), "Added to my courses");
                recyclerView.setAdapter(getAdapter());
            }
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    public List<LI> search(String s, Class c) {
        if (s.isEmpty()) {
            return getList(c);
        }
        return mRealm.where(c).contains(c == RealmMyLibrary.class ? "title" : "courseTitle", s, Case.INSENSITIVE).findAll();
    }

    public List<RealmMyLibrary> filterByTag(String[] tags) {
        List<RealmMyLibrary> list = (List<RealmMyLibrary>) getList(RealmMyLibrary.class);
        if (tags.length == 0) {
            return list;
        }
        Arrays.sort(tags);
        RealmList<RealmMyLibrary> libraries = new RealmList<>();
        for (RealmMyLibrary library : list) {
            if (filter(tags, library)) {
                libraries.add(library);
            }
        }
        return libraries;

    }

    private boolean filter(String[] tags, RealmMyLibrary library) {
        boolean contains = true;
        for (String s : tags) {
            if (!library.getTag().contains(s)) {
                contains = false;
                break;
            }
        }
        return contains;
    }


    public List<LI> getList(Class c) {
        if (c == RealmStepExam.class) {
            return mRealm.where(c).equalTo("type", "surveys").findAll();
        } else {
            return mRealm.where(c).findAll();
        }
    }

}