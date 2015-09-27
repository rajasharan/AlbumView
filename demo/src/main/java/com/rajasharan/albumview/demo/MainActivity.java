package com.rajasharan.albumview.demo;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;

import com.rajasharan.widget.AlbumView;

public class MainActivity extends AppCompatActivity {
    static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AlbumView albumView = (AlbumView) findViewById(R.id.album);
        String p1 = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/template/loc1/bg3.jpg";
        String p2 = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/Screenshots/test.png";
        String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/Camera";
        albumView.addImage(p1)
                .addImage(p2)
                .addImages(dir)
                .show();
    }
}
