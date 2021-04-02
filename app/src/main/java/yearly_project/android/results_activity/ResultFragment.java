package yearly_project.android.results_activity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import it.sephiroth.android.library.xtooltip.Tooltip;
import timber.log.Timber;
import yearly_project.android.Constant;
import yearly_project.android.database.Image;
import yearly_project.android.database.Information;
import yearly_project.android.database.Mole;
import yearly_project.android.database.Result;
import yearly_project.android.database.UserInformation;
import yearly_project.android.R;
import yearly_project.android.utilities.Utilities;

public class ResultFragment extends Fragment {

    private Information information;
    private ImageView mole_image;
    private int ID;

    enum eToolTip {
        ASYMMETRY("Aspiration to a perfect circle", Tooltip.Gravity.RIGHT),
        BORDER_IRREGULARITY("The irregularity of the mole's border", Tooltip.Gravity.TOP),
        CLASSIFICATION_NETWORK("The result from the\nclassifications's neural network", Tooltip.Gravity.RIGHT),
        COLOR("The color of the mole", Tooltip.Gravity.RIGHT),
        SIZE("The size of the mole", Tooltip.Gravity.TOP),
        RESULT("The final verdict upon taking\ninto account all the parameters", Tooltip.Gravity.RIGHT);

        private String toolTip;
        private Tooltip.Gravity gravity;

        eToolTip(String toolTip, Tooltip.Gravity gravity) {
            this.toolTip = toolTip;
            this.gravity = gravity;
        }

        public String getToolTip() {
            return toolTip;
        }
    }

    enum eColor {
        VERY_GOOD(Color.parseColor("#5CB85C")),
        GOOD(Color.parseColor("#5BC0DE")),
        BAD(Color.rgb(255, 165, 0)),
        VERY_BAD(Color.parseColor("#D9534F"));

        int color;

        eColor(int color) {
            this.color = color;
        }

        public int getColor() {
            return color;
        }
    }

    private Map<View, eToolTip> toolTips;
    private Map<ProgressBar, Integer> progress;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.result_fragment, container, false);
        assert getArguments() != null;
        ID = getArguments().getInt("ID");
        int mole_id = getArguments().getInt("mole_id");
        information = UserInformation.getInformation(ID);
        mole_image = view.findViewById(R.id.mole_image);
        drawCircleOnMole(mole_id);
        toolTips = new HashMap<>(eToolTip.values().length);
        TextView summary = view.findViewById(R.id.resultText);
        progress = new HashMap<>();

        toolTips.put(view.findViewById(R.id.tooltip_asymmetry), eToolTip.ASYMMETRY);
        toolTips.put(view.findViewById(R.id.tooltip_border_irregularity), eToolTip.BORDER_IRREGULARITY);
        toolTips.put(view.findViewById(R.id.tooltip_classification), eToolTip.CLASSIFICATION_NETWORK);
        toolTips.put(view.findViewById(R.id.tooltip_color), eToolTip.COLOR);
        toolTips.put(view.findViewById(R.id.tooltip_size), eToolTip.SIZE);
        toolTips.put(view.findViewById(R.id.tooltip_result), eToolTip.RESULT);

        Result result = information.getAverageResultOfMole(mole_id);
        progress.put(view.findViewById(R.id.progress_asymmetry), (int) (result.getAsymmetry() * 100));
        progress.put(view.findViewById(R.id.progress_border_irregularity), (int) (result.getBorderIrregularity() * 100));
        progress.put(view.findViewById(R.id.progress_classification), (int) (result.getClassification() * 100));
        progress.put(view.findViewById(R.id.progress_color), (int) (result.getColor() * 100));
        progress.put(view.findViewById(R.id.progress_size), (int) (result.getSize() * 100));
        progress.put(view.findViewById(R.id.progress_result), (int) (result.getFinalScore() * 100));

        setAction();
        setProgress();
        summary.setText(getResultSummary(Objects.requireNonNull(progress.get(view.findViewById(R.id.progress_result)))));
        return view;
    }

    /**
     * Draws a circle around the mole in the imageView
     * @param mole_id - the id of the mole we're currently working on
     */
    private void drawCircleOnMole(int mole_id) {
        Image image = null;
        try {
            image = information.getImages().getVerifiedImage();
        } catch (Exception e) {
            Timber.i("Couldn't get a valid image");
            Utilities.createAlertDialog(getActivity(), "ERROR", e.getMessage(), (dialog, which) -> {
                Utilities.activityResult(Constant.RESULT_FAILURE, Objects.requireNonNull(getActivity()), ID);
                getActivity().finish();
            });
        }

        assert image != null;
        Bitmap bitmap = image.getImageAsBitmap();
        Mole mole = image.getMoles().getMole(mole_id);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.parseColor("#5BC0DE"));
        paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.STROKE);
        Bitmap workingBitmap = Bitmap.createBitmap(bitmap);
        Bitmap mutableBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        canvas.drawCircle(mole.getCenter().x, mole.getCenter().y, mole.getRadius(), paint);
        mole_image.setAdjustViewBounds(true);
        mole_image.setImageBitmap(mutableBitmap);
    }

    private void setAction() {
        for (Map.Entry<View, eToolTip> entry : toolTips.entrySet()) {
            entry.getKey().setOnClickListener(this::onToolTipClick);
        }

        for(Map.Entry<ProgressBar,Integer> entry : progress.entrySet()){
            entry.getKey().setOnClickListener(this::showPercentage);
        }
    }

    private int getColor(int progress) {
        int color;

        if (Utilities.isBetween(progress, 0, 24)) {
            color = eColor.VERY_GOOD.getColor();
        } else if (Utilities.isBetween(progress, 25, 50)) {
            color = eColor.GOOD.getColor();
        } else if (Utilities.isBetween(progress, 51, 75)) {
            color = eColor.BAD.getColor();
        } else {
            color = eColor.VERY_BAD.getColor();
        }

        return color;
    }

    private String getResultSummary(int progress) {
        String summary;

        if (Utilities.isBetween(progress, 0, 24)) {
            summary = "You passed with flying colors";
        } else if (Utilities.isBetween(progress, 25, 50)) {
            summary = "Results are OKAY, no need for concern";
        } else if (Utilities.isBetween(progress, 51, 75)) {
            summary = "Please visit a doctor regarding the test results";
        } else {
            summary = "Please see a doctor as fast as possible";
        }

        return summary;
    }

    private void setProgress() {
        for (ProgressBar progressBar : progress.keySet()) {
            progressBar.setProgress(Objects.requireNonNull(progress.get(progressBar)));
            DrawableCompat.setTint(progressBar.getProgressDrawable(), getColor(Objects.requireNonNull(progress.get(progressBar))));
        }
    }

    private void onToolTipClick(View view) {
        showToolTip(view);
    }

    private void showToolTip(View view) {
        eToolTip toolTip = toolTips.get(view.findViewById(view.getId()));
        Tooltip builder = new Tooltip.Builder(view.getContext()).anchor(view, 0, 0, false)
                .text(Objects.requireNonNull(toolTip).getToolTip()).maxWidth(600)
                .arrow(true)
                .showDuration(2000).styleId(R.style.ToolTipLayoutCustomStyle)
                .overlay(false).floatingAnimation(Tooltip.Animation.Companion.getDEFAULT())
                .create();
        builder.show(view, toolTip.gravity, false);
    }

    private void showPercentage(View view){
        ProgressBar progressBar = (ProgressBar)view;
        Toast.makeText(getActivity(),String.format("Received %d/100 points in this field", progressBar.getProgress()),Toast.LENGTH_SHORT).show();
    }
}
