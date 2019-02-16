package org.ole.planet.myplanet.ui.survey;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.base.BaseDialogFragment;
import org.ole.planet.myplanet.datamanager.DatabaseService;
import org.ole.planet.myplanet.model.RealmExamQuestion;
import org.ole.planet.myplanet.model.RealmSubmission;
import org.ole.planet.myplanet.model.RealmUserModel;
import org.ole.planet.myplanet.utilities.Utilities;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.Sort;

/**
 * A simple {@link Fragment} subclass.
 */
public class SendSurveyFragment extends BaseDialogFragment {

    ListView listView;
    Realm mRealm;
    DatabaseService dbService;
    ArrayList<Integer> selectedItemsList = new ArrayList<>();

    public SendSurveyFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_send_survey, container, false);

        listView = v.findViewById(R.id.list_users);
        dbService = new DatabaseService(getActivity());
        mRealm = dbService.getRealmInstance();
        if (TextUtils.isEmpty(id)) {
            dismiss();
            return v;
        }
        v.findViewById(R.id.btn_cancel).setOnClickListener(view -> dismiss());
        return v;
    }

    private void createSurveySubmission(String userId) {
        Realm mRealm = new DatabaseService(getActivity()).getRealmInstance();
        List<RealmExamQuestion> questions = mRealm.where(RealmExamQuestion.class).equalTo("examId", id).findAll();
        mRealm.beginTransaction();
        RealmSubmission sub = mRealm.where(RealmSubmission.class)
                .equalTo("userId", userId)
                .equalTo("parentId", id)
                .sort("lastUpdateTime", Sort.DESCENDING)
                .equalTo("status", "pending")
                .findFirst();
        sub = RealmSubmission.createSubmission(sub, questions, mRealm);
        sub.setParentId(id);
        sub.setUserId(userId);
        sub.setType("survey");
        sub.setStatus("pending");
        sub.setStartTime(new Date().getTime());
        mRealm.commitTransaction();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        List<RealmUserModel> users = mRealm.where(RealmUserModel.class).findAll();
        initListView(users);
        getView().findViewById(R.id.send_survey).setOnClickListener(view -> {
            for (int i = 0; i < selectedItemsList.size(); i++) {
                RealmUserModel u = users.get(i);
                createSurveySubmission(u.getId());
            }
            Utilities.toast(getActivity(), "Survey sent to users");
            dismiss();
        });
    }

    private void initListView(List<RealmUserModel> users) {
        ArrayAdapter<RealmUserModel> adapter = new ArrayAdapter<RealmUserModel>(getActivity(), R.layout.rowlayout, R.id.checkBoxRowLayout, users);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this::onItemClick);
    }

    @Override
    protected String getKey() {
        return "surveyId";
    }

    private void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String itemSelected = ((TextView) view).getText().toString();
        if (selectedItemsList.contains(itemSelected)) {
            selectedItemsList.remove(itemSelected);
        } else {
            selectedItemsList.add(i);
        }
    }
}
