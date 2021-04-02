package yearly_project.android.calculate_results_activity;

import java.util.ArrayList;

public class MoleResult {
    public float asymmetric_score, size_score, border_score, color_score, final_score, classification_score, mole_radius;
    public ArrayList<Integer> mole_center;

    public MoleResult(float asymmetric_score, float size_score, float border_score, float color_score, float final_score, float classification_score, float mole_radius, ArrayList<Integer> mole_center) {
        this.asymmetric_score = asymmetric_score;
        this.border_score = border_score;
        this.classification_score = classification_score;
        this.color_score = color_score;
        this.final_score = final_score;
        this.mole_center = mole_center;
        this.mole_radius = mole_radius;
        this.size_score = size_score;
    }
}
