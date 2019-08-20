package me.domin.bilock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

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

    public static final int NONE=0;
    public static final int Z_SCORE=1;
    public static final int SUM=2;
    public static final int MEAN=3;
    public static final int SD=4;

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
    @BindView(R.id.bt_user_offline)
    Button bt_user_offline;
    @BindView(R.id.bt_other_offline)
    Button bt_other_offline;
    @BindView(R.id.bt_test_offline)
    Button bt_test_offline;


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
                mPresenter.trainModel(NONE);
            }
        });

        bt_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toast("test start!");
                 lockPresenter.initRecorder();
                lockPresenter.currentRecordTaskNew();
               // testForTest();

            }
        });
        bt_user_offline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                trainForTest(USER);
            }
        });
        bt_other_offline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                trainForTest(OTHER);
            }
        });
        bt_test_offline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                train();
                //testForTest();
            }
        });
    }

    private Double[] sd_normal(Double[] feature){
        double[] sd=TrainPresenter.sd;
        for(int i=0;i<MFCC.LEN_MELREC;i++){
            feature[i]=feature[i]/sd[i];
        }
        return feature;
    }
    private Double[] sum_normal(Double[] feature){
        double[] sum=TrainPresenter.sum;
        for(int i=0;i<MFCC.LEN_MELREC;i++){
            feature[i]=feature[i]/sum[i];
        }
        return feature;
    }
    private Double[] mean_normal(Double[] feature){
        double[] max=TrainPresenter.max;
        double[] min=TrainPresenter.min;
        for(int i=0;i<MFCC.LEN_MELREC;i++){
            feature[i]=(feature[i]-min[i])/(max[i]-min[i]);
        }
        return feature;
    }
    private Double[] z_score(Double[] feature){
        double[] variance=TrainPresenter.variance;
        double[] average=TrainPresenter.average;
        for(int i=0;i<MFCC.LEN_MELREC;i++){
            feature[i]=(feature[i]-average[i])/variance[i];
        }
        return feature;
    }
    public String preProcess(Double[] features) throws IOException{


        //features=z_score(features);
       //features=mean_normal(features);
//         features=sd_normal(features);
//        features=sum_normal(features);
        String filename=FileUtil.getFilePathName(FileUtil.TEST_FEATURE);
        File file=new File(filename);
        if(!file.exists())
            file.createNewFile();


        BufferedWriter bw=new BufferedWriter(new FileWriter(file));
        for(int i=0;i<features.length;i++){
            bw.write(String.valueOf(i+1));
            bw.write(":"+features[i]+" ");
        }
        bw.flush();
        bw.close();
        return filename;
    }
    @SuppressLint("NewApi")
    public void testForTest(int type,int normal_type){

        File parent=null;
        try {
            if(type==USER)
                parent=new File(FileUtil.absolutePath+"/data/user/test");
            else
                parent=new File(FileUtil.absolutePath+"/data/noise/test");
            File[] files=parent.listFiles();
            for(File file:files){
                if(file.getName().contains("Record")){
                    FileInputStream fis=null;
                    byte[] buffer=new byte[2048];
                    Double[] featureDouble = null;
                    int length=0;
                    int data_length=0;
                        try {
                            data_length=0;
                            fis = new FileInputStream(file);
                            fis.skip(44);
                            while((length=fis.read(buffer))!=-1){
                                for(int i=0;i<length;i=i+2){
                                    //  data[data_length]=(double)(((short)buffer[i+1]<<8)|((short)buffer[i]&0xff));
                                    data_length++;
                                }
                            }
                            String path=FileUtil.getFilePathName(FileUtil.TEST_FEATURE);
                            featureDouble = MFCC.mfcc(path, buffer , data_length, 44100);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            if(KNN(featureDouble,normal_type)){
                                //toast("Unlock successfully!");
                                right++;
                                total++;
                                //updateData();
                            }else{
                                //toast("Unlock Fail!");
                                wrong++;
                                total++;
                                //updateData();
                            }
                   /* String path=preProcess(featureDouble);
                    if ((MFCC.svmPredict(path) == 1)) {
                        toast("Unlock successfully!");
                        right++;
                        total++;
                        updateData();
                    } else {
                        toast("Unlock Fail!");
                        wrong++;
                        total++;
                        updateData();
                    }*/
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean KNN(Double[] features,final int normal_type){
        switch(normal_type){
            case Z_SCORE:  features=z_score(features);break;
            case MEAN: features=mean_normal(features);break;
            case SUM:    features=sum_normal(features);break;
            case SD: features=sd_normal(features);break;
            default:;
        }
        double[][] values=TrainPresenter.values;
        int[] user=TrainPresenter.user;
        ArrayList list=new ArrayList();
        for(int i=0;i<TrainPresenter.count;i++){
            Point p=new Point(values[i],user[i]);
            p.calDis(features);
            list.add(p);
        }
        list.sort(new Comparator() {
            @Override
            public int compare(Object o, Object t1) {
                double dis1=((Point)o).distance;
                double dis2=((Point)t1).distance;
                if(dis1>dis2)
                    return 1;
                else if(dis1==dis2)
                    return 0;
                else
                    return -1;
            }
        });
        int count=0;
        for(int i=0;i<10;i++){
            if(((Point)list.get(i)).user==1)
                count++;
            else
                count--;
        }
        if(count>=0)
            return true;
        else
            return false;
    }
    static public class Point{
        double[] values;
        int user;
        double distance;
        public Point(double[] values,int user){
            this.values=values;
            this.user=user;
        }
        public void calDis(Double[] features){
            distance=0;
            for(int i=0;i<MFCC.LEN_MELREC;i++){
                distance+=Math.pow(features[i]-values[i],2);
            }
            distance=Math.sqrt(distance);
        }
    }

    public void train(){
        File user_train=new File(FileUtil.absolutePath+"/data/user/train");
        File user_test=new File(FileUtil.absolutePath+"/data/user/test/");
        File noise_train=new File(FileUtil.absolutePath+"/data/noise/train");
        File noise_test=new File(FileUtil.absolutePath+"/data/noise/test");
        File result=new File(FileUtil.absolutePath+"result.txt");
        BufferedWriter bw=null;
        try{
            if(!result.exists())
                result.createNewFile();
          bw=new BufferedWriter(new FileWriter(result));

        Random random=new Random();
        int user_sample_num = 10;
        int noise_sample_num=10;
        int normal_type=NONE;

        for(normal_type=NONE;normal_type<=SD;normal_type++){

            user_sample_num=10;
            File[] user_sample=user_train.listFiles();
            do{
                user_sample=user_train.listFiles();
                File file=user_sample[random.nextInt(user_sample.length)];
                file.renameTo(new File(user_test,file.getName()));
            }while (user_sample.length>10);
            for(;user_sample_num<=50;){
                noise_sample_num=10;
                for(;noise_sample_num<=50;){

                    clearFile();
                    trainForTest(USER);
                    trainForTest(OTHER);
                    mPresenter.trainModel(normal_type);
                    bw.write("legal sample:"+user_sample_num+",illegal sample:"+noise_sample_num+";");
                    testForTest(USER,normal_type);
                    rightRate=(double)(right)/(double)total;
                    double legal_rightRate=rightRate;
                    bw.write("legal: right num:"+right+",wrong num:"+wrong+",accuracy:"+String.valueOf(rightRate));
                    right=wrong=0;
                    rightRate=0;
                    total=0;
                    testForTest(OTHER,normal_type);
                    rightRate=(double)(wrong)/(double)total;
                    double illegal_rightRate=rightRate;

                    if(legal_rightRate>0.85&&illegal_rightRate>0.85){
                        bw.write("normal_type:"+normal_type+", legal sample:"+user_sample_num+",illegal sample:"+noise_sample_num+",legal accuracy:"+legal_rightRate+",illegal accuracy:"+illegal_rightRate);
                        bw.write("\r\n");
                        bw.flush();
                    }


                    noise_sample_num+=5;
                    File[] noise_sample=noise_test.listFiles();
                    int i=5;
                    while(i>0){
                        File file=noise_sample[random.nextInt(noise_sample.length)];
                        if(file.getParent().contains("test")){
                            file.renameTo(new File(noise_train,file.getName()));
                            i--;
                        }
                    }
                }
                File[] noise=noise_train.listFiles();
                for(int k=10;k<noise.length;k++){
                    File file=noise[k];
                    file.renameTo(new File(noise_test,file.getName()));
                }
                user_sample_num += 5;
                user_sample=user_test.listFiles();
                if(user_sample.length==0)
                    break;
                int i=5;
                while(i>0){
                    File file=user_sample[random.nextInt(user_sample.length)];
                    if(file.getParent().contains("test")){
                        file.renameTo(new File(user_train,file.getName()));
                        i--;
                    }
                }
            }
        }

            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri uri=Uri.fromFile(result);
            intent.setData(uri);
            getApplicationContext().sendBroadcast(intent);

            bw.close();
        }catch (IOException e){
            Log.e(TAG, "train: "+e.getMessage() );
        }
    }
    public void clearFile(){
        File[] files=new File(FileUtil.getFilePathName(FileUtil.MODEL_PATH)).listFiles();
        for(File file:files){
            file.delete();
        }
        toast("clear finished");
        right=wrong=total=0;
        rightRate=0;
    }
    public void trainForTest(int type){
        File parent=null;
        if(type==USER){
            parent=new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Bilock/data/user/train");
        }else
            parent=new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Bilock/data/noise/train");
        File[] files=parent.listFiles();

        ByteArrayOutputStream bos=new ByteArrayOutputStream();
        FileInputStream fis=null;
        byte[] buffer=new byte[2048];
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
                      //  data[data_length]=(double)(((short)buffer[i+1]<<8)|((short)buffer[i]&0xff));
                        data_length++;
                    }
                }
                String path=FileUtil.getFilePathName(FileUtil.MODEL_RECORD);
                try {

                    featureDouble = MFCC.mfcc(path, buffer , data_length, 44100);
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
