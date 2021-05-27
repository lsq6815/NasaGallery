package com.example.photogallery;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 对 NASA API 进行请求和处理的对象
 */
public class NasaFetcher {
    private static final String TAG = "NasaFetcher";
    private static final String NASA_API_KEY = "NsxVGAKsVacseZxOhYwgpXgeR2pIqBFCs8OBfEsz";

    /**
     * 请求 urlSpec 指定的资源，并以字节串的形式返回
     *
     * @param urlSpec 资源的 URL
     * @return 资源的字节串表达
     * @throws IOException
     */
    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            int httpCode = connection.getResponseCode();
            // 如果请求文件（网页，图片……）失败
            if (httpCode != HttpURLConnection.HTTP_OK) {
                // 处理重定向问题
                if (httpCode > 300 && httpCode < 400) {
                    String redirectHeader = connection.getHeaderField("Location");

                    if (TextUtils.isEmpty(redirectHeader))
                        throw new IOException("Failed to redirect, there no useful redirect location");

                    // 危险的递归，但是很方便不是吗？
                    return getUrlBytes(redirectHeader);
                }
                else {
                    // 生成 IOException
                    Log.e(TAG, "Cannot handler HTTP Code: " + connection.getResponseCode());
                    throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
                }
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 请求 urlSpec 指定的资源，并以字符串的形式返回
     *
     * @param urlSpec 资源的 URL
     * @return 资源的字符串表达
     * @throws IOException 访问 API 异常
     */
    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    /**
     * 请求 API，并返回解析后的数据
     *
     * @return 包含 GalleryItem 的 List
     */
    public List<GalleryItem> fetchGalleryItems() {
        List<GalleryItem> galleryItemList = new ArrayList<>();

        try {
            String url = Uri.parse("https://api.nasa.gov/mars-photos/api/v1/rovers/curiosity/photos")
                    .buildUpon()
                    .appendQueryParameter("sol", "1000")
                    .appendQueryParameter("api_key", NASA_API_KEY)
                    .build().toString();

            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
//            使用 JSON 的手动解析
//            parseGalleryItems(galleryItemList, new JSONObject(jsonString));
//            使用 Gson 的自动解析
            galleryItemList = GalleryItemGsonHelper.toGalleryItemList(new JSONObject(jsonString).getJSONArray("photos"));
        } catch (IOException ioException) {
            Log.e(TAG, "Failed to fetch items", ioException);
        } catch (JSONException jsonException) {
            Log.e(TAG, "Failed to parse JSON", jsonException);
        }

        return galleryItemList;
    }

    /**
     * 从 jsonBody 中解析信息并以 GalleryItem 对象保存到 itemList 中。
     *
     * @param itemList 保存解析结果，保存的数据类型是 GalleryItem
     * @param jsonBody 保存原始数据的 JSON 对象
     * @throws IOException
     * @throws JSONException
     */
    private void parseGalleryItems(List<GalleryItem> itemList, JSONObject jsonBody) throws IOException, JSONException {
        JSONArray photosJsonArray = jsonBody.getJSONArray("photos");
        for (int i = 0; i < photosJsonArray.length(); i++) {
            JSONObject photoJsonObject = photosJsonArray.getJSONObject(i);

            GalleryItem galleryItem = new GalleryItem();
            galleryItem.setId(photoJsonObject.getInt("id"));
            galleryItem.setImageSource(photoJsonObject.getString("img_src"));
            galleryItem.setEarthDate(photoJsonObject.getString("earth_date"));

            Log.d(TAG, "Parsed photo: " + galleryItem);
            itemList.add(galleryItem);
        }
    }
}
