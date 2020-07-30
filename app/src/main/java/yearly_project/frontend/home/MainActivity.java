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

import java.util.ArrayList;
import java.util.List;

import yearly_project.frontend.Constants;
import yearly_project.frontend.DB.Information;
import yearly_project.frontend.DB.UserInformation;
import yearly_project.frontend.R;
import yearly_project.frontend.camera.CameraActivity;
import yearly_project.frontend.results.ResultActivity;
import yearly_project.frontend.utils.Utilities;
import yearly_project.frontend.waitScreen.CalculateResults;

public class MainActivity extends AppCompatActivity {
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
                Toast.makeText(MainActivity.this, "Item number " + String.valueOf(pos + 1) + " got deleted", Toast.LENGTH_SHORT).show();
            }
        }).attachToRecyclerView(recyclerView);
        adapter.setOnItemClickListener(information -> {
            Intent intent = new Intent(MainActivity.this, ResultActivity.class);
            intent.putExtra("ID", information.getSerialNumber());
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
                List<Information> informationList = new ArrayList<>(sender.values());
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
        if (resultCode == Constants.RESULT_SUCCESS) {
            switch (requestCode) {
                case Constants.ON_CAMERA:
                    intent = new Intent(MainActivity.this, CalculateResults.class);
                    intent.putExtra("ID", ID);
                    startActivityForResult(intent, Constants.ON_WAIT_SCREEN);
                    break;
                case Constants.ON_WAIT_SCREEN:
                    intent = new Intent(MainActivity.this, ResultActivity.class);
                    intent.putExtra("ID", ID);
                    startActivityForResult(intent, Constants.ON_RESULT);
                    break;
                case Constants.ON_RESULT:
                    UserInformation.verify(ID);
                    break;
            }
        } else {
            UserInformation.removeInformation(ID);
        }
    }

    private void askForPermission(Integer requestCode, String... permissions) throws Exception {
        PermissionHandler(requestCode, permissions);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        if (requestCode == 10) {
            for (int i = 0; i < grantResults.length; ++i) {
                switch (permissions[i]) {
                    case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                            Utilities.createAlertDialog(this, "No Permissions", "There are no write permissions, and therefore the activity can not write to storage");
                        break;
                    case Manifest.permission.CAMERA:
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            Utilities.createAlertDialog(this, "No Permissions", "There are no camera permissions, and therefore you're not eligible to use this activity");
                        }else{
                            Intent intent = new Intent(this, CameraActivity.class);
                            startActivityForResult(intent, Constants.ON_CAMERA);
                        }
                        break;
                    case Manifest.permission.READ_EXTERNAL_STORAGE:
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                            Utilities.createAlertDialog(this, "No Permissions", "There are no read permissions, and therefore the activity can not read from storage");
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
