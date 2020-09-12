package yearly_project.frontend.waitScreen;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;

import cz.msebera.android.httpclient.Header;
import yearly_project.frontend.Constant;
import yearly_project.frontend.DB.Image;
import yearly_project.frontend.DB.Information;
import yearly_project.frontend.DB.Mole;
import yearly_project.frontend.DB.Result;
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
            counter = information.getImages().getSize();
            client.setResponseTimeout(40000);
            for (Image image : information.getImages()) {
                asyncTask(image);
            }
        }).start();
    }

    private void asyncTask(Image image) {
        File photo = new File(image.getPath());
        Runnable myRunnable = () -> {
            RequestParams params = new RequestParams();
            try {
                params.put("mole_picture", photo, "image/png");
                params.setUseJsonStreamer(false);
            } catch (FileNotFoundException ignored) {
            }

            client.post(activity, "http://" + baseUrl + "/api/analyze?dpi=" + getResources().getDisplayMetrics().densityDpi, params, new JsonHttpResponseHandler() {
                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    Log.i("ERROR - " + image.getName(),statusCode + " " + throwable.getMessage());
//                    Log.i("ERROR - " + image.getName(), errorResponse.toString());
                }

                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject JsonObject) {
                    Gson gson = new Gson();
                    try {
                        for (int i = 0; i< JsonObject.length(); ++i ) {
                            String moleDescription = JsonObject.get(JsonObject.names().get(i).toString()).toString().replaceAll("\\s", "");
                            Log.i("INFO", moleDescription);
                            MoleResult moleResult = gson.fromJson(moleDescription,MoleResult.class);
                            Result result = new Result(moleResult.asymmetric_score,moleResult.border_score,
                                    moleResult.size_score, moleResult.classification_score, moleResult.color_score, moleResult.final_score);
                            Mole mole = new Mole(new Point(moleResult.mole_center.get(1),moleResult.mole_center.get(0)),moleResult.mole_radius, result);
                            image.addMole(mole);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
                if (information.verifyResults()) {
                    finishTask(Constant.RESULT_SUCCESS);
                }
                else{
                    runOnUiThread(() -> Utilities.createAlertDialog(activity, "ERROR", "Failed to get results", ((dialog, which) -> finishTask(Constant.RESULT_FAILURE))));
                }
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

