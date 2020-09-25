package yearly_project.android.camera;

import org.opencv.core.Point;

public class Circle {
    private Point center;
    private float radius;
    private float maximumRadius;
    private float minRadius;

    public Circle(float screenHeight, float screenWidth, float radius, float minRadius, float maximumRadius) {
        setCenter(screenHeight, screenWidth);
        setMinRadius(minRadius);
        setMaximumRadius(maximumRadius);
        setRadius(radius);
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

    private void setCenter(float screenHeight, float screenWidth){
        this.center = new Point(screenHeight / 2, screenWidth / 2);
    }

    private void setMinRadius(float minRadius){
        this.minRadius =minRadius;
    }

    public void setMaximumRadius(float maximumRadius) {
        this.maximumRadius = Math.max(maximumRadius, minRadius);
    }

    public void scale(float mScaleFactor) {
        float newRadius = (radius * mScaleFactor);
        setRadius(newRadius);
    }
}
