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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import yearly_project.frontend.utils.Utilities;

public class SegmentationModel {

    public enum eModel{
//        V2("V2","MobileNet_V2.tflite"),
//        V3_SMALL("V3-Small","MobileNet_V3_small_FLOAT.tflite"),
        V3_LARGE("V3-Large","MobileNet_V3_large.tflite");

        private String fileName;
        public String friendlyName;

        eModel(String friendlyName, String fileName){
            this.fileName = fileName;
            this.friendlyName=friendlyName;
        }

        @NotNull
        @Override public String toString(){
            return friendlyName;
        }
    }

    // image buffers shape
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 1;
    private static int DIM_HEIGHT;
    private static int DIM_WIDTH;
    private static final int INCHANNELS = 3;
    private static final int OUTCHANNELS = 1;
    private static final int IMAGE_MEAN = 0;
    private static final float IMAGE_STD = 1;

    private Interpreter tflite;
    private ByteBuffer inBuffer;                          // model input buffer(uint8)
    private int[][][] outBuffer;
    private GpuDelegate gpuDelegate;


    public SegmentationModel(final Activity activity, eModel segmentationModel) throws IOException {
        Interpreter.Options tfliteOptions = new Interpreter.Options();
        try {
            tfliteOptions.setNumThreads(4);
            gpuDelegate = new GpuDelegate();
            tfliteOptions.addDelegate(gpuDelegate);
            tflite = new Interpreter(Utilities.loadMappedFile(activity, segmentationModel.fileName), tfliteOptions);
        } catch (IOException e) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Utilities.createAlertDialog(activity,"Error", "Failed to load segmentation model");
                }
            });
        }
    }

    public Mat segmentImage(Mat modelMat, int length) {
        if (tflite != null) {
            if (inBuffer == null) initializeInByteBuffer(length);
            if (outBuffer == null) initializeOutBuffer();
            int oLength = modelMat.height();
            Imgproc.resize(modelMat, modelMat, new Size(DIM_WIDTH, DIM_HEIGHT));
            loadMatToBuffer(modelMat);
            modelMat.release();
            tflite.run(inBuffer, outBuffer);
            modelMat = loadFromBufferToMat(outBuffer);
            Imgproc.resize(modelMat, modelMat, new Size(oLength, oLength));
        }

        return modelMat;
    }
//
//    public Mat segmentImage(Mat modelMat, int length) {
//        if (tflite != null) {
//            if (inpImg == null) initializeByteBuffer(length);
//            if (outImg == null) initializeOutImg();
//            File imgFile = new File(Environment.getExternalStorageDirectory().getPath() + "/photos");
//            for (File image : imgFile.listFiles()) {
//                Bitmap bitmap = null;
//                if (image.exists()) {
//                    bitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
//                }
//
//                Bitmap drawableBitmap = bitmap.copy(Bitmap.Config.RGB_565, true);
//                Mat resizedMap = new Mat();
//                Utils.bitmapToMat(drawableBitmap, resizedMap);
//                Imgproc.resize(resizedMap, resizedMap, new Size(DIM_WIDTH, DIM_HEIGHT));
//                Imgproc.cvtColor(resizedMap,resizedMap,Imgproc.COLOR_BGR2RGB);
//                loadMatToBuffer(resizedMap);
//                modelMat.release();
//                tflite.run(inpImg, outImg);
//                modelMat = loadFromBufferToMat(outImg);
//                final Bitmap map = MainActivity.convertMatToBitMap(modelMat);
//                modelMat = loadFromBufferToMat(outImg);
//            }
//        }
//
//        return modelMat;
//    }

    private Mat loadFromBufferToMat(int[][][] outImg) {
        Mat mat = new Mat(DIM_WIDTH, DIM_HEIGHT, CvType.CV_8UC3, Scalar.all(0));

        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < DIM_HEIGHT; j++) {
                for (int k = 0; k < DIM_WIDTH; k++) {
                    if (outImg[i][j][k] != 0) {
                        double[] array = mat.get(j, k);
                        array[1] = 255;
                        mat.put(j, k, array);
                    }
                }
            }
        }

        return mat;
    }


    private void initializeOutBuffer() {
        outBuffer = new int[DIM_BATCH_SIZE][DIM_WIDTH][DIM_HEIGHT * OUTCHANNELS];
//        outImg = new long[DIM_BATCH_SIZE][DIM_WIDTH * DIM_HEIGHT * OUTCHANNELS];
    }

    private void initializeInByteBuffer(int length) {
        DIM_HEIGHT = length;
        DIM_WIDTH = length;
        inBuffer = ByteBuffer.allocateDirect(DIM_BATCH_SIZE * DIM_HEIGHT * DIM_WIDTH * DIM_PIXEL_SIZE * INCHANNELS);
        inBuffer.order(ByteOrder.nativeOrder());
    }

//    private void loadMatToBuffer(Mat mat){
//    inpImg.rewind();
//        map.getPixels(intValues,0,map.getWidth(),0,0,map.getWidth(),map.getHeight());
//        int pixel =0;
//        for(int i=0; i<DIM_HEIGHT; ++i){
//            for(int j=0; j<DIM_WIDTH; ++j){
//                final int val = intValues[pixel++];
////                Log.i("INFO", String.valueOf((((float)(((val >> 16) & 0xFF) - IMAGE_MEAN))/IMAGE_STD)));
//                inpImg.putFloat((((((val >> 16) & 0xFF) - IMAGE_MEAN))/IMAGE_STD));
//                inpImg.putFloat((((((val >> 8) & 0xFF) - IMAGE_MEAN))/IMAGE_STD));
//                inpImg.putFloat((((((val) & 0xFF) - IMAGE_MEAN))/IMAGE_STD));
//            }
//        }
//    }

    private void loadMatToBuffer(Mat mat) {
        inBuffer.rewind();
        for (int i = 0; i < DIM_HEIGHT; ++i) {
            for (int j = 0; j < DIM_WIDTH; ++j) {
                double[] pixel = mat.get(i, j);
                inBuffer.put((byte) pixel[0]);
                inBuffer.put((byte) pixel[1]);
                inBuffer.put((byte) pixel[2]);
            }
        }
    }

    public boolean isSegmentationSuccessful(){
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < DIM_HEIGHT; j++) {
                for (int k = 0; k < DIM_WIDTH; k++) {
                    if (outBuffer[i][j][k] != 0) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public int[][] getResult(){
        return outBuffer[0];
    }

    public Mat getSegmantation(){
        Mat mat  = new Mat(DIM_WIDTH,DIM_HEIGHT, CvType.CV_8UC3, Scalar.all(0));

        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < DIM_HEIGHT; j++) {
                for (int k = 0; k < DIM_WIDTH; k++) {
                    if (outBuffer[i][j][k] != 0) {
                        double[] pixel = mat.get(j,k);
                        pixel[0]=255;
                        pixel[1]=255;
                        pixel[2]=255;
                        mat.put(j, k, pixel);
                    }
                }
            }
        }

        return mat;
    }

    public void close() {
        if (gpuDelegate != null) {
            gpuDelegate.close();
            gpuDelegate = null;
        }
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }
}
