package com.cnws.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;


public class PythonUtil {

    public static void runPython(String command) {

        Process pr;
        try {
            pr = Runtime.getRuntime().exec(command);

            final InputStream errorInfo = pr.getErrorStream();
            BufferedReader buffErrorInfo = new BufferedReader(new InputStreamReader(errorInfo));
            String errorMsg;
            while ((errorMsg = buffErrorInfo.readLine()) != null) {
                LogUtil.info("python error msg :" + errorMsg);
            }

            final InputStream outInfo = pr.getInputStream();
            BufferedReader buffOutInfo = new BufferedReader(new InputStreamReader(outInfo));
            String outMsg;
            while ((outMsg = buffOutInfo.readLine()) != null) {
                LogUtil.info("python out msg :" + outMsg);
            }
            int value = pr.waitFor();
            LogUtil.info("RUN END , return value : " + value);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error(" python program error!!! ");
        }
    }
}
