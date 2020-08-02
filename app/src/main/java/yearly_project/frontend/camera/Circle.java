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
        this.radius = radius>maximumRadius ? maximumRadius : Math.max(radius, minRadius);
    }

    public Point getCenter() {
        return center;
    }

    public void setMaximumRadius(float maximumRadius) {
        this.maximumRadius = Math.max(maximumRadius, minRadius);
    }

    public void scale(float mScaleFactor) {
        float newRadius = (radius * mScaleFactor);
        setRadius(newRadius);
    }
}
