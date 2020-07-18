package yearly_project.frontend.waitScreen;

import android.content.Context;
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
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.Header;
import yearly_project.frontend.R;

public class CalculateResults extends AppCompatActivity {
    private String path;
    private Context activity;
    private String baseUrl = "34.105.175.145";
    private volatile int semaphore;
    private Handler mainHandler;
    AsyncHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculate_results);
        path = getIntent().getStringExtra("folder_path");
        activity = getBaseContext();
        getResults();
    }

    private void getResults() {
        new Thread(() -> {
            final File folder = new File(path);
            mainHandler = new Handler(Looper.getMainLooper());
            client = new AsyncHttpClient();
            client.setMaxRetriesAndTimeout(10, 1000);
            semaphore = Objects.requireNonNull(folder.listFiles()).length;
            for (File photo : Objects.requireNonNull(folder.listFiles())) {
                asyncTask(photo);
            }

            while (semaphore>0){
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

//                populateSmt()
            runOnUiThread(this::finish);
        }).start();
    }

    private void asyncTask(final File photo) {
        Runnable myRunnable = () -> {
            RequestParams params = new RequestParams();
            try {
                params.put("mask", photo, "image/png");  // croppedFile is a FIle
                params.setUseJsonStreamer(false);
            } catch (FileNotFoundException e) {
            }

            client.post(activity, "http://" + baseUrl + "/api/analyze?dpi=" + getResources().getDisplayMetrics().densityDpi, params, new TextHttpResponseHandler() {
                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    Log.i("INFO", throwable.getMessage());
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, String responseString) {
                    Log.i("INFO", responseString);
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
        --semaphore;
    }
}

