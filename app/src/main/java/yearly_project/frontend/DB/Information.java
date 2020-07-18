package yearly_project.frontend.DB;

import com.google.gson.Gson;

import org.opencv.core.Mat;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;

import timber.log.Timber;
import yearly_project.frontend.Constants;

public class Information {
    private Date date;
    private Collection<Image> images;
    private Collection<Result> results;
    private int serialNumber;
    private String path;

    public Information(int serialNumber, String basePath) {
        this.serialNumber = serialNumber;
        images = new LinkedList<>();
        path = basePath + "/" + serialNumber;
    }

    private Information(){}

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Collection<Image> getImages() {
        return images;
    }

    public void addImage(Mat mat) {
        images.add(new Image(Integer.toString(images.size()), path, mat));
    }

    public static Information fetchObject(String path) throws IOException {
        Gson gson = new Gson();

        return gson.fromJson(Files.readAllLines(Paths.get(path)).toString(), Information.class);
    }

    public boolean verify() throws IllegalAccessException {
        return !areNullFields() && areArraySizesCorrect() && verifyComposedObjects();
    }

    private boolean areNullFields() throws IllegalAccessException {
        for (Field field : getClass().getDeclaredFields()) {
            if (field.get(this) == null)
                return true;
        }

        return false;
    }

    private boolean areArraySizesCorrect() {
        return images.size() == Constants.AMOUNT_OF_PICTURES_TO_TAKE && results.size() >= 1;
    }

    private boolean verifyComposedObjects() throws IllegalAccessException {
        return verifyImages() && verifyResults();
    }

    private boolean verifyResults() throws IllegalAccessException {
        boolean areResultsCorrect = true;
        for (Result result : results) {
            areResultsCorrect = areResultsCorrect && result.verify();
        }

        return areResultsCorrect;
    }

    private boolean verifyImages() throws IllegalAccessException {
        boolean areImagesCorrect = true;
        for (Image image : images) {
            areImagesCorrect = areImagesCorrect && image.verify();
        }

        return areImagesCorrect;
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(int serialNumber) {
        this.serialNumber = serialNumber;
    }

    public void saveStateToFile(){
        try (FileWriter file = new FileWriter(path + "/" + Constants.STATE_FILE_NAME)) {
            file.write(new Gson().toJson(this));
            file.flush();
        } catch (IOException e) {
            Timber.i(e.getMessage());
        }
    }

//    public loadInformation(){
//
//    }
}
