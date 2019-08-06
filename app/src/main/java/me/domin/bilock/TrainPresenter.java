package me.domin.bilock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.administrator.mfcc.MFCC;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import permissions.dispatcher.NeedsPermission;

import static android.content.ContentValues.TAG;

/**
 　　* @ClassName: TrainPresenter
 　　* @Description: 处理TrainActivity的逻辑业务，实现录音、获取训练样本的功能
 　　* @author Administrator
 　　*/
public class TrainPresenter implements TrainContract.Presenter{

    TrainContract.View view;

    public final String TAG="TrainPresenter";
    public final int USER=1;
    public int type=0;

    public TrainPresenter(TrainContract.View trainView){
        this.view=trainView;
    }
    public AudioRecord record;
    int sampleRate = 44100;
    int fftlen = 1024;
    int minBytes = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT);
    int numOfReadShort;
    int readChunkSize = fftlen;  // Every hopLen one fft result (overlapped analyze window)
    short[] audioSamples = new short[readChunkSize];
    int bufferSampleSize = Math.max(minBytes / 2, fftlen / 2) * 2;

    @SuppressLint("SetTextI18n")
    @NeedsPermission({Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO})
    /**
        * @Title: trainData
    　　* @Description: 初始化录音器，开启一个线程池，执行RecordTask录音任务。但世界上只用了1个线程
    　　* @param []
    　　* @return void
    　　*/
    public void trainData(int type) {
        //权限
        this.type=type;
        initRecorder();
        ExecutorService executor = Executors.newFixedThreadPool(5);
        executor.execute(new RecordTask());
    }
    @NeedsPermission({Manifest.permission.RECORD_AUDIO})
    /**
        * @Title: initRecorder
    　　* @Description: 初始化录音器，设置采样值等信息
    　　* @param []
    　　* @return void
    　　*/
    public void initRecorder() {
        record = new AudioRecord(6, sampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, 2 * bufferSampleSize);
        record.startRecording();
        bufferSampleSize = (int) Math.ceil(1.0 * sampleRate / bufferSampleSize) * bufferSampleSize;
    }

    @NeedsPermission({Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO})
    @Override
    public void trainModel() {
       /* BufferedOutputStream bw;
        BufferedInputStream br;
        File modelData=new File(FileUtil.getFilePathName(FileUtil.MODEL_DATA));
        File[] features=new File(FileUtil.getFilePathName(FileUtil.MODEL_PATH)).listFiles();
        byte[] buffer=new byte[4150];
        int length=0;

        float[][] values=new float[50][MFCC.LEN_MELREC];
        float[] average=new float[MFCC.LEN_MELREC];
        int[] user=new int[50];
        int count=0;
        double value;

        for(int i=0;i<MFCC.LEN_MELREC;i++)
            average[i]=0;
        try{
            bw=new BufferedOutputStream(new FileOutputStream(modelData));
            for(int j=0;j<features.length;j++) {
                File feature=features[j];
                if (feature.getName().contains("ModelFeature")&&feature.getName().contains(".txt")&&!feature.getName().contains("ORIG")) {
                    br = new BufferedInputStream(new FileInputStream(feature));
//                    dis=new DataInputStream(new FileInputStream(feature));
//                    dis.readInt();
                    length=br.read(buffer);

                    String str=new String(buffer,0,length);
                    String[] strs=str.split(" ");
                    user[count]=Integer.parseInt(strs[0]);
                    for(int k=0;k<strs.length;k++){
                        String[] ss=strs[k].split(":");
                        if(ss.length>=2){
                            values[count][Integer.parseInt(ss[0])-1]=Float.parseFloat(ss[1]);
                            average[Integer.parseInt(ss[0])-1]+=Float.parseFloat(ss[1]);
                        }
                    }
                    count++;
                    //length = br.read(buffer, 0, 1024);
                    //bw.write(buffer, 0, length);
                    br.close();
                    //dis.close();
                }
            }
            for(int i=0;i<MFCC.LEN_MELREC;i++){
                average[i]/=count;
            }

            values=z_score(values,average,count);

            for(int i=1;i<=count;i++){
                StringBuilder sb=new StringBuilder();
                sb.append(user[i-1]);
                sb.append(" ");
                for(int j=1;j<=MFCC.LEN_MELREC;j++){
                    sb.append(j);
                    sb.append(":");
                    sb.append(values[i-1][j-1]);
                    sb.append(" ");
                }
                sb.append("\r\n");
                bw.write(sb.toString().getBytes("UTF-8"));
            }
            bw.flush();
            bw.close();*/
       try{
            MFCC.svmTrain();
            view.finishTrain();
        }catch (IOException e){
            Log.e(TAG, "trainModel: error" );
        }
    }

    private float[][] z_score(float[][] values,float[] average,int count){
        float[] variance=new float[MFCC.LEN_MELREC];

        for(int i=0;i<count;i++){
            for(int j=0;j<MFCC.LEN_MELREC;j++){
                variance[j]+=Math.pow((values[i][j]-average[j]),2);
            }
        }
        for(int j=0;j<MFCC.LEN_MELREC;j++){
            variance[j]=(float)Math.sqrt((double)variance[j]/count);
        }
        for(int i=0;i<count;i++){
            for(int j=0;j<MFCC.LEN_MELREC;j++){
                values[i][j]=(values[i][j]-average[j])/variance[j];
            }
        }

        return values;
    }

    public void trainForTest(){
        File[] files=(new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Bilock/user/")).listFiles();

        FileInputStream fis=null;
        byte[] buffer=new byte[1024];
        double[] feature=new double[]
        int length=0;
        for(File file:files){
            try {
                fis = new FileInputStream(file);
                fis.skip(44);
                while((length=fis.read(buffer))!=-1){
                    for(int i=0;i<length;i=i+2){
                        feature[i/2]=(buffer[i+1]<<8)+buffer[i];
                    }
                }
                Double[] featureDouble = null;
                String path=FileUtil.getFilePathName(FileUtil.MODEL_RECORD);
                try {

                    featureDouble = MFCC.mfcc(path,   , feature.length, 44100);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    bw = createBufferedWriter(FileUtil.getFilePathName(FileUtil.MODEL_FEATURE));
                } catch (IOException e) {
                    Log.e(TAG, "run: record error");
                }
                //将所有MFCC特征写入文件
                //将数据存入文件
                writeData(featureDouble, bw);
            }catch (IOException e){
                Log.e(TAG, "trainForTest: error while reading wav files");
            }
        }
    }
    /**
     　　* @ClassName: RecordTask
     　　* @Description: 用于读取录音数据，用WavWriter处理数据，提取特征，得到10个声音样本
     　　* @author Administrator
     　　*/
    class RecordTask implements Runnable {
        WavWriter wavWriter = new WavWriter(WavWriter.MODEL, sampleRate);
        WavWriter shortWriter=new WavWriter(WavWriter.MODEL,sampleRate);

        @NeedsPermission({Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO})
        @Override
        public void run() {
            wavWriter.start();
            int fileNumber = 0;

            //总共写10个声音文件
            while (fileNumber < 10) {

                int max = 0;
                int num = 0;
                //每个声音样本包括2个峰值段，由wavWriter记录
                while (num != 2) {
                    numOfReadShort = record.read(audioSamples, 0, readChunkSize);   // pulling
                    max = wavWriter.pushAudioShortNew(audioSamples, numOfReadShort);  // Maybe move this to another thread?
                    //max返回-1表示有提取峰值段
                    if (max == -1)
                        num++;
                }


                int[] signal = wavWriter.getSignal();
                BufferedWriter bw = null;



                double[] buffer = new double[signal.length];
                for (int i = 0; i < signal.length; i++) {
                    buffer[i] = signal[i];
                }


                wavWriter.list.clear();




                /*DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH'h'mm'm'ss.SSS's'", Locale.US);
                String nowStr = df.format(new Date());
                Double[] featureDouble = null;
                try {
                    featureDouble = MFCC.mfcc(LockPresenter.absolutePath + "/MFCC/Feature.txt", buffer, singal.length, 44100);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    bw = createBufferedWriter(LockPresenter.absolutePath, "/Bilock/" + username + "/" + nowStr + ".txt");
                } catch (IOException e) {
                    e.printStackTrace();
                }*/

                /*
                    修改日期：2019/6/5
                    修改内容：改变写入的文件路径
                    修改人：July
                 */
                //提取特征值
                Double[] featureDouble = null;
                 String path=FileUtil.getFilePathName(FileUtil.MODEL_RECORD);
                try {

                    featureDouble = MFCC.mfcc(path, buffer, signal.length, 44100);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //获取刚记录的2个峰值段
               /* new Thread(){
                    @Override
                    public void run() {
                        super.run();*/
                        shortWriter.setPathandStart(path.substring(0,path.indexOf(".txt"))+".wav");
                        shortWriter.write(signal,signal.length);
                        shortWriter.stop();
               /*     }
                }.start();*/




                try {
                    bw = createBufferedWriter(FileUtil.getFilePathName(FileUtil.MODEL_FEATURE));
                } catch (IOException e) {
                    Log.e(TAG, "run: record error");
                }
                //将所有MFCC特征写入文件
                //将数据存入文件
                writeData(featureDouble, bw);

                fileNumber++;
                view.changeNum(fileNumber);
            }
            synchronized (this) {
                try {
                    new Thread().sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            record.stop();
            wavWriter.stop();
            record.release();
            view.finishTrain();
        }
        private void writeData(Double feature[], BufferedWriter bw) {

            try {
                if(type==USER)
                    bw.write(1 + " ");
                else{
                    bw.write(-1+" ");
                }
                for (int i = 0; i < feature.length; i++) {
                    bw.write((i + 1) + ":" + String.valueOf(feature[i]));
                    if (i != feature.length - 1)
                        bw.write(" ");
                    else bw.write("\n");
//                Log.d(TAG, "writeModel: feature = " + feature[i]);
                }

                bw.write(" \n");
                bw.flush();

            } catch (IOException e) {
                Log.e(TAG, "writeData: ioexception" );
            }
        }
        /*
            修改日期：2019/6/5
            内容：改变参数
            修改人：July
         */
        private BufferedWriter createBufferedWriter(String name) throws IOException {
            File file = new File( name);
            if (!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            return new BufferedWriter(new FileWriter(file));
        }
    }
}
