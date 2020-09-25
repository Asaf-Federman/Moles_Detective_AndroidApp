package yearly_project.android.camera;

import org.opencv.core.Point;
import org.opencv.core.Rect;

public class SquareWrapper {
    private Rect square;

    public SquareWrapper(int imageX, int imageY, float length) {
        square = new Rect(imageX, imageY, (int) length, (int) length);
    }

    public Point getTopLeft() {
        return square.tl();
    }

    public Point getBottomRight() {
        return square.br();
    }

    public Rect getSquare(){
        return square;
    }
}
