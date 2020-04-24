package yearly_project.frontend;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "MainActivity";
    JavaCameraView cameraHandler;
    SegmentationModel segModel;
    ImageView imageView;

    FrameLayout cameraView;
    Rectangle rect;
    Circle circle;
    private ScaleGestureDetector gestureDetector;

    Mat outputFrame;

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
                    cameraHandler.enableView();
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

        // get interface objects
        cameraView = findViewById(R.id.cameraLayout);
        imageView = findViewById(R.id.imageView);

        // create camera handler
        cameraHandler = new JavaCameraView(this, 0);
        cameraHandler.setCvCameraViewListener(this);
        cameraHandler.setVisibility(SurfaceView.VISIBLE);
        cameraHandler.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        cameraView.addView(cameraHandler);
        cameraHandler.enableView();

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
        if (cameraHandler != null)
            cameraHandler.disableView();
        if (segModel != null) {
            segModel.close();
        }
    }

    @Override
    protected void onDestroy() {
        //stop camera
        super.onDestroy();
        if (cameraHandler != null) {
            cameraHandler.disableView();
        }
        if (segModel != null) {
            segModel.close();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {
        Log.i("onCameraViewStopped: ", "Camera view has stopped, releasing resources");

        outputFrame.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat inputMat = inputFrame.rgba();
        Imgproc.cvtColor(inputMat, inputMat, Imgproc.COLOR_RGBA2RGB);

        initialize(inputMat);
        Mat mask = createMask(inputMat);
        Imgproc.cvtColor(mask,mask,Imgproc.COLOR_BGR2RGB);

        segModel.segmentImage(mask,200);

//        Mat mat = pasteWeights(inputMat,mask);

        final Bitmap bitmap = convertMatToBitMap(mask);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setImageBitmap(bitmap);
            }
        });


        Imgproc.rectangle(inputMat, rect.getTopLeft(), rect.getBottomRight(), new Scalar(255, 0, 0), 2);
        Imgproc.circle(inputMat, circle.getCenter(), circle.getRadius(), new Scalar(0, 0, 255), 2, 8,0);
//
//        outputFrame = inputMat;
        outputFrame = inputMat;

        return outputFrame;
    }

    private Mat pasteWeights(Mat inputMat, Mat mask) {
        Mat mat = new Mat(inputMat.rows(), inputMat.cols(), CvType.CV_8U, Scalar.all(255));
        Mat destination = new Mat(inputMat, rect.rect);
//        mask.rowRange(0,10).copyTo(inputMat.rowRange((int)rect.getTopLeft().y,(int)rect.getBottomRight().y).colRange((int)rect.getTopLeft().x,(int)rect.getBottomRight().x),mat);
        Mat temp = mat.rowRange(100, 200);
        Mat b = temp.t();
        b.copyTo(inputMat.rowRange(100, 200));
//        Imgproc.resize(mask,mask,inputMat.size());
//        Imgproc.resize(mask, mat, mat.size(), 0, 0);
//        mat.copyTo(destination);

        return mask;
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

        return cutRectangle(cropped);
    }

    private void initialize(Mat inputMat) {
        int posHeight = inputMat.height();
        int posWidth = inputMat.width();

        if (rect == null) {
            rect = initializeRectangle(posHeight, posWidth);
        }

        if (circle == null) {
            circle = initializeCircle(posHeight, posWidth);
        }
    }

    private Mat cutRectangle(Mat inputMat) {
        Point point = new Point(circle.getCenter().x, circle.getCenter().y);

        point.x = point.x - circle.getRadius();
        point.y = point.y - circle.getRadius();

        Rectangle rectangle = new Rectangle((int) point.x, (int) point.y, circle.getRadius() * 2, circle.getRadius() * 2, 1);

        Mat smallImage = new Mat(inputMat, rectangle.rect).clone();

//        Core.flip(smallImage.t(), smallImage, 1);

        return smallImage;
    }

    private Circle initializeCircle(int posHeight, int posWidth) {
        Point center = new Point(posWidth / 2, posHeight / 2);
        int radius = posHeight / 6;

        return new Circle(center, radius - 2);
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