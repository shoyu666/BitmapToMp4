package com.shoyu666.record;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;
import com.shoyu666.imagerecord.render.GifRender;
import com.shoyu666.record.view.RecordXView;
import pub.devrel.easypermissions.EasyPermissions;

import java.util.List;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {


    public static String[] pers = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (EasyPermissions.hasPermissions(this, pers)) {
            init();
        } else {
            EasyPermissions.requestPermissions(
                    this, "请赋予权限",
                    0, pers
            );
        }
    }

    TestStageSurfaceView glSurfaceView;

    private void init() {
        final AudioRecordXViewModel model = ViewModelProviders.of(this).get(AudioRecordXViewModel.class);
        RecordXView recordXView = findViewById(R.id.RecordXView);
        recordXView.setRecordX(model.imagerecord);
        glSurfaceView = findViewById(R.id.SurfaceView);
        glSurfaceView.setRecordX(model.imagerecord);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }
}
