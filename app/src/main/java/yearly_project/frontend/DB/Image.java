package yearly_project.frontend.DB;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.lang.reflect.Field;

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

    public Bitmap getImageAsBitmap() {
        File imgFile = new  File(path + "/" + name);
        if(imgFile.exists()){
            return BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        }

        return null;
    }

    public boolean verify() throws IllegalAccessException {
        for (Field field : getClass().getDeclaredFields()) {
            if (field.get(this) == null)
                return false;
        }

        return true;
    }
}
