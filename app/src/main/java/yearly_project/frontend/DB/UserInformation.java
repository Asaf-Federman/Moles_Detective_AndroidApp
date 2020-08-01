package yearly_project.frontend.DB;

import androidx.databinding.ObservableArrayMap;
import androidx.databinding.ObservableMap;

import java.io.File;
import java.util.Objects;

import yearly_project.frontend.Constants;
import yearly_project.frontend.utils.Utilities;

public class UserInformation {
    private static int counter = 0;
    private static String basicPath;
    private static ObservableMap<Integer, Information> informationMap = new ObservableArrayMap<>();

    private static void addInformation(int serialNumber, Information information) {
        counter = Math.max(counter,serialNumber);

        informationMap.put(serialNumber, information);
    }

    public static ObservableMap<Integer, Information> getInformationMap() {
        return informationMap;
    }

    public static void removeInformation(Integer ID) {
        informationMap.get(ID).delete();
        informationMap.remove(ID);
        if(ID == counter) --counter;
    }

    public static Information createNewInformation() {
        int ID = ++counter;
        Information information = new Information(ID, basicPath);
        informationMap.put(ID, information);

        return information;
    }

    public static Information getInformation(Integer ID) {
        return informationMap.get(ID);
    }

    public static void loadInformation() {
        File file = new File(basicPath);
        for (File directory : Objects.requireNonNull(file.listFiles())) {
            if (directory.isDirectory()) {
                String detailFilePath = directory.getAbsolutePath() + "/" + Constants.STATE_FILE_NAME;
                try {
                    if (new File(detailFilePath).exists()) {
                        Information information = Information.fetchObject(detailFilePath);
                        if (information.verify()) {
                            addInformation(information.getSerialNumber(), information);
                        } else {
                            throw new Exception("Invalid information directory");
                        }
                    }else{
                        throw new Exception("No information file");
                    }
                } catch (Exception e) {
                    Utilities.deleteFile(directory.getAbsolutePath());
                }
            }
        }
    }

    public static void setBasicPath(String basicPath) {
        UserInformation.basicPath = basicPath;
    }

    public static void verify(int ID) {
        Information information = getInformation(ID);

        try {
            if (!information.verify()) {
                throw new Exception("Information is not valid");
            }
        } catch (Exception e) {
            removeInformation(information.getSerialNumber());
        }
    }
}