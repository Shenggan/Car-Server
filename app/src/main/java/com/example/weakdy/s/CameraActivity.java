package com.example.weakdy.s;

import com.example.weakdy.s.CameraInterface.CamOpenOverCallback;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;

public class CameraActivity extends Activity implements CamOpenOverCallback {
    private static final String TAG = "haha";
    CameraSurfaceView surfaceView = null;
    ImageButton shutterBtn;
    private CameraActivity.OpenThread openThread;
    float previewRate = -1f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        openThread.start();
        setContentView(R.layout.activity_camera);
        initUI();
        initViewParams();

        shutterBtn.setOnClickListener(new BtnListeners());
    }

    private class OpenThread extends Thread {
        @Override
        public void run() {
            super.run();
            CameraInterface.getInstance().doOpenCamera(CameraActivity.this);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.camera, menu);
        return true;
    }

    private void initUI(){
        surfaceView = (CameraSurfaceView)findViewById(R.id.camera_surfaceview);
        shutterBtn = (ImageButton)findViewById(R.id.btn_shutter);
    }
    private void initViewParams(){
        LayoutParams params = surfaceView.getLayoutParams();
        Point p = DisplayUtil.getScreenMetrics(this);
        params.width = p.x;
        params.height = p.y;
        previewRate = DisplayUtil.getScreenRate(this); //默认全屏的比例预览
        surfaceView.setLayoutParams(params);

        //手动设置拍照ImageButton的大小为120dip×120dip,原图片大小是64×64
        LayoutParams p2 = shutterBtn.getLayoutParams();
        p2.width = DisplayUtil.dip2px(this, 80);
        p2.height = DisplayUtil.dip2px(this, 80);;
        shutterBtn.setLayoutParams(p2);

    }

    @Override
    public void cameraHasOpened() {
        // TODO Auto-generated method stub
        SurfaceHolder holder = surfaceView.getSurfaceHolder();
        CameraInterface.getInstance().doStartPreview(holder, previewRate);
    }
    private class BtnListeners implements OnClickListener{

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            switch(v.getId()){
                case R.id.btn_shutter:
                    CameraInterface.getInstance().doTakePicture();
                    break;
                default:break;
            }
        }

    }

}