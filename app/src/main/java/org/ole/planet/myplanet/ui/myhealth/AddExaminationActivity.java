package org.ole.planet.myplanet.ui.myhealth;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.gson.Gson;

import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.datamanager.DatabaseService;
import org.ole.planet.myplanet.model.RealmMyHealth;
import org.ole.planet.myplanet.model.RealmMyHealthPojo;
import org.ole.planet.myplanet.model.RealmUserModel;
import org.ole.planet.myplanet.service.UserProfileDbHandler;
import org.ole.planet.myplanet.utilities.AndroidDecrypter;
import org.ole.planet.myplanet.utilities.Utilities;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import io.realm.Realm;

public class AddExaminationActivity extends AppCompatActivity {
    Realm mRealm;
    String userId;
    EditText etTemperature, etPulseRate, etBloodPressure, etHeight, etWeight, etVision, etHearing,
            etObservation, etDiag, etTretments, etMedications, etImmunization, etAllergies, etXray, etLabtest, etReferrals;
    RealmUserModel user;
    RealmMyHealthPojo pojo;
    RealmMyHealth health = null;

    private void initViews() {
        etTemperature = findViewById(R.id.et_temperature);
        etPulseRate = findViewById(R.id.et_pulse_rate);
        etBloodPressure = findViewById(R.id.et_bloodpressure);
        etHeight = findViewById(R.id.et_height);
        etWeight = findViewById(R.id.et_weight);
        etVision = findViewById(R.id.et_vision);
        etHearing = findViewById(R.id.et_hearing);
        etObservation = findViewById(R.id.et_observation);
        etDiag = findViewById(R.id.et_diag);
        etTretments = findViewById(R.id.et_treatments);
        etMedications = findViewById(R.id.et_medications);
        etImmunization = findViewById(R.id.et_immunization);
        etAllergies = findViewById(R.id.et_allergies);
        etXray = findViewById(R.id.et_xray);
        etLabtest = findViewById(R.id.et_labtest);
        etReferrals = findViewById(R.id.et_referrals);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_examination);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initViews();
        user = new UserProfileDbHandler(this).getUserModel();
        mRealm = new DatabaseService(this).getRealmInstance();
        userId = user.getId();
        pojo = mRealm.where(RealmMyHealthPojo.class).equalTo("_id", userId).findFirst();

        if (pojo != null) {
            health = new Gson().fromJson(AndroidDecrypter.decrypt(pojo.getData(), user.getKey(), user.getIv()), RealmMyHealth.class);
        }
        if (health == null || health.getProfile() == null) {
            health = new RealmMyHealth();
            RealmMyHealth.RealmMyHealthProfile profile = new RealmMyHealth.RealmMyHealthProfile();
            profile.setFirstName(user.getFirstName());
            profile.setMiddleName(user.getMiddleName());
            profile.setLastName(user.getLastName());
            profile.setLanguage(user.getLanguage());
            profile.setBirthDate(user.getDob());
            profile.setBirthplace(user.getBirthPlace());
            profile.setEmail(user.getEmail());
            profile.setPhone(user.getPhoneNumber());
            health.setProfile(profile);
        }
        findViewById(R.id.btn_save).setOnClickListener(view -> {
            saveData();
        });
    }

    private void saveData() {
        try {
            RealmExamination sign = new RealmExamination();
            sign.setAllergies(etAllergies.getText().toString());
            sign.setBp(etBloodPressure.getText().toString());
            sign.setTemperature(etTemperature.getText().toString());
            sign.setPulse(etPulseRate.getText().toString());
            sign.setWeight(etWeight.getText().toString());
            sign.setDiagnosis(etDiag.getText().toString());
            sign.setHearing(etHearing.getText().toString());
            sign.setHeight(etHeight.getText().toString());
            sign.setImmunizations(etImmunization.getText().toString());
            sign.setTests(etLabtest.getText().toString());
            sign.setXrays(etXray.getText().toString());
            sign.setVision(etVision.getText().toString());
            sign.setTreatments(etTretments.getText().toString());
            sign.setReferrals(etReferrals.getText().toString());
            sign.setNotes(etObservation.getText().toString());
            sign.setMedications(etMedications.getText().toString());
            sign.setDate(new Date().getTime());
            List<RealmExamination> list = health.getEvents();
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(sign);
            health.setEvents(list);
            if (!mRealm.isInTransaction())
                mRealm.beginTransaction();
            if (pojo == null){
                pojo = mRealm.createObject(RealmMyHealthPojo.class, userId);
            }
            pojo.setData(AndroidDecrypter.encrypt(new Gson().toJson(health), user.getKey(), user.getIv()));
            mRealm.commitTransaction();
            Utilities.toast(this, "Added successfully");
            finish();
        } catch (Exception e) {
            Utilities.toast(this, "All fields are required");
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return super.onOptionsItemSelected(item);
    }
}
