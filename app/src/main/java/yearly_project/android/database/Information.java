package yearly_project.android.database;

import android.annotation.SuppressLint;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import timber.log.Timber;
import yearly_project.android.Constant;
import yearly_project.android.utilities.Utilities;

public class Information implements Comparable<Information> {
    private Date date;
    @SuppressLint("SimpleDateFormat")
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
    private Images images;
    private String description;
    private int serialNumber;
    private String path;

    public Information(int serialNumber, String basePath) {
        this.serialNumber = serialNumber;
        setPath(serialNumber, basePath);
        date =  new Date();
        images = new Images(getPath());
        createFolder();
    }

    //////////////////////////////////////////// Getters and Setters ////////////////////////////////////////////
    public String getDate() {
        return dateFormat.format(date);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPath(){
        return this.path;
    }

    private void setPath(int serialNumber, String path){
        this.path = path + "/" + serialNumber;
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    public Images getImages(){
        return images;
    }

    //////////////////////////////////////////// Comparable implementation ////////////////////////////////////////////

    @Override
    public int compareTo(Information information) {
        return information.date.compareTo(date);
    }

    //////////////////////////////////////////// Verification Functions ////////////////////////////////////////////

    public boolean isValid() throws IllegalAccessException {
        return verifyCameraActivity()
                && verifyResults()
                && verifyResultActivity();
    }

    public boolean verifyCameraActivity() throws IllegalAccessException {
        return getImages().verifyImagesAmount() &&
                getImages().areFieldsPopulated() &&
                getDate()!=null &&
                getPath() != null;
    }

    public boolean verifyResults() throws IllegalAccessException {
        return getImages().verifyMoles();
    }

    public boolean verifyResultActivity(){
        return getDescription() !=null;
    }

    //////////////////////////////////////////// IO Operations ////////////////////////////////////////////

    public static Information fetchObject(String path) throws IOException {
        Gson gson = new Gson();

        return gson.fromJson(new FileReader(path), Information.class);
    }

    public void saveStateToFile(){
        try (FileWriter file = new FileWriter(path + "/" + Constant.STATE_FILE_NAME)) {
            file.write(getJson());
            file.flush();
        } catch (IOException e) {
            Timber.i(e);
        }
    }


    public void delete() {
        Utilities.deleteFile(path);
    }

    public String getJson(){
        return new Gson().toJson(this);
    }

    private void createFolder() {
        File file = new File(path);

        file.mkdirs();
    }

    /**
     * Calculates the average result of the same mole among numerous images
     * @param id - the mole's id
     * @return a result object
     */
    public Result getAverageResultOfMole(int id){
        Result result = new Result();
        int amountOfDivision = 0;
        for(Image image : getImages()){
            Mole mole = image.getMoles().getMole(id);
            if(mole != null){
                ++ amountOfDivision;
                result.add(mole.getResult());
            }
        }

        result.division(amountOfDivision);

        return result;
    }
}
