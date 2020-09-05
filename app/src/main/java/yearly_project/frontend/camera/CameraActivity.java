package yearly_project.frontend.camera;

import android.annotation.SuppressLint;
import android.content.Intent;
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
import yearly_project.frontend.waitScreen.CalculateResults;

import static org.opencv.imgproc.Imgproc.cvtColor;
import static yearly_project.frontend.Constant.AMOUNT_OF_PICTURES_TO_TAKE;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener {
    private CameraActivity activity;
    private ImageView startButton;
    private SegmentationModel segModel;
    private SquareWrapper tangentSquare;
    private Circle circle;
    private ScaleGestureDetector gestureDetector;
    private ImageView flash;
    private ExecutorService executor;
    private TextView framesTextView;
    private float shapesLength;
    private int counter = 0;
    private boolean isStart = false;
    private Information information;
    private int cameraWidth;
    private int cameraHeight;
    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private PreviewView previewView;
    private ImageAnalysis imageAnalysis;
    private Preview preview;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageView frontImage;
    private Camera camera;
    private boolean isTorchMode = false;
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
                    shapesLength = Math.max(cameraHeight, cameraWidth) / 3f;
                    initialize(cameraHeight, cameraWidth);
                }
            });
        }

        initializeTensorFlowModel();
    }

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

        if (segModel == null) {
            segModel = new SegmentationModel(CameraActivity.this, SegmentationModel.eModel.V3_LARGE);
        }

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    public Mat onCameraFrame(Mat mat) {
        Mat rectangle, segmentImage;
        cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB);

        rectangle = cutRectangle(mat);
        segmentImage = segModel.segmentImage(rectangle);
        checkForSegmentation(rectangle);
        ifActivityDone();
        addWeights(mat, segmentImage);

//        Imgproc.rectangle(mat, wrappedSquare.getTopLeft(), wrappedSquare.getBottomRight(), new Scalar(0, 0, 0), 3);
        Imgproc.circle(mat, circle.getCenter(), (int) circle.getRadius(), new Scalar(255, 255, 255), 3, Core.LINE_AA);

        return mat;
    }

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
                        runOnUiThread(() -> {
                            Utilities.createAlertDialog(getApplicationContext(), "ERROR", "Failed to verify the need data", (((dialog, which) -> finishTask(Constant.RESULT_FAILURE))));
                        });
                    }
                }
            }
        }
    }

    private void checkForSegmentation(Mat mat) {
        if (isStart) {
            if (segModel.isSegmentationSuccessful()) {
                if (counter < AMOUNT_OF_PICTURES_TO_TAKE) {
                    new Thread(() -> convertMatToPicture(mat.clone())).start();
                }
            }
        }
    }

    private void convertMatToPicture(Mat mat) {
        cvtColor(mat,mat,Imgproc.COLOR_BGR2RGB);
        Imgproc.resize(mat,mat, new org.opencv.core.Size(250,250));
        information.getImages().addImage(mat);
    }

    private void addWeights(Mat src, Mat dest) {
        dest = createMask(dest);
        Mat mat = src.rowRange((int) tangentSquare.getTopLeft().y, (int) tangentSquare.getBottomRight().y).colRange((int) tangentSquare.getTopLeft().x, (int) tangentSquare.getBottomRight().x);
        Core.addWeighted(mat, 1f, dest, 0.3, 1, mat);
    }

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
//        wrappedSquare = initializeRectangle(posHeight, posWidth);
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

//    private SquareWrapper initializeRectangle(int posHeight, int posWidth) {
//        return new SquareWrapper(posWidth, posHeight, shapesLength);
//    }

    @Override
    public void onBackPressed() {
        activityResult(Constant.RESULT_FAILURE);
        super.onBackPressed();
    }

    private void activityResult(int result) {
        Intent data = new Intent(this, CalculateResults.class);
        data.putExtra("ID", information.getSerialNumber());
        setResult(result, data);
    }

    public void OnHomeClick(View view) {
        activityResult(Constant.RESULT_FAILURE);
        finish();
    }

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
            preview.setSurfaceProvider(previewView.createSurfaceProvider());
            setTorch();
        }, ContextCompat.getMainExecutor(this));
    }

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


    private Preview setPreview() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        Size screen = new Size(width, height); //size of the screen

        return new Preview.Builder().setTargetResolution(screen).build();
    }

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
                    runOnUiThread(() -> framesTextView.setText(String.format("%d FRAMES PER SECOND", fps)));
                    lastFpsTimestamp[0] = now;
                }
            }).start();

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

    public void OnTorch(View view) {
        if (camera.getCameraInfo().getTorchState().getValue() == TorchState.OFF) {
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
            activityResult(result);
            finish();
        });
    }
}