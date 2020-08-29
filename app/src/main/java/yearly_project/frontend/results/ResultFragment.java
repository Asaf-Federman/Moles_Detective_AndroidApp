package yearly_project.frontend.results;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import it.sephiroth.android.library.xtooltip.Tooltip;
import yearly_project.frontend.R;
import yearly_project.frontend.utils.Utilities;

public class ResultFragment extends Fragment {
    enum eToolTip {
        ASYMMETRY("Aspiration to a perfect circle", Tooltip.Gravity.RIGHT),
        BLURRY("The blurriness of the mole's border", Tooltip.Gravity.TOP),
        CLASSIFICATION_NETWORK("The result from the\nclassifications's neural network", Tooltip.Gravity.RIGHT),
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
        VERY_GOOD(Color.GREEN),
        GOOD(Color.YELLOW),
        BAD(Color.rgb(255, 165, 0)),
        VERY_BAD(Color.RED);

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

        toolTips = new HashMap<>(eToolTip.values().length);
        progress = new HashMap<>();
        toolTips.put(view.findViewById(R.id.tooltip_asymmetry), eToolTip.ASYMMETRY);
        toolTips.put(view.findViewById(R.id.tooltip_blurry), eToolTip.BLURRY);
        toolTips.put(view.findViewById(R.id.tooltip_classification), eToolTip.CLASSIFICATION_NETWORK);
        toolTips.put(view.findViewById(R.id.tooltip_size), eToolTip.SIZE);
        toolTips.put(view.findViewById(R.id.tooltip_result), eToolTip.RESULT);
//        toolTips.put(view.findViewById(R.id.tooltip_smt), eToolTip.RESULT);
        setAction();
        setProgress();
        return view;
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

    private void setProgress() {
        for (ProgressBar progressBar : progress.keySet()) {
            progressBar.setProgress(progress.get(progressBar));
            progressBar.setIndeterminateTintList(ColorStateList.valueOf(getColor(progress.get(progressBar))));
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
