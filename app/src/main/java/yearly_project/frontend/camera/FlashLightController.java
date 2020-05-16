package yearly_project.frontend.camera;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import yearly_project.frontend.R;

public class FlashLightController {
    private Activity activity;
    private CameraHolder cameraHolder;
    private ImageView flashLight;
    private boolean isTurnedOn;
    private boolean isFlashAvailable;

    public FlashLightController(ImageView flashLight, CameraHolder cameraHolder, Activity activity) {
        this.flashLight=flashLight;
        this.cameraHolder=cameraHolder;
        this.activity = activity;
        isFlashAvailable();
        setCallback();
    }

    private void setCallback() {
//        if(isFlashAvailable){
            flashLight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    changeState();
                }
            });
//        }else{
//            flashLight.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    errorMessage();
//                }
//            });
//        }
    }

    public void errorMessage(){
        Toast.makeText(activity,"No flash light device currently unavailable", Toast.LENGTH_LONG).show();
    }

    private void isFlashAvailable(){
        isFlashAvailable = activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    public void changeState() {
        if (isTurnedOn) {
            turnOff();
        } else {
            turnOn();
        }

        isTurnedOn = !isTurnedOn;
    }

    private void turnOn() {
        cameraHolder.getCameraView().turnOnTheFlash();
        flashLight.setImageResource(R.drawable.ic_flash_off);
    }

    private void turnOff() {
        cameraHolder.getCameraView().turnOffTheFlash();
        flashLight.setImageResource(R.drawable.ic_flash_on);
    }
}
