package yearly_project.frontend.camera;

import org.opencv.core.Point;

public class Circle {
    private Point center;
    private int radius;
    private int maximumRadius;

    public Circle(Point center, int radius) {
        this.center = center;
        this.radius = radius/2;
        maximumRadius = radius;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public Point getCenter() {
        return center;
    }

    public void setCenter(Point center) {
        this.center = center;
    }

    public int getMaximumRadius() {
        return maximumRadius;
    }

    public void setMaximumRadius(int maximumRadius) {
        this.maximumRadius = maximumRadius;
    }

    public void scale(float mScaleFactor) {
        int newRadius = (int)(radius * mScaleFactor);

        if(newRadius > maximumRadius){
            radius=maximumRadius;
        }else if(newRadius < maximumRadius /5){
            radius = maximumRadius/5;
        }else{
            radius= newRadius;
        }
    }
}
