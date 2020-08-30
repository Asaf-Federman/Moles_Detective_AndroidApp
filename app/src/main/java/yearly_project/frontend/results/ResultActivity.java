package yearly_project.frontend.results;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import yearly_project.frontend.Constant;
import yearly_project.frontend.DB.Information;
import yearly_project.frontend.DB.UserInformation;
import yearly_project.frontend.R;
import yearly_project.frontend.utils.Utilities;
import yearly_project.frontend.waitScreen.CalculateResults;

public class ResultActivity extends AppCompatActivity {
    private final int VIEW = 0;
    private final int CREATE = 1;
    private SectionsPageAdapter mSectionsPageAdapter;
    private Information information;
    private String m_Text = "";
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

    private void setupViewPager(ViewPager viewPager, int ID) {
        Bundle bundle = new Bundle();
        bundle.putInt("ID", ID);
        for (int i = 0; i < information.getResults().size() +1; ++i) {
            Fragment fragment = new ResultFragment();
            String title = "Mole " + (i + 1);
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
                        activityResult(Constant.RESULT_FAILURE);
                        finish();
                    }),
                    null);
        } else if (status == VIEW) {
            activityResult(Constant.RESULT_FAILURE);
            super.onBackPressed();
        }
    }

    private void activityResult(int result) {
        Intent data = new Intent(ResultActivity.this, CalculateResults.class);
        data.putExtra("ID", information.getSerialNumber());
        setResult(result, data);
    }

    @Override
    public void onBackPressed() {
        if (status == CREATE) {
            Utilities.createQuestionDialog(this,
                    "Exit Activity",
                    "Are you sure you want to exit? none of the information will be saved",
                    ((dialog, which) -> {
                        activityResult(Constant.RESULT_FAILURE);
                        super.onBackPressed();
                    }),
                    null);
        } else if (status == VIEW) {
            activityResult(Constant.RESULT_FAILURE);
            super.onBackPressed();
        }
    }

    public void OnSubmit(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Description");
        builder.setMessage("Please describe the area of the mole");
// Set up the input
        final EditText input = new EditText(this);
        input.setHint("On my left forearm");
        builder.setView(input);

// Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> {
            String text = input.getText().toString();
            information.setDescription(text);
            if (information.verifyResultActivity()) {
                activityResult(Constant.RESULT_SUCCESS);
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
