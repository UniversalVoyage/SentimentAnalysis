package com.intsig.yann.analysis;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final int LOADER_ID_DATA_LOADER = 101;

    private static final int REQUEST_PERMISSION_CAMERA = 102;
    private static final int REQUEST_CAMERA = 103;
    private static final int REQUEST_CROP = 104;

    private RelativeLayout emptyStatusRelativeLayout;
    private RecyclerView historyRecyclerView;
    private ImageView photoImageView;
    private AnalysisDataAdapter analysisDataAdapter;
    private AnalysisDataCallback analysisDataCallback = null;
    private File currentPhotoFile;
    public static String TempCropFile = Util.TEMP_IMG + "tmp.jpg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initFromXml();
        initView();
        initData();
    }



    private void initFromXml() {
        emptyStatusRelativeLayout = (RelativeLayout) findViewById(R.id.empty_status_RelativeLayout);
        historyRecyclerView = (RecyclerView) findViewById(R.id.history_RecyclerView);
        photoImageView = (ImageView) findViewById(R.id.photo_image);
    }

    private void initView() {
        analysisDataAdapter = new AnalysisDataAdapter(getLayoutInflater(), null);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyRecyclerView.setAdapter(analysisDataAdapter);
        photoImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        PermissionChecker.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_PERMISSION_CAMERA);
                } else {
                    takePhoto();
                }
            }
        });
    }

    private void initData() {
        analysisDataCallback = new AnalysisDataCallback();
        getSupportLoaderManager().restartLoader(LOADER_ID_DATA_LOADER, null, analysisDataCallback);
    }

    private class AnalysisDataCallback implements LoaderManager.LoaderCallbacks<Cursor> {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            final String[] PROJECTION =
                    new String[] {AnalysisData._ID, AnalysisData.SMALL_IMG, AnalysisData.CREATE_DATE, AnalysisData.FATIGUE};
            CursorLoader loader = new CursorLoader(MainActivity.this, AnalysisData.CONTENT_URI, PROJECTION, null,
                    null, AnalysisData.CREATE_DATE + " DESC") {
                @Override
                public Cursor loadInBackground() {
                    Cursor cursor = super.loadInBackground();
                    return cursor;
                }
            };
            loader.setUpdateThrottle(1500);
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data == null || data.getCount() <= 0) {
                emptyStatusRelativeLayout.setVisibility(View.VISIBLE);
                historyRecyclerView.setVisibility(View.GONE);
            } else {
                emptyStatusRelativeLayout.setVisibility(View.GONE);
                historyRecyclerView.setVisibility(View.VISIBLE);
                Cursor cursor = analysisDataAdapter.swapCursor(data);
                Util.safeCloseCursor(cursor);
            }

        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            analysisDataAdapter.swapCursor(null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                doCropPhoto(currentPhotoFile, REQUEST_CROP);
            } else if (requestCode == REQUEST_CROP) {
                String cropFilePath = null;
                if (new File(TempCropFile).exists()) {
                    cropFilePath = TempCropFile;
                } else {
                    if (data == null) {
                        return;
                    }
                    if (data.getData() != null) {
                        cropFilePath = Util.getPathFromUri(this, data.getData());
                    }
                }
                if (TextUtils.isEmpty(cropFilePath) || !new File(cropFilePath).exists()) {
                    return;
                }
                if (currentPhotoFile != null && currentPhotoFile.exists()) {
                    currentPhotoFile.delete();
                }
                String time = Util.getDateAsName();
                Util.copyFile(TempCropFile, Util.ORIGINAL_IMG + "/" + time + ".jpg");
                File PHOTO_DIR = new File(Util.THUMB_IMG);
                PHOTO_DIR.mkdirs();
                Util.copyFile(TempCropFile, Util.THUMB_IMG + "/" + time + ".jpg");
                new File(cropFilePath).delete();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_CAMERA:
                if (grantResults.length > 0) {
                    for(int i = 0; i < permissions.length ; i++){
                        if(TextUtils.equals(permissions[i], Manifest.permission.CAMERA) && PermissionChecker.checkSelfPermission(this, permissions[i]) == PackageManager.PERMISSION_GRANTED){
                            takePhoto();
                            return;
                        }
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        }
    }

    /**
     * Launches Camera to take a picture and store it in a file.
     */
    private void takePhoto() {
        try {
            File PHOTO_DIR = new File(Util.ORIGINAL_IMG);
            PHOTO_DIR.mkdirs();
            currentPhotoFile = new File(PHOTO_DIR, Util.getDateAsName() + ".jpg");
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION );
            }
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(currentPhotoFile));
            startActivityForResult(intent, REQUEST_CAMERA);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.photoPickerNotFoundText, Toast.LENGTH_LONG).show();
        }
    }

    private void doCropPhoto(File oriImg, int requestCode) {
        Uri photoURI = null;
        try {
            photoURI = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + Util.FILE_PROVIDER_AUTHORITIES, oriImg);
        }catch (Exception e){
            e.printStackTrace();
        }

        try {
            File cfile = new File(TempCropFile);
            if (cfile.exists()) {
                cfile.delete();
            }
            // Launch gallery to crop the photo
            Intent intent = new Intent("com.android.camera.action.CROP");
            if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.M) {//7.0以上
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(photoURI, "image/*");
            } else {
                intent.setDataAndType(Uri.fromFile(oriImg), "image/*");
            }
            intent.putExtra("crop", "true");
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
            intent.putExtra("scale", true);
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB
                    || android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(TempCropFile)));
            }
            int outputX = 800;
            int outputY = 800;
            intent.putExtra("outputX", outputX);
            intent.putExtra("outputY", outputY);
            intent.putExtra("return-data", false);// 某些图片剪切出来的bitmap 会很大，导致 intent transaction
            // faield。
            startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            Toast.makeText(this, R.string.photoPickerNotFoundText, Toast.LENGTH_LONG).show();
        }
    }
}
