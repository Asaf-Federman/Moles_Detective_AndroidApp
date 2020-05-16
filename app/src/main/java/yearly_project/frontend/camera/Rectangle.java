package yearly_project.frontend.camera;

import org.opencv.core.Point;
import org.opencv.core.Rect;

public class Rectangle {
    Rect rect;
    private int length;

    public Rectangle(int imageX, int imageY, int width, int height) {
        rect = new Rect(imageX/2 -height/2 ,imageY/2 -width/2 ,width,height);
        length = width;
    }

    public Rectangle(int imageX, int imageY, int width, int height, int flag) {
        rect = new Rect(imageX ,imageY ,width,height);
    }
    public Point getTopLeft() {
        return rect.tl();
    }

    public Point getBottomRight() {
        return rect.br();
    }

    public Rect getRect(){
        return rect;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
