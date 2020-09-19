package yearly_project.frontend.camera;

import android.app.Activity;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import timber.log.Timber;
import yearly_project.frontend.utils.Utilities;

public class SegmentationModel {

    private GpuDelegate gpuDelegate;

    public enum eModel {
        V3_LARGE("V3-Large", "MobileNet_V3_large.tflite");

        private String fileName;
        public String key;

        eModel(String key, String fileName) {
            this.fileName = fileName;
            this.key = key;
        }

        @NotNull
        @Override
        public String toString() {
            return key;
        }
    }

    // image buffers shape
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 1;
    private static final int IN_CHANNELS = 3;
    private static final int OUT_CHANNELS = 1;
    public static final int DIM_LENGTH = 100;
    private static int DIM_HEIGHT;
    private static int DIM_WIDTH;
    private float brightness_scalar = 1f;
    private int direction = -1;
    private boolean isSuccessful;
    private Interpreter tfliteInterpreter;
    private ByteBuffer inBuffer;
    private int[][][] outBuffer;

    public SegmentationModel(final Activity activity, eModel segmentationModel) {
        initializeInByteBuffer();
        initializeOutBuffer();
        createTfliteIntrepreter(activity, segmentationModel);
    }

    /**
     * Creates a tflite interpreter
     * @param activity - the camera activity
     * @param segmentationModel - the tflite model that will be used by the interpreter
     */
    private void createTfliteIntrepreter(final Activity activity, eModel segmentationModel) {
        try {
            Interpreter.Options tfliteOptions = new Interpreter.Options();
            gpuDelegate = new GpuDelegate();
            tfliteOptions.addDelegate(gpuDelegate);
            tfliteInterpreter = new Interpreter(Utilities.loadMappedFile(activity, segmentationModel.fileName), tfliteOptions);

            return;
        } catch (Exception e) {
            Timber.i("Failed to load GpuDelegate");
        }

        try {
            Interpreter.Options tfliteOptions = new Interpreter.Options();
            tfliteInterpreter = new Interpreter(Utilities.loadMappedFile(activity, segmentationModel.fileName), tfliteOptions);
        } catch (Exception e) {
            activity.runOnUiThread(() -> Utilities.createAlertDialog(activity, "Error", "Failed to load segmentation model" , null));
        }
    }

    /**
     * Receives an image, and runs the tflite interpreter on it
     * @param modelMat - the input matrix
     * @return output matrix with the tflite interpreter result
     */
    public Mat segmentImage(Mat modelMat) {
        if (tfliteInterpreter != null) {
            int oLength = modelMat.height();
            changeBrightness();
            Imgproc.resize(modelMat, modelMat, new Size(DIM_WIDTH, DIM_HEIGHT));
            loadMatToBuffer(modelMat);
            tfliteInterpreter.run(inBuffer, outBuffer);
            modelMat = loadFromBufferToMat(outBuffer);
            Imgproc.resize(modelMat, modelMat, new Size(oLength, oLength));
        }

        return modelMat;
    }

    /**
     * Changes the color of the segmentation for the user
     */
    private void changeBrightness() {
        if (brightness_scalar > 1.0f || brightness_scalar < 0.7) {
            direction = direction * -1;
        }

        brightness_scalar = brightness_scalar + 0.15f * direction;
    }

    /**
     * Loads byte buffer into a matrix
     * @param outImg - the tflite's interpreter output
     * @return a matrix built from the tflite's interpreter output
     */
    private Mat loadFromBufferToMat(int[][][] outImg) {
        Mat mat = new Mat(DIM_WIDTH, DIM_HEIGHT, CvType.CV_8UC3, Scalar.all(0));

        isSuccessful = false;
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < DIM_HEIGHT; j++) {
                for (int k = 0; k < DIM_WIDTH; k++) {
                    if (outImg[i][j][k] != 0) {
                        isSuccessful = true;
                        double[] array = mat.get(j, k);
                        array[1] = 255 * brightness_scalar;
                        mat.put(j, k, array);
                    }
                }
            }
        }

        return mat;
    }


    private void initializeOutBuffer() {
        outBuffer = new int[DIM_BATCH_SIZE][DIM_WIDTH][DIM_HEIGHT * OUT_CHANNELS];
    }

    /**
     * Initialize the byte buffer
     */
    private void initializeInByteBuffer() {
        DIM_HEIGHT = DIM_LENGTH;
        DIM_WIDTH = DIM_LENGTH;
        inBuffer = ByteBuffer.allocateDirect(DIM_BATCH_SIZE * DIM_HEIGHT * DIM_WIDTH * DIM_PIXEL_SIZE * IN_CHANNELS);
        inBuffer.order(ByteOrder.nativeOrder());
    }

    /**
     * Loads the mat into a byte buffer to the tflite model
     * @param mat - The input matrix
     */
    private void loadMatToBuffer(Mat mat) {
        inBuffer.rewind();
        byte[] data = new byte[DIM_WIDTH * DIM_HEIGHT * IN_CHANNELS];
        mat.get(0, 0, data);
        inBuffer = ByteBuffer.wrap(data);
    }

    public boolean isSegmentationSuccessful() {
        return isSuccessful;
    }

    /**
     * Closes all the resources used by the class instance
     */
    public void close() {
        if (gpuDelegate != null) {
            gpuDelegate.close();
            gpuDelegate = null;
        }
        if (tfliteInterpreter != null) {
            tfliteInterpreter.close();
            tfliteInterpreter = null;
        }
    }
}
