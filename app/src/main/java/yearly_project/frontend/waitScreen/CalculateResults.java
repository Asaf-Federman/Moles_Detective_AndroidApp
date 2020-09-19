package yearly_project.frontend.waitScreen;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

import cz.msebera.android.httpclient.Header;
import timber.log.Timber;
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
    private volatile int requestsInProcess;
    private Handler mainHandler;
    AsyncHttpClient client;
    private Collection<String> errors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        errors = new HashSet<>();
        Timber.plant(new Timber.DebugTree());
        setContentView(R.layout.activity_calculate_results);
        int ID = getIntent().getIntExtra("ID", 0);
        information = UserInformation.getInformation(ID);
        activity = this;
        analyzeSavedImages();
    }

    /**
     * Creates a numerous (the amount of the available images) async http requests to analyze the pictures.
     */
    private void analyzeSavedImages() {
        new Thread(() -> {
            mainHandler = new Handler(Looper.getMainLooper());
            client = new AsyncHttpClient();
            requestsInProcess = information.getImages().getSize();
            client.setResponseTimeout(60000);
            for (Image image : information.getImages()) {
                sendForAnalyze(image);
            }
        }).start();
    }

    /**
     * Sends an asynchronous http POST request to the server to analyze the image
     * @param image - the image that is being sent to the server
     */
    private void sendForAnalyze(Image image) {
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
                    addExceptionReason(errorResponse);
                    printTraceback(errorResponse);
                }

                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject jsonObject) {
                    super.onSuccess(statusCode, headers, jsonObject);
                    convertJsonToMoles(jsonObject, image);
                }

                @Override
                public void onFinish() {
                    super.onFinish();
                    onTaskCompleted();
                    Timber.i("%s image analyze has been completed", image.getName());
                }
            });
        };

        mainHandler.post(myRunnable);
    }

    /**
     * Gets the server's exception traceback
     * @param errorResponse - The json object that stores the traceback
     */
    private void printTraceback(JSONObject errorResponse) {
        try {
            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<String, String>>() {}.getType();
            String data = errorResponse.get("data").toString();
            LinkedTreeMap<String, String> errorMap = gson.fromJson(data, mapType);
            Timber.i(errorMap.get("traceback"));
        } catch (Exception e) {
            Timber.i("Couldn't get the Exception's traceback");
            e.printStackTrace();
        }
    }

    /**
     * Stores the HTTP failure messages inside an array
     * @param errorResponse - the json object to recover the failure messages from
     */
    private void addExceptionReason(JSONObject errorResponse) {
        try {
            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<String, String>>() {}.getType();
            String data = errorResponse.get("data").toString();
            LinkedTreeMap<String, String> errorMap = gson.fromJson(data, mapType);
            errors.add(errorMap.get("description"));
        } catch (Exception e) {
            e.printStackTrace();
            Timber.i(String.valueOf(errorResponse));
        }
    }

    /**
     * Converts the response json to a mole object
     * @param successResponse - the response json
     * @param image - the image database object
     */
    private void convertJsonToMoles(JSONObject successResponse, Image image) {
        Gson gson = new Gson();

        try {
            for (int i = 0; i < successResponse.length(); ++i) {
                String moleDescription = successResponse.get(Objects.requireNonNull(successResponse.names()).get(i).toString()).toString().replaceAll("\\s", "");
                Timber.i(moleDescription);
                MoleResult moleResult = gson.fromJson(moleDescription, MoleResult.class);
                Result result = new Result(moleResult.asymmetric_score, moleResult.border_score,
                        moleResult.size_score, moleResult.classification_score, moleResult.color_score, moleResult.final_score);
                Mole mole = new Mole(new Point(moleResult.mole_center.get(1), moleResult.mole_center.get(0)), moleResult.mole_radius, result);
                image.addMole(mole);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Timber.i(String.valueOf(successResponse));
        }
    }

    /**
     * Once all the HTTP request has ended, it verifies the results and end activity accordingly
     */
    private synchronized void onTaskCompleted() {
        --requestsInProcess;
        if (requestsInProcess == 0) {
            String errorString = String.join("\n", this.errors);
            if (!errorString.isEmpty())
                runOnUiThread(() -> Toast.makeText(activity, errorString, Toast.LENGTH_LONG).show());
            try {
                if (information.verifyResults()) {
                    finishTask(Constant.RESULT_SUCCESS);
                } else {
                    String errors = "";
                    if (!errorString.isEmpty())
                        errors = "\nReceived the following errors:\n\n" + errorString;
                    String finalErrors = errors;
                    runOnUiThread(() -> Utilities.createAlertDialog(activity, "ERROR", "Failed to get results" + finalErrors, ((dialog, which) -> finishTask(Constant.RESULT_FAILURE))));
                }
            } catch (IllegalAccessException ignore) {
                runOnUiThread(() -> Utilities.createAlertDialog(activity, "ERROR", "Failed to verify the results", ((dialog, which) -> finishTask(Constant.RESULT_FAILURE))));
            }
        }
    }

    private void finishTask(int result) {
        runOnUiThread(() -> {
            Utilities.activityResult(result, this, information.getSerialNumber());
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        Utilities.activityResult(Constant.RESULT_FAILURE, this, information.getSerialNumber());
        super.onBackPressed();
    }
}

