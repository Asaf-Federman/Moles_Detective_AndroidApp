package yearly_project.frontend.DB;

import java.lang.reflect.Field;

public class Result {
    private float asymmetry;
    private float blurriness;
    private float size;
    private float classification;


    public Result(float asymmetry, float blurriness, float size, float classification) {
        this.asymmetry = asymmetry;
        this.blurriness = blurriness;
        this.size = size;
        this.classification = classification;
    }

    public float getBlurriness() {
        return blurriness;
    }

    public void setBlurriness(float blurriness) {
        this.blurriness = blurriness;
    }

    public float getAsymmetry() {
        return asymmetry;
    }

    public void setAsymmetry(float asymmetry) {
        this.asymmetry = asymmetry;
    }

    public float getClassification() {
        return classification;
    }

    public void setClassification(float classification) {
        this.classification = classification;
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

//    public float getResult(){
//        //calculate the result here
//        //return the result
//    }

    public boolean verify() throws IllegalAccessException {
        for (Field field : getClass().getDeclaredFields())
            if (field.get(this) == null)
                return false;
        return true;
    }
}
