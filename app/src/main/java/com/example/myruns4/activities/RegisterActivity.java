package com.example.myruns4.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.example.myruns4.BuildConfig;
import com.example.myruns4.R;
import com.example.myruns4.models.ExerciseEntry;
import com.example.myruns4.models.Login;
import com.example.myruns4.utils.EntryTask;
import com.soundcloud.android.crop.Crop;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * Conduct profile registration and store corresponding information
 *
 * Handle differently based whether the profile is being edited or created
 */
public class RegisterActivity extends AppCompatActivity {
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final CharSequence REGISTRATION_TITLE = "Sign up";
    private static final int REQUEST_CAMERA = 0;
    private static final int REQUEST_WRITE_STORAGE = 1;
    private static final String URI_SAVE_KEY = "save_uri";
    private static final String FILE_SAVE_KEY = "save_file";
    private static final int DELETE_ALL_ENTRIES_COMMAND = 2;

    ImageView profilePic;
    private EditText name, email, password, phone, major, dartClass;
    private RadioButton isFemale, isMale;
    private Login currentLogin;
    private Uri photoURI;
    private File photoFile;
    private Class changeToActivity;
    private boolean storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        profilePic = findViewById(R.id.profile_pic);

        changeToActivity = LoginActivity.class;

        currentLogin = new Login(getApplicationContext());

        if(currentLogin.getNeedAutofill()) setTitle("Profile");

        if(savedInstanceState != null){
            try {
                if(!(savedInstanceState.getParcelable(URI_SAVE_KEY) == null || savedInstanceState.getString(FILE_SAVE_KEY) == null)) {
                    photoURI = savedInstanceState.getParcelable(URI_SAVE_KEY);
                    photoFile = new File(Objects.requireNonNull(savedInstanceState.getString(FILE_SAVE_KEY)));
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoURI);
                    if(!storage) bitmap = fixImageRotation(bitmap, savedInstanceState.getString(FILE_SAVE_KEY));
                    profilePic.setImageBitmap(bitmap);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        changeActionBar();
        changeButton();
        registerButton();
    }

    private void changeActionBar(){
        if(!currentLogin.getNeedAutofill()) setTitle(REGISTRATION_TITLE);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.registermenu, menu);
        if(currentLogin.getNeedAutofill()){
            menu.findItem(R.id.confirm_register).setTitle("Save");
            autofill();
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.confirm_register) {
            handleRegistration();
        }
        else {
            if(changeToActivity.equals(LoginActivity.class)) finishAffinity();      // Clear activity stack if going back to Login
            startActivity(new Intent(this, changeToActivity));
        }
        return super.onOptionsItemSelected(item);
    }

    private void setProfileVariables(){
        profilePic = findViewById(R.id.profile_pic);
        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        phone = findViewById(R.id.phone);
        major = findViewById(R.id.major);
        dartClass = findViewById(R.id.dartmouth_class);
        isFemale = findViewById(R.id.is_female);
        isMale  = findViewById(R.id.is_male);
    }

    /**
     * Check whether required profile variables are empty or improperly formatted
     * If incorrect formatting, produce an error using helper method
     *
     * @return whether the text box inputs were all formatted correctly
     */
    private boolean someProfileVarsEmpty(){
        boolean invalid = false;
        if(name.getText().toString().equals("")){
            requestError(name);
            invalid = true;
        }
        if(email.getText().toString().equals("")){
            requestError(email);
            invalid = true;
        }
        // Use default Android method to determine whether the email was formatted properly
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email.getText().toString()).matches()){
            email.setError("This email address is invalid");
            invalid = true;
        }
        if(password.getText().toString().equals("")){
            requestError(password);
            invalid = true;
        }
        if(!(isMale.isChecked() || isFemale.isChecked())){
            Toast.makeText(getApplicationContext(), "Gender is a required field", Toast.LENGTH_LONG).show();
            invalid = true;
        }
        return invalid;
    }

    /**
     * Helper method to provide an error for a required EditText component having an empty field
     */
    private void requestError(EditText errorProducer){
        errorProducer.setError("This field is required");
    }

    private void saveLogin(){
        String oldPass = currentLogin.getPassword();    // Store old password for later tracking
        String oldEmail = currentLogin.getEmail();      // Store old email to determine whether new account made (uneditable in edit profile)

        currentLogin.clearProfile();
        if(!(photoURI == null)) {
            currentLogin.setImagePath(photoFile.getAbsolutePath());
            currentLogin.setImageURI(photoURI.toString());
        }
        currentLogin.setName(name.getText().toString());
        currentLogin.setEmail(email.getText().toString());
        currentLogin.setPassword(password.getText().toString());
        currentLogin.setPhone(phone.getText().toString());
        currentLogin.setMajor(major.getText().toString());
        currentLogin.setDartClass(dartClass.getText().toString());
        if(isMale.isChecked()) currentLogin.setGender("male");
        else currentLogin.setGender("female");
        currentLogin.commit();
        Toast.makeText(getApplicationContext(), "Profile Saved", Toast.LENGTH_LONG).show();

        // Whether or not you're in edit profile mode, if the password has been changed user must login again
        if(!oldPass.equals(currentLogin.getPassword())) {
            changeToActivity = LoginActivity.class;

            // Clear the database if in the new registration page
            if(getTitle().equals(REGISTRATION_TITLE)){
                EntryTask taskToClearDb = new EntryTask(this, DELETE_ALL_ENTRIES_COMMAND, 0, new ExerciseEntry());
                taskToClearDb.execute();
            }
        }

        if(changeToActivity.equals(LoginActivity.class)) finishAffinity();      // Clear activity stack if going back to Login
        startActivity(new Intent(this, changeToActivity));
    }

    private void changeButton(){
        Button changeButton = findViewById(R.id.change_profile);
        changeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setProfileVariables();
                requestAppPermissions();
                openPictureDialog();
            }
        });
    }

    private void registerButton(){
        Button registerButton = findViewById(R.id.confirm_register);
        registerButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                handleRegistration();
            }
        });
    }

    private void handleRegistration(){
        setProfileVariables();
        if(!someProfileVarsEmpty()){
            if(password.getText().toString().length() >= MIN_PASSWORD_LENGTH) {
                saveLogin();
            }
            else{
                password.setError("Password must be at least six characters");
            }
        }
    }

    private void requestAppPermissions(){
        if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
        }
        else {
            if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) || shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                AlertDialog.Builder explainPermissionUse = new AlertDialog.Builder(this);
                explainPermissionUse.setTitle("Change Permissions in Device Settings:");
                explainPermissionUse.setMessage("Full app functionality is unavailable");
                explainPermissionUse.show();
            }
        }
    }

    private void openPictureDialog(){
        AlertDialog.Builder photoDialog = new AlertDialog.Builder(this);

        photoDialog.setTitle("Choose Profile Photo");
        photoDialog.setPositiveButton("Take from camera", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                takePhoto();
            }
        });

        photoDialog.setNegativeButton("Select from gallery", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                choosePhotoFromStorage();
            }
        });
        photoDialog.show();
    }

    private void takePhoto(){
        try{
            requestAppPermissions();
            Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            photoFile = File.createTempFile("taken_profile_photo", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            photoURI = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(intent, REQUEST_CAMERA);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private void choosePhotoFromStorage(){
        try{
            requestAppPermissions();
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_WRITE_STORAGE);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CAMERA){
            storage = false;
            try {
                assert data != null;
                cropPhoto();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        else if(requestCode == REQUEST_WRITE_STORAGE){
            try {
                assert data != null;
                storage = true;
                photoURI = data.getData();
                cropPhoto();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if(requestCode == Crop.REQUEST_CROP){
            if(data!=null) {
                photoURI = Crop.getOutput(data);
                Bitmap bitmap;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoURI);
                    bitmap = fixImageRotation(bitmap, photoFile.getAbsolutePath());
                    profilePic.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void cropPhoto() {
        assert photoURI != null;
        try {
            photoFile = File.createTempFile("taken_profile_photo", ".JPEG", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            Crop.of(photoURI, Uri.fromFile(photoFile)).asSquare().start(this);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private Bitmap fixImageRotation(Bitmap originalBitMap, String photoFilePath) {
        Bitmap rotatedBitmap = originalBitMap;
        try{
            ExifInterface imageFormat = new ExifInterface(photoFilePath);
            int rotationSet = imageFormat.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

            if(rotationSet == ExifInterface.ORIENTATION_ROTATE_90){
                rotatedBitmap = rotateBitmap(rotatedBitmap, 90);
            }
            else if(rotationSet == ExifInterface.ORIENTATION_ROTATE_180){
                rotatedBitmap = rotateBitmap(rotatedBitmap, 180);
            }
            else if(rotationSet == ExifInterface.ORIENTATION_ROTATE_270){
                rotatedBitmap = rotateBitmap(rotatedBitmap, 270);
            }
            return rotateBitmap(rotatedBitmap, 90);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return rotateBitmap(rotatedBitmap, 90);
    }

    private Bitmap rotateBitmap(Bitmap original, float angle) {
        Matrix orientationMatrix = new Matrix();
        orientationMatrix.postRotate(angle);
        return Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), orientationMatrix, true);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if(!(photoURI == null)) {
            outState.putParcelable(URI_SAVE_KEY, photoURI);
            outState.putString(FILE_SAVE_KEY, photoFile.getAbsolutePath());
        }
    }

    /**
     * If the profile needs to be autofilled with current information (occurs in edit profile mode
     * in MainActivity), fill in the stored values
     */
    private void autofill(){
        try{
            ((TextView)(findViewById(R.id.confirm_register))).setText(R.string.save);
            try {
                // Load the stored picture for the profile
                if(!(currentLogin.getImageURI() == null || currentLogin.getImagePath() == null)) {
                    photoURI = Uri.parse(currentLogin.getImageURI());
                    photoFile = new File(currentLogin.getImagePath());
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoURI);
                    if(!storage) bitmap = fixImageRotation(bitmap, photoURI.getPath());
                    profilePic.setImageBitmap(bitmap);
                }
//                currentLogin.getImagePath()
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Load all of the standard profile information values with whatever the user has saved in the past
            ((EditText)(findViewById(R.id.name))).setText(currentLogin.getName());
            if(currentLogin.getGender().equals("male")) ((RadioButton)(findViewById(R.id.is_male))).setChecked(true);
            else((RadioButton)(findViewById(R.id.is_female))).setChecked(true);
            ((EditText)(findViewById(R.id.email))).setText(currentLogin.getEmail());
            (findViewById(R.id.email)).setFocusable(false);     //Make the email uneditable
            ((EditText)(findViewById(R.id.password))).setText(currentLogin.getPassword());
            ((EditText)(findViewById(R.id.phone))).setText(currentLogin.getPhone());
            ((EditText)(findViewById(R.id.major))).setText(currentLogin.getMajor());
            ((EditText)(findViewById(R.id.dartmouth_class))).setText(currentLogin.getDartClass());
            if(currentLogin.getGender().equals("male")) ((RadioButton)(findViewById(R.id.is_male))).setChecked(true);
            else((RadioButton)(findViewById(R.id.is_female))).setChecked(true);

            // Once save is clicked, user should be brought to MainActivity (not LoginActivity)
            changeToActivity = MainActivity.class;
        }
        catch(Exception e){
            System.err.println("Profile not loaded correctly for Autofill");
            e.printStackTrace();
        }
    }
}
