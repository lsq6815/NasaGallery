package com.example.photogallery;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;

public class PhotoGalleryActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        return PhotoGalleryFragment.newInstance();
    }
}