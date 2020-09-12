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

    public synchronized void addImage(Mat mat, Mat segmentation) {
        Image image = new Image(Integer.toString(images.size()), path, mat);
        Image segmentationImage = new Image("mask" + Integer.toString(images.size()), path, segmentation);
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
             if(image.verifyMoles())
                 return true;
        }

        return false;
    }

    public Image getVerifiedImage() throws IllegalAccessException {
        for(Image image : getImagesCollection()){
            if (image.verifyMoles()) {
                return image;
            }
        }

        return null;
    }
}
