package com.example.photogallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SNIHostName;

public class PhotoGalleryFragment extends Fragment {
    private static final String TAG = "PhotoGalleryFragment";
    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mGalleryItemList = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchGalleryItemsTask().execute();

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler, getActivity());
        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap thumbnail) {
                Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                photoHolder.bindDrawable(drawable);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = (RecyclerView) view.findViewById(R.id.photo_recycler_view);
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 3);
        layoutManager.setItemPrefetchEnabled(true);
        layoutManager.setInitialPrefetchItemCount(10);
        mPhotoRecyclerView.setLayoutManager(layoutManager);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
        clearInternalStorageCache();
    }

    /**
     * 清除外存缓存
     */
    private void clearInternalStorageCache() {
        File localDir = getActivity().getFilesDir();
        File[] files = localDir.listFiles();
        for (File file : files) {
            String[] strings = file.getName().split("\\.");
            String fileExtension = strings[strings.length - 1];
            if (fileExtension.matches("jpg|JPG|png|PNG|gif|GIF")) {
                Log.d(TAG, "List File to be delete: " + file.getName());
                file.delete();
            }
        }
    }

    private void setupAdapter() {
        // fragment 被添加到 Activity 中，保证 getActivity() 不会返回 null
        if (isAdded()) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mGalleryItemList));
        }
    }

    /**
     * 在后台请求 API，解析获得数据后更新 UI
     * AsyncTask 泛型的 3 个参数：
     * 1. execute() 的参数
     * 2. 更新进度需要的类型
     * 3. doInBackground() 的返回值和 onPostExecute 的参数
     */
    private class FetchGalleryItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {
        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            return new NasaFetcher().fetchGalleryItems();
        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItemList) {
            mGalleryItemList = galleryItemList;
            // UI 操作
            setupAdapter();
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        private ImageView mImageView;

        public PhotoHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.item_image_view);
        }

        public void bindDrawable(Drawable drawable) {
            mImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> mGalleryItemList;

        public PhotoAdapter(List<GalleryItem> galleryItemList) {
            mGalleryItemList = galleryItemList;
        }

        @NonNull
        @NotNull
        @Override
        public PhotoHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.list_item_gallery, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull @NotNull PhotoGalleryFragment.PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItemList.get(position);
            Drawable drawable = getResources().getDrawable(R.drawable.bill_up_close);
            photoHolder.bindDrawable(drawable);
            mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getImageSource());
        }

        @Override
        public int getItemCount() {
            return mGalleryItemList.size();
        }
    }


}
