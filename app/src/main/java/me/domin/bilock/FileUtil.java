package me.domin.bilock;

import android.os.Environment;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;


/*
Create by Bilock
User: July
Description: Used To manage all files written by Bilock into sdcard
Date: 2019/6/5
 */
public class FileUtil {
    public static String absolutePath= Environment.getExternalStorageDirectory().getAbsolutePath()+"/Bilock/";
    public static String userName="user/";
    public static String otherName="other/";
    public static String temp="temp/";
    public static final int MODEL_RECORD = 1;
    public static final int MODEL_FEATURE = 2;
    public static final int TEST_RECORD = 3;
    public static final int TEST_FEATURE = 4;
    public static final int MODEL_WAV=5;
    public static final int TEST_WAV=6;
    public static final int TOUCH=7;
    public static final int MODEL_DATA=10;
    public static final int MODEL_PATH=8;
    public static final int TEST_PATH=9;
    public static final int FEATURE_USER_PATH=11;
    public static final int FEATURE_OTHER_PATH=12;

    public static String currentTime="";

    private static HashMap<Integer, String> mFileNameMap = new HashMap<Integer, String>(){
        {
            put(MODEL_RECORD,"ModelRecord.txt");
            put(MODEL_WAV,"Model_WAV.wav");
            put(MODEL_FEATURE,"ModelFeature.txt");
            put(TEST_RECORD,"TestRecord.txt");
            put(TEST_FEATURE,"TestFeature.txt");
            put(TEST_WAV,"TEST_WAV.wav");
            put(TOUCH,"touch.txt");
            put(MODEL_DATA,"MFCCs_model.txt");
        }
    };

    /*
    @Title: getFilePathName
    @Description: return the absolute path according to the file type。根据文件类型返回文件路径
                如果是MODEL_WAV或MODEL_FEATURE类型，返回带时间戳的路径
    @Param : type   [文件类型]
    @Return: String
     */
    public static String getFilePathName(int type){
        switch (type){
            case 1:
            case 2:
            case 5: return absolutePath+userName+getTime()+mFileNameMap.get(type);
            case 3:
            case 4:  return absolutePath+temp+getTime()+mFileNameMap.get(type);
            case 6:
            case 7: return absolutePath+temp+mFileNameMap.get(type);

            case 8:
            case 11: return absolutePath+userName;
            case 9: return absolutePath+temp;
            case 10: return absolutePath+userName+mFileNameMap.get(type);
            case 12: return absolutePath+userName+otherName;



        }
        return null;
    }

    /*
    @Title: getTime
    @Description: return current time in specific format
    @Param : void
    @Return: String [current time]
     */
    public static String getTime(){
        DateFormat df;
        df = new SimpleDateFormat("yyyy-MM-dd_HH'h'mm'm'ss.SSS's'", Locale.US);
        currentTime=df.format(new Date());
        return currentTime;
    }
    public static String getUserPath(){
        return absolutePath+userName;
    }

}
