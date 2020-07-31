package yearly_project.frontend.camera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import yearly_project.frontend.Constants;
import yearly_project.frontend.DB.Information;
import yearly_project.frontend.DB.UserInformation;
import yearly_project.frontend.R;
import yearly_project.frontend.waitScreen.CalculateResults;

import static yearly_project.frontend.Constants.AMOUNT_OF_PICTURES_TO_TAKE;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "CameraActivity";
    private ImageView startButton;
    private SegmentationModel segModel;
    private SquareWrapper wrappedSquare, tangentSquare;
    private Circle circle;
    private ScaleGestureDetector gestureDetector;
    private Activity activity;
    private float shapesLength;
    private static final int DIM_LENGTH = 100;
    private int counter = 0;
    private boolean isStart = false;
    private Information information;
    private int cameraWidth;
    private int cameraHeight;
    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private PreviewView previewView;
    private ImageView ivBitmap;
    private ImageAnalysis imageAnalysis;
    private Preview preview;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    // ---------------------------------------------------------------------------------------------

    static {
        if (!OpenCVLoader.initDebug())
            Log.d("ERROR", "Unable to load OpenCV");
        else
            Log.d("SUCCESS", "OpenCV loaded");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        previewView = findViewById(R.id.preview_view);

        activity = this;

        information = UserInformation.createNewInformation();
        startButton = findViewById(R.id.startButton);
        ImageView home = findViewById(R.id.home);
        gestureDetector = new ScaleGestureDetector(this, new GestureListener());


        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        ViewTreeObserver viewTreeObserver = previewView.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    previewView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    cameraWidth = previewView.getWidth();
                    cameraHeight = previewView.getHeight();
                    shapesLength = Math.max(cameraHeight,cameraWidth) /3;
                    initialize(cameraHeight, cameraWidth);
                }
            });
        }

        initializeTensorFlowModel();
    }

    public void OnStartButtonClick(View view){
        startButton.setClickable(false);
        AlphaAnimation animation = new AlphaAnimation(1F, 0.0F);
        animation.setDuration(2000);
        view.startAnimation(animation);
        view.setVisibility(View.GONE);
        isStart = true;
    }

    private void initializeTensorFlowModel() {
        try {
            segModel = new SegmentationModel(CameraActivity.this, SegmentationModel.eModel.V3_LARGE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return true;
    }

    @Override
    public void onClick(View v) {

    }

    private class GestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float mScaleFactor = detector.getScaleFactor();

            circle.scale(mScaleFactor);

            return true;
        }

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
            segModel = new SegmentationModel(CameraActivity.this, SegmentationModel.eModel.V3_LARGE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (segModel != null) {
            segModel.close();
        }
    }

    @Override
    protected void onDestroy() {
        //stop camera
        super.onDestroy();
        if (segModel != null) {
            segModel.close();
        }
    }
    public Mat onCameraFrame(Mat mat) {
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB);

        Mat mask = cutRectangle(mat);
        Imgproc.cvtColor(mask, mask, Imgproc.COLOR_BGR2RGB);
        mask = segModel.segmentImage(mask, DIM_LENGTH);
        checkForSegmentation();
        pasteWeights(mat, mask);

        Imgproc.rectangle(mat, wrappedSquare.getTopLeft(), wrappedSquare.getBottomRight(), new Scalar(0, 0, 0), 2);
        Imgproc.circle(mat, circle.getCenter(), (int) circle.getRadius(), new Scalar(255, 255, 255), 2, Core.LINE_AA);

        return mat;
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
            isStart = false;
            runOnUiThread(() -> {
                activityResult(Constants.RESULT_SUCCESS);
                finish();
            });
        }
    }

    private void convertMatToPicture(Mat mat) {
        Mat gray = new Mat();
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY);
        information.addImage(gray);
    }

    private void pasteWeights(Mat src, Mat dest) {
        Mat mat = src.rowRange((int) tangentSquare.getTopLeft().y, (int) tangentSquare.getBottomRight().y).colRange((int) tangentSquare.getTopLeft().x, (int) tangentSquare.getBottomRight().x);
        Core.addWeighted(mat, 1f, dest, 0.3, 1, mat);
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

    @Override
    public void onBackPressed() {
        activityResult(Constants.RESULT_FAILURE);
        super.onBackPressed();
    }

    private void activityResult(int result) {
        Intent data = new Intent(activity, CalculateResults.class);
        data.putExtra("ID", information.getSerialNumber());
        setResult(result,data);
    }

    public void OnHomeClick(View view){
        activityResult(Constants.RESULT_FAILURE);
        finish();
    }

    @SuppressLint("RestrictedApi")
    private void startCamera() {

        preview = setPreview();
        imageAnalysis = setImageAnalysis();
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        cameraProviderFuture.addListener(()->{
            ProcessCameraProvider cameraProvider = null;
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            this.previewView.setPreferredImplementationMode(PreviewView.ImplementationMode.TEXTURE_VIEW);
            preview.setSurfaceProvider(previewView.createSurfaceProvider());
            cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
        },ContextCompat.getMainExecutor(this));
    }


    private Preview setPreview() {
        Size screen = new Size(previewView.getWidth(), previewView.getHeight()); //size of the screen

        Preview preview = new Preview.Builder().setTargetResolution(screen).build();

        return preview;
    }

    private ImageAnalysis setImageAnalysis() {
        // Setup image analysis pipeline that computes average pixel luminance
        imageAnalysis = new ImageAnalysis.Builder().setImageQueueDepth(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), new LuminosityAnalyzer());

        return imageAnalysis;

    }

    private class LuminosityAnalyzer implements ImageAnalysis.Analyzer {

        @Override
        public void analyze(@NonNull ImageProxy image) {
            final Bitmap bitmap = previewView.getBitmap();
            image.getImage().
            if(bitmap==null)
                return;
            Mat mat = new Mat();
            Utils.bitmapToMat(bitmap, mat);
            mat = onCameraFrame(mat);
            Utils.matToBitmap(mat, bitmap);
        }
    }

//    private void updateTransform() {
//        Matrix mx = new Matrix();
//        float w = previewView.getMeasuredWidth();
//        float h = previewView.getMeasuredHeight();
//
//        float cX = w / 2f;
//        float cY = h / 2f;
//
//        int rotationDgr;
//        int rotation = (int) previewView.getRotation();
//
//        switch (rotation) {
//            case Surface.ROTATION_0:
//                rotationDgr = 0;
//                break;
//            case Surface.ROTATION_90:
//                rotationDgr = 90;
//                break;
//            case Surface.ROTATION_180:
//                rotationDgr = 180;
//                break;
//            case Surface.ROTATION_270:
//                rotationDgr = 270;
//                break;
//            default:
//                return;
//        }
//
//        mx.postRotate((float) rotationDgr, cX, cY);
//        previewView.setTransform(mx);
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted() {

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}