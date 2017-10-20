package com.example.weakdy.s;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;
import android.os.Handler;
import android.view.WindowManager;
import android.content.Intent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    public static BufferedInputStream inputStream = null;
    public static BufferedOutputStream outputStream = null;
    public static Socket mSocket = null;
    public static ByteArrayOutputStream byteArray = null;
    public static boolean connected = false;
    public static ServerSocket mServer;
    public static TextView IP_content;
    private SocketThread receive_thread;
    private ImageThread image_thread;
    private static Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams. FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        IP_content = (TextView) findViewById(R.id.IP_content);
        IP_content.setText("Your IP Address is:" + getIpAddress());
        TextView wifi_info = (TextView) findViewById(R.id.wifi_info);
        if(get_wifi_info().equals("<unknown ssid>")){
            wifi_info.setText("No WIFI Connection");
        }
        else wifi_info.setText("Wifi:"+get_wifi_info());

        receive_thread = new SocketThread();
        image_thread = new ImageThread();

        receive_thread.start();
        image_thread.start();
        //Intent intent = new Intent();
        //intent.setClass(MainActivity.this, CameraActivity.class);
        //startActivity(intent);

    }

    private class ImageThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (true) {
                if (connected == false) {
                    continue;
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent();
                        intent.setClass(MainActivity.this, CameraActivity.class);
                        startActivity(intent);
                    }
                });
                break;
            }
        }

    }

    private class SocketThread extends Thread {
        @Override
        public void run()
        {
            try {
                mServer = new ServerSocket(8888);
            } catch (Exception e) {}
            while (!Thread.currentThread().isInterrupted()) {
                if (connected == true) {
                    break;
                }
                try {
                    Thread.sleep(1000);

                    mSocket = mServer.accept();
                    if (mSocket == null)
                        continue;
                    BufferedOutputStream outputStream = new BufferedOutputStream(mSocket.getOutputStream());
                    final BufferedInputStream inputStream = new BufferedInputStream(mSocket.getInputStream());
                    byte[] buff = new byte[4];
                    int len=0;
                    while ((len=inputStream.read(buff)) != -1) {
                        String ss = bytes2Hex(buff);
                        if (!(len>0)){
                            continue;
                        }
                        if (ss.equals("34333132")){
                            connected = true;
                            String key = "4312";
                            outputStream.write(key.getBytes());
                            outputStream.flush();
                            break;
                        }
                        else {
                            continue;
                        }
                    }
                    outputStream.close();
                    inputStream.close();
                }
                catch (InterruptedException e) {}
                catch (IOException e) {}
            }
        }
    }

    private String getIpAddress(){
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        String ipAddressFormatted = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        return ipAddressFormatted;
    }

    private String get_wifi_info(){
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wi = wifiManager.getConnectionInfo();
        return wi.getSSID();
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
}
