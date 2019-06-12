package me.domin.bilock;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
//import com.chrischen.waveview.WaveView;
import com.example.administrator.mfcc.MFCC;
//import com.maple.recorder.recording.AudioChunk;
//import com.maple.recorder.recording.AudioRecordConfig;
//import com.maple.recorder.recording.MsRecorder;
//import com.maple.recorder.recording.PullTransport;
//import com.maple.recorder.recording.Recorder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


import github.bewantbe.audio_analyzer_for_android.STFT;

import static android.content.ContentValues.TAG;

/**
 * Created by Administrator on 2018/4/14.
 */

public class LockPresenter implements LockContract.Presenter {

    int TEST_MFCC_FILE = 1;
    int CURRENT_TEST_MFCC = 2;
    int WRITE_MODEL_MFCC = 3;
    int TEST_DATA_MFCC = 4;
    int MODEL_FILE = 5;
    int TEST_FILE = 6;


    public static String absolutePath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Bilock/";
    private static String dictionaryPath = absolutePath + "/MFCC2/";
    public STFT stft;   // use with care

    private HashMap<Integer, String> mFileNameMap = new HashMap<>();

    private LockContract.View mLockView;


    private WaveFileReader mWaveReader;

    static {
        System.loadLibrary("native-lib");
    }

    private boolean isRecord = false;

    LockPresenter(LockContract.View lockView) {
        mLockView = lockView;
        mFileNameMap.put(TEST_MFCC_FILE, "TestMFCC.txt");
        mFileNameMap.put(CURRENT_TEST_MFCC, "testRecord.txt");
        mFileNameMap.put(WRITE_MODEL_MFCC, "model.txt");
        mFileNameMap.put(TEST_DATA_MFCC, "test.txt");
        mFileNameMap.put(TEST_FILE, "WaveRecord");
        mFileNameMap.put(MODEL_FILE, "ModeRecord");
    }

    public void initRecorder() {
        record = new AudioRecord(6, sampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, 2 * bufferSampleSize);
        record.startRecording();
        bufferSampleSize = (int) Math.ceil(1.0 * sampleRate / bufferSampleSize) * bufferSampleSize;
    }

    public void stopRecorder() {
        isRecord = false;
//        record.stop();
//        record.release();

    }

    @Override
    public void svmTrain() {
        try {
            MFCC.svmTrain();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopRecord() {

    }

    @Override
    public void startRecord() {

    }

    @Override
    public void currentRecord() {

    }


    @Override
    public void currentRecordTaskNew() {
        isRecord = true;
        new CurrentRecordTaskNew().execute();
    }




    @Override
    public boolean hasModel() {
        File file = new File(dictionaryPath);
        file.mkdir();
        File[] files = file.listFiles();
        for (File file1 : files) {
            if (file1.getName().equals("data_model.txt"))
                return true;
        }

        return false;
    }


    //判断录音是否可用
    @Override
    public boolean isRecordSuccess() {

        File file = new File(absolutePath + "TestRecord/waveRecord.wav");

        InputStream in = null;
        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        //波形出错
        mWaveReader = new WaveFileReader(" ", in);

        int[][] buffer = mWaveReader.getData();

        //将buffer转为double
        double[] signal = new double[buffer[0].length];
        //归一化
        for (int i = 0; i < buffer[0].length; i++) {
            signal[i] = buffer[0][i];
        }

        //得到峰值索引
        int result[] = getPeaks(signal);

        //得到峰值之间的数据
        Double[] featureDouble = null;
        double[] bufferBetween = getBufferBetween(result[0], result[1], signal);
        Log.e(TAG, "isRecordSuccess: 0 = " + signal[result[0]]);
        Log.e(TAG, "isRecordSuccess: 1 = " + signal[result[1]]);

        Log.e(TAG, "isRecordSuccess: buffer len = " + bufferBetween.length);

        try {
            BufferedWriter bw = createBufferedWriter(absolutePath + "/singal/"+"touch.txt" );
            for (int i = 0; i < bufferBetween.length; i++) {
                bw.write(bufferBetween[i] + " ");
            }
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*
            修改时间：2019/6/5
            修改内容：改变文件路径
            修改人：July
         */
        try {
            featureDouble = MFCC.mfcc(FileUtil.getFilePathName(FileUtil.TEST_RECORD), bufferBetween, bufferBetween.length, 44100);
//            featureDouble[featureDouble.length - 1] = getRMS(signal, result);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        normalizationData(maxBuffer, minBuffer, featureDouble);

//        File file2 = new File(dictionaryPath + "testRecord.txt");
        BufferedWriter bw = null;
        /*
            修改时间：2019/6/5
            修改内容：改变文件路径
            修改人：July
         */
        try {
            bw = createBufferedWriter(FileUtil.getFilePathName(FileUtil.TEST_FEATURE));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //将所有MFCC特征写入文件
        //将数据存入文件
//            Log.d(TAG, "writeModel: flag = " + flag + " len = " + feature.length);
        writeData(featureDouble, bw);

        try {
            return (MFCC.svmPredict(FileUtil.getFilePathName(FileUtil.TEST_FEATURE))) == 1;
        } catch (IOException e) {
            e.printStackTrace();
        }

//        file.delete();

        return false;
    }

    @Override
    public void writeModel() throws IOException {

    }


    private int[] getPeaks(double[] signal) {
        int[] result = new int[2];
        double max = 0;
        for (int i = 0; i < signal.length; i++) {
            if (max < signal[i]) {
                max = signal[i];
                result[0] = i;
            }
        }

        max = 0;
        for (int i = 0; i < signal.length; i++) {
            if (max < signal[i] && (i > result[0] + 1000 || i < result[0] - 1000)) {
                max = signal[i];
                result[1] = i;
            }
        }

        Arrays.sort(result);

//        Log.d(TAG, "getPeaks: 1 = " + result[0] + " 2 = " + result[1]);
        return result;
    }


    /*
                修改时间：2019/6/5
                修改内容：改变参数
                修改人：July
             */
    private BufferedWriter createBufferedWriter(String name) throws IOException {
        File file = new File(name);
        if (!file.getParentFile().exists())
            file.getParentFile().mkdir();
        return new BufferedWriter(new FileWriter(file));
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



    /**
     * @param a
     * @param b
     * @param sample
     * @return
     */
    //得到峰值索引的那一段buffera，双峰值
    private double[] getBufferBetween(int a, int b, double[] sample) {

//        Log.d(TAG, "getBufferBetween: a = " + a + " b = " + b);

        LinkedList<Double> bufferList = new LinkedList<>();
        //-100 ~ +200
        if (a - 100 > 0) {
            for (int i = 0; i <= 300 && a - 100 + i < sample.length; i++) {
//                if ((int) sample[a - 100 + i] != 0)
                bufferList.add(sample[a - 100 + i]);
            }
        } else {
            for (int i = 0; i < sample.length && i <= 300 - a; i++) {
                bufferList.add(sample[i]);
            }
        }

        if (b - 100 > 0) {
            for (int i = 0; i <= 300 && b - 100 + i < sample.length; i++) {
//                if ((int) sample[a - 100 + i] != 0)
                bufferList.add(sample[b - 100 + i]);
            }
        } else {
            for (int i = 0; i < sample.length && i <= 300 - b; i++) {
                bufferList.add(sample[i]);
            }
        }

//        if (b - 401 + i > 0)
//            for (; i <= 600 && b - 401 + i < sample.length; i++) {
////                if ((int) sample[b - 401 + i] != 0)
//                bufferList.add(sample[b - 401 + i]);
//
//            }

        double[] buffer = new double[bufferList.size()];
        for (int j = 0; j < buffer.length; j++) {
            buffer[j] = bufferList.get(j);
        }
        return buffer;
    }





    public AudioRecord record = null;
    int sampleRate = 44100;
    int fftlen = 1024;
    int minBytes = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT);
    int numOfReadShort;
    int readChunkSize = fftlen;  // Every hopLen one fft result (overlapped analyze window)
    short[] audioSamples = new short[readChunkSize];
    int bufferSampleSize = Math.max(minBytes / 2, fftlen / 2) * 2;

    public int getMax() {

        // tolerate up to about 1 sec.

        int max = 0;
        for (int i = 0; i < audioSamples.length; i++) {
            if (max < audioSamples[i])
                max = audioSamples[i];
        }
        return max;
    }
    /*
                修改日期：2019/6/8
                内容：修改文件路径
                修改人：July
             */
    class CurrentRecordTaskNew extends AsyncTask<Void, Boolean, Void> {
        WavWriter wavWriter = new WavWriter(WavWriter.TEST, sampleRate);

        @Override
        protected Void doInBackground(Void... voids) {
//            int bufferSampleSize = Math.max(minBytes / 2, fftlen / 2) * 2;
            wavWriter.start();
            int max = 0;

            int num = 0;
            //max < MAX_NOISE
            while (num != 2 && isRecord) {
                numOfReadShort = record.read(audioSamples, 0, readChunkSize);   // pulling
                max = wavWriter.pushAudioShortNew(audioSamples, numOfReadShort);  // Maybe move this to another thread?
                if (max == -1)
                    num++;
            }

//            for (int i = 0; i < audioSamples.length; i++) {
//                audioSamples[i] = 1000;
//            }
            if (!isRecord)
            {
                record.stop();
                record.release();
                return null;
            }
            int[] singal = wavWriter.getSignal();

            BufferedWriter bw = null;
            stop();

            double[] buffer = new double[singal.length];
            for (int i = 0; i < singal.length; i++) {
                buffer[i] = singal[i];
            }

//            try {
//                bw = createBufferedWriter(absolutePath + "/singal/", "air.txt");
//                for (int i = 0; i < singal.length; i++) {
//                    bw.write(buffer[i] + " ");
//                }
//                bw.flush();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

            Double[] featureDouble = null;
            try {
                featureDouble = MFCC.mfcc(FileUtil.getFilePathName(FileUtil.TEST_RECORD), buffer, singal.length, 44100);
//            featureDouble[featureDouble.length - 1] = getRMS(signal, result);
            } catch (IOException e) {
                e.printStackTrace();
            }
//        normalizationData(maxBuffer, minBuffer, featureDouble);

//        File file2 = new File(dictionaryPath + "testRecord.txt");
            try {
                bw = createBufferedWriter(FileUtil.getFilePathName(FileUtil.TEST_FEATURE));
            } catch (IOException e) {
                e.printStackTrace();
            }
            //将所有MFCC特征写入文件
            //将数据存入文件
//            Log.d(TAG, "writeModel: flag = " + flag + " len = " + feature.length);
            writeData(featureDouble, bw);

            try {
                if ((MFCC.svmPredict(FileUtil.getFilePathName(FileUtil.TEST_FEATURE))) == 1) {
                    publishProgress(true);
                } else {
                    publishProgress(false);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        protected void onProgressUpdate(Boolean... values) {
            if (values[0])
                mLockView.unlockSuccess();
            else mLockView.unlockFail();
        }

        void stop() {
            wavWriter.stop();
        }
    }

}
