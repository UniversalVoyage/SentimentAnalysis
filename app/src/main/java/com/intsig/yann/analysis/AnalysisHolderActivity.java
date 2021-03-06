package com.intsig.yann.analysis;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.PermissionChecker;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;

public class AnalysisHolderActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String ACCOUNT_ID = "ACCOUNT_ID";
    private static final int LOADER_ID_DATA_LOADER = 101;
    private static final int REQUEST_PERMISSION = 102;
    private static final int REQUEST_CAMERA = 103;
    private static final int REQUEST_CROP = 104;
    private static final int REQUEST_ALBUM = 105;
    private static final int REQUEST_PERMISSION2 = 106;
    private static final int REQUEST_TAKE_PICTURE = 107;


    private RelativeLayout emptyStatusRelativeLayout;
    private RecyclerView historyRecyclerView;
    private ImageView photoImageView;
    private AnalysisDataAdapter analysisDataAdapter;
    private AnalysisDataCallback analysisDataCallback = null;
    private ImageView mySmallImagView;
    private TextView nameTextView;
    private TextView dateTextView;
    private TextView changeAccountTextView;
    private File currentPhotoFile;
    public static String TempCropFile = Util.TEMP_IMG + "tmp.jpg";
    private long accountId;
    private ImageView photoSelectView;
    private String myBigImage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        initFromXml();
        initView();
        initData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportLoaderManager().restartLoader(LOADER_ID_DATA_LOADER, null, analysisDataCallback);
    }

    private void initFromXml() {
        emptyStatusRelativeLayout = findViewById(R.id.empty_status_RelativeLayout);
        historyRecyclerView = findViewById(R.id.history_RecyclerView);
        photoImageView = findViewById(R.id.photo_image);
        mySmallImagView = findViewById(R.id.small_img_ImageView);
        nameTextView = findViewById(R.id.status_detail_TextView);
        dateTextView = findViewById(R.id.photo_date_TextView);
        changeAccountTextView = findViewById(R.id.change_account_TextView);
        photoSelectView = findViewById(R.id.photo_select);
    }

    private void initView() {
        analysisDataAdapter = new AnalysisDataAdapter(getLayoutInflater(), null);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyRecyclerView.setAdapter(analysisDataAdapter);

        photoImageView.setOnClickListener(this);
        photoSelectView.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        //DrawCircle mDraw = new DrawCircle(this, null);
        //Intent intent = new Intent(this, Camera_test.class);
        int id = v.getId();
        if (id == R.id.photo_image) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    PermissionChecker.checkSelfPermission(AnalysisHolderActivity.this, Manifest.permission.CAMERA) != PermissionChecker.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION);
            } else {
                takePicture();//takePhoto();
            }
        } else if (id == R.id.photo_select) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    ContextCompat.checkSelfPermission(AnalysisHolderActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(AnalysisHolderActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION2);
            } else {
                openAlbum();
            }
        }
        getSdcardPermission();
    }


    public long getAccountId() {
        return accountId;
    }

    private void getSdcardPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                PermissionChecker.checkSelfPermission(AnalysisHolderActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        }
    }


    private void initData() {
        if (getIntent() != null) {
            accountId = getIntent().getLongExtra(ACCOUNT_ID, 0L);
        }
        if (accountId < 1) {
            Toast.makeText(this, R.string.error_account, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        refreshMyInfo();
        analysisDataCallback = new AnalysisDataCallback();
        getSupportLoaderManager().restartLoader(LOADER_ID_DATA_LOADER, null, analysisDataCallback);
    }

    private void refreshMyInfo() {
        Cursor cursor = getContentResolver().query(AccountData.CONTENT_URI, null, AccountData._ID + "=?",
                new String[]{accountId + ""}, null);
        if (cursor == null || !cursor.moveToFirst()) {
            Toast.makeText(this, R.string.error_account, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mySmallImagView.setImageBitmap(Util.loadBitmap(cursor.getString(cursor.getColumnIndex(AccountData.SMALL_IMG))));
        nameTextView.setText(cursor.getString(cursor.getColumnIndex(AccountData.ACCOUNT_NAME)));
        dateTextView.setText(getString(R.string.create_data,
                Util.parseDateString(cursor.getLong(cursor.getColumnIndex(AccountData.CREATE_DATE)))));
        changeAccountTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //LoginOrRegisterActivity.startActivity(AnalysisHolderActivity.this, true);
                Intent intent1;
                intent1 = new Intent(AnalysisHolderActivity.this, LoginOrRegisterActivity.class);
                intent1.putExtra(LoginOrRegisterActivity.EXTRA_IS_LOGIN, true);
                startActivity(intent1);
                //finish();???????
            }
        });
    }

    private class AnalysisDataCallback implements LoaderManager.LoaderCallbacks<Cursor> {

        @NonNull
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            final String[] PROJECTION =
                    new String[] {AnalysisData._ID, AnalysisData.SMALL_IMG, AnalysisData.CREATE_DATE, AnalysisData.FATIGUE};
            CursorLoader loader = new CursorLoader(AnalysisHolderActivity.this, AnalysisData.CONTENT_URI, PROJECTION,
                    AnalysisData.ACCOUNT_ID + "=?", new String[] {accountId + ""}, AnalysisData.CREATE_DATE + " DESC") {
                @Override
                public Cursor loadInBackground() {
                    Cursor cursor = super.loadInBackground();
                    return cursor;
                }
            };
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
                if (android.os.Build.VERSION.SDK_INT < 14) {

                    Util.safeCloseCursor(cursor);
                }

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
            } else if (requestCode == REQUEST_TAKE_PICTURE) {
                doCropPicture();

            } else if (requestCode == REQUEST_ALBUM) {
                if (resultCode == Activity.RESULT_OK) {

                    if (Build.VERSION.SDK_INT >= 19) {
                        //4.4及以上系统使用这个方法处理图片

                        handleImageOnKitKat(data);
                    } else {
                        //4.4以下系统使用这个方法处理图片
                        handleImageBeforeKitKat(data);
                    }
                }
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
                String time = Util.getDateAsName();
                File PHOTO_DIR = new File(Util.THUMB_IMG);
                if (!PHOTO_DIR.exists()) {
                    PHOTO_DIR.mkdirs();
                }

                Util.copyFile(TempCropFile, Util.THUMB_IMG + "/" + time + ".jpg");

                Util.copyFile(currentPhotoFile.getAbsolutePath(), Util.ORIGINAL_IMG + "/" + time + ".jpg");
                new File(cropFilePath).delete();
                if (currentPhotoFile != null && currentPhotoFile.exists()) {
                    currentPhotoFile.delete();
                }
                AnalysisDetailActivity.startActivity(this, time + ".jpg", accountId);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION:
                if (grantResults.length > 0) {
                    for(int i = 0; i < permissions.length ; i++){
                        if(TextUtils.equals(permissions[i], Manifest.permission.CAMERA) &&
                                PermissionChecker.checkSelfPermission(this, permissions[i]) == PermissionChecker.PERMISSION_GRANTED) {
                            takePicture();//takePhoto();
                            return;
                        } else if (TextUtils.equals(permissions[i], Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                                PermissionChecker.checkSelfPermission(this, permissions[i]) != PermissionChecker.PERMISSION_GRANTED) {
                            Toast.makeText(this, R.string.need_sdcard_permission, Toast.LENGTH_LONG).show();
                            finish();
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
    /*private void takePhoto() {

        try {
            File PHOTO_DIR = new File(Util.ORIGINAL_IMG);
            PHOTO_DIR.mkdirs();
            currentPhotoFile = new File(PHOTO_DIR, Util.getDateAsName() + ".jpg");
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
            Uri uri = null;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + Util.FILE_PROVIDER_AUTHORITIES, currentPhotoFile);
            } else {
                uri = Uri.fromFile(currentPhotoFile);
            }
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            startActivityForResult(intent, REQUEST_CAMERA);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.photoPickerNotFoundText, Toast.LENGTH_LONG).show();
        }
    }*/
    private void takePicture() {
        Intent intent = new Intent(this, Camera_test.class);
        String time = Util.getDateAsName();
        myBigImage = Util.ORIGINAL_IMG + "/" + time + ".jpg";
        intent.putExtra("uri", myBigImage);
        File PHOTO_DIR = new File(Util.ORIGINAL_IMG);
        if (PHOTO_DIR.exists()) {
            currentPhotoFile = new File(PHOTO_DIR, time + ".jpg");
        }
        startActivityForResult(intent, REQUEST_TAKE_PICTURE);
    }

    private void openAlbum() {
        File PHOTO_DIR = new File(Util.ORIGINAL_IMG);
        PHOTO_DIR.mkdirs();
        String time = Util.getDateAsName();
        myBigImage = Util.ORIGINAL_IMG + "/" + time + ".jpg";
        currentPhotoFile = new File(PHOTO_DIR, time + ".jpg");


        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_ALBUM);
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
            intent.putExtra("aspectX", 1);//裁剪框比例
            intent.putExtra("aspectY", 1);
            intent.putExtra("scale", true);
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB
                    || android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(TempCropFile))); //uri 用来存放bitmap
            }
            int outputX = 1840;
            int outputY = 3264;
            intent.putExtra("outputX", outputX);
            intent.putExtra("outputY", outputY);
            intent.putExtra("return-data", false);// 某些图片剪切出来的bitmap 会很大，导致 intent transaction
            // faield。
            intent.putExtra("noFaceDetection", true);

            startActivityForResult(intent, requestCode);
            Log.d("hey!!!", "look at me!");
        } catch (Exception e) {
            Toast.makeText(this, R.string.photoPickerNotFoundText, Toast.LENGTH_LONG).show();
        }
    }

    private void doCropPicture() {
        Intent intent = new Intent(this, Crop.class);
        intent.putExtra("uri", currentPhotoFile.toString());

        startActivityForResult(intent, REQUEST_CROP);
    }

    @TargetApi(19)
    private void handleImageOnKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];//解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            //如果是content类型的Uri，则使用普通方式处理
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            //如果是file类型的Uri，直接获取图片路径即可
            imagePath = uri.getPath();
        }

        File currentPhotoFile = new File(imagePath);
        //Util.copyFile(imagePath, myBigImage);

        Util.copyFile(currentPhotoFile.getAbsolutePath(), myBigImage);

        doCropPhoto(currentPhotoFile, REQUEST_CROP);
    }

    private void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        File currentPhotoFile = new File(imagePath);
        Util.copyFile(currentPhotoFile.toString(), imagePath);
        doCropPhoto(currentPhotoFile, REQUEST_CROP);
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        //通过Uri和selection来获取真实的图片路径
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }


    public static void startActivity(Activity activity, long acountId) {
        Intent intent = new Intent(activity, AnalysisHolderActivity.class);
        intent.putExtra(ACCOUNT_ID, acountId);
        activity.startActivity(intent);
    }


}
