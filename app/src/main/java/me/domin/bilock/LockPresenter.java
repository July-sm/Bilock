package me.domin.bilock;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
import static me.domin.bilock.TrainPresenter.MEAN;
import static me.domin.bilock.TrainPresenter.NONE;
import static me.domin.bilock.TrainPresenter.SD;
import static me.domin.bilock.TrainPresenter.SUM;
import static me.domin.bilock.TrainPresenter.Z_SCORE;

/**
  * @ProjectName:    Bilock
  * @Package:        me.domin.bilock
  * @ClassName:      LockPresenter
  * @Description:    处理锁屏界面 LockScreenActivity 的逻辑业务，包括从移动端获取录音，传递数据给Model，并根据结果更新UI
  * @Author:         Administrator
  * @CreateDate:     2018/4/14
  * @UpdateUser:     July
  * @UpdateDate:
  * @UpdateRemark:   将trainData,trainModel等方法移动到TrainPresenter类中，更改写入文件的路径
  * @Version:        1.0
 */

public class LockPresenter implements LockContract.Presenter {



    public static String absolutePath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Bilock/";
    private static String dictionaryPath = absolutePath + "/MFCC2/";
    public STFT stft;   // use with care

    private LockContract.View mLockView;


    private WaveFileReader mWaveReader;

    public LockPresenter(LockContract.View view){
        mLockView=view;
    }

    /*载入MFCC的c代码库*/
    static {
        System.loadLibrary("native-lib");
    }

    private boolean isRecord = false;

    /**
     　　* @Title: initRecorder
     　　* @Description:    实例化一个AudioRecord对象并开始录音，设置相关参数
     　　* @param void
     　　* @return void
     　　*/

    public void initRecorder() {
        record = new AudioRecord(6, sampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, 2 * bufferSampleSize);
        record.startRecording();
        bufferSampleSize = (int) Math.ceil(1.0 * sampleRate / bufferSampleSize) * bufferSampleSize;
    }

    public void stopRecorder() {
        isRecord = false;
        record.stop();
        record.release();

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



    /**
     　　* @Title: isRecordSuccess
     　　* @Description:    判断录音是否可用，暂时没用到
     　　* @param void
     　　* @return void
     　　*/
    @Override
    public boolean isRecordSuccess() {

        File file = new File(FileUtil.getFilePathName(FileUtil.TEST_WAV));

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
            BufferedWriter bw = createBufferedWriter( FileUtil.getFilePathName(FileUtil.TOUCH));
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
     /*   try {
            featureDouble = MFCC.mfcc(FileUtil.getFilePathName(FileUtil.TEST_RECORD),buffer , bufferBetween.length, 44100);
//            featureDouble[featureDouble.length - 1] = getRMS(signal, result);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
//        featureDouble=mean_normal(featureDouble);
//        normalizationData(maxBuffer, minBuffer, featureDouble);

//        File file2 = new File(dictionaryPath + "testRecord.txt");
        BufferedWriter bw = null;
        /*
            修改时间：2019/6/5
            修改内容：改变文件路径
            修改人：July
         */

        String path=FileUtil.getFilePathName(FileUtil.TEST_FEATURE);
        try {
            bw = createBufferedWriter(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //将所有MFCC特征写入文件
        //将数据存入文件
//            Log.d(TAG, "writeModel: flag = " + flag + " len = " + feature.length);
        writeData(featureDouble, bw);

        try {
            return (MFCC.svmPredict(path)) == 1;
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
                @description: 用于获取BufferedWriter对象
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
     * @description:得到峰值索引的那一段buffera，双峰值，暂时也没用
     * @param a
     * @param b
     * @param sample
     * @return double[]
     */
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
    //AudioRecord录音需要的最小缓存数组大小
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
    
    /**
     　　* @ClassName: CurrentRecordTaskNew
     　　* @Description: 用于读取录音数据，用WavWriter处理数据并提取特征，判断是否合法
     　　* @author Administrator
     　　* ${tags}
     　　*/

    
    class CurrentRecordTaskNew extends AsyncTask<Void, Boolean, Void> {
        WavWriter wavWriter = new WavWriter(WavWriter.TEST, sampleRate);

        @SuppressLint("NewApi")
        @Override
        /**  
            * @Title: doInBackground   
        　　* @Description: 相当于run方法，完成读取录音数据并进行处理的功能
        　　* @param [voids]       
        　　* @return java.lang.Void       
        　　* @throws   
        　　*/
        protected Void doInBackground(Void... voids) {
//            int bufferSampleSize = Math.max(minBytes / 2, fftlen / 2) * 2;
            wavWriter.start();
            int max = 0;

            int num = 0;
            //max < MAX_NOISE
            //一直读取录音数据，直到获取两个在阈值范围内的峰值，视作牙齿咬合声音
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
            //获取wavWriter提取好的声音峰值
            int[] signal = wavWriter.getSignal();

            BufferedWriter bw = null;
            //让wavwriter更新wav文件长度信息
            stop();

            //将int类型的声音数据转换为double类型数据
            byte[] buffer = new byte[signal.length*2];
            for (int i = 0; i < signal.length; i++) {
                buffer[2*i]=(byte)(signal[i]&0xff);
                buffer[2*i+1]=(byte)((signal[i]>>8)&0xff);
            }

//            try {
//                bw = createBufferedWriter(absolutePath + "/signal/", "air.txt");
//                for (int i = 0; i < signal.length; i++) {
//                    bw.write(buffer[i] + " ");
//                }
//                bw.flush();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

            //用MFCC获取声音的特征值，存入featureDouble数组中
            Double[] featureDouble = null;
            try {
                String path=FileUtil.getFilePathName(FileUtil.TEST_RECORD);
                File file=new File(path);
                File parent=new File(file.getParent());
                parent.mkdirs();
                featureDouble = MFCC.mfcc(file.getAbsolutePath(), buffer, signal.length, 44100);
//            featureDouble[featureDouble.length - 1] = getRMS(signal, result);
            } catch (IOException e) {
                e.printStackTrace();
            }
//        normalizationData(maxBuffer, minBuffer, featureDouble);

//        File file2 = new File(dictionaryPath + "testRecord.txt");
            String path=FileUtil.getFilePathName(FileUtil.TEST_FEATURE);
            try {
                bw = createBufferedWriter(path);
            } catch (IOException e) {
                e.printStackTrace();
            }


            //将所有MFCC特征写入文件
            //将数据存入文件
//            Log.d(TAG, "writeModel: flag = " + flag + " len = " + feature.length);

            writeData(featureDouble, bw);


            //调用svmPredict方法判断特征是否合法，并调用publishProgress更新结果
           /* try {
                if ((MFCC.svmPredict(path)) == 1) {
                    publishProgress(true);
                } else {
                    publishProgress(false);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }*/

           if(KNN(featureDouble,NONE)){
               publishProgress(true);
           }else {
               publishProgress(false);
           }


            return null;
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
                Test2Activity.Point p=new Test2Activity.Point(values[i],user[i]);
                p.calDis(features);
                list.add(p);
            }
            list.sort(new Comparator() {
                @Override
                public int compare(Object o, Object t1) {
                    double dis1=((Test2Activity.Point)o).distance;
                    double dis2=((Test2Activity.Point)t1).distance;
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
                if(((Test2Activity.Point)list.get(i)).user==1)
                    count++;
                else
                    count--;
            }
            if(count>=0)
                return true;
            else
                return false;
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

        @Override
        /**
            * @Title: onProgressUpdate
        　　* @Description: 根据publishProgress方法传入的value值更新UI线程
        　　* @param [values]
        　　* @return void
        　　* @throws
        　　*/
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
