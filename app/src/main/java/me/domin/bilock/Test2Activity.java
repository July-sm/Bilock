package me.domin.bilock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class Test2Activity extends AppCompatActivity implements TrainContract.View,LockContract.View{

    TrainPresenter mPresenter;
    LockPresenter lockPresenter;
    @BindView(R.id.bt_clear_all)
    Button bt_clear_all;

    @BindView(R.id.bt_record)
    Button bt_record;
    @BindView(R.id.bt_clear_other)
    Button bt_clear_other;
    @BindView(R.id.bt_clear_user)
    Button bt_clear_user;
    @BindView(R.id.bt_train)
    Button bt_train;
    @BindView(R.id.bt_test)
    Button bt_test;
    @BindView(R.id.tv_data)
    TextView tv_data;
    @BindView(R.id.bt_other)
    Button bt_other;


    static final public int CHANGE_NUM=0,MAX=1;
    static final public int USER=1,OTHER=2;
    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case CHANGE_NUM:
                    int num=(int)msg.obj;
                    toast("File number:"+num);
                    break;
                case MAX:
                    toast("train finish!");

            }
        }
    };

    int right=0,wrong=0,total=0;
    float rightRate=0;

    @SuppressLint("SetTextI18n")
    @NeedsPermission({Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO})

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test2);
        mPresenter=new TrainPresenter(this);
        lockPresenter=new LockPresenter(this);
        ButterKnife.bind(this);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();


        bt_clear_all.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                File[] files=new File(FileUtil.getFilePathName(FileUtil.MODEL_PATH)).listFiles();
                for(File file:files){
                    file.delete();
                }
                toast("clear finished");
                right=wrong=total=0;
                rightRate=0;
            }
        });
        bt_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPresenter.trainData(USER);
            }
        });
        bt_other.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPresenter.trainData(OTHER);
            }
        });
        bt_train.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toast("start training!");
                mPresenter.trainModel();

            }
        });

        bt_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toast("test start!");
                 lockPresenter.initRecorder();
                 lockPresenter.currentRecordTaskNew();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
      //  lockPresenter.initRecorder();
       // lockPresenter.currentRecordTaskNew();
    }

    @Override
    protected void onStop() {
        super.onStop();
        lockPresenter.stopRecorder();
    }

    public void toast(String str){
        Toast.makeText(getApplicationContext(), str,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void changeNum(int fileNum) {
        Message message=new Message();
        message.what=CHANGE_NUM;
        message.obj=fileNum;
        handler.sendMessage(message);
    }


    @Override
    public void finishTrain() {
            handler.sendEmptyMessage(MAX);
    }

    @Override
    public void clear() {

    }

    @Override
    public InputStream getInputStream(String fileName) {
        return null;
    }

    @Override
    public void unlockSuccess() {
        toast("Unlock successfully!");
        right++;
        total++;
        updateData();
        lockPresenter.stopRecorder();
    }

    @Override
    public void unlockFail() {
       // lockPresenter.currentRecordTaskNew();
        toast("Unlock Fail!");
        wrong++;
        total++;
        updateData();
        lockPresenter.stopRecorder();
    }
    public void updateData(){
        rightRate=(float)right/total;
        tv_data.setText("Total: "+total+" \r\n Right:"+right+"\r\n Wrong: "+wrong+"\r\n Correct rate: "+rightRate*100+"%");
    }
}
