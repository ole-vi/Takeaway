package org.ole.planet.myplanet;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.kittinunf.fuel.Fuel;
import com.github.kittinunf.fuel.core.FuelError;
import com.github.kittinunf.fuel.core.Handler;
import com.github.kittinunf.fuel.core.Request;
import com.github.kittinunf.fuel.core.Response;

import org.ole.planet.myplanet.Data.Download;
import org.ole.planet.myplanet.Data.MyPlanet;
import org.ole.planet.myplanet.callback.SuccessListener;
import org.ole.planet.myplanet.datamanager.ApiClient;
import org.ole.planet.myplanet.datamanager.ApiInterface;
import org.ole.planet.myplanet.datamanager.Service;
import org.ole.planet.myplanet.service.AutoSyncService;
import org.ole.planet.myplanet.service.UploadManager;
import org.ole.planet.myplanet.userprofile.UserProfileDbHandler;
import org.ole.planet.myplanet.utilities.DialogUtils;
import org.ole.planet.myplanet.utilities.FileUtils;
import org.ole.planet.myplanet.utilities.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageButton;
import retrofit2.Call;
import retrofit2.Callback;

import static org.ole.planet.myplanet.Dashboard.MESSAGE_PROGRESS;


public class LoginActivity extends SyncActivity implements Service.CheckVersionCallback {
    boolean connectionResult;
    EditText serverUrl;
    EditText serverPassword;
    Fuel ful = new Fuel();
    private EditText inputName, inputPassword;
    private TextInputLayout inputLayoutName, inputLayoutPassword;
    private Button btnSignIn;
    private ImageButton imgBtnSetting;
    private View positiveAction;
    private GifDrawable gifDrawable;
    private GifImageButton syncIcon;
    private CheckBox save;
    private static final int PERMISSION_REQUEST_CODE_FILE = 111;
    private static final int PERMISSION_REQUEST_CODE_CAMERA = 112;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        changeLogoColor();
        inputLayoutName = findViewById(R.id.input_layout_name);
        inputLayoutPassword = findViewById(R.id.input_layout_password);
        imgBtnSetting = findViewById(R.id.imgBtnSetting);
        save = findViewById(R.id.save);
        declareElements();
        declareMoreElements();
        if (getIntent().getBooleanExtra("showWifiDialog", false)) {
            DialogUtils.showWifiSettingDialog(this);
        }
        requestPermission();
        new Service(this).checkVersion(this, settings);
        btnSignIn = findViewById(R.id.btn_signin); //buttons
        btnSignIn.setOnClickListener(view -> submitForm());
        registerReceiver();
    }

    public boolean checkPermission(String strPermission) {
        int result = ContextCompat.checkSelfPermission(this, strPermission);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    public void requestPermission(String strPermission, int perCode) {
        ActivityCompat.requestPermissions(this, new String[]{strPermission}, perCode);
    }

    public void requestPermission() {
        if (!checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) || !checkPermission(Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE_FILE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("Main Activity", "onRequestPermissionsResult: permission granted");
        } else {
            Utilities.toast(this, "Download and camera Function will not work, please grant the permission.");
        }
    }

    public void changeLogoColor() {
        ImageView logo = findViewById(R.id.logoImageView);
        final int newColor = getResources().getColor(android.R.color.white);
        int alpha = Math.round(Color.alpha(newColor) * 10);
        int red = Color.red(newColor);
        int green = Color.green(newColor);
        int blue = Color.blue(newColor);
        int alphaWhite = Color.argb(alpha, red, green, blue);
        logo.setColorFilter(alphaWhite, PorterDuff.Mode.SRC_ATOP);
    }

    public void declareElements() {
        //Settings button
        imgBtnSetting.setOnClickListener(view -> {
            MaterialDialog.Builder builder = new MaterialDialog.Builder(LoginActivity.this);
            builder.title(R.string.action_settings).customView(R.layout.dialog_server_url_, true)
                    .positiveText(R.string.btn_sync).negativeText(R.string.btn_sync_cancel).neutralText(R.string.btn_sync_save)
                    .onPositive((dialog, which) -> continueSync(dialog)).onNeutral((dialog, which) -> saveConfigAndContinue(dialog));
            settingDialog(builder);
        });
    }


    private void continueSync(MaterialDialog dialog) {
        String processedUrl = saveConfigAndContinue(dialog);
        if (TextUtils.isEmpty(processedUrl)) return;
        try {
            isServerReachable(processedUrl);
        } catch (Exception e) {
            DialogUtils.showAlert(LoginActivity.this, "Unable to sync", "Please enter valid url.");
        }
    }


    public void declareMoreElements() {
        syncIcon = findViewById(R.id.syncIcon);
        syncIcon.setImageResource(R.drawable.sync_icon);
        syncIcon.getScaleType();
        gifDrawable = (GifDrawable) syncIcon.getDrawable();
        gifDrawable.setSpeed(3.0f);
        gifDrawable.stop();
        syncIcon.setOnClickListener(v -> {
            gifDrawable.reset();
            startUpload();
        });
        declareHideKeyboardElements();
        inputName = findViewById(R.id.input_name);//editText
        inputPassword = findViewById(R.id.input_password);
        inputName.addTextChangedListener(new MyTextWatcher(inputName));
        inputPassword.addTextChangedListener(new MyTextWatcher(inputPassword));

        if (settings.getBoolean("saveUsernameAndPassword", false)) {
            inputName.setText(settings.getString("loginUserName", ""));
            inputPassword.setText(settings.getString("loginUserPassword", ""));
            save.setChecked(true);
        }
    }


    /**
     * Form  Validation
     */
    private void submitForm() {
        SharedPreferences.Editor editor = settings.edit();
        if (!validateEditText(inputName, inputLayoutName, getString(R.string.err_msg_name))) {
            return;
        }
        if (!validateEditText(inputPassword, inputLayoutPassword, getString(R.string.err_msg_password))) {
            return;
        }
        editor.putBoolean("saveUsernameAndPassword", save.isChecked());
        if (save.isChecked()) {
            editor.putString("loginUserName", inputName.getText().toString());
            editor.putString("loginUserPassword", inputPassword.getText().toString());
        }
        editor.commit();
        if (authenticateUser(settings, inputName.getText().toString(), inputPassword.getText().toString(), this)) {
            Toast.makeText(getApplicationContext(), "Thank You!", Toast.LENGTH_SHORT).show();
            UserProfileDbHandler handler = new UserProfileDbHandler(this);
            handler.onLogin();
            handler.onDestory();
            Intent dashboard = new Intent(getApplicationContext(), Dashboard.class);
            startActivity(dashboard);
        } else {
            alertDialogOkay(getString(R.string.err_msg_login));
        }
    }

    public void settingDialog(MaterialDialog.Builder builder) {
        MaterialDialog dialog = builder.build();
        positiveAction = dialog.getActionButton(DialogAction.POSITIVE);
        serverUrl = dialog.getCustomView().findViewById(R.id.input_server_url);
        serverPassword = dialog.getCustomView().findViewById(R.id.input_server_Password);
        serverUrl.setText(settings.getString("serverURL", ""));
        serverPassword.setText(settings.getString("serverPin", ""));
        serverUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //action before text change
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                positiveAction.setEnabled(s.toString().trim().length() > 0 && URLUtil.isValidUrl(s.toString()));
            }

            @Override
            public void afterTextChanged(Editable s) {
                //action after text change
            }
        });
        dialog.show();
        sync(dialog);
    }

    public boolean isServerReachable(String processedUrl) throws Exception {
        progressDialog.setMessage("Connecting to server....");
        progressDialog.show();
        ful.get(processedUrl + "/_all_dbs").responseString(new Handler<String>() {
            @Override
            public void success(Request request, Response response, String s) {
                try {
                    progressDialog.dismiss();
                    List<String> myList = Arrays.asList(s.split(","));
                    if (myList.size() < 8) {
                        alertDialogOkay("Check the server address again. What i connected to wasn't the Planet Server");
                    } else {
                        startSync();
                    }
                } catch (Exception e) {
                }
            }

            @Override
            public void failure(Request request, Response response, FuelError fuelError) {
                alertDialogOkay("Device couldn't reach server. Check and try again");
                if (mRealm != null)
                    mRealm.close();
                progressDialog.dismiss();
            }
        });
        return connectionResult;
    }


    @Override
    public void onSuccess(String s) {
        DialogUtils.showSnack(btnSignIn, s);
    }

    @Override
    public void onUpdateAvailable(String filePath) {
        Utilities.toast(this, "Update available " + filePath);
        new AlertDialog.Builder(this).setTitle("New version of my planet available")
                .setCancelable(false).setMessage("Download first to continue.").setPositiveButton("Upgrade", (dialogInterface, i) -> {
            ArrayList url = new ArrayList();
            url.add(filePath);
            progressDialog.setMessage("Downloading file...");
            progressDialog.setCancelable(false);
            progressDialog.show();
            Utilities.openDownloadService(this, url);
        }).show();
    }

    @Override
    public void onCheckingVersion() {
        progressDialog.setMessage("Checking version....");
        progressDialog.show();
    }

    private void registerReceiver() {
        LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MESSAGE_PROGRESS);
        bManager.registerReceiver(broadcastReceiver, intentFilter);
    }


    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MESSAGE_PROGRESS) && progressDialog != null) {
                Download download = intent.getParcelableExtra("download");
                if (!download.isFailed()) {
                    progressDialog.setMessage("Downloading .... " + download.getProgress() + "% complete");
                    if (download.isCompleteAll()) {
                        progressDialog.dismiss();
                        Utilities.log("File " + download.getFileName());
                        FileUtils.installApk(LoginActivity.this, download.getFileName());
                    }
                } else {
                    progressDialog.dismiss();
                    DialogUtils.showError(progressDialog, download.getMessage());
                }
            }
        }
    };


    @Override
    public void onError(String msg) {
        Utilities.toast(this, msg);
        progressDialog.dismiss();
    }

    private class MyTextWatcher implements TextWatcher {
        private View view;

        private MyTextWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            //action on or during text change
        }

        public void afterTextChanged(Editable editable) {
            switch (view.getId()) {
                case R.id.input_name:
                    validateEditText(inputName, inputLayoutName, getString(R.string.err_msg_name));
                    break;
                case R.id.input_password:
                    validateEditText(inputPassword, inputLayoutPassword, getString(R.string.err_msg_password));
                    break;
                default:
                    break;
            }
        }
    }
}
