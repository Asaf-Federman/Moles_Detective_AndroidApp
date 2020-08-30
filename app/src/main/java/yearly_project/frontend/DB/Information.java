package yearly_project.frontend.DB;

import android.annotation.SuppressLint;

import com.google.gson.Gson;

import org.opencv.core.Mat;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import timber.log.Timber;
import yearly_project.frontend.Constant;
import yearly_project.frontend.utils.Utilities;

public class Information implements Comparable<Information> {
    private Date date;
    @SuppressLint("SimpleDateFormat")
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
    private List<Image> images;
    private String description;
    private Collection<Result> results;
    private int serialNumber;
    private String path;

    public Information(int serialNumber, String basePath) {
        this.serialNumber = serialNumber;
        images = new LinkedList<>();
        results = new LinkedList<>();
        setPath(serialNumber, basePath);
        date =  new Date();
        createFolder();
    }

    private void createFolder() {
        File file = new File(path);

        file.mkdirs();
    }

    public String getDate() {
        return dateFormat.format(date);
    }

    public List<Image> getImages() {
        return images;
    }

    public void addImage(Mat mat) {
        Image image = new Image(Integer.toString(images.size()), path, mat);
        synchronized (this){
            images.add(image);
        }
    }

    public static Information fetchObject(String path) throws IOException {
        Gson gson = new Gson();

        return gson.fromJson(new FileReader(path), Information.class);
    }

    public boolean verify() throws IllegalAccessException {
        return verifyCameraActivity()
//                && verifyCalculateResultActivity()
                && verifyResultActivity();
    }

    public boolean verifyCameraActivity() throws IllegalAccessException {
        return images.size() >= Constant.AMOUNT_OF_PICTURES_TO_TAKE &&
                verifyImages() &&
                getDate()!=null &&
                getPath() != null;
    }

    public boolean verifyCalculateResultActivity() throws IllegalAccessException {
        return results.size()>0 && verifyResults();
    }

    public boolean verifyResultActivity(){
        return getDescription() !=null;
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

    private void setSerialNumber(int serialNumber) {
        this.serialNumber = serialNumber;
    }

    private void setPath(int serialNumber, String path){
        this.path = path + "/" + serialNumber;
    }

    public void saveStateToFile(){
        try (FileWriter file = new FileWriter(path + "/" + Constant.STATE_FILE_NAME)) {
            file.write(getJson());
            file.flush();
        } catch (IOException e) {
            Timber.i(e);
        }
    }

    public String getPath(){
        return this.path;
    }

    public void delete() {
        Utilities.deleteFile(path);
    }

    @Override
    public int compareTo(Information information) {
        return information.date.compareTo(date);
    }

    public String getJson(){
        return new Gson().toJson(this);
    }

    public String getDescription() {
        return description;
    }

    public boolean setDescription(String description) {
        if(description.isEmpty())
            return false;

        this.description = description;
        return true;
    }

    public void addResult(Result result){
        results.add(result);
    }

    public Collection<Result> getResults(){
        return results;
    }

}
