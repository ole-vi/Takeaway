package org.ole.planet.myplanet.ui.sync;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.datamanager.Service;
import org.ole.planet.myplanet.service.UserProfileDbHandler;
import org.ole.planet.myplanet.ui.dashboard.DashboardActivity;
import org.ole.planet.myplanet.ui.viewer.WebViewActivity;
import org.ole.planet.myplanet.utilities.DialogUtils;
import org.ole.planet.myplanet.utilities.FileUtils;
import org.ole.planet.myplanet.utilities.Utilities;


import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageButton;

import static org.ole.planet.myplanet.ui.dashboard.DashboardActivity.MESSAGE_PROGRESS;


public class LoginActivity extends SyncActivity implements Service.CheckVersionCallback {
    EditText serverUrl;
    EditText serverPassword;
    private EditText inputName;
    TextInputEditText inputPassword;
    private TextInputLayout inputLayoutName, inputLayoutPassword;
    private Button btnSignIn;
    private ImageButton imgBtnSetting;
    private View positiveAction;
    private GifDrawable gifDrawable;
    private GifImageButton syncIcon;
    private CheckBox save;
    private boolean isSync = false, isUpload = false, forceSync = false;
    String processedUrl;

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
        showWifiDialog();
        btnSignIn = findViewById(R.id.btn_signin); //buttons
        btnSignIn.setOnClickListener(view -> submitForm());
        registerReceiver();
        forceSync = getIntent().getBooleanExtra("forceSync", false);
        if (forceSync) {
            isUpload = false;
            isSync = false;
            processedUrl = Utilities.getUrl();
        }
        if (getIntent().hasExtra("filePath")) {
            onUpdateAvailable(getIntent().getStringExtra("filePath"), getIntent().getBooleanExtra("cancelable", false));
        } else {
            new Service(this).checkVersion(this, settings);
        }
    }

    private void showWifiDialog() {
        if (getIntent().getBooleanExtra("showWifiDialog", false)) {
            DialogUtils.showWifiSettingDialog(this);
        }
    }


    public void declareElements() {
        findViewById(R.id.become_member).setOnClickListener(v -> {
            if (!Utilities.getUrl().isEmpty()) {
                startActivity(new Intent(this, WebViewActivity.class).putExtra("title", "Become a member")
                        .putExtra("link", Utilities.getUrl().replaceAll("/db", "") + "/eng/login/newmember"));
            } else {
                Utilities.toast(this, "Please enter server url first.");
                settingDialog();
            }
        });
        imgBtnSetting.setOnClickListener(view -> settingDialog());
    }


    private void continueSync(MaterialDialog dialog) {
        processedUrl = saveConfigAndContinue(dialog);
        if (TextUtils.isEmpty(processedUrl)) return;
        isUpload = false;
        isSync = true;
        if (checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) && settings.getBoolean("firstRun", true)) {
            clearInternalStorage();
        }
        new Service(this).checkVersion(this, settings);
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
            isUpload = true;
            isSync = false;
            new Service(this).checkVersion(this, settings);
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
            Intent dashboard = new Intent(getApplicationContext(), DashboardActivity.class);
            dashboard.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(dashboard);
        } else {
            alertDialogOkay(getString(R.string.err_msg_login));
        }
    }

    public void settingDialog() {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(LoginActivity.this);
        builder.title(R.string.action_settings).customView(R.layout.dialog_server_url_, true)
                .positiveText(R.string.btn_sync).negativeText(R.string.btn_sync_cancel).neutralText(R.string.btn_sync_save)
                .onPositive((dialog, which) -> continueSync(dialog)).onNeutral((dialog, which) -> saveConfigAndContinue(dialog));

        MaterialDialog dialog = builder.build();
        positiveAction = dialog.getActionButton(DialogAction.POSITIVE);
        serverUrl = dialog.getCustomView().findViewById(R.id.input_server_url);
        serverPassword = dialog.getCustomView().findViewById(R.id.input_server_Password);
        serverUrl.setText(settings.getString("serverURL", ""));
        serverPassword.setText(settings.getString("serverPin", ""));
        serverUrl.addTextChangedListener(new MyTextWatcher(serverUrl));
        dialog.show();
        sync(dialog);
    }


    @Override
    public void onSuccess(String s) {
        if (progressDialog.isShowing() && s.contains("Crash"))
            progressDialog.dismiss();
        DialogUtils.showSnack(btnSignIn, s);
    }

    @Override
    public void onUpdateAvailable(String filePath, boolean cancelable) {
        AlertDialog.Builder builder = DialogUtils.getUpdateDialog(this, filePath, progressDialog);
        if (cancelable) {
            builder.setNegativeButton("Update Later", (dialogInterface, i) -> {
                continueSyncProcess();
            });
        }
        builder.show();
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


    @Override
    public void onError(String msg, boolean block) {
        Utilities.toast(this, msg);
        progressDialog.dismiss();
        if (!block)
            continueSyncProcess();
    }

    public void continueSyncProcess() {
        try {
            if (isSync) {
                isServerReachable(processedUrl);
            } else if (isUpload) {
                startUpload();
            } else if (forceSync) {
                isServerReachable(processedUrl);
                startUpload();
            }
        } catch (Exception e) {
        }
    }

    private class MyTextWatcher implements TextWatcher {
        private View view;

        private MyTextWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void onTextChanged(CharSequence s, int i, int i1, int i2) {
            if (view.getId() == R.id.input_server_url)
                positiveAction.setEnabled(s.toString().trim().length() > 0 && URLUtil.isValidUrl(s.toString()));

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRealm != null && !mRealm.isClosed())
            mRealm.close();
    }
}
