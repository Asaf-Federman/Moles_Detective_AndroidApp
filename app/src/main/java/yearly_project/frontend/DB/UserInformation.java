package yearly_project.frontend.DB;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class UserInformation {
    private static int counter = 0;
    private static String basicPath;
    private static Map<Integer, Information> informationMap = new HashMap<>();
    private int serialNumber;
    private String path;

    public UserInformation() throws Exception {
        ++counter;
        serialNumber = counter;
        createPath();
        Information information = createNewInformation();
        addInformation(serialNumber, information);
    }

    private void createPath() throws Exception {
        setPath(basicPath + "/" + serialNumber);
        File file = new File(path);
        if (!file.mkdirs()) throw new Exception("Failed to create a directory in path: " + path);
    }

    private static void addInformation(int serialNumber, Information information) {
        ++counter;
        if(counter != serialNumber){

        }

        informationMap.put(serialNumber, information);
    }

    public static void removeInformationById(Integer ID) {
        informationMap.remove(ID);
    }

    public static Information getInformation(Integer ID) {
        return informationMap.get(ID);
    }

    public static void loadInformation() throws IOException, IllegalAccessException {
        File file = new File(basicPath);
        for(File directories : Objects.requireNonNull(file.listFiles())){
            Information information = Information.fetchObject(directories.getAbsolutePath());
            if(information.verify()){
                addInformation(information.getSerialNumber(), information);
            }
        }
    }

    public Information getInformation() {
        return informationMap.get(serialNumber);
    }

    private Information createNewInformation() {
        return new Information(serialNumber, path);
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(int serialNumber) {
        this.serialNumber = serialNumber;
    }

    public void setPath(String path) {
        this.path = path;
    }
}