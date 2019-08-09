package me.domin.bilock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.administrator.mfcc.MFCC;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
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

    private static final String TAG="Test2Activity";

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
    double rightRate=0;

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
                //trainForTest(USER);
            }
        });
        bt_other.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPresenter.trainData(OTHER);
                //trainForTest(OTHER);
            }
        });
        bt_train.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toast("start training!");
                mPresenter.trainModel();
                //trainForTest(USER);

            }
        });

        bt_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toast("test start!");
                 lockPresenter.initRecorder();
                 lockPresenter.currentRecordTaskNew();
                //testForTest();

            }
        });
    }

    private double[] sd_normal(double[] feature){
        double[] sd=TrainPresenter.sd;
        for(int i=0;i<MFCC.LEN_MELREC;i++){
            feature[i]=feature[i]/sd[i];
        }
        return feature;
    }
    private double[] sum_normal(double[] feature){
        double[] sum=TrainPresenter.sum;
        for(int i=0;i<MFCC.LEN_MELREC;i++){
            feature[i]=feature[i]/sum[i];
        }
        return feature;
    }
    private double[] mean_normal(double[] feature){
        double[] max=TrainPresenter.max;
        double[] min=TrainPresenter.min;
        for(int i=0;i<MFCC.LEN_MELREC;i++){
            feature[i]=(feature[i]-min[i])/(max[i]-min[i]);
        }
        return feature;
    }
    private double[] z_score(double[] feature){
        double[] variance=TrainPresenter.variance;
        double[] average=TrainPresenter.average;
        for(int i=0;i<MFCC.LEN_MELREC;i++){
            feature[i]=(feature[i]-average[i])/variance[i];
        }
        return feature;
    }
    public void preProcess(File file) throws IOException{
        BufferedInputStream  bis=new BufferedInputStream(new FileInputStream(file));
        byte[] buffer=new byte[2048];
        int length;
        length=bis.read(buffer);
        bis.close();
        String str=new String(buffer,0,length);
        String[] data=str.split(" ");
        double[] features=new double[MFCC.LEN_MELREC];
        for(int i=0;i<MFCC.LEN_MELREC;i++){
            String[] ss=data[i].split(":");
            if(ss.length>=2){
                features[i]=Double.parseDouble(ss[1]);
            }
        }


        //features=mean_normal(features);
        features=sum_normal(features);


        BufferedWriter bw=new BufferedWriter(new FileWriter(file));
        for(int i=0;i<features.length;i++){
            bw.write(String.valueOf(i+1));
            bw.write(":"+features[i]+" ");
        }
        bw.flush();
        bw.close();
    }
    public void testForTest(){

        try {
            File parent=new File(FileUtil.getFilePathName(FileUtil.TEST_PATH));
            File[] files=parent.listFiles();
            for(File file:files){
                if(file.getName().contains("ModelFeature")){
                    preProcess(file);
                    if ((MFCC.svmPredict(FileUtil.getFilePathName(FileUtil.TEST_FEATURE))) == 1) {
                        unlockSuccess();
                    } else {
                        unlockFail();
                    }
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void trainForTest(int type){
        File parent=null;
        if(type==USER){
            parent=new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Bilock/data/user");
        }else
            parent=new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Bilock/data/noise");
        File[] files=parent.listFiles();

        FileInputStream fis=null;
        byte[] buffer=new byte[1024];
        double[] data=new double[300000];
        Double[] featureDouble = null;
        int length=0;
        int data_length=0;
        for(File file:files){
            try {
                data_length=0;
                fis = new FileInputStream(file);
                fis.skip(44);
                while((length=fis.read(buffer))!=-1){
                    for(int i=0;i<length;i=i+2){
                        data[data_length]=(double)(((short)buffer[i+1]<<8)|((short)buffer[i]&0xff));
                        data_length++;
                    }
                }

                String path=FileUtil.getFilePathName(FileUtil.MODEL_RECORD);
                try {

                    featureDouble = MFCC.mfcc(path, data , data_length, 44100);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                BufferedWriter bw=null;
                try {
                    bw = new BufferedWriter(new FileWriter(FileUtil.getFilePathName(FileUtil.MODEL_FEATURE)));
                } catch (IOException e) {
                    Log.e(TAG, "run: record error");
                }
                //将所有MFCC特征写入文件
                //将数据存入文件
                try {
                    if(type==USER)
                        bw.write(1 + " ");
                    else{
                        bw.write(-1+" ");
                    }
                    for (int i = 0; i < featureDouble.length; i++) {
                        bw.write((i + 1) + ":" + String.valueOf(featureDouble[i]));
                        if (i != featureDouble.length - 1)
                            bw.write(" ");
                        else
                            bw.write("\n");
//                Log.d(TAG, "writeModel: feature = " + feature[i]);
                    }

                    bw.write(" \n");
                    bw.flush();

                } catch (IOException e) {
                    Log.e(TAG, "writeData: ioexception" );
                }
            }catch (IOException e){
                Log.e(TAG, "trainForTest: error while reading wav files");
            }
        }
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
        rightRate=(double)right/total;
        tv_data.setText("Total: "+total+" \r\n Right:"+right+"\r\n Wrong: "+wrong+"\r\n Correct rate: "+rightRate*100+"%");
    }
}
