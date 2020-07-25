package yearly_project.frontend.DB;

import android.graphics.Bitmap;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.lang.reflect.Field;

import yearly_project.frontend.utils.Utilities;

public class Image {
    private final static String type = "png";
    private final static String folderName = "Pictures";
    private String path;
    private String name;

    public Image(String name, String path, Mat mat) {
        this.name = name + "." + type;
        this.path = path + "/" + folderName;
        createImage(mat);
    }

    private void createImage(Mat mat) {
        new File(path).mkdirs();
        Imgcodecs.imwrite(path + "/" + name, mat);//
    }

    public String getPath(){
        return path;
    }

    public Mat getImageAsMat() {
        return Imgcodecs.imread(path + "/" + name);
    }

    public Bitmap getImageAsBitmap() {
        Mat mat = getImageAsMat();

        return Utilities.convertMatToBitMap(mat);
    }

    public boolean verify() throws IllegalAccessException {
        for (Field field : getClass().getDeclaredFields()) {
            if (field.get(this) == null)
                return false;
        }

        return true;
    }
}
