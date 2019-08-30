package org.ole.planet.myplanet.ui.team.teamTask;


import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.model.RealmTeamTask;
import org.ole.planet.myplanet.ui.team.AdapterTeam;
import org.ole.planet.myplanet.ui.team.BaseTeamFragment;
import org.ole.planet.myplanet.utilities.TimeUtils;
import org.ole.planet.myplanet.utilities.Utilities;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import io.realm.Realm;

/**
 * A simple {@link Fragment} subclass.
 */
public class TeamTaskFragment extends BaseTeamFragment implements AdapterTask.OnCompletedListener {

    RecyclerView rvTask;
    Calendar deadline;
    TextView datePicker;

    DatePickerDialog.OnDateSetListener listener = (view, year, monthOfYear, dayOfMonth) -> {
        deadline = Calendar.getInstance();
        deadline.set(Calendar.YEAR, year);
        deadline.set(Calendar.MONTH, monthOfYear);
        deadline.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        if (datePicker != null)
            datePicker.setText(TimeUtils.formatDateTZ(deadline.getTimeInMillis()));
    };

    public TeamTaskFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_team_task, container, false);
        rvTask = v.findViewById(R.id.rv_task);
        v.findViewById(R.id.fab).setOnClickListener(view -> {
            showTaskAlert();
        });
        return v;
    }

    private void showTaskAlert() {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.alert_task, null);
        EditText title = v.findViewById(R.id.et_task);
        EditText description = v.findViewById(R.id.et_description);
        datePicker = v.findViewById(R.id.tv_pick);
        Calendar myCalendar = Calendar.getInstance();
        datePicker.setOnClickListener(view -> new DatePickerDialog(getActivity(), listener, myCalendar
                .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)).show());

        new AlertDialog.Builder(getActivity()).setTitle("Add Task").setView(v).setPositiveButton("Save", (dialogInterface, i) -> {
            String task = title.getText().toString();
            String desc = description.getText().toString();
            if (task.isEmpty())
                Utilities.toast(getActivity(), "Task title is required");
            else if (deadline == null)
                Utilities.toast(getActivity(), "Deadline is required");
            else
                createNewTask(task, desc);
        }).setNegativeButton("Cancel", null).show();
    }

    private void createNewTask(String task, String desc) {
        mRealm.executeTransactionAsync(realm -> {
            RealmTeamTask t = realm.createObject(RealmTeamTask.class, UUID.randomUUID().toString());
            t.setTitle(task);
            t.setDescription(desc);
            t.setDeadline(TimeUtils.formatDateTZ(deadline.getTimeInMillis()));
            t.setTeamId(teamId);
            JsonObject ob = new JsonObject();
            ob.addProperty("teams", teamId);
            JsonObject links = new JsonObject();
            links.add("links", ob);
            t.setLink(new Gson().toJson(links));
            JsonObject obsync = new JsonObject();
            ob.addProperty("type", "local");
            ob.addProperty("planetCode", user.getPlanetCode());
            JsonObject sync = new JsonObject();
            sync.add("sync", obsync);
            t.setSync(new Gson().toJson(sync));
            t.setCompleted(false);

        }, () -> {
            if (rvTask.getAdapter() != null)
                rvTask.getAdapter().notifyDataSetChanged();
            Utilities.toast(getActivity(), "Task added successfully");
        });
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        rvTask.setLayoutManager(new LinearLayoutManager(getActivity()));
        List<RealmTeamTask> list = mRealm.where(RealmTeamTask.class).equalTo("teamId", teamId).findAll();
        Utilities.log("List size " + list.size());
        AdapterTask adapterTask = new AdapterTask(getActivity(), list);
        adapterTask.setListener(this);
        rvTask.setAdapter(adapterTask);
    }

    @Override
    public void onCheckChange(RealmTeamTask realmTeamTask, boolean b) {
        Utilities.log("cHECK CHANGED");
        if (!mRealm.isInTransaction())
            mRealm.beginTransaction();
        realmTeamTask.setCompleted(b);
        mRealm.commitTransaction();
    }
}
