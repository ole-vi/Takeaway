package org.ole.planet.myplanet.service;


import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import org.ole.planet.myplanet.MainApplication;
import org.ole.planet.myplanet.utilities.NetworkUtils;

public class StayOnLineService extends JobService {

    @Override
    public boolean onStartJob(JobParameters job) {
        if (NetworkUtils.isWifiConnected())
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("SHOW_WIFI_ALERT"));
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }
}
