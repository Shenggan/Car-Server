package com.example.weakdy.s;

import android.content.pm.ActivityInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
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
import com.dd.CircularProgressButton;
import com.skyfishjy.library.RippleBackground;
import me.drakeet.materialdialog.MaterialDialog;
import android.util.TypedValue;

public class MainActivity extends AppCompatActivity {

    public static BufferedInputStream inputStream = null;
    public static BufferedOutputStream outputStream = null;
    //public static Socket mSocket = null;
    public static ByteArrayOutputStream byteArray = null;
    public static boolean connected = false;
    //public static ServerSocket mServer;
    public static TextView IP_content;
    // private SocketThread receive_thread;
    private ImageThread image_thread;
    private static Handler handler = new Handler();
    private Camera2Activity camera_activity;
    public static boolean Camera_Open = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams. FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        /*===UI PART BEGIN=========================*/
        final MaterialDialog mMaterialDialog = new MaterialDialog(this)
                .setTitle("注意事项")
                .setMessage("请确保WIFI处于连接状态!\n");
        mMaterialDialog.setPositiveButton("OK", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMaterialDialog.dismiss();
            }
        }).setNegativeButton("CANCEL", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMaterialDialog.dismiss();
            }
        });


        final RippleBackground rippleBackground=(RippleBackground)findViewById(R.id.connect);
        ImageView imageView=(ImageView)findViewById(R.id.centerImage);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (get_wifi_info().equals("<unknown ssid>")){
                    mMaterialDialog.show();
                    rippleBackground.stopRippleAnimation();
                }
                else {
                    rippleBackground.startRippleAnimation();

                }
            }
        });

        if (get_wifi_info().equals("<unknown ssid>")){
            mMaterialDialog.show();
            rippleBackground.stopRippleAnimation();
        }else {
            rippleBackground.startRippleAnimation();
        }

        TextView IP = (TextView) findViewById(R.id.IP);
        final TextView IP_address = (TextView) findViewById(R.id.IP_address);
        final TextView wifi = (TextView) findViewById(R.id.wifi);
        IP.setTextSize(TypedValue.COMPLEX_UNIT_SP, 35);
        IP_address.setTextSize(TypedValue.COMPLEX_UNIT_SP, 35);
        wifi.setTextSize(TypedValue.COMPLEX_UNIT_SP, 35);
        refresh(IP_address,wifi);

        final CircularProgressButton circularButton1 = (CircularProgressButton) findViewById(R.id.circularButton1);
        circularButton1.setIndeterminateProgressMode(true);
        circularButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println(circularButton1.getProgress());
                circularButton1.setProgress(50);
                refresh(IP_address,wifi);
                if (get_wifi_info().equals("<unknown ssid>")){
                    mMaterialDialog.show();
                    rippleBackground.stopRippleAnimation();
                }
                else {
                    rippleBackground.startRippleAnimation();
                }
                circularButton1.setProgress(100);
                circularButton1.setProgress(0);
            }
        });

        /*===UI PART END=========================*/
//        receive_thread = new SocketThread();
//        image_thread = new ImageThread();
//
//
//        receive_thread.start();
//        image_thread.start();
        /*Intent intent = new Intent();
        intent.setClass(MainActivity.this, Camera2Activity.class);
        startActivity(intent);*/

    }

    private class ImageThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (true) {
                try {
                    Thread.sleep(300);
                }catch (InterruptedException e){

                }
                if (connected == false) {
                    continue;
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent();
                        intent.setClass(MainActivity.this, Camera2Activity.class);
                        startActivity(intent);

                        //setContentView(R.layout.activity_camera2);
                    }
                });
                break;
            }
        }

    }

    /*private class SocketThread extends Thread {
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
    }*/

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


    public void refresh(TextView IP_address,TextView wifi){
        IP_address.setText(getIpAddress());
        if(get_wifi_info().equals("<unknown ssid>")){
            wifi.setText("No WIFI Connection");
        }
        else wifi.setText("WIFI:"+get_wifi_info());
    }

    /*public static String bytes2Hex(byte[] src) {
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
    }*/
}
