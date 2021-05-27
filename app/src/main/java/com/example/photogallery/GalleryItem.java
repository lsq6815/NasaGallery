package com.example.photogallery;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 保存图片的元数据的对象
 */
public class GalleryItem {
    private int mId;
    private int mSol;
    private Camera mCamera = new GalleryItem.Camera();
    private String mImageSource;
    private String mEarthDate;
    private Rover mRover = new GalleryItem.Rover();

    class Camera {
        private int mId;
        private String mName;
        private int mRoverId;
        private String mFullName;

        @Override
        public @NotNull String toString() {
            return "Camera{" +
                    "mId=" + mId +
                    ", mName='" + mName + '\'' +
                    ", mRoverId=" + mRoverId +
                    ", mFullName='" + mFullName + '\'' +
                    '}';
        }

        public int getId() {
            return mId;
        }

        public void setId(int id) {
            mId = id;
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            mName = name;
        }

        public int getRoverId() {
            return mRoverId;
        }

        public void setRoverId(int roverId) {
            mRoverId = roverId;
        }

        public String getFullName() {
            return mFullName;
        }

        public void setFullName(String fullName) {
            mFullName = fullName;
        }
    }

    class Rover {
        private int mId;
        private String mName;
        private String mLandingDate;
        private String mLaunchDate;
        private String mStatus;

        @Override
        public @NotNull String toString() {
            return "Rover{" +
                    "mId=" + mId +
                    ", mName='" + mName + '\'' +
                    ", mLandingDate='" + mLandingDate + '\'' +
                    ", mLaunchDate='" + mLaunchDate + '\'' +
                    ", mStatus='" + mStatus + '\'' +
                    '}';
        }

        public int getId() {
            return mId;
        }

        public void setId(int id) {
            mId = id;
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            mName = name;
        }

        public String getLandingDate() {
            return mLandingDate;
        }

        public void setLandingDate(String landingDate) {
            mLandingDate = landingDate;
        }

        public String getLaunchDate() {
            return mLaunchDate;
        }

        public void setLaunchDate(String launchDate) {
            mLaunchDate = launchDate;
        }

        public String getStatus() {
            return mStatus;
        }

        public void setStatus(String status) {
            mStatus = status;
        }
    }

    @Override
    public @NotNull String toString() {
        return "GalleryItem{" +
                "mId=" + mId +
                ", mSol=" + mSol +
                ", mCamera=" + mCamera +
                ", mImageSource='" + mImageSource + '\'' +
                ", mEarthDate='" + mEarthDate + '\'' +
                ", mRover=" + mRover +
                '}';
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    public int getSol() {
        return mSol;
    }

    public void setSol(int sol) {
        mSol = sol;
    }

    public Camera getCamera() {
        return mCamera;
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
    }

    public String getImageSource() {
        return mImageSource;
    }

    public void setImageSource(String imageSource) {
        mImageSource = imageSource;
    }

    public String getEarthDate() {
        return mEarthDate;
    }

    public void setEarthDate(String earthDate) {
        mEarthDate = earthDate;
    }

    public Rover getRover() {
        return mRover;
    }

    public void setRover(Rover rover) {
        mRover = rover;
    }
}

/**
 * 对使用 Gson 解析 GalleryItem 提供帮助的类
 */
class GalleryItemGsonHelper {
    int id;
    int sol;
    CameraGsonHelper camera = new GalleryItemGsonHelper.CameraGsonHelper();
    String img_src;
    String earth_date;
    RoverGsonHelper rover = new GalleryItemGsonHelper.RoverGsonHelper();

    class CameraGsonHelper {
        int id;
        String name;
        int rover_id;
        String full_name;
    }

    class RoverGsonHelper {
        int id;
        String name;
        String landing_date;
        String launch_date;
        String status;
    }

    /**
     * 将 GalleryItemGsonHelper 对象转换为 GalleryItem 对象
     *
     * @return GalleryItem 对象
     */
    private GalleryItem generateGalleryItem() {
        GalleryItem galleryItem = new GalleryItem();
        galleryItem.setId(id);
        galleryItem.setSol(sol);
        galleryItem.getCamera().setId(camera.id);
        galleryItem.getCamera().setName(camera.name);
        galleryItem.getCamera().setRoverId(camera.rover_id);
        galleryItem.getCamera().setFullName(camera.full_name);
        galleryItem.setImageSource(img_src);
        galleryItem.setEarthDate(earth_date);
        galleryItem.getRover().setId(rover.id);
        galleryItem.getRover().setName(rover.name);
        galleryItem.getRover().setLandingDate(rover.landing_date);
        galleryItem.getRover().setLaunchDate(rover.launch_date);
        galleryItem.getRover().setStatus(rover.status);
        return galleryItem;
    }

    /**
     * 从 jsonObject 中提取 GalleryItem 对象并返回
     * @param jsonObject GalleryItem 的 JSON 表示的 JSONObject 对象
     * @return GalleryItem 对象
     */
    public static GalleryItem toGalleryItem(JSONObject jsonObject) {
        Gson gson = new Gson();
        GalleryItemGsonHelper helper = gson.fromJson(String.valueOf(jsonObject), GalleryItemGsonHelper.class);
        return helper.generateGalleryItem();
    }

    /**
     * 从 jsonArray 中提取 GalleryItem 对象并存入 List<GalleryItem> 中，最后返回 List。
     * @param jsonArray 保存一系列 GalleryItem 的 JSON 表示的 JSONArray 对象
     * @return 保存一系列 GalleryItem 对象的 List
     */
    public static List<GalleryItem> toGalleryItemList(JSONArray jsonArray) {
        Gson gson = new Gson();
        GalleryItemGsonHelper[] helpers = gson.fromJson(String.valueOf(jsonArray), GalleryItemGsonHelper[].class);

        List<GalleryItem> galleryItemList = new ArrayList<>();
        for (GalleryItemGsonHelper helper : helpers) {
            galleryItemList.add(helper.generateGalleryItem());
        }

        return galleryItemList;
    }
}