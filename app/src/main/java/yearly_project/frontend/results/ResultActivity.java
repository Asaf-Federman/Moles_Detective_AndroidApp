package yearly_project.frontend.results;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import yearly_project.frontend.Constants;
import yearly_project.frontend.DB.Information;
import yearly_project.frontend.DB.UserInformation;
import yearly_project.frontend.R;
import yearly_project.frontend.waitScreen.CalculateResults;

public class ResultActivity extends AppCompatActivity {
    private SectionsPageAdapter mSectionsPageAdapter;
    private Information information;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        int ID = getIntent().getIntExtra("ID", -1);
        information = UserInformation.getInformation(ID);
        information.saveStateToFile();
        mSectionsPageAdapter = new SectionsPageAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        ViewPager mViewPager = findViewById(R.id.view_pager);
        setupViewPager(mViewPager);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
    }

    private void setupViewPager(ViewPager viewPager) {
        mSectionsPageAdapter.addFragment(new ResultFragment(), "TAB1");
        mSectionsPageAdapter.addFragment(new ResultFragment(), "TAB2");
        viewPager.setAdapter(mSectionsPageAdapter);
    }

    public void OnHomeClick(View view){
        activityResult();
        finish();
    }

    private void activityResult() {
        Intent data = new Intent(ResultActivity.this, CalculateResults.class);
        data.putExtra("ID", information.getSerialNumber());
        setResult(Constants.RESULT_SUCCESS,data);
    }

    @Override
    public void onBackPressed() {
        activityResult();
        super.onBackPressed();
    }
}
