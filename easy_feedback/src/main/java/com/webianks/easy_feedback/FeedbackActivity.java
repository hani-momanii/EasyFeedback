package com.webianks.easy_feedback;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.webianks.easy_feedback.components.DeviceInfo;
import com.webianks.easy_feedback.components.SystemLog;
import com.webianks.easy_feedback.text_formatting.Spanning;


/**
 * Created by R Ankit on 28-10-2016.
 */

public class FeedbackActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText editText;
    private String emailId;
    private final int REQUEST_APP_SETTINGS = 321;
    private final int REQUEST_PERMISSIONS = 123;
    private String deviceInfo;
    private boolean withInfo;
    private int PICK_IMAGE_REQUEST = 125;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedback_layout);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        init();


    }


    private void init() {

        editText = (EditText) findViewById(R.id.editText);

        TextView info = (TextView) findViewById(R.id.info_legal);
        Button submitSuggestion = (Button) findViewById(R.id.submitSuggestion);
        submitSuggestion.setOnClickListener(this);

        emailId = getIntent().getStringExtra("email");
        withInfo = getIntent().getBooleanExtra("with_info", false);

        deviceInfo = DeviceInfo.getAllDeviceInfo(this, false);

        if (withInfo) {
            Spanning spanning = new Spanning(this, info, getAppLabel(this));
            spanning.colorPartOfText();
        } else
            info.setVisibility(View.GONE);

    }


    public void selectImage(View view) {


        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {


            int hasWriteContactsPermission = checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE);
            if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_PERMISSIONS);
                return;

            } else
                //already granted
                selectPicture();


        } else {
            //normal process
            selectPicture();
        }



    }

    private void selectPicture() {

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }


    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {

        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {

            case REQUEST_PERMISSIONS:

                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    selectPicture();

                } else {
                    // Permission Denied
                    showMessageOKCancel("You need to allow access to SD card to select images.",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    goToSettings();

                                }
                            });

                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        }
    }


    private void goToSettings() {

        Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
        myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(myAppSettings, REQUEST_APP_SETTINGS);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_APP_SETTINGS) {

            if (hasPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)) {

                //Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show();
                selectPicture();

            } else {

                showMessageOKCancel("You need to allow access to SD card to select images.",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                goToSettings();

                            }
                        });

            }
        } else if (requestCode == PICK_IMAGE_REQUEST &&
                resultCode == RESULT_OK && data != null && data.getData() != null) {

            String realPath;

            if (Build.VERSION.SDK_INT < 19)
                realPath = RealPathUtil.getRealPathFromURI_API11to18(this, data.getData());
            else
                realPath = RealPathUtil.getRealPathFromURI_API19(this, data.getData());

            Log.d("webi",realPath);

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public boolean hasPermissions(@NonNull String... permissions) {
        for (String permission : permissions)
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, permission))
                return false;
        return true;
    }


    public void sendEmail(String body) {

        StringBuilder finalBody = new StringBuilder(body);

        if (withInfo) {
            finalBody.append(deviceInfo);
            finalBody.append(SystemLog.extractLogToString());
        }

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setType("image/jpeg");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, getAppLabel(this) + " Feedback");
        emailIntent.putExtra(Intent.EXTRA_TEXT, finalBody.toString());
        emailIntent.setData(Uri.parse("mailto: " + emailId));
        startActivity(Intent.createChooser(emailIntent, getString(R.string.send_feedback_two)));

    }


    @Override
    public void onClick(View view) {

        String suggestion = editText.getText().toString();

        if (suggestion.trim().length() > 0)
            sendEmail(suggestion);
        else
            editText.setError(getString(R.string.please_write));

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home)
            finish();

        return super.onOptionsItemSelected(item);
    }

    public String getAppLabel(Context context) {

        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = packageManager.getApplicationInfo(context.getApplicationInfo().packageName, 0);
        } catch (final PackageManager.NameNotFoundException e) {
        }
        return (String) (applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo) : "Unknown");
    }


}
