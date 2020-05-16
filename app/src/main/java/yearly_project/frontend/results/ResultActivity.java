package yearly_project.frontend.results;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import yearly_project.frontend.R;

public class ResultActivity extends AppCompatActivity {
    private SectionsPageAdapter mSectionsPageAdapter;
    private FloatingActionButton fab;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        mSectionsPageAdapter = new SectionsPageAdapter(getSupportFragmentManager());
        fab = findViewById(R.id.fab);
        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        setupViewPager(mViewPager);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
    }

    private void setupViewPager(ViewPager viewPager) {
        mSectionsPageAdapter.addFragment(new ResultFragment(), "TAB1");
        mSectionsPageAdapter.addFragment(new ResultFragment(), "TAB2");
        viewPager.setAdapter(mSectionsPageAdapter);
    }

    public void OnFabClick(View view){
        finish();
    }
}
