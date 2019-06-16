package me.domin.bilock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.administrator.mfcc.MFCC;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
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
    public void trainData() {
        //权限
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

    @Override
    public void trainModel() {

    }
    /**
     　　* @ClassName: RecordTask
     　　* @Description: 用于读取录音数据，用WavWriter处理数据，提取特征，得到10个声音样本
     　　* @author Administrator
     　　*/
    class RecordTask implements Runnable {
        WavWriter wavWriter = new WavWriter(WavWriter.MODEL, sampleRate);


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


                //获取刚记录的2个峰值段
                int[] singal = wavWriter.getSignal();
                BufferedWriter bw = null;

                double[] buffer = new double[singal.length];
                for (int i = 0; i < singal.length; i++) {
                    buffer[i] = singal[i];
                }

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
                try {
                    featureDouble = MFCC.mfcc(FileUtil.getFilePathName(FileUtil.MODEL_RECORD), buffer, singal.length, 44100);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    bw = createBufferedWriter(FileUtil.getFilePathName(FileUtil.MODEL_FEATURE));
                } catch (IOException e) {
                    e.printStackTrace();
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
                bw.write(1 + " ");
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
                e.printStackTrace();
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
