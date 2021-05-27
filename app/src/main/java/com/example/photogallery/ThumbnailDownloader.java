package com.example.photogallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 下载图片的工具类，处理在 UI 线程后后台线程间的交互
 *
 * @param <T>
 */
public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;
    private Boolean mHasQuit = false;
    private Handler mRequestHandler; // UI -> Background
    private Handler mResponseHandler; // Background -> UI
    private ConcurrentHashMap<T, String> mRequestMap = new ConcurrentHashMap<>(); // 线程安全 map
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    /// 内存 LRU 缓存
    private static final int mMemoryCacheSize = 512 * 1024 * 1024; // 缓存大小：512 MB
    /// LruCache 可以在 UI 线程中初始化
    private final LruCache<String, Bitmap> mBitmapLruCache = new LruCache<String, Bitmap>(mMemoryCacheSize) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }

        /**
         * 该函数在缓存不存在时被调用，创建需求的对象并缓存，最后返回给 get()
         *
         * @param url URL
         * @return Bitmap 对象
         */
        @Override
        protected Bitmap create(String url) {
            try {
                byte[] bitmapBytes = new NasaFetcher().getUrlBytes(url);
                final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes,
                        0,
                        bitmapBytes.length);
                Log.i(TAG, "Bitmap created");
                return bitmap;
            } catch (IOException ioException) {
                Log.e(TAG, "Error downloading image", ioException);
            }
            return null;
        }
    };

    private final BitmapDiskCache mBitmapDiskCache;

    class BitmapDiskCache {
        private final Context mContext;

        BitmapDiskCache(Context context) {
            mContext = context;
        }

        /**
         * 模仿 LruCache 尝试读取缓存，如果不存在就下载并缓存，再返回结果
         *
         * @param url 图片的 URL
         * @return Bitmap 对象
         */
        public Bitmap get(String url) {
            Bitmap bitmap;
            String filename = generateFilename(url);
            // 如果缓存不存在
            if ((bitmap = loadBitmap(filename)) == null) {
                // 下载图片
                try {
                    byte[] bitmapBytes = new NasaFetcher().getUrlBytes(url);
                    bitmap = BitmapFactory.decodeByteArray(bitmapBytes,
                            0,
                            bitmapBytes.length);
                    Log.i(TAG, "Bitmap created");

                    // 缓存图片
                    saveBitmap(bitmap, filename);
                    return bitmap;
                } catch (IOException ioException) {
                    Log.e(TAG, "Error downloading image", ioException);
                    return null;
                }
            }

            // 如果缓存存在，直接返回
            Log.i(TAG, "Use Disk Cache");
            return bitmap;
        }

        /**
         * 将 Bitmap 对象保存到本地
         *
         * @param bitmap   Bitmap 对象
         * @param filename 文件名
         */
        public void saveBitmap(Bitmap bitmap, String filename) {
            FileOutputStream out;
            try {
                out = mContext.openFileOutput(filename, Context.MODE_PRIVATE);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to save file to: " + filename);
            }
        }

        /**
         * 通过 filename 从本地载入 Bitmap
         *
         * @param filename 图片文件名
         * @return Bitmap 对象
         */
        public Bitmap loadBitmap(String filename) {
            FileInputStream in;
            try {
                in = mContext.openFileInput(filename);
                return BitmapFactory.decodeStream(in);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Failed to load file from: " + filename);
            }
            return null;
        }

        /**
         * 为 url 生成唯一的识别符
         *
         * @param url URL
         * @return 唯一的识别符
         */
        public String generateFilename(String url) {
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            String[] strings = url.split("/");
            return strings[strings.length - 1];
        }
    }


    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        mThumbnailDownloadListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler, Context context) {
        super(TAG);
        mResponseHandler = responseHandler;
        mBitmapDiskCache = new BitmapDiskCache(context);
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }

    public void queueThumbnail(T target, String url) {
        Log.i(TAG, "Got a URL: " + url);

        if (url == null) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget();
        }
    }

    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestMap.clear();
    }

    /**
     * 在后台进行请求处理
     *
     * @param target
     */
    private void handleRequest(final T target) {

        final String url = mRequestMap.get(target);
        if (url == null) {
            return;
        }

        // 使用内存 LRU 缓存
//        final Bitmap bitmap = mBitmapLruCache.get(url);

        // 使用外存缓存
        final Bitmap bitmap = mBitmapDiskCache.get(url);

        mResponseHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mRequestMap.get(target) != url || mHasQuit) {
                    return;
                }

                mRequestMap.remove(target);
                mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
            }
        });
    }
}
