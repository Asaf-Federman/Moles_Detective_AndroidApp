package yearly_project.android.results_activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import yearly_project.android.Constant;
import yearly_project.android.database.Information;
import yearly_project.android.database.UserInformation;
import yearly_project.android.R;
import yearly_project.android.utilities.Utilities;

import static yearly_project.android.Constant.CREATE;
import static yearly_project.android.Constant.VIEW;

public class ResultActivity extends AppCompatActivity {
    private SectionsPageAdapter mSectionsPageAdapter;
    private Information information;
    private int status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        int ID = getIntent().getIntExtra("ID", -1);
        status = getIntent().getIntExtra("status", 0);
        information = UserInformation.getInformation(ID);
        Button submit = findViewById(R.id.submit);
        submit.setVisibility(status == CREATE ? View.VISIBLE : View.GONE);
        mSectionsPageAdapter = new SectionsPageAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        ViewPager mViewPager = findViewById(R.id.view_pager);
        setupViewPager(mViewPager, ID);
        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
    }

    /**
     * Setup fragments according to the amount of moles found.
     * @param viewPager - the view object
     * @param ID - the information database ID
     */
    private void setupViewPager(ViewPager viewPager, int ID) {
        for (int i = 0; i < information.getImages().getMaximumAmountOfMoles(); ++i) {
            Bundle bundle = new Bundle();
            bundle.putInt("ID", ID);
            bundle.putInt("mole_id", i);
            Fragment fragment = new ResultFragment();
            String title = "Mole " + (i +1);
            fragment.setArguments(bundle);
            mSectionsPageAdapter.addFragment(fragment,title);
        }

        viewPager.setAdapter(mSectionsPageAdapter);
    }

    public void OnHomeClick(View view) {
        if (status == CREATE) {
            Utilities.createQuestionDialog(this,
                    "Exit Activity",
                    "Are you sure you want to exit? none of the information will be saved",
                    ((dialog, which) -> {
                        Utilities.activityResult(Constant.RESULT_FAILURE, this, information.getSerialNumber());
                        finish();
                    }),
                    null);
        } else if (status == VIEW) {
            Utilities.activityResult(Constant.RESULT_FAILURE, this, information.getSerialNumber());
            super.onBackPressed();
        }
    }

    @Override
    public void onBackPressed() {
        if (status == CREATE) {
            Utilities.createQuestionDialog(this,
                    "Exit Activity",
                    "Are you sure you want to exit? none of the information will be saved",
                    ((dialog, which) -> {
                        Utilities.activityResult(Constant.RESULT_FAILURE, this, information.getSerialNumber());
                        super.onBackPressed();
                    }),
                    null);
        } else if (status == VIEW) {
            Utilities.activityResult(Constant.RESULT_FAILURE, this, information.getSerialNumber());
            super.onBackPressed();
        }
    }

    public void OnSubmit(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Description");
        builder.setMessage("Please describe the area of the mole");
        final EditText input = new EditText(this);
        input.setHint("On my left forearm");
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String text = input.getText().toString();
            information.setDescription(text);
            if (information.verifyResultActivity()) {
                Utilities.activityResult(Constant.RESULT_SUCCESS, this, information.getSerialNumber());
                information.saveStateToFile();
                finish();
            } else {
                Utilities.createAlertDialog(this, "ERROR", "Can not submit the result, description is invalid", null);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
