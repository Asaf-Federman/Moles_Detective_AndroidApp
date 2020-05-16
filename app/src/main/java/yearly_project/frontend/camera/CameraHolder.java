package yearly_project.frontend.camera;

public class CameraHolder {
    private ExtendedJavaCameraView cameraView;

    public CameraHolder(ExtendedJavaCameraView cameraView) {
        this.cameraView = cameraView;
    }

    public ExtendedJavaCameraView getCameraView() {
        return cameraView;
    }

    public void setCameraView(ExtendedJavaCameraView cameraView) {
        this.cameraView = cameraView;
    }
}
