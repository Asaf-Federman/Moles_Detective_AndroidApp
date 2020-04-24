package yearly_project.frontend;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class SegmentationModel {
    private static final String MODEL_PATH = "MobileNet_V2.tflite";    // model to use

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
    private long[][][] outImg;                            // model output buffer(int64)

    public SegmentationModel(final Activity activity) throws IOException {
        this.activity = activity;
        try {
            tflite = new Interpreter(loadModelFile(activity));
            tflite.setNumThreads(4);
        } catch (IOException e) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity.getApplicationContext(),"Failed to load segmentation model",Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

//    public void segmentImage(Mat modelMat, int length) {
//        if (tflite != null) {
//            if (inpImg == null) initializeByteBuffer(length);
//            if (outImg == null) initializeOutImg();
//            Mat resizedMap = new Mat();
//            Imgproc.resize(modelMat, resizedMap, new Size(DIM_WIDTH, DIM_HEIGHT));
//            loadMatToBuffer(resizedMap);
//            tflite.run(inpImg, outImg);
////            Bitmap bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.RGB_565);
////            ByteBuffer buffer = ByteBuffer.wrap(inpImg.array());
////            bmp.copyPixelsFromBuffer(buffer);
//            checkWhatContains(outImg);
//        }
//    }

    public void segmentImage(Mat modelMat, int length) {
        if (tflite != null) {
            if (inpImg == null) initializeByteBuffer(length);
            if (outImg == null) initializeOutImg();
            File imgFile = new File(Environment.getExternalStorageDirectory().getPath() + "/photos");
            for (File image : imgFile.listFiles()) {
                Bitmap bitmap = null;
                if (image.exists()) {
                    bitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
                }

                Bitmap drawableBitmap = bitmap.copy(Bitmap.Config.RGB_565, true);
                Mat resizedMap = new Mat();
                Utils.bitmapToMat(drawableBitmap, resizedMap);
                Imgproc.resize(resizedMap, resizedMap, new Size(DIM_WIDTH, DIM_HEIGHT));
//                Imgproc.cvtColor(resizedMap, resizedMap, Imgproc.COLOR_RGBA2RGB);
                loadMatToBuffer(resizedMap);
                tflite.run(inpImg, outImg);

//                Bitmap bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.RGB_565);
//                ByteBuffer buffer = ByteBuffer.wrap(inpImg.array());
//                bmp.copyPixelsFromBuffer(buffer);
                checkWhatContains(outImg);
            }
        }
    }

    private void loadMatToBuffer(Mat mat){
        Bitmap image = Bitmap.createBitmap(200,
                200, Bitmap.Config.RGB_565);

        Utils.matToBitmap(mat, image);

        Bitmap bitmap = (Bitmap) image;
        inpImg.rewind();
        bitmap.copyPixelsToBuffer(inpImg);
        inpImg.rewind();
    }


    private void checkWhatContains(long[][][] outImg) {
        boolean isDifferentThanZero = false;
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < DIM_HEIGHT; j++) {
                for (int k = 0; k < DIM_WIDTH; k++) {
                    if (outImg[i][j][k] != 0) {
                        isDifferentThanZero = true;
                        break;
                    }
                }
//                if (outImg[i][j] != 0) {
//                    isDifferentThanZero = true;
//                    break;
//                }
            }
        }
        if (isDifferentThanZero) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity.getApplicationContext(),"MADE IT",Toast.LENGTH_LONG).show();
            }
        });
            Log.i("INFO", "MADE IT");
        }
    }

    private void writeToFile(String data, Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("config.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private void initializeOutImg() {
        outImg = new long[DIM_BATCH_SIZE][DIM_WIDTH][DIM_HEIGHT * OUTCHANNELS];
//        outImg = new long[DIM_BATCH_SIZE][DIM_WIDTH * DIM_HEIGHT * OUTCHANNELS];
    }

    private void initializeByteBuffer(int length) {
        DIM_HEIGHT = length;
        DIM_WIDTH = length;
        inpImg = ByteBuffer.allocateDirect(DIM_BATCH_SIZE * DIM_HEIGHT * DIM_WIDTH * DIM_PIXEL_SIZE * INCHANNELS);
        inpImg.order(ByteOrder.nativeOrder());
    }

//    private void loadMatToBuffer(Mat inMat) {
//        //convert opencv mat to tensorflowlite input
//        inpImg.rewind();
//        byte[] data = new byte[DIM_WIDTH * DIM_HEIGHT * INCHANNELS];
//        inMat.get(0, 0, data);
//        inpImg = ByteBuffer.wrap(data);
//    }

//    private void loadMatToBuffer(Bitmap map){
//        inpImg.rewind();
//        int[] intValues = new int[120000];
//
//        map.getPixels(intValues,0,map.getWidth(),0,0,map.getWidth(),map.getHeight());
//        int pixel =0;
//        for(int i=0; i<200; ++i){
//            for(int j=0; j<200; ++j){
//                final int val = intValues[pixel++];
////                Log.i("INFO", String.valueOf((((float)(((val >> 16) & 0xFF) - IMAGE_MEAN))/IMAGE_STD)));
//                inpImg.putFloat((((((val >> 16) & 0xFF) - IMAGE_MEAN))/IMAGE_STD));
//                inpImg.putFloat((((((val >> 8) & 0xFF) - IMAGE_MEAN))/IMAGE_STD));
//                inpImg.putFloat((((((val) & 0xFF) - IMAGE_MEAN))/IMAGE_STD));
//            }
//        }
//    }

    private void loadMatToBuffer(Bitmap map){
        inpImg.rewind();
        for(int i=0; i<200; ++i){
            for(int j=0; j<200; ++j){
                int pixel = map.getPixel(i,j);
                inpImg.put((byte) ((pixel >> 16)& 0xFF));
                inpImg.put((byte) ((pixel >> 8)& 0xFF));
                inpImg.put((byte) ((pixel)& 0xFF));
            }
        }
    }

    private void loadBufferToMat(Mat modelMat) {
        //convert tensorflowlite output to opencv mat
        Mat temp_outSegment = new Mat(DIM_HEIGHT, DIM_WIDTH, CvType.CV_32SC3);  // temp mask(Mat) -> class colors(int32)

        temp_outSegment.convertTo(modelMat, CvType.CV_8UC3);
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }
}
