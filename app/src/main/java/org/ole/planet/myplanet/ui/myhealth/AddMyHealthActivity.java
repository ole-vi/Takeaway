package org.ole.planet.myplanet.ui.myhealth;

import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Spinner;

import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.datamanager.DatabaseService;
import org.ole.planet.myplanet.model.RealmMyHealth;
import org.ole.planet.myplanet.model.RealmUserModel;
import org.ole.planet.myplanet.utilities.Utilities;

import java.util.UUID;

import io.realm.Realm;

public class AddMyHealthActivity extends AppCompatActivity {
    Realm realm;
    TextInputLayout fname, mname, lname, email, phone, birthplace, birthdate, emergencyNumber, contact, specialNeed, otherNeed;
    Spinner contactType;
    RealmMyHealth userModel;
    RealmUserModel userModelB;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_my_health);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        realm = new DatabaseService(this).getRealmInstance();
        userId = getIntent().getStringExtra("userId");
        userModel = realm.where(RealmMyHealth.class).equalTo("userId", userId).findFirst();
        if(userModel == null){
            userModelB = realm.where(RealmUserModel.class).equalTo("id", userId).findFirst();
        }
        initViews();
        findViewById(R.id.btn_submit).setOnClickListener(view -> realm.executeTransactionAsync(realm -> createExamination(realm), () -> Utilities.toast(AddMyHealthActivity.this, "My health saved successfully")));
    }

    private void createExamination(Realm realm) {
        RealmMyHealth health = realm.where(RealmMyHealth.class).equalTo("userId",userId).findFirst();
        if(health == null) health = realm.createObject(RealmMyHealth.class, UUID.randomUUID().toString());
        health.setFirstName(fname.getEditText().getText().toString());
        health.setMiddleName(mname.getEditText().getText().toString());
        health.setLastName(lname.getEditText().getText().toString());
        health.setEmail(email.getEditText().getText().toString());
        health.setBirthDate(birthdate.getEditText().getText().toString());
        health.setBirthPlace(birthplace.getEditText().getText().toString());
        health.setEmergency(emergencyNumber.getEditText().getText().toString());
        health.setContact(contact.getEditText().getText().toString());
        health.setContactType(contactType.getSelectedItem().toString());
        health.setSpecialNeeds(specialNeed.getEditText().getText().toString());
        health.setOtherNeeds(otherNeed.getEditText().getText().toString());
        health.setUserId(userId);
        Log.e("TAG", "Created with "+ userId + " health " + health.getId());
        finish();
    }

    private void initViews() {
        fname = findViewById(R.id.et_fname);
        mname = findViewById(R.id.et_mname);
        lname = findViewById(R.id.et_lname);
        email = findViewById(R.id.et_email);
        phone = findViewById(R.id.et_phone);
        birthplace = findViewById(R.id.et_birthplace);
        birthdate = findViewById(R.id.et_birthdate);
        emergencyNumber = findViewById(R.id.et_emergency);
        contact = findViewById(R.id.et_contact);
        specialNeed = findViewById(R.id.et_special_need);
        otherNeed = findViewById(R.id.et_other_need);
        contactType = findViewById(R.id.spn_contact_type);
        if (userModel != null) {
            fname.getEditText().setText(userModel.getFirstName());
            mname.getEditText().setText(userModel.getMiddleName());
            lname.getEditText().setText(userModel.getLastName());
            email.getEditText().setText(userModel.getEmail());
            phone.getEditText().setText(userModel.getPhone());
            emergencyNumber.getEditText().setText(userModel.getEmergency());
            birthdate.getEditText().setText(userModel.getBirthDate());
            birthplace.getEditText().setText(userModel.getBirthPlace());
            specialNeed.getEditText().setText(userModel.getSpecialNeeds());
            otherNeed.getEditText().setText(userModel.getOtherNeeds());
        }else if (userModelB != null){
            fname.getEditText().setText(userModelB.getFirstName());
            mname.getEditText().setText(userModelB.getMiddleName());
            lname.getEditText().setText(userModelB.getLastName());
            email.getEditText().setText(userModelB.getEmail());
            phone.getEditText().setText(userModelB.getPhoneNumber());
            birthdate.getEditText().setText(userModelB.getDob());
            birthplace.getEditText().setText(userModelB.getBirthPlace());
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (realm != null)
            realm.close();
    }
}
