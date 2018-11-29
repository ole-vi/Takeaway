package org.ole.planet.myplanet.userprofile;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.ole.planet.myplanet.Data.realm_stepExam;
import org.ole.planet.myplanet.Data.realm_submissions;
import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.datamanager.DatabaseService;

import java.util.HashMap;
import java.util.List;

import io.realm.Realm;

/**
 * A simple {@link Fragment} subclass.
 */
public class MySubmissionFragment extends Fragment {
    Realm mRealm;
    RecyclerView rvSurvey;
    String type = "";
    HashMap<String, realm_stepExam> exams;

    public static Fragment newInstance(String type) {
        MySubmissionFragment fragment = new MySubmissionFragment();
        Bundle b = new Bundle();
        b.putString("type", type);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
            type = getArguments().getString("type");
    }

    public MySubmissionFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_my_submission, container, false);
        exams = new HashMap<>();

        rvSurvey = v.findViewById(R.id.rv_mysurvey);
        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRealm = new DatabaseService(getActivity()).getRealmInstance();
        List<realm_submissions> submissions = mRealm.where(realm_submissions.class).findAll();
        createHashMap(submissions);
        if (type.equals("survey")) {
            submissions = mRealm.where(realm_submissions.class).equalTo("type", "survey").findAll();
        } else {
            submissions = mRealm.where(realm_submissions.class).notEqualTo("type", "survey").findAll();
        }
        rvSurvey.setLayoutManager(new LinearLayoutManager(getActivity()));
        rvSurvey.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
        AdapterMySubmission adapter = new AdapterMySubmission(getActivity(), submissions, exams);
        adapter.setType(type);
        rvSurvey.setAdapter(adapter);

    }

    private void createHashMap(List<realm_submissions> submissions) {
        for (realm_submissions sub : submissions) {
            realm_stepExam survey = mRealm.where(realm_stepExam.class).equalTo("id", sub.getParentId()).findFirst();
            if (survey != null)
                exams.put(sub.getParentId(), survey);
        }
    }
}
