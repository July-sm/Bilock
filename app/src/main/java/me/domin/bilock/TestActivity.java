package me.domin.bilock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

//用于测试的Activity
@RuntimePermissions
public class TestActivity extends AppCompatActivity implements LockContract.View {

    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
    String date = df.format(new Date());// new Date()为获取当前系统时间，也可使用当前时间戳
    LockPresenter mPresenter;

    @BindView(R.id.bt_svm_train)
    Button btGetPeaks;
    @BindView(R.id.mode_number)
    TextView tvShowModeNumber;
    @BindView(R.id.bt_text)
    Button btText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mPresenter = new LockPresenter(this);

        //隐藏actionBar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        TestActivityPermissionsDispatcher.askForPermissionWithPermissionCheck(this);
    }

    //检查权限
    @NeedsPermission({Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO})
    protected void askForPermission() {
        super.onResume();
        updateTextView();
    }

    @SuppressLint("SetTextI18n")
    void updateTextView() {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        TestActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }


    //显示峰值
    @OnClick(R.id.bt_svm_train)
    void svmTrain() {
        mPresenter.svmTrain();
    }

    @OnClick(R.id.button_train)
    public void train() {
        mPresenter.trainData();
        Toast.makeText(this, "Training Start", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void unlockSuccess() {

    }

    @Override
    public void unlockFail() {

    }

}
