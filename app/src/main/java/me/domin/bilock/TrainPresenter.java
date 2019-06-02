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

public class TrainPresenter implements TrainContract.Presenter{

    TrainContract.View view;
    private String username = "user";
    //需写入util
    public String path = LockPresenter.absolutePath + "/Bilock/" + username + File.separator;

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
    public void trainData() {
        //权限
        initRecorder();
        ExecutorService executor = Executors.newFixedThreadPool(5);
        executor.execute(new RecordTask());
    }
    @NeedsPermission({Manifest.permission.RECORD_AUDIO})
    public void initRecorder() {
        record = new AudioRecord(6, sampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, 2 * bufferSampleSize);
        record.startRecording();
        bufferSampleSize = (int) Math.ceil(1.0 * sampleRate / bufferSampleSize) * bufferSampleSize;
    }

    @Override
    public void trainModel() {

    }
    class RecordTask implements Runnable {
        WavWriter wavWriter = new WavWriter("/MFCC/", sampleRate);


        @NeedsPermission({Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO})
        @Override
        public void run() {
            wavWriter.start();
            int fileNumber = 0;

            while (fileNumber < 10) {

                int max = 0;
                int num = 0;
                while (num != 2) {
                    numOfReadShort = record.read(audioSamples, 0, readChunkSize);   // pulling
                    max = wavWriter.pushAudioShortNew(audioSamples, numOfReadShort);  // Maybe move this to another thread?
                    if (max == -1)
                        num++;
                }


                int[] singal = wavWriter.getSignal();
                BufferedWriter bw = null;

                double[] buffer = new double[singal.length];
                for (int i = 0; i < singal.length; i++) {
                    buffer[i] = singal[i];
                }

                DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH'h'mm'm'ss.SSS's'", Locale.US);
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
        private BufferedWriter createBufferedWriter(String path, String name) throws IOException {
            File file = new File(path + name);
            if (!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            return new BufferedWriter(new FileWriter(file));
        }
    }
}
