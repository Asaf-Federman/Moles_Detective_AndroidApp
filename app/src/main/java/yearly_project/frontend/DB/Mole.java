package yearly_project.frontend.DB;

import android.graphics.Point;

public class Mole {
    private Point center;
    private float radius;
    private Result result;

    public Mole(Point center, float radius, Result result) {
        setCenter(center);
        setRadius(radius);
        setResult(result);
    }

    boolean isValidMole() throws IllegalAccessException {
        return result.verify() && center != null && radius!= 0;
    }

    public Point getCenter() {
        return center;
    }

    public void setCenter(Point center) {
        this.center = center;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }
}
