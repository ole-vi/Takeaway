package org.ole.planet.myplanet.ui.myhealth;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.borax12.materialdaterangepicker.date.AccessibleDateAnimator;
import com.google.gson.Gson;

import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.datamanager.DatabaseService;
import org.ole.planet.myplanet.model.RealmMyHealth;
import org.ole.planet.myplanet.model.RealmMyHealthPojo;
import org.ole.planet.myplanet.model.RealmUserModel;
import org.ole.planet.myplanet.service.UserProfileDbHandler;
import org.ole.planet.myplanet.ui.userprofile.BecomeMemberActivity;
import org.ole.planet.myplanet.utilities.AndroidDecrypter;
import org.ole.planet.myplanet.utilities.Constants;
import org.ole.planet.myplanet.utilities.Utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * A simple {@link Fragment} subclass.
 */
public class MyHealthFragment extends Fragment {

    public UserProfileDbHandler profileDbHandler;
    RecyclerView rvRecord;
    Button fab, btnNewPatient, btnUpdateRecord;
    TextView lblName;
    String userId;
    Realm mRealm;
    RealmUserModel userModel;
    AlertDialog dialog;
    TextView txtFullname, txtEmail, txtLanguage, txtDob, txtBirthPlace, txtEmergency, txtSpecial, txtOther, txtMessage;
    LinearLayout llUserDetail;

    public MyHealthFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_vital_sign, container, false);
        rvRecord = v.findViewById(R.id.rv_records);
        lblName = v.findViewById(R.id.lblHealthName);
        txtFullname = v.findViewById(R.id.txt_full_name);
        txtEmail = v.findViewById(R.id.txt_email);
        txtLanguage = v.findViewById(R.id.txt_language);
        txtDob = v.findViewById(R.id.txt_dob);
        txtBirthPlace = v.findViewById(R.id.txt_birth_place);
        txtEmergency = v.findViewById(R.id.txt_emergency_contact);
        txtSpecial = v.findViewById(R.id.txt_special_needs);
        txtOther = v.findViewById(R.id.txt_other_need);
        llUserDetail = v.findViewById(R.id.layout_user_detail);
        txtMessage = v.findViewById(R.id.tv_message);
        mRealm = new DatabaseService(getActivity()).getRealmInstance();
        fab = v.findViewById(R.id.add_new_record);
        fab.setOnClickListener(view -> {
            startActivity(new Intent(getActivity(), AddExaminationActivity.class).putExtra("userId", userId));
        });
        btnUpdateRecord = v.findViewById(R.id.update_health);
        btnUpdateRecord.setOnClickListener(view -> startActivity(new Intent(getActivity(), AddMyHealthActivity.class).putExtra("userId", userId)));
        btnNewPatient = v.findViewById(R.id.btnnew_patient);
        v.findViewById(R.id.fab_add_member).setOnClickListener(view -> startActivity(new Intent(getActivity(), BecomeMemberActivity.class)));
        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        View v = getLayoutInflater().inflate(R.layout.alert_users_spinner, null);
        rvRecord.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
        profileDbHandler = new UserProfileDbHandler(v.getContext());
        userId = profileDbHandler.getUserModel().getId();
        getHealthRecords(userId);
//        selectPatient();
        btnNewPatient.setOnClickListener(view -> selectPatient());
        btnNewPatient.setVisibility(Constants.showBetaFeature(Constants.KEY_HEALTHWORKER, getActivity()) ? View.VISIBLE : View.GONE);
    }

    private void getHealthRecords(String memberId) {
        userId = memberId;
        userModel = mRealm.where(RealmUserModel.class).equalTo("id", userId).findFirst();
        lblName.setText(userModel.getFullName());
        showRecords();
    }

    private void selectPatient() {
        RealmResults<RealmUserModel> userModelList = mRealm.where(RealmUserModel.class).findAll();
        List<String> memberFullNameList = new ArrayList<>();
        HashMap<String, String> map = new HashMap<>();
        for (RealmUserModel um : userModelList) {
            memberFullNameList.add(um.getName());
            map.put(um.getName(), um.getId());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, memberFullNameList);
        View alertHealth = LayoutInflater.from(getActivity()).inflate(R.layout.alert_health_list, null);
        EditText etSearch = alertHealth.findViewById(R.id.et_search);
        setTextWatcher(etSearch, adapter);
        ListView lv = alertHealth.findViewById(R.id.list);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener((adapterView, view, i, l) -> {
            String user = ((TextView) view).getText().toString();
            userId = map.get(user);
            Utilities.log(userId);
            getHealthRecords(userId);
            dialog.dismiss();
        });
        dialog = new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.select_health_member))
                .setView(alertHealth)
                .setCancelable(false).setNegativeButton("Dismiss", null).create();
        dialog.show();

    }

    private void setTextWatcher(EditText etSearch, ArrayAdapter<String> adapter) {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                adapter.getFilter().filter(charSequence);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        showRecords();
    }

    private void showRecords() {
        llUserDetail.setVisibility(View.VISIBLE);
        txtMessage.setVisibility(View.GONE);
        txtFullname.setText(userModel.getFirstName() + " " + userModel.getMiddleName() + " " + userModel.getLastName());
        txtEmail.setText(TextUtils.isEmpty(userModel.getEmail()) ? "N/A" : userModel.getEmail());
        txtLanguage.setText(TextUtils.isEmpty(userModel.getLanguage()) ? "N/A" : userModel.getLanguage());
        txtDob.setText(TextUtils.isEmpty(userModel.getDob()) ? "N/A" : userModel.getDob());
        RealmMyHealthPojo mh = mRealm.where(RealmMyHealthPojo.class).equalTo("_id", userId).findFirst();
        if (mh != null) {
            RealmMyHealth mm = getHealthProfile(mh);
            if (mm == null) {
                Utilities.toast(getActivity(), "Health Record not available.");
                return;
            }
            RealmMyHealth.RealmMyHealthProfile myHealths = mm.getProfile();
            txtOther.setText(TextUtils.isEmpty(myHealths.getNotes()) ? "N/A" : myHealths.getNotes());
            txtSpecial.setText(TextUtils.isEmpty(myHealths.getSpecialNeeds()) ? "N/A" : myHealths.getSpecialNeeds());
            txtBirthPlace.setText(TextUtils.isEmpty(myHealths.getBirthplace()) ? "N/A" : myHealths.getBirthplace());
            txtEmergency.setText("Name : " + myHealths.getEmergencyContactName() + "\nType : " + myHealths.getEmergencyContactName() + "\nContact : " + myHealths.getEmergencyContact());
            List<RealmExamination> list = mm.getEvents();
            rvRecord.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
            rvRecord.setNestedScrollingEnabled(false);
            AdapterHealthExamination adapter = new AdapterHealthExamination(getActivity(), list, mh, userModel);
            adapter.setmRealm(mRealm);
            rvRecord.setAdapter(adapter);
            List<RealmExamination> finalList = list;
            rvRecord.post(() -> rvRecord.scrollToPosition(finalList.size() - 1));
        } else {
            txtOther.setText("");
            txtSpecial.setText("");
            txtBirthPlace.setText("");
            txtEmergency.setText("");
            rvRecord.setAdapter(null);
//            llUserDetail.setVisibility(View.GONE);
//            txtMessage.setText(R.string.no_records);
//            txtMessage.setVisibility(View.VISIBLE);
        }
    }

    private RealmMyHealth getHealthProfile(RealmMyHealthPojo mh) {
        String json = TextUtils.isEmpty(userModel.getIv()) ? mh.getData() : AndroidDecrypter.decrypt(mh.getData(), userModel.getKey(), userModel.getIv());
        if (TextUtils.isEmpty(json)) {
            if (!userModel.getRealm().isInTransaction()) {
                userModel.getRealm().beginTransaction();
            }
            userModel.setIv("");
            userModel.setKey("");
            userModel.getRealm().commitTransaction();
            return null;
        } else {
            Utilities.log("Json " + json);
            try {
                return new Gson().fromJson(json, RealmMyHealth.class);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

}
