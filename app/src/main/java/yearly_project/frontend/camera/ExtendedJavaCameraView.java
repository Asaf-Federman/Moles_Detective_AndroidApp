package yearly_project.frontend.camera;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

public class ExtendedJavaCameraView extends JavaCameraView {

    int cameraPosition = CAMERA_ID_BACK;

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
        int index;
        try {
            index = cameraPosition == CAMERA_ID_FRONT ? CAMERA_ID_BACK : CAMERA_ID_FRONT;
            this.disableView();
            this.setCameraIndex(index);
            this.enableView();
            cameraPosition = index;
        }catch(Exception e){
        }

    }

    public boolean IsCameraBack(){
        return cameraPosition == CAMERA_ID_BACK ;
    }
}
