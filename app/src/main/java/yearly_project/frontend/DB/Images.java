package yearly_project.frontend.DB;

import androidx.annotation.NonNull;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import yearly_project.frontend.Constant;

public class Images implements Iterable<Image> {
    private List<Image> images;
    private String path;

    public Images(String path) {
        images = new ArrayList<>();
        this.path = path;
    }

    private List<Image> getImagesCollection() {
        return images;
    }

    public int getSize(){
        return getImagesCollection().size();
    }

    public Image getImage(int position) {
        return getImagesCollection().get(position);
    }

    public synchronized void addImage(Mat mat) {
        Image image = new Image(Integer.toString(images.size()), path, mat);
        images.add(image);
    }

    public boolean verifyImagesAmount() {
        return images.size() >= Constant.AMOUNT_OF_PICTURES_TO_TAKE;
    }

    public boolean areFieldsPopulated() throws IllegalAccessException {
        boolean AreFieldsPopulated = true;

        for (Image image : images) {
            AreFieldsPopulated = AreFieldsPopulated && image.verifyFields();
        }

        return AreFieldsPopulated;
    }

    @NonNull
    @Override
    public Iterator<Image> iterator() {
        return images.iterator();
    }

    public boolean verifyMoles() throws IllegalAccessException {
        for(Image image : getImagesCollection()){
             if(image.verifyMoles(getMaximumAmountOfMoles()))
                 return true;
        }

        return false;
    }

    public Image getVerifiedImage() throws IllegalAccessException {
        for(Image image : getImagesCollection()){
            if (image.verifyMoles(getMaximumAmountOfMoles())) {
                return image;
            }
        }

        return null;
    }

    public int getMaximumAmountOfMoles(){
        int amountOfMoles = 0;
        for(Image image : getImagesCollection()){
            amountOfMoles = Math.max(amountOfMoles,image.getMoles().getSize());
        }

        return amountOfMoles;
    }
}
