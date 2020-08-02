package yearly_project.frontend.results;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import it.sephiroth.android.library.xtooltip.Tooltip;
import yearly_project.frontend.R;

public class ResultFragment extends Fragment {
    enum eToolTip{
        ASYMMETRY("Aspiration to a perfect circle", Tooltip.Gravity.RIGHT),
        BLURRY("The blurriness of the mole's border", Tooltip.Gravity.TOP),
        CLASSIFICATION_NETWORK("The result from the\nclassifications's neural network", Tooltip.Gravity.RIGHT),
        SIZE("The size of the mole", Tooltip.Gravity.TOP),
        RESULT("The final verdict upon taking\ninto account all the parameters", Tooltip.Gravity.RIGHT);

        private String toolTip;
        private Tooltip.Gravity gravity;

        eToolTip(String toolTip, Tooltip.Gravity gravity){
            this.toolTip = toolTip;
            this.gravity = gravity;
        }

        public String getToolTip() {
            return toolTip;
        }
    }

    private Map<View,eToolTip> toolTips;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.result_fragment,container,false);

        toolTips = new HashMap<>(eToolTip.values().length);
        toolTips.put(view.findViewById(R.id.tooltip1),eToolTip.ASYMMETRY);
        toolTips.put(view.findViewById(R.id.tooltip2),eToolTip.BLURRY);
        toolTips.put(view.findViewById(R.id.tooltip3),eToolTip.CLASSIFICATION_NETWORK);
        toolTips.put(view.findViewById(R.id.tooltip4),eToolTip.SIZE);
        toolTips.put(view.findViewById(R.id.tooltip5),eToolTip.RESULT);
        setAction();
        return view;
    }

    private void setAction() {
        for(Map.Entry<View,eToolTip> entry : toolTips.entrySet()){
            entry.getKey().setOnClickListener(this::onToolTipClick);
        }
    }

    private void onToolTipClick(View view){
        showToolTip(view);
    }

    private void showToolTip(View view) {
        eToolTip toolTip= toolTips.get(view.findViewById(view.getId()));
        Tooltip builder = new Tooltip.Builder(view.getContext()).anchor(view, 0,0,false)
                .text(Objects.requireNonNull(toolTip).getToolTip()).maxWidth(600)
                .arrow(true)
                .showDuration(2000).styleId(R.style.ToolTipLayoutCustomStyle)
                .overlay(false).floatingAnimation(Tooltip.Animation.Companion.getDEFAULT())
                .create();
        builder.show(view, toolTip.gravity,false);
    }
}
