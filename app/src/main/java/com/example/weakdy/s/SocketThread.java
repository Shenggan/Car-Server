package com.example.weakdy.s;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Weakdy on 2017/10/22.
 */

public class SocketThread extends Thread {
    public static ServerSocket mServer;
    public static Socket mSocket = null;
    private Camera2Activity Camera;

    @Override
    public void run()
    {
        try {
            mServer = new ServerSocket(8888);
        } catch (Exception e) {}
        while (!Thread.currentThread().isInterrupted()) {
            /*if (MainActivity.connected == true) {
                break;
            }*/
            try {
                Thread.sleep(1000);

                mSocket = mServer.accept();
                if (mSocket == null)
                    continue;
                BufferedOutputStream outputStream = new BufferedOutputStream(mSocket.getOutputStream());
                final BufferedInputStream inputStream = new BufferedInputStream(mSocket.getInputStream());
                byte[] buff = new byte[256];
                int len;
                String msg;
                /*Connecting*/
                if (!MainActivity.connected){
                    while (!Thread.currentThread().isInterrupted()&&(len=inputStream.read(buff)) != -1) {
                        String ss = bytes2Hex(buff);
                        if (!(len>0)){
                            continue;
                        }
                        MainActivity.connected = true;
                        break;
                        /*if (ss.equals("4312")){
                            MainActivity.connected = true;
                            String key = "4312";
                            outputStream.write(key.getBytes());
                            outputStream.flush();
                            break;
                        }*/
                    }
                }
                if(MainActivity.Camera_Open){
                    JsonObject jsonObj = new JsonObject();
                    jsonObj.addProperty("type", "data");
                    jsonObj.addProperty("length", Camera2Activity.get_length());
                    jsonObj.addProperty("width", Camera2Activity.get_width());
                    jsonObj.addProperty("height", Camera2Activity.get_height());
                    outputStream.write(jsonObj.toString().getBytes());
                    outputStream.flush();

                    while (!Thread.currentThread().isInterrupted()&&((len=inputStream.read(buff)) != -1)){
                        msg = new String(buff, 0, len);
                        JsonParser parser = new JsonParser();
                        boolean isJSON = true;
                        JsonElement element = null;
                        try {
                            element = parser.parse(msg);
                        } catch (JsonParseException e) {
                            isJSON = false;
                        }

                        if (isJSON && element != null){
                            JsonObject obj = element.getAsJsonObject();
                            element = obj.get("state");
                            if (element != null && element.getAsString().equals("ok")){

                                while (true){
                                    YuvImage image = new YuvImage(Camera2Activity.getImage(), ImageFormat.NV21, Camera2Activity.get_width(), Camera2Activity.get_height(), null);
                                    ByteArrayOutputStream myoutputstream = new ByteArrayOutputStream();
                                    image.compressToJpeg(new Rect(0, 0, Camera2Activity.get_width(), Camera2Activity.get_height()), 60, myoutputstream);
                                    myoutputstream.flush();
                                    myoutputstream.close();
                                    byte tmp[]=myoutputstream.toByteArray();
                                    outputStream.write(intToBytes2(tmp.length));
                                    outputStream.write(tmp);
                                    System.out.println("cxy: send"+tmp.length+":"+tmp[0]+tmp[500]+tmp[5000]+tmp[tmp.length-5000]);
                                    outputStream.flush();
                                    if (Thread.currentThread().isInterrupted()) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                outputStream.close();
                inputStream.close();
            }
            catch (InterruptedException e) {}
            catch (IOException e) {}
        }
    }

    public static String bytes2Hex(byte[] src) {
        if (src == null || src.length <= 0) {
            return null;
        }

        char[] res = new char[src.length * 2];
        final char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        for (int i = 0, j = 0; i < src.length; i++) {
            res[j++] = hexDigits[src[i] >> 4 & 0x0f];
            res[j++] = hexDigits[src[i] & 0x0f];
        }

        return new String(res);
    }
    public static byte[] intToBytes2(int value)
    {
        byte[] src = new byte[4];
        src[0] = (byte) ((value>>24) & 0xFF);
        src[1] = (byte) ((value>>16)& 0xFF);
        src[2] = (byte) ((value>>8)&0xFF);
        src[3] = (byte) (value & 0xFF);
        return src;
    }

}
