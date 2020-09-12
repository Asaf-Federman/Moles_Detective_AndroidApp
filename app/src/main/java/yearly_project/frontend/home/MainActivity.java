package yearly_project.frontend.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.ObservableMap;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;
import yearly_project.frontend.Constant;
import yearly_project.frontend.DB.Information;
import yearly_project.frontend.DB.UserInformation;
import yearly_project.frontend.R;
import yearly_project.frontend.camera.CameraActivity;
import yearly_project.frontend.results.ResultActivity;
import yearly_project.frontend.utils.Utilities;
import yearly_project.frontend.waitScreen.CalculateResults;

public class MainActivity extends AppCompatActivity {

    public final static int ON_CAMERA = 0;
    public final static int ON_WAIT_SCREEN = 1;
    public final static int ON_RESULT = 2;
    RecyclerView recyclerView;
    Adapter adapter;
    ObservableMap.OnMapChangedCallback callback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        UserInformation.setBasicPath(this.getFilesDir().getPath());
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        adapter = new Adapter();
        ObservableMap<Integer, Information> informationMap = UserInformation.getInformationMap();
        createCallBack();
        informationMap.addOnMapChangedCallback(callback);
        recyclerView.setAdapter(adapter);
        UserInformation.loadInformation();
        askForPermissions();

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                Information information = adapter.getInformationAt(pos);
                UserInformation.removeInformation(information.getSerialNumber());
                Toast.makeText(MainActivity.this, "Item number " + (pos + 1) + " got deleted", Toast.LENGTH_SHORT).show();
            }
        }).attachToRecyclerView(recyclerView);
        adapter.setOnItemClickListener(information -> {
            Intent intent = new Intent(MainActivity.this, ResultActivity.class);
            intent.putExtra("ID", information.getSerialNumber());
            intent.putExtra("status", Constant.VIEW);
            startActivity(intent);
        });
    }

    public void OnCameraClick(View view) {
        try {
            askForPermission(10, Manifest.permission.CAMERA);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setInformationCollection(List<Information> informationList) {
        informationList.sort(Information::compareTo);
        adapter.submitList(informationList);
    }

    private void createCallBack() {
        callback = new ObservableMap.OnMapChangedCallback() {
            @Override
            public void onMapChanged(ObservableMap sender, Object key) {
                List<Information> informationList = new ArrayList<Information>(sender.values());
                setInformationCollection(informationList);
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Intent intent;
        assert data != null;
        int ID = data.getIntExtra("ID", -1);
        Information information = UserInformation.getInformation(ID);
        if (resultCode == Constant.RESULT_SUCCESS) {
            switch (requestCode) {
                case ON_CAMERA:
                    intent = new Intent(MainActivity.this, CalculateResults.class);
                    intent.putExtra("ID", ID);
                    startActivityForResult(intent, ON_WAIT_SCREEN);
                    break;
                case ON_WAIT_SCREEN:
                    intent = new Intent(MainActivity.this, ResultActivity.class);
                    intent.putExtra("ID", ID);
                    intent.putExtra("status", Constant.CREATE);
                    startActivityForResult(intent, ON_RESULT);
                    break;
                case ON_RESULT:
                    try {
                        if(information.isValid()){
                            adapter.updateInformationAt(0);
                        }else{
                            UserInformation.removeInformation(ID);
                        }
                    } catch (IllegalAccessException e) {
                        Timber.i("Couldn't validate the information - %s", e.getMessage());
                        UserInformation.removeInformation(ID);
                    }

                    break;
            }
        } else {
            UserInformation.removeInformation(ID);
        }
    }

    private void askForPermission(Integer requestCode, String... permissions) {
        PermissionHandler(requestCode, permissions);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NotNull String[] permissions, @NotNull int[] grantResults) {
        if (requestCode == 10) {
            for (int i = 0; i < grantResults.length; ++i) {
                switch (permissions[i]) {
                    case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                            Utilities.createAlertDialog(this, "No Permissions", "There are no write permissions, and therefore the activity can not write to storage", null);
                        break;
                    case Manifest.permission.CAMERA:
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            Utilities.createAlertDialog(this, "No Permissions", "There are no camera permissions, and therefore you're not eligible to use this activity", null);
                        }else{
                            Intent intent = new Intent(this, CameraActivity.class);
                            startActivityForResult(intent, ON_CAMERA);
                        }
                        break;
                    case Manifest.permission.READ_EXTERNAL_STORAGE:
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                            Utilities.createAlertDialog(this, "No Permissions", "There are no read permissions, and therefore the activity can not read from storage", null);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void PermissionHandler(Integer requestCode, String... permissionsToRequest) {
        ActivityCompat.requestPermissions(this, permissionsToRequest, requestCode);
    }

    private void askForPermissions() {
        try {
            askForPermission(10, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE);
        } catch (Exception e) {
            this.finish();
        }
    }
}
