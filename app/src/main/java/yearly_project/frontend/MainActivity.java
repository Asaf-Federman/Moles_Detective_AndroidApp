package yearly_project.frontend;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "MainActivity";

    private CameraHolder cameraHolder;
    private SegmentationModel segModel;
    private FlashLightController cameraFlash;
    private SwitchCamera switchCamera;
    private ImageView home;
    private FrameLayout cameraView;
    private Rectangle wrappedRectangle, tangentRectangle;
    private Circle circle;
    private ScaleGestureDetector gestureDetector;

    private Mat inputMat;
    private Mat mask;
    private Mat outputFrame;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV fail to load");
        } else {
            Log.i(TAG, "OpenCV loaded");
            System.loadLibrary("opencv_java3");
        }
    }

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    cameraHolder.getCameraView().enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };
    // ---------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        home = findViewById(R.id.home);
        // get interface objects
        cameraView = findViewById(R.id.cameraLayout);

        // create camera handler
        ExtendedJavaCameraView cameraHandler = new ExtendedJavaCameraView(this, 0);
        cameraHandler.setCvCameraViewListener(this);
        cameraHandler.setVisibility(SurfaceView.VISIBLE);
        cameraHandler.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        cameraView.addView(cameraHandler);
        cameraHandler.enableView();
        cameraHolder = new CameraHolder(cameraHandler);
        cameraFlash=new FlashLightController((ImageView) findViewById(R.id.cameraFlash),cameraHolder, this);
        switchCamera = new SwitchCamera((ImageView) findViewById(R.id.switchCamera),cameraHolder, this);

        gestureDetector = new ScaleGestureDetector(this, new GestureListener());

        // create tensorflow model
        try {
            segModel = new SegmentationModel(MainActivity.this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // ask for permissions
        askForPermission(Manifest.permission.CAMERA, 10);                   // ask camera permission
        askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 10);   // ask storage permission
        askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE, 10);    // ask storage permission
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return true;
    }

    private class GestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float mScaleFactor = detector.getScaleFactor();

            circle.scale(mScaleFactor);

            return true;
        }

    }

    private void askForPermission(String permission, Integer requestCode) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
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
            segModel = new SegmentationModel(MainActivity.this);
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
        initialize(height,width);
    }

    @Override
    public void onCameraViewStopped() {
        Log.i("onCameraViewStopped: ", "Camera view has stopped, releasing resources");

        inputMat.release();
        mask.release();
        outputFrame.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        inputMat = inputFrame.rgba();
//        final Bitmap bitmap = convertMatToBitMap(inputMat);
        switchCamera.checkForFlip(inputMat);
        Imgproc.cvtColor(inputMat, inputMat, Imgproc.COLOR_RGBA2RGB);

        mask = createMask(inputMat);
        Imgproc.cvtColor(mask,mask,Imgproc.COLOR_BGR2RGB);

        Mat mat = segModel.segmentImage(mask,200);

        pasteWeights(inputMat,mat);

//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                imageView.setImageBitmap(bitmap);
//            }
//        });

        Imgproc.rectangle(inputMat, wrappedRectangle.getTopLeft(), wrappedRectangle.getBottomRight(), new Scalar(0, 0, 255), 2);
        Imgproc.circle(inputMat, circle.getCenter(), circle.getRadius(), new Scalar(255, 0, 0), 2, Core.LINE_AA);

        outputFrame = inputMat;

        return outputFrame;
    }

    private void pasteWeights(Mat src, Mat dest) {
        Mat mat = src.rowRange((int) tangentRectangle.getTopLeft().y,(int) tangentRectangle.getBottomRight().y).colRange((int) tangentRectangle.getTopLeft().x, (int) tangentRectangle.getBottomRight().x);
        Core.addWeighted(mat,1f,dest,0.3,1,mat);
    }

    private Mat createMask(Mat inputMat) {
        Mat mask = new Mat(inputMat.rows(), inputMat.cols(), CvType.CV_8U, Scalar.all(0));

        Imgproc.circle(mask,
                circle.getCenter(),
                circle.getRadius(),
                new Scalar(255, 255, 255),
                -1);
        Mat cropped = new Mat();
        inputMat.copyTo(cropped, mask);
        mask.release();

        return cutRectangle(cropped);
    }

    private void initialize(int posHeight,int posWidth) {
            wrappedRectangle = initializeRectangle(posHeight, posWidth);
            circle = initializeCircle(posHeight, posWidth);
    }

    private Mat cutRectangle(Mat inputMat) {
        Point point = new Point(circle.getCenter().x, circle.getCenter().y);

        point.x = point.x - circle.getRadius();
        point.y = point.y - circle.getRadius();
        tangentRectangle = new Rectangle((int) point.x, (int) point.y, circle.getRadius() * 2, circle.getRadius() * 2, 1);
        Mat smallImage = new Mat(inputMat, tangentRectangle.rect).clone();
        inputMat.release();

        return smallImage;
    }

    private Circle initializeCircle(int posHeight, int posWidth) {
        Point center = new Point(posWidth / 2, posHeight / 2);
        int radius = posHeight / 6;

        return new Circle(center, radius - 5);
    }

    private Rectangle initializeRectangle(int posHeight, int posWidth) {
        int edgeLength = posHeight / 3;

        return new Rectangle(posWidth, posHeight, edgeLength, edgeLength);
    }

    public static Bitmap convertMatToBitMap(Mat input) {
        Bitmap bmp = null;
        Mat rgb = new Mat();

        Imgproc.cvtColor(input, rgb, Imgproc.COLOR_BGR2RGB);
        try {
            bmp = Bitmap.createBitmap(rgb.cols(), rgb.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(rgb, bmp);
        } catch (CvException e) {
            Log.d("Exception", e.getMessage());
        }

        return bmp;
    }
}