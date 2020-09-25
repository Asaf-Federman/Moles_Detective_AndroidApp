package yearly_project.android.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

import timber.log.Timber;

public class Utilities {

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
            try {
                fileDescriptor.close();
            } catch (Throwable var10) {
                var13.addSuppressed(var10);
            }

            throw var13;
        }

        fileDescriptor.close();

        return var9;
    }

    public static void createAlertDialog(Context context, String title, String content, DialogInterface.OnClickListener clickListener){
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton(android.R.string.ok, clickListener)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .show();
    }

    public static void createQuestionDialog(Context context, String title, String content, DialogInterface.OnClickListener yesListener, DialogInterface.OnClickListener noListener){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(content)
                .setPositiveButton("Yes", yesListener)
                .setNegativeButton("No", noListener)
                .show();
    }

    public static Bitmap convertMatToBitMap(Mat input) {
        Bitmap bmp = null;

        try {
            bmp = Bitmap.createBitmap(input.cols(), input.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(input, bmp);
        } catch (CvException e) {
            Timber.d(e);
        }

        return bmp;
    }

    public static void deleteFile(String path){
        File file = new File(path);
        if (file.isDirectory())
            for (File child : Objects.requireNonNull(file.listFiles()))
                deleteFile(child.getAbsolutePath());

        file.delete();
    }

    public static boolean isBetween(int x, int lower, int upper) {
        return lower <= x && x <= upper;
    }

    public static void activityResult(int result, Activity activity, int ID) {
        Intent data = new Intent();
        data.putExtra("ID", ID);
        activity.setResult(result, data);
    }
}
