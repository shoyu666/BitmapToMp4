package com.shoyu666.record;

import android.Manifest;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.shoyu666.imagerecord.gif.GifLoader;
import com.shoyu666.imagerecord.render.GifRender;
import com.shoyu666.record.view.MyRecordXView;
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

    MyStageSurfaceView glSurfaceView;

    private void init() {
        final MyMp4Record model = ViewModelProviders.of(this).get(MyMp4Record.class);
        MyRecordXView recordXView = findViewById(R.id.RecordXView);
        recordXView.setMyMp4RecordViewModel(model);
        glSurfaceView = findViewById(R.id.SurfaceView);
        model.addVideoFeeder(glSurfaceView.getVideoFeeder());
        GifLoader gifLoader = new GifLoader(this) {

            @Override
            public void onOutonComplete() {
            }

            @Override
            public void onOutNext(GifDecoder value) {
                if (value != null) {
                    glSurfaceView.addRenders(new GifRender(value));
                }
            }

            @Override
            public void onOutError(Throwable value) {
                if (value instanceof OutOfMemoryError) {
                }
            }
        };
        gifLoader.initResoure(getAssets(), "20181127123806.gif");
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }
}
