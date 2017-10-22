package com.example.weakdy.s;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

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
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Created by Weakdy on 2017/10/20.
 */


public class Camera2Activity extends AppCompatActivity implements View.OnClickListener {
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    ///为了使照片竖直显示
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private ImageView iv_show;
    private CameraManager mCameraManager;//摄像头管理器
    private Handler childHandler, mainHandler;
    private String mCameraID;//摄像头Id 0 为后  1 为前
    private ImageReader mImageReader;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraDevice mCameraDevice;
    public static LinkedList<byte[]> mQueue = new LinkedList<byte[]>();
    private static final int MAX_BUFFER = 15;
    public static byte[] image_data = null;
    private SocketThread mthread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("1==============");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams. FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        System.out.println("2==============");
        setContentView(R.layout.activity_camera2);
        System.out.println("3==============");
        initVIew();
        System.out.println("4==============");
        MainActivity.Camera_Open = true;
        mthread = new SocketThread();
        mthread.start();
    }

    /**
     * 初始化
     */
    private void initVIew() {
        iv_show = (ImageView) findViewById(R.id.iv_show_camera2_activity);
        //mSurfaceView
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_view_camera2_activity);
        mSurfaceView.setOnClickListener(this);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setKeepScreenOn(true);
        // mSurfaceView添加回调
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) { //SurfaceView创建
                // 初始化Camera
                initCamera2();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) { //SurfaceView销毁
                // 释放Camera资源
                if (null != mCameraDevice) {
                    mCameraDevice.close();
                    Camera2Activity.this.mCameraDevice = null;
                }
            }
        });
    }


    /**
     * 初始化Camera2
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initCamera2() {
        HandlerThread handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        childHandler = new Handler(handlerThread.getLooper());
        mainHandler = new Handler(getMainLooper());
        mCameraID = "" + CameraCharacteristics.LENS_FACING_FRONT;//后摄像头
        mImageReader = ImageReader.newInstance(1080, 1920, ImageFormat.JPEG,1);

//        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() { //可以在这里处理拍照得到的临时照片 例如，写入本地
//            @Override
//            public void onImageAvailable(ImageReader reader) {
//                mCameraDevice.close();
//                mSurfaceView.setVisibility(View.GONE);
//                iv_show.setVisibility(View.VISIBLE);
//                // 拿到拍照照片数据
//                Image image = reader.acquireNextImage();
//                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
//                byte[] bytes = new byte[buffer.remaining()];
//                buffer.get(bytes);//由缓冲区存入字节数组
//
//                final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//                if (bitmap != null) {
//                    iv_show.setImageBitmap(bitmap);
//                }
//
//                /*Push Image Data*/
//                synchronized (mQueue) {
//                    if (mQueue.size() == MAX_BUFFER) {
//                        mQueue.poll();
//                    }
//                    mQueue.add(bytes);
//                }
//                image.close();
//            }
//        }, mainHandler);
        //获取摄像头管理
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            //打开摄像头
            mCameraManager.openCamera(mCameraID, stateCallback, mainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            System.out.println("CANNOT ACCESS =====================");
        }
    }

    public static byte[] getImage() {
        synchronized (mQueue) {
            System.out.println("_________________________________________"+mQueue.size());
            if (mQueue.size() > 0) {
                image_data = mQueue.poll();
            }
        }
        return image_data;
    }

    /**
     * 摄像头创建监听
     */
    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {//打开摄像头
            mCameraDevice = camera;
            //开启预览
            takePreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {//关闭摄像头
            if (null != mCameraDevice) {
                mCameraDevice.close();
                Camera2Activity.this.mCameraDevice = null;
            }
        }

        @Override
        public void onError(CameraDevice camera, int error) {//发生错误
            Toast.makeText(Camera2Activity.this, "摄像头开启失败", Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * 开始预览
     */
    private void takePreview() {
        try {
            // 创建预览需要的CaptureRequest.Builder
            final CaptureRequest.Builder previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 将SurfaceView的surface作为CaptureRequest.Builder的目标
            previewRequestBuilder.addTarget(mSurfaceHolder.getSurface());
            // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求
            mCameraDevice.createCaptureSession(Arrays.asList(mSurfaceHolder.getSurface(), mImageReader.getSurface()), new CameraCaptureSession.StateCallback() // ③
            {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    if (null == mCameraDevice) return;
                    // 当摄像头已经准备好时，开始显示预览
                    mCameraCaptureSession = cameraCaptureSession;
                    try {
                        // 自动对焦
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        // 打开闪光灯
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                        // 显示预览
                        CaptureRequest previewRequest = previewRequestBuilder.build();
                        mCameraCaptureSession.setRepeatingRequest(previewRequest, null, childHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(Camera2Activity.this, "配置失败", Toast.LENGTH_SHORT).show();
                }
            }, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public static int get_width(){
        return 1920;
    }

    public static int get_height(){
        return 1080;
    }

    public static int get_length(){
        return 1920*1080* ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8;
    }
    /**
     * 点击事件
     */
    @Override
    public void onClick(View v) { getImage(); }

    /**
     * 拍照
     */
    private void takePicture() {
        if (mCameraDevice == null) return;
        // 创建拍照需要的CaptureRequest.Builder
        final CaptureRequest.Builder captureRequestBuilder;
        try {
            captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            // 将imageReader的surface作为CaptureRequest.Builder的目标
            captureRequestBuilder.addTarget(mImageReader.getSurface());
            // 自动对焦
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 自动曝光
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 获取手机方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            // 根据设备方向计算设置照片的方向
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            //拍照
            CaptureRequest mCaptureRequest = captureRequestBuilder.build();
            mCameraCaptureSession.capture(mCaptureRequest, null, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public class SocketThread extends Thread {
        public ServerSocket mServer;
        public Socket mSocket = null;
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
                    BufferedInputStream inputStream = new BufferedInputStream(mSocket.getInputStream());
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
                        outputStream.close();
                        inputStream.close();
                    }
                    outputStream = new BufferedOutputStream(mSocket.getOutputStream());
                    inputStream = new BufferedInputStream(mSocket.getInputStream());
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
                                        System.out.println("______________________________________________________________________haha");
                                        YuvImage image = new YuvImage(getImage(), ImageFormat.NV21, get_width(), get_height(), null);
                                        ByteArrayOutputStream myoutputstream = new ByteArrayOutputStream();
                                        image.compressToJpeg(new Rect(0, 0, get_width(), get_height()), 60, myoutputstream);
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

        public String bytes2Hex(byte[] src) {
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
        public byte[] intToBytes2(int value)
        {
            byte[] src = new byte[4];
            src[0] = (byte) ((value>>24) & 0xFF);
            src[1] = (byte) ((value>>16)& 0xFF);
            src[2] = (byte) ((value>>8)&0xFF);
            src[3] = (byte) (value & 0xFF);
            return src;
        }

    }
}
