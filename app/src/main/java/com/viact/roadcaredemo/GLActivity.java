package com.viact.roadcaredemo;

import static com.viact.roadcaredemo.utils.Const.APP_TAG;
import static com.viact.roadcaredemo.utils.Const.LOCATION_UPDATE_PERIOD;
import static com.viact.roadcaredemo.utils.Const.SERVER_IP;
import static com.viact.roadcaredemo.utils.Const.SERVER_PORT;
import static com.viact.roadcaredemo.utils.Const.rtmp_url;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.pedro.encoder.input.gl.SpriteGestureController;
import com.pedro.encoder.input.gl.render.filters.AnalogTVFilterRender;
import com.pedro.encoder.input.gl.render.filters.AndroidViewFilterRender;
import com.pedro.encoder.input.gl.render.filters.BasicDeformationFilterRender;
import com.pedro.encoder.input.gl.render.filters.BeautyFilterRender;
import com.pedro.encoder.input.gl.render.filters.BlackFilterRender;
import com.pedro.encoder.input.gl.render.filters.BlurFilterRender;
import com.pedro.encoder.input.gl.render.filters.BrightnessFilterRender;
import com.pedro.encoder.input.gl.render.filters.CartoonFilterRender;
import com.pedro.encoder.input.gl.render.filters.ChromaFilterRender;
import com.pedro.encoder.input.gl.render.filters.CircleFilterRender;
import com.pedro.encoder.input.gl.render.filters.ColorFilterRender;
import com.pedro.encoder.input.gl.render.filters.ContrastFilterRender;
import com.pedro.encoder.input.gl.render.filters.DuotoneFilterRender;
import com.pedro.encoder.input.gl.render.filters.EarlyBirdFilterRender;
import com.pedro.encoder.input.gl.render.filters.EdgeDetectionFilterRender;
import com.pedro.encoder.input.gl.render.filters.ExposureFilterRender;
import com.pedro.encoder.input.gl.render.filters.FireFilterRender;
import com.pedro.encoder.input.gl.render.filters.GammaFilterRender;
import com.pedro.encoder.input.gl.render.filters.GlitchFilterRender;
import com.pedro.encoder.input.gl.render.filters.GreyScaleFilterRender;
import com.pedro.encoder.input.gl.render.filters.HalftoneLinesFilterRender;
import com.pedro.encoder.input.gl.render.filters.Image70sFilterRender;
import com.pedro.encoder.input.gl.render.filters.LamoishFilterRender;
import com.pedro.encoder.input.gl.render.filters.MoneyFilterRender;
import com.pedro.encoder.input.gl.render.filters.NegativeFilterRender;
import com.pedro.encoder.input.gl.render.filters.NoFilterRender;
import com.pedro.encoder.input.gl.render.filters.PixelatedFilterRender;
import com.pedro.encoder.input.gl.render.filters.PolygonizationFilterRender;
import com.pedro.encoder.input.gl.render.filters.RGBSaturationFilterRender;
import com.pedro.encoder.input.gl.render.filters.RainbowFilterRender;
import com.pedro.encoder.input.gl.render.filters.RippleFilterRender;
import com.pedro.encoder.input.gl.render.filters.RotationFilterRender;
import com.pedro.encoder.input.gl.render.filters.SaturationFilterRender;
import com.pedro.encoder.input.gl.render.filters.SepiaFilterRender;
import com.pedro.encoder.input.gl.render.filters.SharpnessFilterRender;
import com.pedro.encoder.input.gl.render.filters.SnowFilterRender;
import com.pedro.encoder.input.gl.render.filters.SwirlFilterRender;
import com.pedro.encoder.input.gl.render.filters.TemperatureFilterRender;
import com.pedro.encoder.input.gl.render.filters.ZebraFilterRender;
import com.pedro.encoder.input.gl.render.filters.object.GifObjectFilterRender;
import com.pedro.encoder.input.gl.render.filters.object.ImageObjectFilterRender;
import com.pedro.encoder.input.gl.render.filters.object.SurfaceFilterRender;
import com.pedro.encoder.input.gl.render.filters.object.TextObjectFilterRender;
import com.pedro.encoder.input.video.CameraOpenException;
import com.pedro.encoder.utils.gl.TranslateTo;
import com.pedro.rtmp.utils.ConnectCheckerRtmp;
import com.pedro.rtplibrary.rtmp.RtmpCamera1;
import com.pedro.rtplibrary.view.OpenGlView;
import com.viact.roadcaredemo.services.GPSTracker;
import com.viact.roadcaredemo.utils.PathUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import pub.devrel.easypermissions.EasyPermissions;

public class GLActivity extends AppCompatActivity implements ConnectCheckerRtmp, View.OnClickListener, SurfaceHolder.Callback,
        View.OnTouchListener{

    private RtmpCamera1 rtmpCamera1;
    private Button button;
    private Button bRecord;

    private String currentDateAndTime = "";
    private File folder;
    private OpenGlView openGlView;
    private SpriteGestureController spriteGestureController = new SpriteGestureController();

    private GPSTracker gps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glactivity);
        folder = PathUtils.getRecordPath(this);
        openGlView = findViewById(R.id.surfaceView);
        button = findViewById(R.id.b_start_stop);
        button.setOnClickListener(this);
        bRecord = findViewById(R.id.b_record);
        bRecord.setOnClickListener(this);
        Button switchCamera = findViewById(R.id.switch_camera);
        switchCamera.setOnClickListener(this);

        rtmpCamera1 = new RtmpCamera1(openGlView, this);
        openGlView.getHolder().addCallback(this);
        openGlView.setOnTouchListener(this);

        boolean hasPermissions = EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)
                && EasyPermissions.hasPermissions(this, Manifest.permission.RECORD_AUDIO)
                && EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (!hasPermissions) {
            Toast.makeText(this, "Please, grant all permissions in app settings", Toast.LENGTH_LONG).show();
        }
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
        getMenuInflater().inflate(R.menu.gl_menu, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Stop listener for image, text and gif stream objects.
        spriteGestureController.stopListener();
        switch (item.getItemId()) {
            case R.id.e_d_fxaa:
                rtmpCamera1.getGlInterface().enableAA(!rtmpCamera1.getGlInterface().isAAEnabled());
                Toast.makeText(this,
                        "FXAA " + (rtmpCamera1.getGlInterface().isAAEnabled() ? "enabled" : "disabled"),
                        Toast.LENGTH_SHORT).show();
                return true;
            //filters. NOTE: You can change filter values on fly without reset the filter.
            // Example:
            // ColorFilterRender color = new ColorFilterRender()
            // rtmpCamera1.setFilter(color);
            // color.setRGBColor(255, 0, 0); //red tint
            case R.id.no_filter:
                rtmpCamera1.getGlInterface().setFilter(new NoFilterRender());
                return true;
            case R.id.analog_tv:
                rtmpCamera1.getGlInterface().setFilter(new AnalogTVFilterRender());
                return true;
            case R.id.android_view:
                AndroidViewFilterRender androidViewFilterRender = new AndroidViewFilterRender();
                androidViewFilterRender.setView(findViewById(R.id.switch_camera));
                rtmpCamera1.getGlInterface().setFilter(androidViewFilterRender);
                return true;
            case R.id.basic_deformation:
                rtmpCamera1.getGlInterface().setFilter(new BasicDeformationFilterRender());
                return true;
            case R.id.beauty:
                rtmpCamera1.getGlInterface().setFilter(new BeautyFilterRender());
                return true;
            case R.id.black:
                rtmpCamera1.getGlInterface().setFilter(new BlackFilterRender());
                return true;
            case R.id.blur:
                rtmpCamera1.getGlInterface().setFilter(new BlurFilterRender());
                return true;
            case R.id.brightness:
                rtmpCamera1.getGlInterface().setFilter(new BrightnessFilterRender());
                return true;
            case R.id.cartoon:
                rtmpCamera1.getGlInterface().setFilter(new CartoonFilterRender());
                return true;
            case R.id.chroma:
                ChromaFilterRender chromaFilterRender = new ChromaFilterRender();
                rtmpCamera1.getGlInterface().setFilter(chromaFilterRender);
                chromaFilterRender.setImage(BitmapFactory.decodeResource(getResources(), R.drawable.bg_chroma));
                return true;
            case R.id.circle:
                rtmpCamera1.getGlInterface().setFilter(new CircleFilterRender());
                return true;
            case R.id.color:
                rtmpCamera1.getGlInterface().setFilter(new ColorFilterRender());
                return true;
            case R.id.contrast:
                rtmpCamera1.getGlInterface().setFilter(new ContrastFilterRender());
                return true;
            case R.id.duotone:
                rtmpCamera1.getGlInterface().setFilter(new DuotoneFilterRender());
                return true;
            case R.id.early_bird:
                rtmpCamera1.getGlInterface().setFilter(new EarlyBirdFilterRender());
                return true;
            case R.id.edge_detection:
                rtmpCamera1.getGlInterface().setFilter(new EdgeDetectionFilterRender());
                return true;
            case R.id.exposure:
                rtmpCamera1.getGlInterface().setFilter(new ExposureFilterRender());
                return true;
            case R.id.fire:
                rtmpCamera1.getGlInterface().setFilter(new FireFilterRender());
                return true;
            case R.id.gamma:
                rtmpCamera1.getGlInterface().setFilter(new GammaFilterRender());
                return true;
            case R.id.glitch:
                rtmpCamera1.getGlInterface().setFilter(new GlitchFilterRender());
                return true;
            case R.id.gif:
                setGifToStream();
                return true;
            case R.id.grey_scale:
                rtmpCamera1.getGlInterface().setFilter(new GreyScaleFilterRender());
                return true;
            case R.id.halftone_lines:
                rtmpCamera1.getGlInterface().setFilter(new HalftoneLinesFilterRender());
                return true;
            case R.id.image:
                setImageToStream();
                return true;
            case R.id.image_70s:
                rtmpCamera1.getGlInterface().setFilter(new Image70sFilterRender());
                return true;
            case R.id.lamoish:
                rtmpCamera1.getGlInterface().setFilter(new LamoishFilterRender());
                return true;
            case R.id.money:
                rtmpCamera1.getGlInterface().setFilter(new MoneyFilterRender());
                return true;
            case R.id.negative:
                rtmpCamera1.getGlInterface().setFilter(new NegativeFilterRender());
                return true;
            case R.id.pixelated:
                rtmpCamera1.getGlInterface().setFilter(new PixelatedFilterRender());
                return true;
            case R.id.polygonization:
                rtmpCamera1.getGlInterface().setFilter(new PolygonizationFilterRender());
                return true;
            case R.id.rainbow:
                rtmpCamera1.getGlInterface().setFilter(new RainbowFilterRender());
                return true;
            case R.id.rgb_saturate:
                RGBSaturationFilterRender rgbSaturationFilterRender = new RGBSaturationFilterRender();
                rtmpCamera1.getGlInterface().setFilter(rgbSaturationFilterRender);
                //Reduce green and blue colors 20%. Red will predominate.
                rgbSaturationFilterRender.setRGBSaturation(1f, 0.8f, 0.8f);
                return true;
            case R.id.ripple:
                rtmpCamera1.getGlInterface().setFilter(new RippleFilterRender());
                return true;
            case R.id.rotation:
                RotationFilterRender rotationFilterRender = new RotationFilterRender();
                rtmpCamera1.getGlInterface().setFilter(rotationFilterRender);
                rotationFilterRender.setRotation(90);
                return true;
            case R.id.saturation:
                rtmpCamera1.getGlInterface().setFilter(new SaturationFilterRender());
                return true;
            case R.id.sepia:
                rtmpCamera1.getGlInterface().setFilter(new SepiaFilterRender());
                return true;
            case R.id.sharpness:
                rtmpCamera1.getGlInterface().setFilter(new SharpnessFilterRender());
                return true;
            case R.id.snow:
                rtmpCamera1.getGlInterface().setFilter(new SnowFilterRender());
                return true;
            case R.id.swirl:
                rtmpCamera1.getGlInterface().setFilter(new SwirlFilterRender());
                return true;
            case R.id.surface_filter:
                SurfaceFilterRender surfaceFilterRender =
                        new SurfaceFilterRender(new SurfaceFilterRender.SurfaceReadyCallback() {
                            @Override
                            public void surfaceReady(SurfaceTexture surfaceTexture) {
                                //You can render this filter with other api that draw in a surface. for example you can use VLC
                                MediaPlayer mediaPlayer =
                                        MediaPlayer.create(GLActivity.this, R.raw.big_bunny_240p);
                                mediaPlayer.setSurface(new Surface(surfaceTexture));
                                mediaPlayer.start();
                            }
                        });
                rtmpCamera1.getGlInterface().setFilter(surfaceFilterRender);
                //Video is 360x240 so select a percent to keep aspect ratio (50% x 33.3% screen)
                surfaceFilterRender.setScale(50f, 33.3f);
                spriteGestureController.setBaseObjectFilterRender(surfaceFilterRender); //Optional
                return true;
            case R.id.temperature:
                rtmpCamera1.getGlInterface().setFilter(new TemperatureFilterRender());
                return true;
            case R.id.text:
                setTextToStream();
                return true;
            case R.id.zebra:
                rtmpCamera1.getGlInterface().setFilter(new ZebraFilterRender());
                return true;
            default:
                return false;
        }
    }

    private void setTextToStream() {
        TextObjectFilterRender textObjectFilterRender = new TextObjectFilterRender();
        rtmpCamera1.getGlInterface().setFilter(textObjectFilterRender);
        textObjectFilterRender.setText("Hello world", 22, Color.RED);
        textObjectFilterRender.setDefaultScale(rtmpCamera1.getStreamWidth(),
                rtmpCamera1.getStreamHeight());
        textObjectFilterRender.setPosition(TranslateTo.CENTER);
        spriteGestureController.setBaseObjectFilterRender(textObjectFilterRender); //Optional
    }

    private void setImageToStream() {
        ImageObjectFilterRender imageObjectFilterRender = new ImageObjectFilterRender();
        rtmpCamera1.getGlInterface().setFilter(imageObjectFilterRender);
        imageObjectFilterRender.setImage(
                BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        imageObjectFilterRender.setDefaultScale(rtmpCamera1.getStreamWidth(),
                rtmpCamera1.getStreamHeight());
        imageObjectFilterRender.setPosition(TranslateTo.RIGHT);
        spriteGestureController.setBaseObjectFilterRender(imageObjectFilterRender); //Optional
        spriteGestureController.setPreventMoveOutside(false); //Optional
    }

    private void setGifToStream() {
        try {
            GifObjectFilterRender gifObjectFilterRender = new GifObjectFilterRender();
            gifObjectFilterRender.setGif(getResources().openRawResource(R.raw.banana));
            rtmpCamera1.getGlInterface().setFilter(gifObjectFilterRender);
            gifObjectFilterRender.setDefaultScale(rtmpCamera1.getStreamWidth(),
                    rtmpCamera1.getStreamHeight());
            gifObjectFilterRender.setPosition(TranslateTo.BOTTOM);
            spriteGestureController.setBaseObjectFilterRender(gifObjectFilterRender); //Optional
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectionStartedRtmp(String rtmpUrl) {
    }

    @Override
    public void onConnectionSuccessRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(GLActivity.this, "Connection success", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onConnectionFailedRtmp(final String reason) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(GLActivity.this, "Connection failed. " + reason, Toast.LENGTH_SHORT)
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(GLActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAuthErrorRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(GLActivity.this, "Auth error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAuthSuccessRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(GLActivity.this, "Auth success", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.b_start_stop:
                if (!rtmpCamera1.isStreaming()) {
                    if (rtmpCamera1.isRecording()
                            || rtmpCamera1.prepareAudio() && rtmpCamera1.prepareVideo()) {
                        startStream();
                    } else {
                        Toast.makeText(this, "Error preparing stream, This device cant do it",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    stopStream();
                }
                break;
            case R.id.switch_camera:
                try {
                    rtmpCamera1.switchCamera();
                } catch (CameraOpenException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.b_record:
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
                                bRecord.setText("stop_record");
                                Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Error preparing stream, This device cant do it",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            rtmpCamera1.startRecord(folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
                            bRecord.setText("stop_record");
                            Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        rtmpCamera1.stopRecord();
                        bRecord.setText("start_record");
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    rtmpCamera1.stopRecord();
                    bRecord.setText("start_record");
                    Toast.makeText(this,
                            "file " + currentDateAndTime + ".mp4 saved in " + folder.getAbsolutePath(),
                            Toast.LENGTH_SHORT).show();
                    currentDateAndTime = "";
                }
                break;
            default:
                break;
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
            bRecord.setText("start_record");
            Toast.makeText(this,
                    "file " + currentDateAndTime + ".mp4 saved in " + folder.getAbsolutePath(),
                    Toast.LENGTH_SHORT).show();
            currentDateAndTime = "";
        }
        if (rtmpCamera1.isStreaming()) {
            stopStream();
        }
        rtmpCamera1.stopPreview();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (spriteGestureController.spriteTouched(view, motionEvent)) {
            spriteGestureController.moveSprite(view, motionEvent);
            spriteGestureController.scaleSprite(motionEvent);
            return true;
        }
        return false;
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

    void startSendRealData(){
        if (gps == null){
            gps = new GPSTracker(this);
        }
        //test
//        sendCurLocation();
        loc_timer = new Timer();
        loc_timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (bSendable && rtmpCamera1.isStreaming()){
                    sendCurLocation();
                }
            }
        }, LOCATION_UPDATE_PERIOD);
    }

    void stopSendRealData(){
        if (loc_timer != null){
            loc_timer.cancel();
            loc_timer = null;
        }
    }

    void sendCurLocation(){
        runOnUiThread(() -> {
            if(gps.canGetLocation()){
                try{
                    Location cur_loc = gps.getLocation();
                    if (cur_loc != null){
                        String gps_loc = cur_loc.getLatitude() + "," + cur_loc.getLongitude();
                        Long cur_time = System.currentTimeMillis()/1000;
                        JSONObject obj = new JSONObject();
                        obj.put("key", uuid);
                        obj.put("gps", gps_loc);
                        obj.put("datetime", cur_time.toString());
                        String cur_data = obj.toString();
//                    Toast.makeText(this,obj.toString(), Toast.LENGTH_SHORT).show();
                        Log.d(APP_TAG, obj.toString());
                        bSendable = false;
                        new GLActivity.AsyncSocketTask(this).execute(cur_data);
                    }
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            } else{
                Toast.makeText(this,"Can not get the current location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    boolean bSendable = true;
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
                InputStream in = socket.getInputStream();
                byte buf[] = new byte[1024];
                int nbytes;
                while ((nbytes = in.read(buf)) != -1) {
                    sb.append(new String(buf, 0, nbytes));
                }
                socket.close();
            } catch(IOException e) {
                this.ioException = e;
                return "error";
            }
            return sb.toString();
        }
        @Override
        protected void onPostExecute(String result) {
            if (this.ioException != null) {
                new AlertDialog.Builder(this.activity)
                        .setTitle("An error occurred")
                        .setMessage(this.ioException.toString())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            } else {
                Toast.makeText(GLActivity.this,"Location sent : " + result, Toast.LENGTH_SHORT).show();
            }
            bSendable = true;
        }
    }

    public String createStreamUUID() throws Exception{
        return UUID.randomUUID().toString(); //.replaceAll("-", "").toUpperCase()
    }
}