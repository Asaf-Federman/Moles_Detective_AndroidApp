package yearly_project.frontend;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import org.opencv.android.JavaCameraView;

public class FlashLightController {
    private Activity activity;
    private JavaCameraView camera;
    private ImageView flashLight;
    private boolean isTurnedOn;
    private boolean isFlashAvailable;

    public FlashLightController(ImageView flashLight, JavaCameraView camera, Activity activity) {
        this.flashLight=flashLight;
        this.camera=camera;
        this.activity = activity;
        isFlashAvailable();
        setCallback();
    }

    private void setCallback() {
        if(isFlashAvailable){
            flashLight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    changeState();
                }
            });
        }else{
            flashLight.setOnClickListener(new View.OnClickListener() {
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
        camera.turnOnTheFlash();
        flashLight.setImageResource(R.drawable.ic_flash_off_black);
    }

    private void turnOff() {
        camera.turnOffTheFlash();
        flashLight.setImageResource(R.drawable.ic_flash_on_black);
    }
}
