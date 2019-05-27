package com.cnws.util;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogUtil {

    public static void info(String info){
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println(df.format(new Date()) + " " + info);
    }
}
