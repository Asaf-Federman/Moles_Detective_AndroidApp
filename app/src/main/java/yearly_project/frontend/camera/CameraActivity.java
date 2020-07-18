package yearly_project.frontend.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.time.Instant;

import timber.log.Timber;
import yearly_project.frontend.DB.UserInformation;
import yearly_project.frontend.R;
import yearly_project.frontend.utils.Utilities;
import yearly_project.frontend.waitScreen.CalculateResults;

import static yearly_project.frontend.Constants.AMOUNT_OF_PICTURES_TO_TAKE;

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "CameraActivity";
    private ImageView startButton;
    private CameraHolder cameraHolder;
    private SegmentationModel segModel;
    private FlashLightController cameraFlash;
    private SwitchCamera switchCamera;
    private SquareWrapper wrappedSquare, tangentSquare;
    private Circle circle;
    private ScaleGestureDetector gestureDetector;
    private Spinner spinner;
    private Activity activity;
    private boolean isSpinnerChanged = false;
    private float shapesLength;
    private static final int DIM_LENGTH = 100;
    private int counter = 0;
    private int[][][] result;
    private long timeInMilliSeconds;
    private boolean isStart = false;
    private Mat inputMat;
    private Mat mask;
    private UserInformation userInformation;

    static {
        if (!OpenCVLoader.initDebug()) {
            Timber.i("OpenCV fail to load");
        } else {
            Timber.i("OpenCV loaded");
            System.loadLibrary("opencv_java3");
        }
    }

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                cameraHolder.getCameraView().enableView();
            } else {
                super.onManagerConnected(status);
            }
        }
    };
    // ---------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activity = this;
        try {
            userInformation = new UserInformation();
        } catch (Exception e) {
            Log.i("WARN", e.getMessage());
        }

        timeInMilliSeconds = Instant.now().toEpochMilli();
        startButton = findViewById(R.id.startButton);
        ImageView home = findViewById(R.id.home);
        gestureDetector = new ScaleGestureDetector(this, new GestureListener());
        result = new int[10][][];

//        Intent myIntent = new Intent(this, ResultActivity.class);
//        startActivity(myIntent);

        initializeSpinner();
        initializeCamera();
        initializeTensorFlowModel();
        askForPermissions();
    }

    public void OnStartButtonClick(View view){
        startButton.setClickable(false);
        AlphaAnimation animation = new AlphaAnimation(1F, 0.0F);
        animation.setDuration(2000);
        view.startAnimation(animation);
        view.setVisibility(View.GONE);
        isStart = true;
    }

    private void initializeSpinner() {
        ArrayAdapter<SegmentationModel.eModel> adapter;
        int spinnerPosition;

        spinner = findViewById(R.id.spinner);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, SegmentationModel.eModel.values());
        spinner.setAdapter(adapter);
        SegmentationModel.eModel model = SegmentationModel.eModel.V2;
        spinnerPosition = adapter.getPosition(model);
        spinner.setSelection(spinnerPosition);
    }

    private void initializeCamera() {
        FrameLayout cameraView = findViewById(R.id.cameraLayout);
        final ExtendedJavaCameraView cameraHandler = new ExtendedJavaCameraView(this, 0);

        cameraHandler.setCvCameraViewListener(this);
        cameraHandler.setVisibility(SurfaceView.VISIBLE);
        cameraHandler.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        cameraView.addView(cameraHandler);
        cameraHandler.enableView();
        cameraHolder = new CameraHolder(cameraHandler);
        cameraFlash = new FlashLightController((ImageView) findViewById(R.id.cameraFlash), cameraHolder, this);
        switchCamera = new SwitchCamera((ImageView) findViewById(R.id.switchCamera), cameraHolder, this);
    }

    private void initializeTensorFlowModel() {
        try {
            segModel = new SegmentationModel(CameraActivity.this, (SegmentationModel.eModel) spinner.getSelectedItem());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void askForPermissions() {
        try {
            askForPermission(10, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA);
        } catch (Exception e) {
            activity.finish();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (isSpinnerChanged) {
                    try {
                        cameraHolder.getCameraView().disableView();
                        segModel.close();
                        segModel = new SegmentationModel(activity, (SegmentationModel.eModel) parentView.getItemAtPosition(position));
                        cameraHolder.getCameraView().enableView();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    isSpinnerChanged = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
    }

    private class GestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float mScaleFactor = detector.getScaleFactor();

            circle.scale(mScaleFactor);

            return true;
        }

    }

    private void askForPermission(Integer requestCode, String... permissions) throws Exception {
        PermissionHandler(requestCode, permissions);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        if (requestCode == 10) {
            for (int i = 0; i < grantResults.length; ++i) {
                switch (permissions[i]) {
                    case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                            Utilities.createAlertDialog(activity, "No Permissions", "There are no write permissions, and therefore the activity can not write to storage");
                        break;
                    case Manifest.permission.CAMERA:
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                            Utilities.createAlertDialog(activity, "No Permissions", "There are no camera permissions, and therefore you're not eligible to use this activity");
                        break;
                    case Manifest.permission.READ_EXTERNAL_STORAGE:
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                            Utilities.createAlertDialog(activity, "No Permissions", "There are no read permissions, and therefore the activity can not read from storage");
                        break;
                    default:
                        break;

                }
            }
        }
    }

    private void PermissionHandler(Integer requestCode, String... permissionsToRequest) {
        ActivityCompat.requestPermissions(activity, permissionsToRequest, requestCode);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        } else {
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        try {
            segModel = new SegmentationModel(CameraActivity.this, (SegmentationModel.eModel) spinner.getSelectedItem());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraHolder.getCameraView() != null)
            cameraHolder.getCameraView().disableView();
        if (segModel != null) {
            segModel.close();
        }
    }

    @Override
    protected void onDestroy() {
        //stop camera
        super.onDestroy();
        if (cameraHolder.getCameraView() != null) {
            cameraHolder.getCameraView().disableView();
        }
        if (segModel != null) {
            segModel.close();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        shapesLength = (float) Math.max(height, width) / 3;
        initialize(height, width);
    }

    @Override
    public void onCameraViewStopped() {
        Timber.i("Camera view has stopped, releasing resources");

        if (inputMat != null) inputMat.release();
        if (mask != null) mask.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        inputMat = inputFrame.rgba();

        switchCamera.checkForFlip(inputMat);
        Imgproc.cvtColor(inputMat, inputMat, Imgproc.COLOR_RGBA2RGB);

        mask = cutRectangle(inputMat);
        Imgproc.cvtColor(mask, mask, Imgproc.COLOR_BGR2RGB);
        mask = segModel.segmentImage(mask, DIM_LENGTH);
        checkForSegmentation();
        pasteWeights(inputMat, mask);

        Imgproc.rectangle(inputMat, wrappedSquare.getTopLeft(), wrappedSquare.getBottomRight(), new Scalar(0, 0, 0), 2);
        Imgproc.circle(inputMat, circle.getCenter(), (int) circle.getRadius(), new Scalar(255, 255, 255), 2, Core.LINE_AA);

        return inputMat;
    }

    private void checkForSegmentation() {
        if (segModel.isSegmentationSuccessful() && isStart) {
            if (counter < AMOUNT_OF_PICTURES_TO_TAKE) {
                final Mat mat = segModel.getSegmantation();
                new Thread(() -> convertMatToPicture(mat)).start();
            }

            ++counter;
        }

        if (counter == 10) {
            runOnUiThread(() -> {
                Intent myIntent = new Intent(activity, CalculateResults.class);
                myIntent.putExtra("folder_path", getFilesDir().getAbsolutePath() + "/photos/" + timeInMilliSeconds);
                startActivity(myIntent);
            });
        }
    }

    private void convertMatToPicture(Mat mat) {
        Mat gray = new Mat();
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY);
        userInformation.getInformation().addImage(gray);
    }

    private void pasteWeights(Mat src, Mat dest) {
        Mat mat = src.rowRange((int) tangentSquare.getTopLeft().y, (int) tangentSquare.getBottomRight().y).colRange((int) tangentSquare.getTopLeft().x, (int) tangentSquare.getBottomRight().x);
        Core.addWeighted(mat, 1f, dest, 0.3, 1, mat);
    }

    private Mat createMask(Mat inputMat) {
        Mat mask = new Mat(inputMat.rows(), inputMat.cols(), CvType.CV_8U, Scalar.all(0));

        Imgproc.circle(mask,
                circle.getCenter(),
                (int) circle.getRadius(),
                new Scalar(255, 255, 255),
                -1);
        Mat cropped = new Mat();
        inputMat.copyTo(cropped, mask);
        mask.release();

        return cutRectangle(cropped);
    }

    private void initialize(int posHeight, int posWidth) {
        wrappedSquare = initializeRectangle(posHeight, posWidth);
        circle = initializeCircle(posHeight, posWidth);
    }

    private Mat cutRectangle(Mat inputMat) {
        Point point = circle.getCenter().clone();

        point.x = point.x - circle.getRadius();
        point.y = point.y - circle.getRadius();
        tangentSquare = new SquareWrapper((int) point.x, (int) point.y, circle.getRadius() * 2, 1);
        return new Mat(inputMat, tangentSquare.square).clone();
    }

    private Circle initializeCircle(int posHeight, int posWidth) {
        return new Circle(posWidth, posHeight, shapesLength / 2 - 5);
    }

    private SquareWrapper initializeRectangle(int posHeight, int posWidth) {
        return new SquareWrapper(posWidth, posHeight, shapesLength);
    }
}