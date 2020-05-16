package yearly_project.frontend.camera;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.nnapi.NnApiDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class SegmentationModel {

    public enum eModel{
        V2("V2","MobileNet_V2.tflite"),
        V3_SMALL("V3-Small","MobileNet_V3_small_FLOAT.tflite"),
        V3_LARGE("V3-Large","MobileNet_V3_large_FLOAT.tflite");

        private String fileName;
        public String friendlyName;

        eModel(String friendlyName, String fileName){
            this.fileName = fileName;
            this.friendlyName=friendlyName;
        }

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
    private Activity activity;
    private ByteBuffer inpImg;                          // model input buffer(uint8)
    private int[][][] outImg;
    private GpuDelegate gpuDelegate;
    private eModel segmentationModel;


    public SegmentationModel(final Activity activity, eModel segmentationModel) throws IOException {
        this.segmentationModel = segmentationModel;
        this.activity = activity;
        Interpreter.Options tfliteOptions = new Interpreter.Options();
        try {
            tfliteOptions.setNumThreads(4);
            gpuDelegate = new GpuDelegate();
            tfliteOptions.addDelegate(gpuDelegate);
            tflite = new Interpreter(loadMappedFile(activity, this.segmentationModel.fileName), tfliteOptions);
        } catch (IOException e) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity.getApplicationContext(), "Failed to load segmentation model", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public Mat segmentImage(Mat modelMat, int length) {
        if (tflite != null) {
            if (inpImg == null) initializeByteBuffer(length);
            if (outImg == null) initializeOutImg();
            int oLength = modelMat.height();
            Imgproc.resize(modelMat, modelMat, new Size(DIM_WIDTH, DIM_HEIGHT));
            loadMatToBuffer(modelMat);
            modelMat.release();
            tflite.run(inpImg, outImg);
            modelMat = loadFromBufferToMat(outImg);
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
//        Imgproc.cvtColor(mat,mat,Imgproc.COLOR_RGBA2RGB);

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


    private void initializeOutImg() {
        outImg = new int[DIM_BATCH_SIZE][DIM_WIDTH][DIM_HEIGHT * OUTCHANNELS];
//        outImg = new long[DIM_BATCH_SIZE][DIM_WIDTH * DIM_HEIGHT * OUTCHANNELS];
    }

    private void initializeByteBuffer(int length) {
        DIM_HEIGHT = length;
        DIM_WIDTH = length;
        inpImg = ByteBuffer.allocateDirect(DIM_BATCH_SIZE * DIM_HEIGHT * DIM_WIDTH * DIM_PIXEL_SIZE * INCHANNELS);
        inpImg.order(ByteOrder.nativeOrder());
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
        inpImg.rewind();
        for (int i = 0; i < DIM_HEIGHT; ++i) {
            for (int j = 0; j < DIM_WIDTH; ++j) {
                double[] pixel = mat.get(i, j);
                inpImg.put((byte) pixel[0]);
                inpImg.put((byte) pixel[1]);
                inpImg.put((byte) pixel[2]);
            }
        }
    }

    public void close() {
//        if(nnApiDelegate != null){
//            nnApiDelegate.close();
//            nnApiDelegate = null;
//        }
        if (gpuDelegate != null) {
            gpuDelegate.close();
            gpuDelegate = null;
        }
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }

    @NonNull
    public static MappedByteBuffer loadMappedFile(@NonNull Context context, @NonNull String filePath) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(filePath);

        MappedByteBuffer var9;
        try {
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());

            try {
                FileChannel fileChannel = inputStream.getChannel();
                long startOffset = fileDescriptor.getStartOffset();
                long declaredLength = fileDescriptor.getDeclaredLength();
                var9 = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
            } catch (Throwable var12) {
                try {
                    inputStream.close();
                } catch (Throwable var11) {
                    var12.addSuppressed(var11);
                }

                throw var12;
            }

            inputStream.close();
        } catch (Throwable var13) {
            if (fileDescriptor != null) {
                try {
                    fileDescriptor.close();
                } catch (Throwable var10) {
                    var13.addSuppressed(var10);
                }
            }

            throw var13;
        }

        if (fileDescriptor != null) {
            fileDescriptor.close();
        }

        return var9;
    }
}
