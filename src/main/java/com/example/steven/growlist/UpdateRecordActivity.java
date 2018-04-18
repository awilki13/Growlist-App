package com.example.steven.growlist;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.steven.growlist.PlantDBHelper;
import com.example.steven.growlist.Plant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UpdateRecordActivity extends AppCompatActivity {

    private EditText mGenusEditText;
    private EditText mSpeciesEditText;
    private EditText mQuantityEditText;
    private ImageView mImageView;
    private Button mUpdateBtn;
    private Button mCameraBtn;
    private Button mGalleryBtn;

    private PlantDBHelper dbHelper;
    private long receivedPlantId;
    String mCurrentPhotoPath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_record);

        //init
        mGenusEditText = (EditText)findViewById(R.id.userGenusUpdate);
        mSpeciesEditText = (EditText)findViewById(R.id.userSpeciesUpdate);
        mQuantityEditText = (EditText)findViewById(R.id.userQuantityUpdate);
        mImageView = (ImageView) findViewById(R.id.imagePreview);
        mUpdateBtn = (Button)findViewById(R.id.updateUserButton);
        mCameraBtn = (Button)findViewById(R.id.UserCameraButton);
        mGalleryBtn = (Button)findViewById(R.id.UserOpenGallery);

        dbHelper = new PlantDBHelper(this);

        try {
            //get intent to get plant id
            receivedPlantId = getIntent().getLongExtra("USER_ID", 1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /***populate plant data before update***/
        Plant queriedPlant = dbHelper.getPlant(receivedPlantId);
        //set field to this user data
        mGenusEditText.setText(queriedPlant.getGenus());
        mSpeciesEditText.setText(queriedPlant.getSpecies());
        mQuantityEditText.setText(queriedPlant.getQuantity());
        //mImageEditText.setText(queriedPlant.getImage());
        int orientation = 0;
        mCurrentPhotoPath = queriedPlant.getImage();
        try {
            ExifInterface exif = new ExifInterface(mCurrentPhotoPath);
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,1);
            if (orientation == 1) orientation = 0;
            else if (orientation == 3) orientation = 180;
            else if (orientation == 6) orientation = 90;
            else if (orientation == 8) orientation = 270;
            Log.d("Orientation: ", Integer.toString(orientation));
        } catch (IOException e){
            e.printStackTrace();
        }
        //int targetW = mImageView.getWidth();
        //int targetH = mImageView.getHeight();
        //Log.d("Width:", Integer.toString(targetW));
        //Log.d("Height:",Integer.toString(targetH));
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        //int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        bmOptions.inJustDecodeBounds = false;
        //bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        Matrix matrix = new Matrix();
        matrix.postRotate(orientation);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
        mImageView.setImageBitmap(bitmap);


        mGalleryBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                dispatchOpenGalleryIntent();
            }
        });
        mCameraBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                dispatchTakePictureIntent();
            }
        });
        //listen to add button click to update
        mUpdateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //call the save plant method
                updatePlant();
            }
        });





    }
    static final int SELECT_PICTURE = 12;
    private void dispatchOpenGalleryIntent(){
        Intent OpenGallery = new Intent();
        OpenGallery.setType("image/*");
        OpenGallery.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(OpenGallery, "Select Picture"), SELECT_PICTURE);
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);

            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){

        Log.d("resultCode:", Integer.toString(resultCode));
        if (resultCode == RESULT_OK) {
            Log.d("requestCode:", Integer.toString(requestCode));
            Uri selectedImageUri = null;
            if (requestCode == SELECT_PICTURE) {
                selectedImageUri = data.getData();
                mCurrentPhotoPath = getPath(selectedImageUri);
                Log.d("mCurrentPhotoPath:", mCurrentPhotoPath);
            } else {
                selectedImageUri = galleryAddPic();
                Log.d("SelectedImageUri:", selectedImageUri.toString());
            }
            int orientation = 0;
            try {
                ExifInterface exif = new ExifInterface(mCurrentPhotoPath);
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,1);
                if (orientation == 1) orientation = 0;
                else if (orientation == 3) orientation = 180;
                else if (orientation == 6) orientation = 90;
                else if (orientation == 8) orientation = 270;
                Log.d("Orientation: ", Integer.toString(orientation));
            } catch (IOException e){
                e.printStackTrace();
            }
            int targetW = mImageView.getWidth();
            int targetH = mImageView.getHeight();

            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight(), matrix, true);
            mImageView.setImageBitmap(bitmap);

        }
    }

    public String getPath(Uri uri){
        if(uri == null) return null;
        InputStream inputStream = null;
        String filePath = null;
        if (uri.getAuthority()!= null){
            try{
                inputStream = getContentResolver().openInputStream(uri);
                File photoFile = createTemporalFileFrom(inputStream);

                filePath = photoFile.getPath();
            } catch (FileNotFoundException e){
                Log.e("file not found:", "Could not find file");
            } catch (IOException e){
                e.printStackTrace();
                Log.e("IO exception:", "an IO error occurred");
            } finally{
                try {
                    inputStream.close();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }

        }
        return  filePath;
    }

    private File createTemporalFileFrom(InputStream inputStream) throws IOException {
        File targetFile = null;
        if (inputStream != null){
            int read;
            byte[] buffer = new byte[8*1024];
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_" + ".jpg";
            targetFile = new File(getExternalCacheDir(), imageFileName);
            OutputStream outputStream = new FileOutputStream(targetFile);
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            try {
                outputStream.close();
            } catch (IOException e){
                e.printStackTrace();
            }

        }
        return targetFile;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private Uri galleryAddPic(){
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
        return contentUri;
    }

    static final int REQUEST_TAKE_PHOTO = 1;


    private void updatePlant(){
        String Genus = mGenusEditText.getText().toString().trim();
        String Species = mSpeciesEditText.getText().toString().trim();
        String Quantity = mQuantityEditText.getText().toString().trim();
        String Notes = "";
        String image = "";
        if (mCurrentPhotoPath != null) {
            image = mCurrentPhotoPath.trim();
        }


        if(Genus.isEmpty()){
            //error name is empty
            Toast.makeText(this, "You must enter a name", Toast.LENGTH_SHORT).show();
        }

        if(Species.isEmpty()){
            //error name is empty
            Toast.makeText(this, "You must enter an age", Toast.LENGTH_SHORT).show();
        }

        if(Quantity.isEmpty()){
            //error name is empty
            Toast.makeText(this, "You must enter an occupation", Toast.LENGTH_SHORT).show();
        }

        if(image.isEmpty()){
            //error name is empty
            Toast.makeText(this, "You must enter an image link", Toast.LENGTH_SHORT).show();
        }

        //create updated plant
        Plant updatedPlant = new Plant(Genus, Species, Quantity, image, Notes);

        //call dbhelper update
        dbHelper.updatePlantRecord(receivedPlantId, this, updatedPlant);

        //finally redirect back home
        // NOTE you can implement an sqlite callback then redirect on success delete
        goBackHome();

    }

    private void goBackHome(){
        finish();
    }
}