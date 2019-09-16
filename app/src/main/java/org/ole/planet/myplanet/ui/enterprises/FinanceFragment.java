package org.ole.planet.myplanet.ui.enterprises;


import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;

import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.datamanager.DatabaseService;
import org.ole.planet.myplanet.model.RealmMyTeam;
import org.ole.planet.myplanet.model.RealmUserModel;
import org.ole.planet.myplanet.service.UserProfileDbHandler;
import org.ole.planet.myplanet.ui.team.BaseTeamFragment;
import org.ole.planet.myplanet.utilities.TimeUtils;
import org.ole.planet.myplanet.utilities.Utilities;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import io.realm.Realm;

/**
 * A simple {@link Fragment} subclass.
 */
public class FinanceFragment extends BaseTeamFragment {

    RecyclerView rvFinance;
    FloatingActionButton fab;
    TextView nodata;
    Realm mRealm;
    AdapterFinance adapterFinance;
    TextInputLayout tlNote;
    Spinner spnType;
    TextInputLayout tlAmount;
    Calendar date;
    TextView tvSelectDate;

    public FinanceFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_finance, container, false);
        mRealm = new DatabaseService(getActivity()).getRealmInstance();
        rvFinance = v.findViewById(R.id.rv_finance);
        fab = v.findViewById(R.id.add_transaction);
        nodata = v.findViewById(R.id.tv_nodata);
        date = Calendar.getInstance();
        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        fab.setOnClickListener(view -> addTransaction());
        List<RealmMyTeam> list = mRealm.where(RealmMyTeam.class).equalTo("teamId", teamId).equalTo("docType", "transaction").findAll();
        adapterFinance = new AdapterFinance(getActivity(), list);
        rvFinance.setLayoutManager(new LinearLayoutManager(getActivity()));
        rvFinance.setAdapter(adapterFinance);
        showNoData(nodata, list.size());
    }

    DatePickerDialog.OnDateSetListener listener = (view, year, monthOfYear, dayOfMonth) -> {
        date = Calendar.getInstance();
        date.set(Calendar.YEAR, year);
        date.set(Calendar.MONTH, monthOfYear);
        date.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        if (date != null)
            tvSelectDate.setText(TimeUtils.formatDateTZ(date.getTimeInMillis()));
    };

    private void addTransaction() {
        new AlertDialog.Builder(getActivity()).setView(setUpAlertUi())
                .setTitle("Add Transaction")
                .setPositiveButton("Submit", (dialogInterface, i) -> {
                    String type = spnType.getSelectedItem().toString();
                    String note = tlNote.getEditText().getText().toString();
                    String amount = tlAmount.getEditText().getText().toString();

                    if (note.isEmpty()) {
                        Utilities.toast(getActivity(), "Note is required");
                    } else if (amount.isEmpty()) {
                        Utilities.toast(getActivity(), "Amount is required");
                    } else if (date == null) {
                        Utilities.toast(getActivity(), "Date is required");
                    } else {
                        mRealm.executeTransactionAsync(realm -> {
                            createTransactionObject(realm,type, note, amount);
                        }, () -> {
                            Utilities.toast(getActivity(), "Transaction added");
                            adapterFinance.notifyDataSetChanged();
                            showNoData(nodata, adapterFinance.getItemCount());
                        });
                    }
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void createTransactionObject(Realm realm, String type, String note, String amount) {
        RealmMyTeam team = realm.createObject(RealmMyTeam.class, UUID.randomUUID().toString());
        team.setStatus("active");
        team.setDate(new Date().getTime());
        if (type != null)
            team.setTeamType(type);
        team.setType(type);
        team.setDescription(note);
        team.setTeamId(teamId);
        team.setAmount(Integer.parseInt(amount));
        team.setParentCode(user.getParentCode());
        team.setTeamPlanetCode(user.getPlanetCode());
        team.setTeamType("sync");
        team.setDocType("transaction");

    }


    private View setUpAlertUi() {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.add_transaction, null);
        spnType = v.findViewById(R.id.spn_type);
        tlNote = v.findViewById(R.id.tl_note);
        tlAmount = v.findViewById(R.id.tl_amount);
        tvSelectDate = v.findViewById(R.id.tv_select_date);
        tvSelectDate.setOnClickListener(view -> new DatePickerDialog(getActivity(), listener, date
                .get(Calendar.YEAR), date.get(Calendar.MONTH),
                date.get(Calendar.DAY_OF_MONTH)).show());

        return v;
    }
}
