package com.assignment.secquraise;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1111;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1112;


    private static final int BACK_CAMERA_ID = 0;

    private TextView CaptureResultTv,FrequencyResultTv,ConnectivityResult,ResultBatteryCharging,batteryChargeResultTv,locationResultTv,FrequencyTv;

    private LocationManager locationManager;

    private TextView dateTimeTv;
    private ImageView imageView;
    private FirebaseStorage firebaseStorage;
    private DatabaseReference databaseReference;
    private String deviceId;

    private LocationListener locationListener;

    private int captureCount = 0;

    private  ProgressBar progressBar;
    private float frequency = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView bannerTv = findViewById(R.id.bannerTv);
        dateTimeTv = findViewById(R.id.dateTimeTv);
        imageView = findViewById(R.id.imageView);
        Button refreshButton = findViewById(R.id.refreshButton);


        deviceId = getDeviceId(this);
         progressBar = findViewById(R.id.progressBar);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        CaptureResultTv = findViewById(R.id.CaptureResultTv);
        FrequencyResultTv = findViewById(R.id.FrequencyResultTv);
        FrequencyResultTv.setText("15.0");
        ConnectivityResult = findViewById(R.id.ConnectivityResult);
        ResultBatteryCharging = findViewById(R.id.ResultBatteryCharging);
        batteryChargeResultTv = findViewById(R.id.batteryChargeResultTv);
        locationResultTv = findViewById(R.id.locationResultTv);
        FrequencyTv = findViewById(R.id.FrequencyTv);



        firebaseStorage = FirebaseStorage.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("data");

        permissions();

        refreshButton.setOnClickListener(v -> refreshData());

        FrequencyTv.setOnClickListener(v -> setFrequency());
    }



    private void refreshData() {
        getCurrentDateTime();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            captureImage();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
    }


    private void setFrequency() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Frequency");
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf((int) frequency));
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String frequencyText = input.getText().toString();
                try {
                    int minutes = Integer.parseInt(frequencyText);
                    frequency = minutes;
                    saveFrequency(frequency); 
                } catch (NumberFormatException e) {
                    frequency = 0;
                }
                Toast.makeText(MainActivity.this, "Frequency set to: " + frequency + " minutes", Toast.LENGTH_SHORT).show();
                FrequencyResultTv.setText(String.valueOf(frequency));
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra("android.intent.extras.CAMERA_FACING", BACK_CAMERA_ID);
        startActivityForResult(intent, CAMERA_PERMISSION_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && resultCode == RESULT_OK) {
            Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(imageBitmap);
            uploadImageToFirebaseStorage(imageBitmap);
        }
    }

    private void uploadImageToFirebaseStorage(Bitmap imageBitmap) {
        StorageReference storageReference = firebaseStorage.getReference();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String dateTime = sdf.format(new Date());

        String imageName = "image_" + deviceId + "_" + dateTime + ".jpg";
        StorageReference imageRef = storageReference.child("images/" + imageName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageData = baos.toByteArray();

        UploadTask uploadTask = imageRef.putBytes(imageData);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            Toast.makeText(MainActivity.this, "Image uploaded to Firebase Storage", Toast.LENGTH_SHORT).show();

            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                updateDataInFirebase(imageUrl);
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(MainActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void updateDataInFirebase(String imageUrl) {
        progressBar.setVisibility(View.VISIBLE);
        captureCount++;
        saveCaptureCount(captureCount);
        frequency = getSavedFrequency();
        boolean isNetworkConnected = getConnectivity();
        boolean isCharging = getChargingStatus();
        String batteryPercentage = getBatteryChargePercentage();
        double latitude = getLocation();
        String dateTimeUpdate = dateTimeTv.getText().toString();
        String captureCount = String.valueOf(getCaptureCount());
        locationResultTv.setText(String.valueOf(latitude));
        Data data = new Data();
        data.setDeviceID(deviceId);
        data.setCaptureCount(Integer.parseInt(captureCount));
        data.setFrequency(frequency);
        data.setNetworkConnectivity(isNetworkConnected);
        data.setCharging(isCharging);
        data.setBatteryPercentage(Integer.parseInt(batteryPercentage));
        data.setLatitude(latitude);
        data.setDateTimeUpdate(dateTimeUpdate);
        data.setImageUrl(imageUrl);
        getLocation();

        databaseReference.child(deviceId).setValue(data)
                .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "Data updated in Firebase Database.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Failed to update data in Firebase Database: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        CaptureResultTv.setText(captureCount);
        progressBar.setVisibility(View.GONE);
    }





    private String getBatteryChargePercentage() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        String res = String.valueOf((int) ((level / (float) scale) * 100));
        batteryChargeResultTv.setText(res+"%");
        return res;
    }

    private boolean getChargingStatus() {
        boolean isCharging = isBatteryCharging();
        String chargingStatus = (isCharging ? "YES" : "NO");
        if(chargingStatus.equals("YES")){
            ResultBatteryCharging.setTextColor(getResources().getColor(R.color.Green));
            ResultBatteryCharging.setText(chargingStatus);
        }else{
            ResultBatteryCharging.setTextColor(getResources().getColor(R.color.Red));
            ResultBatteryCharging.setText(chargingStatus);
        }

        return isCharging;
    }


    private boolean isBatteryCharging() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
    }

    private boolean getConnectivity() {
        boolean isNetworkConnected = isNetworkConnected();
        String connectivityStatus = (isNetworkConnected ? "ON" : "OFF");
        if(connectivityStatus.equals("ON")){
            ConnectivityResult.setTextColor(getResources().getColor(R.color.Green));
            ConnectivityResult.setText(connectivityStatus);
        }else{
            ConnectivityResult.setTextColor(getResources().getColor(R.color.Red));
            ConnectivityResult.setText(connectivityStatus);
        }
        ConnectivityResult.setText(connectivityStatus);
        return isNetworkConnected;
    }

    private boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void saveCaptureCount(int count) {
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("captureCount", count);
        editor.apply();
    }

    private int getCaptureCount() {
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        return sharedPreferences.getInt("captureCount", 0);
    }

    public void getCurrentDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        Date currentDate = new Date();
        String formattedDateTime = dateFormat.format(currentDate);
        dateTimeTv.setText(formattedDateTime);
    }

    private void permissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            requestLocationPermission();
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private String getDeviceId(Context context) {
        String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (deviceId != null) {
            return deviceId;
        }
        return "unknown_device";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationPermission();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                getCurrentDateTime();
                captureImage();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }




    private double getLocation() {
        final double[] latitudeHolder = new double[1];

        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                double latitude = location.getLatitude();

               locationResultTv.setText(String.valueOf(latitude));
                locationManager.removeUpdates(this);

                latitudeHolder[0] = latitude;
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        } else {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
        }

        return latitudeHolder[0];
    }


    private void saveFrequency(float frequency) {
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat("frequency", frequency);
        editor.apply();
    }

    private float getSavedFrequency() {
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        return sharedPreferences.getFloat("frequency", 1.0f);

    }
}
