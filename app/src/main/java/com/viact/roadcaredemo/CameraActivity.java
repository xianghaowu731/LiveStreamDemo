package com.viact.roadcaredemo;

import static com.viact.roadcaredemo.utils.Const.APP_TAG;
import static com.viact.roadcaredemo.utils.Const.LOCATION_UPDATE_PERIOD;
import static com.viact.roadcaredemo.utils.Const.SERVER_IP;
import static com.viact.roadcaredemo.utils.Const.SERVER_PORT;
import static com.viact.roadcaredemo.utils.Const.rtmp_url;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.pedro.encoder.input.video.CameraOpenException;
import com.pedro.rtmp.utils.ConnectCheckerRtmp;
import com.pedro.rtplibrary.rtmp.RtmpCamera1;
import com.viact.roadcaredemo.services.GPSTracker;
import com.viact.roadcaredemo.utils.PathUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pub.devrel.easypermissions.EasyPermissions;

public class CameraActivity extends AppCompatActivity implements ConnectCheckerRtmp, SurfaceHolder.Callback{

    private RtmpCamera1 rtmpCamera1;
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.b_start_stop)  Button button;
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.b_record) Button bRecord;

    private String currentDateAndTime = "";
    private File folder;

    private GPSTracker gps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);

        folder = PathUtils.getRecordPath(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Please, grant all permissions in app settings to ensure the every features.", Toast.LENGTH_LONG).show();
            requestPermission();
        } else {
            initRTMP();
        }
    }

    void initRTMP(){
        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        rtmpCamera1 = new RtmpCamera1(surfaceView, this);
        rtmpCamera1.setReTries(10);
        surfaceView.getHolder().addCallback(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        gps = new GPSTracker(this);
        if(!gps.canGetLocation()){
            gps.showSettingsAlert();
        }
    }

    @Override
    protected void onStop() {
        gps.stopUsingGPS();
        super.onStop();
    }

        @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:

                return true;
            case R.id.microphone:
                if (!rtmpCamera1.isAudioMuted()) {
                    item.setIcon(getResources().getDrawable(R.drawable.ic_mic_off));
                    rtmpCamera1.disableAudio();
                } else {
                    item.setIcon(getResources().getDrawable(R.drawable.ic_mic));
                    rtmpCamera1.enableAudio();
                }
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onConnectionStartedRtmp(String rtmpUrl) {
    }

    @Override
    public void onConnectionSuccessRtmp() {
        runOnUiThread(() -> Toast.makeText(CameraActivity.this, "Connection success", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onConnectionFailedRtmp(final String reason) {
        runOnUiThread(() -> {
            if (rtmpCamera1.reTry(5000, reason)) {
                Toast.makeText(CameraActivity.this, "Retry", Toast.LENGTH_SHORT)
                        .show();
            } else {
                Toast.makeText(CameraActivity.this, "Connection failed. " + reason, Toast.LENGTH_SHORT)
                        .show();
                stopStream();
            }
        });
    }

    @Override
    public void onNewBitrateRtmp(long bitrate) {

    }

    @Override
    public void onDisconnectRtmp() {
        runOnUiThread(() -> Toast.makeText(CameraActivity.this, "Disconnected", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onAuthErrorRtmp() {
        runOnUiThread(() -> Toast.makeText(CameraActivity.this, "Auth error", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onAuthSuccessRtmp() {
        runOnUiThread(() -> Toast.makeText(CameraActivity.this, "Auth success", Toast.LENGTH_SHORT).show());
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.b_record) void onClickRecord(){
        if (!rtmpCamera1.isRecording()) {
            try {
                if (!folder.exists()) {
                    folder.mkdir();
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
                currentDateAndTime = sdf.format(new Date());
                if (!rtmpCamera1.isStreaming()) {
                    if (rtmpCamera1.prepareAudio() && rtmpCamera1.prepareVideo()) {
                        rtmpCamera1.startRecord(
                                folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
                        bRecord.setText("Stop Record");
                        Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Error preparing stream, This device cant do it",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    rtmpCamera1.startRecord(
                            folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
                    bRecord.setText("Stop Record");
                    Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                rtmpCamera1.stopRecord();
                bRecord.setText("Start Record");
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            rtmpCamera1.stopRecord();
            bRecord.setText("Start Record");
            Toast.makeText(this,
                    "file " + currentDateAndTime + ".mp4 saved in " + folder.getAbsolutePath(),
                    Toast.LENGTH_SHORT).show();
            currentDateAndTime = "";
        }
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.b_start_stop) void onClickStart(){
        if (!rtmpCamera1.isStreaming()) {
            if (rtmpCamera1.isRecording() || rtmpCamera1.prepareAudio() && rtmpCamera1.prepareVideo()) {

                startStream();
            } else {
                Toast.makeText(this, "Error preparing stream, This device cant do it",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            stopStream();
        }
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.switch_camera) void onClickSwitch(){
        try {
            rtmpCamera1.switchCamera();
        } catch (CameraOpenException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        rtmpCamera1.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (rtmpCamera1.isRecording()) {
            rtmpCamera1.stopRecord();
            bRecord.setText("Start");
            Toast.makeText(this,"file " + currentDateAndTime + ".mp4 saved in " + folder.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            currentDateAndTime = "";
        }
        if (rtmpCamera1.isStreaming()) {
            stopStream();
        }
        rtmpCamera1.stopPreview();
    }

    void startStream(){
        button.setText("Stop");
        try{
            uuid = createStreamUUID();
        } catch (Exception ex){
            ex.printStackTrace();
            uuid = "Android live-stream key";
        }

        rtmpCamera1.startStream(rtmp_url);
        startSendRealData();
    }

    void stopStream(){
        rtmpCamera1.stopStream();
        stopSendRealData();
        button.setText("Start");
    }

    //Socket module to send the real-time GPS location
    private String uuid;
    private Timer loc_timer;
    private Location cur_loc;
    private Socket gps_socket = null;
    OutputStream out = null;

    void startSendRealData(){
        if (gps == null){
            gps = new GPSTracker(this);
        }
        //create socket and os
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try{
            gps_socket = new Socket(SERVER_IP, SERVER_PORT);
            out = gps_socket.getOutputStream();
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host: " + SERVER_IP);
            Toast.makeText(this,"Don't know about host: " + SERVER_IP, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to: " + SERVER_IP);
            Toast.makeText(this,"Couldn't get I/O for the connection to: " + SERVER_IP, Toast.LENGTH_SHORT).show();
        }

        loc_timer = new Timer();
        loc_timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (gps_socket != null && rtmpCamera1.isStreaming()){//
                    runOnUiThread(() -> cur_loc = gps.getLocation());
                    sendCurLocation();
                }
            }
        }, 0, LOCATION_UPDATE_PERIOD);
    }

    void stopSendRealData(){
        if (loc_timer != null){
            loc_timer.cancel();
            loc_timer = null;
        }

        if (gps_socket != null){
            try {
                out.close();
                gps_socket.shutdownOutput();
                gps_socket.close();
                gps_socket = null;
            } catch (IOException eee){
                eee.printStackTrace();
            }
        }
    }

    void sendCurLocation(){
        try{
            if (cur_loc != null){
                String gps_loc = cur_loc.getLatitude() + "," + cur_loc.getLongitude();
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ");
                Long cur_time = System.currentTimeMillis();
                String dateString = formatter.format(new Date(cur_time));
                JSONObject obj = new JSONObject();
                obj.put("key", uuid);
                obj.put("gps", gps_loc);
                obj.put("datetime", dateString);
                String cur_data = obj.toString();
                Log.d(APP_TAG, obj.toString());
                runOnUiThread(() -> {
                    try {
                        out.write(cur_data.getBytes());
                        out.flush();
                    } catch (IOException io) {
                        io.printStackTrace();
                    }
                });

            }
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    /*boolean bSendable = true;
    private class AsyncSocketTask extends AsyncTask<String, Void, String> {
        Activity activity;
        IOException ioException;
        AsyncSocketTask(Activity activity) {
            super();
            this.activity = activity;
            this.ioException = null;
        }
        @Override
        protected String doInBackground(String... params) {
            StringBuilder sb = new StringBuilder();
            try {
                Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                OutputStream out = socket.getOutputStream();
                out.write(params[0].getBytes());
                out.flush();
//                InputStream in = socket.getInputStream();
//                byte buf[] = new byte[1024];
//                int nbytes;
//                while ((nbytes = in.read(buf)) != -1) {
//                    sb.append(new String(buf, 0, nbytes));
//                }
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                sb.append(inputStream.readUTF());
                socket.close();
                bSendable = true;
            } catch(IOException e) {
                this.ioException = e;
                return "error";
            }
            return sb.toString();
        }
        @Override
        protected void onPostExecute(String result) {
            bSendable = true;
        }
    }*/

    public String createStreamUUID() {
        return UUID.randomUUID().toString(); //.replaceAll("-", "").toUpperCase()
    }

    //permission checking
    final int REQUEST_CUSTOM_PERMISSION = 9989;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CUSTOM_PERMISSION && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Please, grant all permissions in app settings to ensure the every features.", Toast.LENGTH_LONG).show();
            finish();
        } else {
            initRTMP();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, REQUEST_CUSTOM_PERMISSION);
        } else {
            initRTMP();
        }
    }
}