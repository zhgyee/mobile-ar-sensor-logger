package edu.osu.pcv.marslogger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.text.BreakIterator;
import java.text.SimpleDateFormat;
import java.util.Date;

import timber.log.Timber;

public class InfoActivity extends Activity {
    private static final String TAG = InfoActivity.class.getName();;
    protected boolean mGoogleEnabled = false;
    protected boolean mPaypalEnabled = true;

    protected boolean mDebug = false;
    private boolean mRecordingEnabled = false;
    private TextView mOutputDirText;
    private IMUManager mImuManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        // https://www.android-examples.com/add-hyperlink-in-android-application-through-textview/
        TextView hyperlink = findViewById(R.id.linkTextView);
        String linkText = getResources().getString(R.string.link_foreword);
        Spanned text = FileHelper.fromHtml(linkText + " " +
                "<a href='https://github.com/OSUPCVLab/mobile-ar-sensor-logger/'>GitHub</a>.");
        hyperlink.setMovementMethod(LinkMovementMethod.getInstance());
        hyperlink.setText(text);

        mGoogleEnabled = BuildConfig.DONATIONS_GOOGLE;
        mPaypalEnabled = !BuildConfig.DONATIONS_GOOGLE;

        addButtonListeners();
    }

    private void addButtonListeners() {
        Button button_done = (Button) findViewById(R.id.button_done);
        button_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button button = (Button)v;

                mRecordingEnabled = !mRecordingEnabled;
                if (mRecordingEnabled) {
                    String outputDir = renewOutputDir();
                    String basename = outputDir.substring(outputDir.lastIndexOf("/")+1);
                    mOutputDirText.setText(basename);
                    String inertialFile = outputDir + File.separator + "gyro_accel.csv";
                    mImuManager.startRecording(inertialFile);
                    button.setText("stop");
                } else {
                    mImuManager.stopRecording();
                    button.setText("start");
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mImuManager == null) {
            mImuManager = new IMUManager(this);
        }
        mOutputDirText = (TextView) findViewById(R.id.cameraOutputDir_text);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mImuManager.register();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mImuManager.unregister();
    }

    /**
     * Open dialog
     */
    void openDialog(int icon, int title, String message) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setIcon(icon);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setCancelable(true);
        dialog.setNeutralButton(R.string.donations__button_close,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }
        );
        dialog.show();
    }
    protected String renewOutputDir() {
        SimpleDateFormat dateFormat =
                new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        String folderName = dateFormat.format(new Date());
        String dir1 = getFilesDir().getAbsolutePath();
        String dir2 = Environment.getExternalStorageDirectory().
                getAbsolutePath() + File.separator + "mars_logger";

        String dir3 = getExternalFilesDir(
                Environment.getDataDirectory().getAbsolutePath()).getAbsolutePath();
        Timber.d("dir 1 %s\ndir 2 %s\ndir 3 %s", dir1, dir2, dir3);
        // dir1 and dir3 are always available for the app even the
        // write external storage permission is not granted.
        // "Apparently in Marshmallow when you install with Android studio it
        // never asks you if you should give it permission it just quietly
        // fails, like you denied it. You must go into Settings, apps, select
        // your application and flip the permission switch on."
        // ref: https://stackoverflow.com/questions/40087355/android-mkdirs-not-working
        String outputDir = dir3 + File.separator + folderName;
        (new File(outputDir)).mkdirs();
        return outputDir;
    }
}
