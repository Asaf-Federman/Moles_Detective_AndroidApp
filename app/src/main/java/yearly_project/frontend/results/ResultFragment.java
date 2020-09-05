package yearly_project.frontend.results;

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

import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import it.sephiroth.android.library.xtooltip.Tooltip;
import yearly_project.frontend.Constant;
import yearly_project.frontend.DB.Information;
import yearly_project.frontend.DB.UserInformation;
import yearly_project.frontend.R;
import yearly_project.frontend.utils.Utilities;

public class ResultFragment extends Fragment {

    private Information information;
    private ImageView mole;

    enum eToolTip {
        ASYMMETRY("Aspiration to a perfect circle", Tooltip.Gravity.RIGHT),
        BLURRY("The blurriness of the mole's border", Tooltip.Gravity.TOP),
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

    private TextView summary;
    private Map<View, eToolTip> toolTips;
    private Map<ProgressBar, Integer> progress;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.result_fragment, container, false);
        int ID = getArguments().getInt("ID");
        information = UserInformation.getInformation(ID);
        mole = view.findViewById(R.id.mole_image);
        drawCircleOnMole();
        toolTips = new HashMap<>(eToolTip.values().length);
        summary = view.findViewById(R.id.resultText);
        progress = new HashMap<>();

        toolTips.put(view.findViewById(R.id.tooltip_asymmetry), eToolTip.ASYMMETRY);
        toolTips.put(view.findViewById(R.id.tooltip_blurry), eToolTip.BLURRY);
        toolTips.put(view.findViewById(R.id.tooltip_classification), eToolTip.CLASSIFICATION_NETWORK);
        toolTips.put(view.findViewById(R.id.tooltip_color), eToolTip.COLOR);
        toolTips.put(view.findViewById(R.id.tooltip_size), eToolTip.SIZE);
        toolTips.put(view.findViewById(R.id.tooltip_result), eToolTip.RESULT);

        progress.put(view.findViewById(R.id.progress_asymmetry), 24);
        progress.put(view.findViewById(R.id.progress_blurry), 51);
        progress.put(view.findViewById(R.id.progress_classification), 80);
        progress.put(view.findViewById(R.id.progress_color), 75);
        progress.put(view.findViewById(R.id.progress_size), 20);
        progress.put(view.findViewById(R.id.progress_result), 25);

        setAction();
        setProgress();
        summary.setText(getResultSummary(progress.get(view.findViewById(R.id.progress_result))));
        return view;
    }

    private void drawCircleOnMole() {
        Bitmap bitmap = information.getImages().getImage(Constant.AMOUNT_OF_PICTURES_TO_TAKE / 2).getImageAsBitmap();
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.parseColor("#5BC0DE"));
        paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.STROKE);

        Bitmap workingBitmap = Bitmap.createBitmap(bitmap);
        Bitmap mutableBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);

        Canvas canvas = new Canvas(mutableBitmap);
        canvas.drawCircle(bitmap.getHeight()/2f, bitmap.getWidth()/2f + 35, 25, paint);

        mole.setAdjustViewBounds(true);
        mole.setImageBitmap(mutableBitmap);
    }

    private void setAction() {
        for (Map.Entry<View, eToolTip> entry : toolTips.entrySet()) {
            entry.getKey().setOnClickListener(this::onToolTipClick);
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
            summary = "Please see a doctor as fast as possible regarding those test results";
        }

        return summary;
    }

    private void setProgress() {
        for (ProgressBar progressBar : progress.keySet()) {
            progressBar.setProgress(progress.get(progressBar));
            DrawableCompat.setTint(progressBar.getProgressDrawable(),getColor(progress.get(progressBar)));
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
}
