package yearly_project.frontend;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

public class ExtendedJavaCameraView extends JavaCameraView {

    public ExtendedJavaCameraView(Context context, int cameraId) {
        super(context, cameraId);
    }

    public ExtendedJavaCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public void turnOnTheFlash() {
        Camera.Parameters params = mCamera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(params);
    }

    public void turnOffTheFlash() {
        Camera.Parameters params = mCamera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(params);
    }

    public void switchCameras() {
        int index = CAMERA_ID_FRONT;
        if (mCameraIndex == CAMERA_ID_FRONT)
            index = CAMERA_ID_BACK;
        this.disableView();
        this.setCameraIndex(index);
        this.enableView();
    }
}
