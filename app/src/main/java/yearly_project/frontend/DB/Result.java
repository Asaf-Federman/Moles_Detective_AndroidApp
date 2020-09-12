package yearly_project.frontend.DB;

import java.lang.reflect.Field;

public class Result {
    private float asymmetry;
    private float blurriness;
    private float size;
    private float classification;
    private float color;
    private float finalScore;

    public Result(float asymmetry, float blurriness, float size, float classification, float color, float finalScore) {
        this.asymmetry = asymmetry;
        this.blurriness = blurriness;
        this.size = size;
        this.classification = classification;
        this.color = color;
        this.finalScore = finalScore;
    }

    public Result(){
        setAsymmetry(0);
        setBlurriness(0);
        setSize(0);
        setClassification(0);
        setColor(0);
        setFinalScore(0);
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

    public boolean verify() throws IllegalAccessException {
        for (Field field : getClass().getDeclaredFields())
            if (field.get(this) == null && (float)field.get(this) != 0f)
                return false;
        return true;
    }

    public float getFinalScore() {
        return this.finalScore;
    }

    public void setFinalScore(float finalScore) {
        this.finalScore = finalScore;
    }

    public float getColor() {
        return color;
    }

    public void setColor(float color) {
        this.color = color;
    }

    public void add(Result source) {
        setAsymmetry(getAsymmetry() + source.getAsymmetry());
        setBlurriness(getBlurriness() + source.getBlurriness());
        setSize(getSize() + source.getSize());
        setClassification(getClassification() + source.getClassification());
        setColor(getColor() + source.getColor());
        setFinalScore(getFinalScore() + source.getFinalScore());
    }

    public void division(int divisionAmount) {
        setAsymmetry(getAsymmetry() / divisionAmount);
        setBlurriness(getBlurriness() / divisionAmount);
        setSize(getSize() / divisionAmount);
        setClassification(getClassification() / divisionAmount);
        setColor(getColor() / divisionAmount);
        setFinalScore(getFinalScore() / divisionAmount);
    }
}
