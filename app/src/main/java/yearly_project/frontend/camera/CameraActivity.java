package yearly_project.frontend.camera;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.TorchState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.quickbirdstudios.yuv2mat.Yuv;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import timber.log.Timber;
import yearly_project.frontend.Constant;
import yearly_project.frontend.DB.Information;
import yearly_project.frontend.DB.UserInformation;
import yearly_project.frontend.R;
import yearly_project.frontend.utils.Utilities;

import static org.opencv.imgproc.Imgproc.cvtColor;
import static yearly_project.frontend.Constant.AMOUNT_OF_PICTURES_TO_TAKE;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener {

    private CameraActivity activity;
    private ImageView startButton, frontImage, flash;
    private SegmentationModel segModel;
    private SquareWrapper tangentSquare;
    private Circle circle;
    private ScaleGestureDetector gestureDetector;
    private ExecutorService executor;
    private TextView framesTextView;
    private Information information;
    private int cameraWidth, cameraHeight;
    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private PreviewView previewView;
    private ImageAnalysis imageAnalysis;
    private Preview preview;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Camera camera;
    private boolean isTorchMode = false, isStart = false;
    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (!(status == LoaderCallbackInterface.SUCCESS)) {
                super.onManagerConnected(status);
            }
        }
    };

    // ---------------------------------------------------------------------------------------------

    static {
        if (!OpenCVLoader.initDebug())
            Timber.d("Unable to load OpenCV");
        else
            Timber.d("OpenCV loaded");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        activity = this;
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        executor = Executors.newSingleThreadExecutor();
        previewView = findViewById(R.id.view_finder);
        frontImage = findViewById(R.id.imageView);
        information = UserInformation.createNewInformation();
        startButton = findViewById(R.id.startButton);
        flash = findViewById(R.id.cameraFlash);
        framesTextView = findViewById(R.id.framesTextView);
        gestureDetector = new ScaleGestureDetector(this, new GestureListener());

        ViewTreeObserver viewTreeObserver = previewView.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    previewView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    cameraWidth = previewView.getWidth();
                    cameraHeight = previewView.getHeight();
                    initialize(cameraHeight, cameraWidth);
                }
            });
        }

        initializeTensorFlowModel();
    }

    /**
     * Occurs on the main button click event, starts looking and saving images of the mole(s)
     * @param view - the start button
     */
    public void OnStartButtonClick(View view) {
        startButton.setClickable(false);
        AlphaAnimation animation = new AlphaAnimation(1F, 0.0F);
        animation.setDuration(2000);
        view.startAnimation(animation);
        view.setVisibility(View.GONE);
        isStart = true;
    }

    private void initializeTensorFlowModel() {
        segModel = new SegmentationModel(CameraActivity.this, SegmentationModel.eModel.V3_LARGE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return true;
    }

    @Override
    public void onClick(View v) {}

    private class GestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        /**
         * Scales the circle according to the hand gestures
         * @param detector - detector object that catches the hand gestures
         * @return a boolean value that concur it succeeds (in our use case it is always true)
         */
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            circle.scale(detector.getScaleFactor());

            return true;
        }
    }

    /**
     * Reinitialize activity
     */
    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        } else {
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        if (segModel == null) {
            segModel = new SegmentationModel(CameraActivity.this, SegmentationModel.eModel.V3_LARGE);
        }

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    /**
     * Modify the matrix it receives on each camera frame
     * @param inputMatrix - original matrix
     * @return modified matrix
     */
    public Mat onCameraFrame(Mat inputMatrix) {
        Mat rectangle, segmentImage;

        cvtColor(inputMatrix, inputMatrix, Imgproc.COLOR_RGBA2RGB);
        rectangle = cutSquare(inputMatrix);
        segmentImage = segModel.segmentImage(rectangle);
        checkForSegmentation(rectangle);
        ifActivityDone();
        addWeights(inputMatrix, segmentImage);
        Imgproc.circle(inputMatrix, circle.getCenter(), (int) circle.getRadius(), new Scalar(255, 255, 255), 3, Core.LINE_AA);

        return inputMatrix;
    }

    /**
     * If the activity is done, it verifies the data and exits the activity.
     */
    private void ifActivityDone() {
        if (information.getImages().getSize()  >= AMOUNT_OF_PICTURES_TO_TAKE) {
            synchronized (this) {
                if (information.getImages().getSize() == AMOUNT_OF_PICTURES_TO_TAKE) {
                    isStart = false;
                    try {
                        boolean isVerified = information.verifyCameraActivity();
                        runOnUiThread(() -> {
                            if
                            (!isVerified) Utilities.createAlertDialog(activity, "ERROR", "Failed to obtain the needed data", ((dialog, which) -> finishTask(Constant.RESULT_FAILURE)));
                            else
                                finishTask(Constant.RESULT_SUCCESS);
                        });
                    } catch (IllegalAccessException ignore) {
                        runOnUiThread(() -> Utilities.createAlertDialog(getApplicationContext(), "ERROR", "Failed to verify the need data", (((dialog, which) -> finishTask(Constant.RESULT_FAILURE)))));
                    }
                }
            }
        }
    }

    /**
     * Checks for segmentation in the matrix, and saves the matrix if segmentation is found and the user clicked the button
     * @param mat - the input matrix
     */
    private void checkForSegmentation(Mat mat) {
        if (isStart) {
            if (segModel.isSegmentationSuccessful()) {
                if (information.getImages().getSize() < AMOUNT_OF_PICTURES_TO_TAKE) {
                    new Thread(() -> convertMatToPicture(mat.clone())).start();
                }
            }
        }
    }

    /**
     * Modify the matrix and send it to the Database
     * @param mat - input matrix
     */
    private void convertMatToPicture(Mat mat) {
        cvtColor(mat,mat,Imgproc.COLOR_BGR2RGB);
        Imgproc.resize(mat,mat, new org.opencv.core.Size(250,250));
        information.getImages().addImage(mat);
    }

    /**
     * Paste the segmentation on top of the input source matrix
     * @param sourceMatrix - source matrix
     * @param segmentationResult - the output of the segmentation
     */
    private void addWeights(Mat sourceMatrix, Mat segmentationResult) {
        segmentationResult = createMask(segmentationResult);
        Mat mat = sourceMatrix.rowRange((int) tangentSquare.getTopLeft().y, (int) tangentSquare.getBottomRight().y).colRange((int) tangentSquare.getTopLeft().x, (int) tangentSquare.getBottomRight().x);
        Core.addWeighted(mat, 1f, segmentationResult, 0.3, 1, mat);
    }

    /**
     * Creates a mask that surround the circle that is delineated inside the square
     * @param inputMat - the original matrix
     * @return a modified image that contains only the delineated circle and black background that surrounds him
     */
    private Mat createMask(Mat inputMat) {
        Mat mask = new Mat(inputMat.rows(), inputMat.cols(), CvType.CV_8UC3, Scalar.all(0));

        Imgproc.circle(mask,
                new Point(circle.getRadius(), circle.getRadius()),
                (int) circle.getRadius(),
                new Scalar(255, 255, 255),
                -1, Core.LINE_AA);

        Core.bitwise_and(inputMat, mask, mask);

        return mask;
    }


    private void initialize(int posHeight, int posWidth) {
        circle = initializeCircle(posHeight, posWidth);
    }

    /**
     * Cuts a square that delineate the circle
     * @param inputMat - the original matrix
     * @return a new matrix of the square that delineate the circle
     */
    private Mat cutSquare(Mat inputMat) {
        Point point = circle.getCenter().clone();

        point.x = point.x - circle.getRadius();
        point.y = point.y - circle.getRadius();
        tangentSquare = new SquareWrapper((int) point.x, (int) point.y, circle.getRadius() * 2);

        return new Mat(inputMat, tangentSquare.getSquare()).clone();
    }

    private Circle initializeCircle(int posHeight, int posWidth) {
        return new Circle(posWidth, posHeight,75,75,150);
    }

    @Override
    public void onBackPressed() {
        Utilities.activityResult(Constant.RESULT_FAILURE, this, information.getSerialNumber());
        super.onBackPressed();
    }


    public void OnHomeClick(View view) {
        Utilities.activityResult(Constant.RESULT_FAILURE, this, information.getSerialNumber());
        finish();
    }

    /**
     * Initialize the camera and it's lifecycle activities (including the preview and the image analysis)
     */
    @SuppressLint("RestrictedApi")
    private void startCamera() {
        cameraProviderFuture.addListener(() -> {
            ProcessCameraProvider cameraProvider = null;
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

            preview = setPreview();
            imageAnalysis = setImageAnalysis();
            CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
            assert cameraProvider != null;
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
            setTorch();
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Set the camera's torch accordingly to the button
     */
    private void setTorch() {
        try {
            CameraInfo cameraInfo = camera.getCameraInfo();
            boolean isFlashAvailable = cameraInfo.hasFlashUnit();
            flash.setVisibility(isFlashAvailable ? View.VISIBLE : View.INVISIBLE);
            camera.getCameraControl().enableTorch(isTorchMode);
        } catch (Exception e) {
            Timber.tag("INFO").w(e, "Cannot get flash available information");
            flash.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Sets the camera's preview
     * @return a preview object as part of the camera's lifecycle
     */
    private Preview setPreview() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        Size screen = new Size(width, height); //size of the screen

        return new Preview.Builder().setTargetResolution(screen).build();
    }

    /**
     * Sets the image analysis
     * @return an image analysis object as part of the camera's lifecycle
     */
    @SuppressLint("UnsafeExperimentalUsageError")
    private ImageAnalysis setImageAnalysis() {
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder().setTargetResolution(new Size(previewView.getWidth(), previewView.getHeight()));
        imageAnalysis = builder.build();

        final AtomicInteger[] frameCounter = {new AtomicInteger()};
        final long[] lastFpsTimestamp = {System.currentTimeMillis()};

        imageAnalysis.setAnalyzer(executor, image -> {
            Bitmap bitmap = Bitmap.createBitmap(previewView.getWidth(), previewView.getHeight(), Bitmap.Config.RGB_565);
            Mat src = Yuv.rgb(Objects.requireNonNull(image.getImage()));
            Core.transpose(src, src);
            Core.flip(src, src, 1);
            Imgproc.resize(src, src, new org.opencv.core.Size(previewView.getWidth(), previewView.getHeight()));
            Mat dst;

            dst = onCameraFrame(src);
            Utils.matToBitmap(dst, bitmap);
            runOnUiThread(() -> this.frontImage.setImageBitmap(bitmap));

            new Thread(() -> {
                int frameCount = 10;
                if (frameCounter[0].incrementAndGet() % frameCount == 0) {
                    frameCounter[0].set(0);
                    long now = System.currentTimeMillis();
                    long delta = now - lastFpsTimestamp[0];
                    long fps = 1000 * frameCount / delta;
                    runOnUiThread(() -> framesTextView.setText(String.format(Locale.getDefault(),"%d FRAMES PER SECOND", fps)));
                    lastFpsTimestamp[0] = now;
                }}).start();

            image.close();
        });

        return imageAnalysis;
    }

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

    /**
     * Pause the activity
     */
    @Override
    protected void onPause() {
        super.onPause();
        try {
            ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
            cameraProvider.unbindAll();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Destroys the activity and it's resources
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
            cameraProvider.unbindAll();
            segModel.close();
            segModel = null;
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Click listen for the torch button
     * @param view - the torch button
     */
    public void OnTorch(View view) {
        int torchState;

        try{
            torchState = Objects.requireNonNull(camera.getCameraInfo().getTorchState().getValue());
        } catch (NullPointerException exception){
            Toast.makeText(this,"Couldn't get the state of the torch", Toast.LENGTH_LONG).show();
            return;
        }

        if ( torchState == TorchState.OFF) {
            isTorchMode = true;
            flash.setImageResource(R.drawable.ic_flash_off);
        } else {
            isTorchMode = false;
            flash.setImageResource(R.drawable.ic_flash_on);
        }

        setTorch();
    }

    void finishTask(int result) {
        runOnUiThread(() -> {
            Utilities.activityResult(result, this, information.getSerialNumber());
            finish();
        });
    }
}