package org.ole.planet.takeout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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

import org.ole.planet.takeout.userprofile.UserProfileDbHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageButton;


public class LoginActivity extends SyncActivity {
    Context context;
    boolean connectionResult;
    dbSetup dbsetup = new dbSetup();
    EditText serverUrl;
    EditText serverPassword;
    Fuel ful = new Fuel();
    int[] syncTimeInteval = {15 * 60, 30 * 60, 60 * 60, 3 * 60 * 60};
    private EditText inputName, inputPassword;
    private TextInputLayout inputLayoutName, inputLayoutPassword;
    private Button btnSignIn;
    private ImageButton imgBtnSetting;
    private View positiveAction;
    private GifDrawable gifDrawable;
    private GifImageButton syncIcon;

    private View constraintLayout;
    private CheckBox save;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        context = this.getApplicationContext();
        settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        changeLogoColor();
        //layouts
        inputLayoutName = findViewById(R.id.input_layout_name);
        inputLayoutPassword = findViewById(R.id.input_layout_password);
        imgBtnSetting = findViewById(R.id.imgBtnSetting);
        save = findViewById(R.id.save);

        declareElements();
        declareMoreElements();

        btnSignIn = findViewById(R.id.btn_signin); //buttons
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitForm();
            }
        });
        dbsetup.Setup_db(this.context);
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
        imgBtnSetting.setOnClickListener(new View.OnClickListener() { //Settings button
            @Override
            public void onClick(View view) {
                MaterialDialog.Builder builder = new MaterialDialog.Builder(LoginActivity.this);
                builder.title(R.string.action_settings).customView(R.layout.dialog_server_url_, true)
                        .positiveText(R.string.btn_sync).negativeText(R.string.btn_sync_cancel).neutralText(R.string.btn_sync_save)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                                isServerReachable((EditText) dialog.getCustomView().findViewById(R.id.input_server_url), (EditText) dialog.getCustomView().findViewById(R.id.input_server_Password));
                            }
                        }).onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putBoolean("autoSync", syncSwitch.isChecked());
                        editor.putInt("autoSyncInterval", syncTimeInteval[spinner.getSelectedItemPosition()]);
                        editor.commit();
                        Toast.makeText(LoginActivity.this, "Saving sync settings...", Toast.LENGTH_SHORT).show();
                    }
                });
                settingDialog(builder);
            }
        });
    }

    public void declareMoreElements() {
        //Sync Gif-Button
        syncIcon = findViewById(R.id.syncIcon);
        syncIcon.setImageResource(R.drawable.sync_icon);
        syncIcon.getScaleType();
        gifDrawable = (GifDrawable) syncIcon.getDrawable();
        gifDrawable.setSpeed(3.0f);
        gifDrawable.stop();

        syncIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gifDrawable.reset();
                Toast.makeText(LoginActivity.this, "Syncing now...", Toast.LENGTH_SHORT).show();
            }
        });

        // allows the user to touch anywhere else on the screen to dismiss the keyboard
        declareHideKeyboardElements();

        //listeners / actions
        inputName = findViewById(R.id.input_name);//editText
        inputPassword = findViewById(R.id.input_password);
        inputName.addTextChangedListener(new MyTextWatcher(inputName));
        inputPassword.addTextChangedListener(new MyTextWatcher(inputPassword));

        if (settings.getBoolean("saveUsernameAndPassword", false)) {
            inputName.setText(settings.getString("url_user", ""));
            inputPassword.setText(settings.getString("url_pwd", ""));
            save.setChecked(true);
        }

    }

    /**
     * Form  Validation
     */
    private void submitForm() {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("saveUsernameAndPassword", save.isChecked());
        editor.commit();
        if (!validateEditText(inputName, inputLayoutName, getString(R.string.err_msg_name))) {
            return;
        }
        if (!validateEditText(inputPassword, inputLayoutPassword, getString(R.string.err_msg_password))) {
            return;
        }
        if (authenticateUser(settings, inputName.getText().toString(), inputPassword.getText().toString(), context)) {
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
        serverPassword.setText(settings.getString("url_pwd", ""));
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

    public boolean isServerReachable(EditText textUrl, EditText textPassword) {
        //serverUrl = textUrl;
        final String url = textUrl.getText().toString();
        final String pswd = textPassword.getText().toString();
        String processedUrl = setUrlParts(url, pswd, context);

        ful.get(processedUrl + "/_all_dbs").responseString(new Handler<String>() {
            @Override
            public void success(Request request, Response response, String s) {
                try {
                    List<String> myList = new ArrayList<>();
                    myList.clear();
                    myList = Arrays.asList(s.split(","));
                    if (myList.size() < 8) {
                        alertDialogOkay("Check the server address again. What i connected to wasn't the Planet Server");
                    } else {
                        //alertDialogOkay("Test successful. You can now click on \"Save and Proceed\" ");
                        startSync();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failure(Request request, Response response, FuelError fuelError) {
                Log.e("error", fuelError.toString());
                alertDialogOkay("Device couldn't reach server. Check and try again");
                if (mRealm != null)
                    mRealm.close();
            }
        });
        return connectionResult;
    }

    protected void hideKeyboard(View view) {
        InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        in.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public void declareHideKeyboardElements() {
        constraintLayout = findViewById(R.id.constraintLayout);
        constraintLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent ev) {
                hideKeyboard(view);
                return false;
            }
        });
    }

    private class MyTextWatcher implements TextWatcher {
        private View view;

        private MyTextWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            //action before text change
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
