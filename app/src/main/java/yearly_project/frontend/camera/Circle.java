package yearly_project.frontend.camera;

import org.opencv.core.Point;

public class Circle {
    private Point center;
    private float radius;
    private float maximumRadius;
    private float minRadius;

    public Circle(float posHeight, float posWidth, float radius) {
        center = new Point(posHeight / 2, posWidth / 2);
        minRadius=75;
        setMaximumRadius(radius);
        setRadius(radius/2);
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        if(radius > maximumRadius){
            this.radius=maximumRadius;
        }else if(radius < minRadius){
            this.radius = minRadius;
        }else{
            this.radius = radius;
        }
    }

    public Point getCenter() {
        return center;
    }

    public void setCenter(Point center) {
        this.center = center;
    }

    public float getMaximumRadius() {
        return maximumRadius;
    }

    public void setMaximumRadius(float maximumRadius) {
        if(maximumRadius < minRadius){
            this.maximumRadius = minRadius;
        }else{
            this.maximumRadius = maximumRadius;
        }
    }

    public void scale(float mScaleFactor) {
        float newRadius = (radius * mScaleFactor);
        setRadius(newRadius);
    }
}
