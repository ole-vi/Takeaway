package org.ole.planet.myplanet.ui.sync;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.ActionMenuItemView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.ole.planet.myplanet.MainApplication;
import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.callback.OnRatingChangeListener;
import org.ole.planet.myplanet.service.UserProfileDbHandler;
import org.ole.planet.myplanet.ui.SettingActivity;
import org.ole.planet.myplanet.ui.course.CourseFragment;
import org.ole.planet.myplanet.ui.dashboard.BellDashboardFragment;
import org.ole.planet.myplanet.ui.dashboard.DashboardActivity;
import org.ole.planet.myplanet.ui.dashboard.DashboardFragment;
import org.ole.planet.myplanet.ui.feedback.FeedbackFragment;
import org.ole.planet.myplanet.ui.library.LibraryFragment;
import org.ole.planet.myplanet.ui.rating.RatingFragment;
import org.ole.planet.myplanet.ui.survey.SurveyFragment;
import org.ole.planet.myplanet.utilities.Constants;
import org.ole.planet.myplanet.utilities.Utilities;

import static org.ole.planet.myplanet.ui.dashboard.DashboardFragment.PREFS_NAME;

/**
 * Extra class for excess methods in DashboardActivity activities
 */

public abstract class DashboardElementActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {
    public BottomNavigationView navigationView;

    public UserProfileDbHandler profileDbHandler;
    private SharedPreferences settings;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileDbHandler = new UserProfileDbHandler(this);
        settings = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

    }

    public void onClickTabItems(int position) {
        switch (position) {
            case 0:
                openCallFragment(new BellDashboardFragment(), "dashboard");
                break;
            case 1:
                openCallFragment(new LibraryFragment(), "library");
                break;
            case 2:
                openCallFragment(new CourseFragment(), "course");
                break;
            case 3:
                openCallFragment(new SurveyFragment(), "survey");
                break;
            case 4:
                new FeedbackFragment().show(getSupportFragmentManager(), "feedback");
                break;
            case 5:
                logout();
                break;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dashboard, menu);
        return true;
    }


    public void openCallFragment(Fragment newfragment, String tag) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, newfragment, tag);
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        fragmentTransaction.addToBackStack("");
        fragmentTransaction.commit();
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_goOnline).setVisible(Constants.showBetaFeature(Constants.KEY_SYNC, this));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
//        if (id == R.id.menu_profile) {
//            return true;
//        }
        if (id == R.id.menu_goOnline) {
            wifiStatusSwitch();
        } else if (id == R.id.menu_logout) {
            logout();
        } else if (id == R.id.action_setting) {
            startActivity(new Intent(this, SettingActivity.class));
        } else if (id == R.id.action_sync) {
            startActivity(new Intent(this, LoginActivity.class).putExtra("forceSync", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("RestrictedApi")
    private void wifiStatusSwitch() {
        ActionMenuItemView goOnline = findViewById(R.id.menu_goOnline);
        Drawable resIcon = getResources().getDrawable(R.drawable.goonline);
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (mWifi.isConnected()) {
            wifi.setWifiEnabled(false);
            resIcon.mutate().setColorFilter(getApplicationContext().getResources().getColor(R.color.green), PorterDuff.Mode.SRC_ATOP);
            goOnline.setIcon(resIcon);
            Toast.makeText(this, "Wifi is turned Off. Saving battery power", Toast.LENGTH_LONG).show();
        } else {
            wifi.setWifiEnabled(true);
            Toast.makeText(this, "Turning on Wifi. Please wait...", Toast.LENGTH_LONG).show();
            (new Handler()).postDelayed(this::connectToWifi, 5000);
            resIcon.mutate().setColorFilter(getApplicationContext().getResources().getColor(R.color.accent), PorterDuff.Mode.SRC_ATOP);
            goOnline.setIcon(resIcon);


        }
    }

    private void connectToWifi() {
        int id = settings.getInt("LastWifiID", -1);
        Utilities.log("LAST SSID " + id);
        WifiManager wifiManager = (WifiManager) MainApplication.context.getSystemService(WIFI_SERVICE);
        int netId = -1;
        if (wifiManager == null) {
            Utilities.toast(this, "Unable to connect to planet wifi.");
            return;
        }

        for (WifiConfiguration tmp : wifiManager.getConfiguredNetworks()) {
            if (tmp.networkId > -1 && tmp.networkId == id) {
                netId = tmp.networkId;
                wifiManager.enableNetwork(netId, true);
                Toast.makeText(this, "You are now connected " + netId, Toast.LENGTH_SHORT).show();
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("ACTION_NETWORK_CHANGED"));
                break;
            }
            Utilities.log("SSID " + tmp.SSID);
        }

    }

    public void logout() {
        profileDbHandler.onLogout();
        settings.edit().putBoolean(Constants.KEY_LOGIN, false).commit();
        Intent loginscreen = new Intent(this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(loginscreen);
        doubleBackToExitPressedOnce = true;
        this.finish();
    }


    boolean doubleBackToExitPressedOnce;

    @Override
    public void finish() {
        if (doubleBackToExitPressedOnce) {
            super.finish();
        } else {
            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Press BACK again to exit", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
        }
    }


    public void showRatingDialog(String type, String resource_id, String title, OnRatingChangeListener listener) {
        RatingFragment f = RatingFragment.newInstance(type, resource_id, title);
        f.setListener(listener);
        f.show(getSupportFragmentManager(), "");
    }

    @Override
    public void onBackStackChanged() {
        Fragment f = (getSupportFragmentManager()).findFragmentById(R.id.fragment_container);
        String fragmentTag = f.getTag();
        if (f instanceof CourseFragment) {
            if ("shelf".equals(fragmentTag))
                navigationView.getMenu().findItem(R.id.menu_mycourses).setChecked(true);
            else
                navigationView.getMenu().findItem(R.id.menu_courses).setChecked(true);
        } else if (f instanceof LibraryFragment) {
            if ("shelf".equals(fragmentTag))
                navigationView.getMenu().findItem(R.id.menu_mylibrary).setChecked(true);
            else
                navigationView.getMenu().findItem(R.id.menu_library).setChecked(true);
        } else if (f instanceof DashboardFragment) {
            navigationView.getMenu().findItem(R.id.menu_home).setChecked(true);
        } else if (f instanceof SurveyFragment) {
            // navigationView.getMenu().findItem(R.id.menu_survey).setChecked(true);
        }

    }

}
