package yearly_project.frontend.camera;

import org.opencv.core.Point;
import org.opencv.core.Rect;

public class SquareWrapper {
    Rect square;
    private int length;

    public SquareWrapper(int imageX, int imageY, float length) {
        int xCoordinate = (int) (imageX / 2 - length / 2);
        int yCoordinate = (int) (imageY / 2 - length / 2);
        this.length = (int) length;

        square = new Rect(xCoordinate, yCoordinate, this.length, this.length);
    }

    public SquareWrapper(int imageX, int imageY, float length, int flag) {
        this.length = (int) length;
        square = new Rect(imageX, imageY, this.length, this.length);
    }

    public Point getTopLeft() {
        return square.tl();
    }

    public Point getBottomRight() {
        return square.br();
    }

    public Rect getSquare() {
        return square;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
