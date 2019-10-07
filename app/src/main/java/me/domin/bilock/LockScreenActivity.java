package me.domin.bilock;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.am.siriview.DrawView;
import com.am.siriview.UpdaterThread;
import com.dnkilic.waveform.WaveView;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.OnClickListener;
import com.orhanobut.dialogplus.OnItemClickListener;
import com.orhanobut.dialogplus.ViewHolder;
import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;
import com.stephentuso.welcome.WelcomeHelper;
import com.wang.avi.AVLoadingIndicatorView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import at.markushi.ui.CircleButton;
import butterknife.BindView;
import me.wangyuwei.particleview.ParticleView;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class LockScreenActivity extends AppCompatActivity implements LockContract.View {

    private static final String TAG = "LockScreen";
    ShimmerTextView mShimmerTextView;
//    WaveView waveView;
    Vibrator vibrator;  //手机震动器
    Intent intentToMain;
    LockPresenter mPresenter;

    CircleButton button;
    ImageView setting;

    @Override
    public void updateMax(int max) {

    }

    AVLoadingIndicatorView load;
    //    WaveRecordFragment fragment;
    ImageView imageView;
    TextView textView;


    HashMap musicId = new HashMap();

    //设置音效池的属性
    AudioAttributes audioAttributes = new AudioAttributes.Builder()
            //设置音效使用场景
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
//设置音效类型                                     .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build();

    //创建SoundPool对象
    @SuppressLint("NewApi")
    SoundPool soundPool = new SoundPool.Builder()
            //设置音效池属性
            .setAudioAttributes(audioAttributes)
            //设置音效类型
            .setMaxStreams(10)
            .build();

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mPresenter.stopRecorder();
        finish();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.lock_layout);
        intentToMain = new Intent(LockScreenActivity.this, MainActivity.class);
//        fragment = (WaveRecordFragment) getSupportFragmentManager().findFragmentById(R.id.wave_record);
        mPresenter = new LockPresenter(this);
        textView = findViewById(R.id.profile_name);
        button = findViewById(R.id.button);
        load = findViewById(R.id.load);
        imageView = findViewById(R.id.profile_image);
        setting = findViewById(R.id.settings);
//        waveView = findViewById(R.id.waveview);

//        mShimmerTextView = findViewById(R.id.shimmer);
//        (new Shimmer()).start(mShimmerTextView);

        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);

//        button.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View view) {
//                Log.e(TAG, "onLongClick: yes");
//                Toast.makeText(LockScreenActivity.this, "Start.", Toast.LENGTH_SHORT).show();
//                mPresenter.startRecord();
//                load.show();
//                return false;
//            }
//        });
        /*
            创建一个更换用户对话，点击是则把原用户数据删除，否则取消
         */
        final DialogPlus dialog = DialogPlus.newDialog(this)
                .setContentHolder(new ViewHolder(R.layout.content))
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(DialogPlus dialog, View view) {
                        switch (view.getId()){
                            case R.id.yes:
                                //Intent intent = new Intent(LockScreenActivity.this,StartActivity.class);
                                //startActivity(intent);
                                String username = "user";
                                String path = FileUtil.getUserPath();
                                File file = new File(path);
                                File[] files = file.listFiles();
                                for (int i = 0; i < files.length; i++) {
                                    File file1 = files[i];
                                    file1.delete();
                                }
                                onStop();

                                break;
                            case R.id.no:
                                dialog.dismiss();
                                break;
                        }
                    }
                })
                .setExpanded(true)  // This will enable the expand feature, (similar to android L share dialog)
                .create();
        setting.bringToFront();
        /*
        将对话绑定到按钮
         */
        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
            }
        });

        musicId.put(1, soundPool.load(this, R.raw.unlock, 1));

        //头像图标增加隐藏按钮到MainActivity
        ImageView imageView = findViewById(R.id.profile_image);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
  //              startActivity(intentToMain);

            }
        });

    }

    void soundOfUnlock() {
        soundPool.play((Integer) musicId.get(1), 1, 1, 0, 0, 1);
    }

    //更新UI的间隔
    long REFRESH_INTERVAL_MS = 30;
    private boolean keepGoing = true;
    LinearLayout layout;
//    AudioRecord mRecorder = mPresenter.record;
    float tr = 400.0f;
    UpdaterThread up;
    DrawView view;



    private long redraw() {
        long t = System.currentTimeMillis();
        display_game();
        return System.currentTimeMillis() - t;
    }

    private void display_game() {
        runOnUiThread(new Runnable() {
            public void run() {
                LockScreenActivity.this.view.setMaxAmplitude(((float) mPresenter.getMax() * 0.7f));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume: start");
        mPresenter.startRecord(LockPresenter.NONE);

        //用于申请该活动所有的权限
        LockScreenActivityPermissionsDispatcher.askForPermissionWithPermissionCheck(this);
        //开启线程，实时更新画面
        this.view = (DrawView) findViewById(R.id.root);
        new Thread(new Runnable() {
            public void run() {
                while (LockScreenActivity.this.keepGoing) {
                    try {
                        Thread.sleep(Math.max(0, LockScreenActivity.this.REFRESH_INTERVAL_MS - LockScreenActivity.this.redraw()));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @NeedsPermission({Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.VIBRATE
    })
    void askForPermission() {
        if (!mPresenter.hasModel())
            ;
     //       startActivity(intentToMain);
    }

    private void shake() {
        @SuppressLint("ObjectAnimatorBinding") ObjectAnimator objectAnimator = new ObjectAnimator().ofFloat(imageView, "translationX",
                0, 25, -25, 25, -25, 15, -15, 6, -6, 0).setDuration(1000);
        @SuppressLint("ObjectAnimatorBinding") ObjectAnimator objectAnimator2 = new ObjectAnimator().ofFloat(textView, "translationX",
                0, 25, -25, 25, -25, 15, -15, 6, -6, 0).setDuration(1000);
        objectAnimator.start();
        objectAnimator2.start();

        vibrator.vibrate(500);
    }


    @Override
    public void clear() {

    }

    @Override
    public InputStream getInputStream(String fileName) {
        return null;
    }

    @Override
    //Toast被动画掩盖，无法正常演示。
    public void unlockSuccess() {
        Toast.makeText(LockScreenActivity.this, "Welcome", Toast.LENGTH_SHORT).show();
        soundOfUnlock();
        System.exit(0);
    }

    @Override
    public void unlockFail() {
        shake();
        Toast.makeText(LockScreenActivity.this, "Wrong Pin code", Toast.LENGTH_SHORT).show();
        mPresenter.currentRecordTaskNew();
    }

}
