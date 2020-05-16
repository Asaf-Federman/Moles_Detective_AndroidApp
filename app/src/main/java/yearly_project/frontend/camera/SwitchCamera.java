package yearly_project.frontend.camera;

import android.app.Activity;
import android.hardware.Camera;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.core.Core;
import org.opencv.core.Mat;

import yearly_project.frontend.R;

public class SwitchCamera {

    private enum eCameraSide{
        FRONT_CAMERA,
        BACK_CAMERA
    }

    private ImageView cameraPicture;
    private Activity activity;
    private CameraHolder cameraHolder;
    private eCameraSide cameraSide;
    private boolean isFrontCameraAvailable;
//    private boolean isResetCamera=true;

    public SwitchCamera(ImageView cameraPicture, CameraHolder cameraHolder, Activity activity) {
        this.cameraPicture = cameraPicture;
        this.activity = activity;
        this.cameraHolder = cameraHolder;
        this.cameraSide = eCameraSide.BACK_CAMERA;
        isFrontCameraAvailable = isFrontCameraAvailable();
        setCallback();
    }

    private void setCallback() {
        if(isFrontCameraAvailable){
            cameraPicture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    changeState();
                }
            });
        }else{
            cameraPicture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    errorMessage();
                }
            });
        }
    }

    public void errorMessage(){
        Toast.makeText(activity,"No flash light device currently available", Toast.LENGTH_LONG).show();
    }

    public void changeState() {
        changeCamera();
        if (cameraSide == eCameraSide.BACK_CAMERA) {
            cameraPicture.setImageResource(R.drawable.ic_camera_front);
            cameraSide = eCameraSide.FRONT_CAMERA;
        } else {
            cameraPicture.setImageResource(R.drawable.ic_camera_rear);
            cameraSide = eCameraSide.BACK_CAMERA;
        }

//        isResetCamera=true;
    }

    private void changeCamera() {
        cameraHolder.getCameraView().switchCameras();
    }


    public boolean isFrontCameraAvailable() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return true;
            }
        }

        return false;
    }

    public void checkForFlip(Mat inputMat) {
        if(cameraSide == eCameraSide.FRONT_CAMERA){
            Core.flip(inputMat,inputMat,-1); //rotate 180 degrees clock wise
            Core.flip(inputMat,inputMat,0); //from mirror image to identical
        }else{
//            Core.flip(inputMat.t(),inputMat,1);
        }
    }

//    public void consumeResetResource(){
//        setResetCamera(false);
//    }
}
