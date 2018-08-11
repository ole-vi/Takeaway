package org.ole.planet.takeout.base;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.ole.planet.takeout.Data.Download;
import org.ole.planet.takeout.Data.realm_myLibrary;
import org.ole.planet.takeout.DownloadFiles;
import org.ole.planet.takeout.R;
import org.ole.planet.takeout.SyncActivity;
import org.ole.planet.takeout.callback.OnHomeItemClickListener;
import org.ole.planet.takeout.utilities.DialogUtils;
import org.ole.planet.takeout.utilities.Utilities;

import java.util.ArrayList;

import io.realm.RealmResults;

import static android.content.Context.MODE_PRIVATE;
import static org.ole.planet.takeout.Dashboard.MESSAGE_PROGRESS;

public abstract class BaseContainerFragment extends Fragment {
    public static SharedPreferences settings;
    static ProgressDialog prgDialog;
    public OnHomeItemClickListener homeItemClickListener;
    ArrayList<Integer> selectedItemsList = new ArrayList<>();
    ListView lv;
    View convertView;
    ArrayList<String> names = new ArrayList<>();
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MESSAGE_PROGRESS) && prgDialog != null) {
                Download download = intent.getParcelableExtra("download");
                if (!download.isFailed()) {
                    setProgress(download);
                } else {
                    DialogUtils.showError(prgDialog, download.getMessage());
                }
            }
        }
    };

    public void createListView(RealmResults<realm_myLibrary> db_myLibrary) {
        lv = (ListView) convertView.findViewById(R.id.alertDialog_listView);
        for (int i = 0; i < db_myLibrary.size(); i++) {
            names.add(db_myLibrary.get(i).getTitle().toString());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity().getBaseContext(), R.layout.rowlayout, R.id.checkBoxRowLayout, names);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String itemSelected = ((TextView) view).getText().toString();
                if (selectedItemsList.contains(itemSelected)) {
                    selectedItemsList.remove(itemSelected);
                } else {
                    selectedItemsList.add(i);
                }
                Toast.makeText(getContext(), "Clicked on  : " + itemSelected + "Number " + i, Toast.LENGTH_SHORT).show();
            }
        });
    }

    protected void showDownloadDialog(final RealmResults<realm_myLibrary> db_myLibrary) {

        if (!db_myLibrary.isEmpty()) {
            LayoutInflater inflater = getLayoutInflater();
            convertView = (View) inflater.inflate(R.layout.my_library_alertdialog, null);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
            alertDialogBuilder.setView(convertView).setTitle(R.string.download_suggestion);
            createListView(db_myLibrary);
            alertDialogBuilder.setPositiveButton(R.string.download_selected, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    startDownload(DownloadFiles.downloadFiles(db_myLibrary, selectedItemsList, settings));
                }
            }).setNeutralButton(R.string.download_all, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    startDownload(DownloadFiles.downloadAllFiles(db_myLibrary, settings));
                }
            }).setNegativeButton(R.string.txt_cancel, null).show();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prgDialog = DialogUtils.getProgressDialog(getActivity());
        settings = getActivity().getSharedPreferences(SyncActivity.PREFS_NAME, MODE_PRIVATE);
        registerReceiver();
    }


    public void startDownload(ArrayList urls) {
        if (!urls.isEmpty()) {
            prgDialog.show();
            Utilities.openDownloadService(getActivity(), urls);
        }
    }


    public void setProgress(Download download) {
        prgDialog.setProgress(download.getProgress());
        if (!TextUtils.isEmpty(download.getFileName())) {
            prgDialog.setTitle(download.getFileName());
        }
        if (download.isCompleteAll()) {
            DialogUtils.showError(prgDialog, "All files downloaded successfully");
        }
    }

    private void registerReceiver() {
        LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(getActivity());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MESSAGE_PROGRESS);
        bManager.registerReceiver(broadcastReceiver, intentFilter);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnHomeItemClickListener) {
            homeItemClickListener = (OnHomeItemClickListener) context;
        }
    }
}
