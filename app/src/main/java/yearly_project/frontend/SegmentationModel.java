package yearly_project.frontend;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.support.annotation.NonNull;
import android.widget.Toast;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
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
    private int[][][] outImg;                            // model output buffer(int64)

    public SegmentationModel(final Activity activity) throws IOException {
        this.activity = activity;
        Interpreter.Options tfliteOptions = new Interpreter.Options();
        try {
            tfliteOptions.setNumThreads(4);
            tflite = new Interpreter(loadMappedFile(activity, MODEL_PATH), tfliteOptions);
        } catch (IOException e) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity.getApplicationContext(),"Failed to load segmentation model",Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public void segmentImage(Mat modelMat, int length) {
        if (tflite != null) {
            if (inpImg == null) initializeByteBuffer(length);
            if (outImg == null) initializeOutImg();
            int oLength=modelMat.height();
            Imgproc.resize(modelMat, modelMat, new Size(DIM_WIDTH, DIM_HEIGHT));
            loadMatToBuffer(modelMat);
            tflite.run(inpImg, outImg);
            checkWhatContains(outImg, modelMat);
            Imgproc.resize(modelMat, modelMat, new Size(oLength, oLength));
        }
    }

//    public void segmentImage(Mat modelMat, int length) {
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
//                loadMatToBuffer(resizedMap);
//                tflite.run(inpImg, outImg);
//                checkWhatContains(outImg,resizedMap,image.getName());
//                final Bitmap map = MainActivity.convertMatToBitMap(resizedMap);
//                checkWhatContains(outImg,resizedMap,image.getName());
//            }
//        }
//    }

    private void checkWhatContains(int[][][] outImg,Mat mat) {
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < DIM_HEIGHT; j++) {
                for (int k = 0; k < DIM_WIDTH; k++) {
                    if (outImg[i][j][k] != 0) {
                        double[] array = mat.get(j,k);
                        array[2]=255;
                        mat.put(j,k,array);
                    }
                }
            }
        }
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

    private void loadMatToBuffer(Mat mat){
        inpImg.rewind();
        for(int i=0; i<DIM_HEIGHT; ++i){
            for(int j=0; j<DIM_WIDTH; ++j){
                double[] pixel = mat.get(i,j);
                inpImg.put((byte) pixel[0]);
                inpImg.put((byte)pixel[1]);
                inpImg.put((byte) pixel[2]);
            }
        }
    }

    public void close() {
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
