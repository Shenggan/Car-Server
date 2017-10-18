package com.example.weakdy.s;

import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class MainActivity extends AppCompatActivity {

    public static BufferedInputStream inputStream = null;
    public static BufferedOutputStream outputStream = null;
    public static Socket mSocket = null;
    public static ByteArrayOutputStream byteArray = null;
    public static boolean connected = false;
    public static ServerSocket mServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        TextView IP_content = (TextView) findViewById(R.id.IP_content);
        IP_content.setText("Your IP Address is:"+getIpAddress());
        TextView wifi_info = (TextView) findViewById(R.id.wifi_info);
        if(get_wifi_info().equals("<unknown ssid>")){
            wifi_info.setText("No WIFI Connection");
        }
        else wifi_info.setText("Wifi:"+get_wifi_info());

        receive_data();

    }

    private void receive_data(){

        try {
            mServer = new ServerSocket(8888);
        }catch (Exception e){
        }
        while (!Thread.currentThread().isInterrupted()){
            try {
                Thread.sleep(1000);

                mSocket=mServer.accept();
                if (mSocket == null)
                    continue;
                /*BufferedOutputStream outputStream = new BufferedOutputStream(mSocket.getOutputStream());
                final BufferedInputStream inputStream = new BufferedInputStream(mSocket.getInputStream());
                byte[] buff = new byte[256];
                int len=0;*/
                /*while ((len=inputStream.read(buff)) != -1){
                    if (!(len>0)){
                        continue;
                    }
                    if (buff.equals("4312")){
                        String key = "4312";
                        outputStream.write(key.getBytes());
                        outputStream.flush();
                    }
                    else {

                    }
                }*/
            }catch (InterruptedException e){

            }catch (IOException e){

            }


        }
    }
    private String getIpAddress(){
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        String ipAddressFormatted = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        return ipAddressFormatted;
    }

    private String get_wifi_info(){
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wi = wifiManager.getConnectionInfo();
        return wi.getSSID();
    }
}
