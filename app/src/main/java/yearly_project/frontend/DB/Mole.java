package yearly_project.frontend.DB;

import android.graphics.Point;

public class Mole {
    private Point center;
    private int radius;
    private Result result;

    public Mole(Point center, int radius, Result result) {
        this.center = center;
        this.radius = radius;
        this.result = result;
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

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }
}
