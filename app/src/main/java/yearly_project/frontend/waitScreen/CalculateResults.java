package yearly_project.frontend.waitScreen;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import java.io.File;
import java.io.FileNotFoundException;

import cz.msebera.android.httpclient.Header;
import timber.log.Timber;
import yearly_project.frontend.Constants;
import yearly_project.frontend.DB.Image;
import yearly_project.frontend.DB.Information;
import yearly_project.frontend.DB.UserInformation;
import yearly_project.frontend.R;

public class CalculateResults extends AppCompatActivity {
    private Information information;
    private Context activity;
    private String baseUrl = "34.105.175.145";
    private volatile int counter;
    private Handler mainHandler;
    AsyncHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculate_results);
        int ID = getIntent().getIntExtra("ID",0);
        information = UserInformation.getInformation(ID);
        activity = getBaseContext();
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
//                    Log.i("INFO", throwable.getMessage());
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

    private synchronized void onTaskCompleted(){
        --counter;
        if(counter == 0){
            finishTask();
        }
    }

    private void finishTask() {
        runOnUiThread(()->{
            activityResult(Constants.RESULT_SUCCESS);
            finish();
        });
    }

    private void activityResult(int result) {
        Intent data = new Intent(activity, CalculateResults.class);
        data.putExtra("ID", information.getSerialNumber());
        setResult(result,data);
    }

    @Override
    public void onBackPressed() {
        activityResult(Constants.RESULT_FAILURE);
        super.onBackPressed();
    }
}

