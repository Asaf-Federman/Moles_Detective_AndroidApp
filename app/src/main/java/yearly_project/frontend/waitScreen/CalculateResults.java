package yearly_project.frontend.waitScreen;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import java.io.File;
import java.io.FileNotFoundException;

import cz.msebera.android.httpclient.Header;
import timber.log.Timber;
import yearly_project.frontend.Constant;
import yearly_project.frontend.DB.Image;
import yearly_project.frontend.DB.Information;
import yearly_project.frontend.DB.UserInformation;
import yearly_project.frontend.R;
import yearly_project.frontend.utils.Utilities;

public class CalculateResults extends AppCompatActivity {
    private Information information;
    private Activity activity;
    private String baseUrl = "34.105.175.145";
    private volatile int counter;
    private Handler mainHandler;
    AsyncHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculate_results);
        int ID = getIntent().getIntExtra("ID", 0);
        information = UserInformation.getInformation(ID);
        activity = this;
        getResults();
    }

    private void getResults() {
        new Thread(() -> {
            mainHandler = new Handler(Looper.getMainLooper());
            client = new AsyncHttpClient();
            client.setMaxRetriesAndTimeout(10, 1000);
            counter = information.getImages().size();
            for (Image image : information.getImages()) {
                asyncTask(new File(image.getPath()));
            }
        }).start();
    }

    private void asyncTask(final File photo) {
        Runnable myRunnable = () -> {
            RequestParams params = new RequestParams();
            try {
                params.put("mask", photo, "image/png");  // croppedFile is a FIle
                params.setUseJsonStreamer(false);
            } catch (FileNotFoundException ignored) {
            }

            client.post(activity, "http://" + baseUrl + "/api/analyze?dpi=" + getResources().getDisplayMetrics().densityDpi, params, new TextHttpResponseHandler() {
                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    Log.i("INFO", throwable.getMessage());
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, String responseString) {
                    Timber.i(responseString);
                }

                @Override
                public void onFinish() {
                    super.onFinish();
                    onTaskCompleted();
                }
            });
        };

        mainHandler.post(myRunnable);
    }

    private synchronized void onTaskCompleted() {
        --counter;
        if (counter == 0) {
            try {
                boolean isVerified = information.verifyCalculateResultActivity();
                finishTask(Constant.RESULT_SUCCESS);
//                if (!isVerified)
//                    runOnUiThread(() -> Utilities.createAlertDialog(activity, "ERROR", "Failed to get results", ((dialog, which) -> finishTask(Constant.RESULT_FAILURE))));
//                else{
//                    finishTask(Constant.RESULT_SUCCESS);
//                }
            } catch (IllegalAccessException ignore) {
                runOnUiThread(() -> Utilities.createAlertDialog(activity, "ERROR", "Failed to verify the results", ((dialog, which) -> finishTask(Constant.RESULT_FAILURE))));
            }
        }
    }

    private void finishTask(int result) {
        runOnUiThread(() -> {
            activityResult(result);
            finish();
        });
    }

    private void activityResult(int result) {
        Intent data = new Intent(activity, CalculateResults.class);
        data.putExtra("ID", information.getSerialNumber());
        setResult(result, data);
    }

    @Override
    public void onBackPressed() {
        activityResult(Constant.RESULT_FAILURE);
        super.onBackPressed();
    }
}

