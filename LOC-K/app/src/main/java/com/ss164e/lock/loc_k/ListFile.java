package com.ss164e.lock.loc_k;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListFile extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private String path;
    private Button button, newButton, existingButton;
    String internalKey;
    static final int READ_BLOCK_SIZE = 100;
    final Context context = this;
    static int count = 0;
    double latitude;
    double longitude;
    double radius;
    GoogleApiClient mGoogleApiClient;
    double currentLatitude;
    double currentLongitude;
    private LocationRequest mLocationRequest;
    String[] result;
    String EncText = "";
    public static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 99;
    TelephonyManager tManager;
    String deviceIMEI;
    String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_file);
        buildGoogleApiClient();
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1000); // 1 second, in milliseconds

        //reading secret key from file
        try {
            FileInputStream fileIn = openFileInput("secretKey.txt");
            if (count == 0){
                Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                count++;
            } else {
                Toast.makeText(this,"",Toast.LENGTH_SHORT).cancel();
            }
        } catch (Exception e) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                checkPhonePermission();
            }
            else{
                createSecretKey();
            }
        }
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File directory = contextWrapper.getDir("LOC-K", Context.MODE_PRIVATE);

        // Use the current directory as title
        path = directory.getAbsolutePath();
        if(getIntent().hasExtra("path")){
            path = getIntent().getStringExtra("path");
        }

        // Read all files sorted into the values-array
        List values = new ArrayList();
        File dir = new File(path);
        if(!dir.canRead()){
            setTitle(getTitle() + " (inaccessible)");
        }
        String[] list = dir.list();
        if(list != null){
            for(String file : list){
                if(!file.startsWith(".")){
                    values.add(file);
                }
            }
        }
        Collections.sort(values);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_2, android.R.id.text1, values);
        ListView lv = (ListView)findViewById(android.R.id.list);
        lv.setAdapter(adapter);

        button = (Button) findViewById(R.id.newFileButton);

        // add button listener
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View arg0){
                final Dialog dialog = new Dialog(context);
                dialog.setContentView(R.layout.activity_add_file_dialog);
                dialog.setTitle("Encrypt");
                dialog.show();

                newButton = (Button) dialog.findViewById(R.id.newButton);
                newButton.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View arg0){
                        Intent intent = new Intent(ListFile.this, MapsActivity.class);
                        Bundle extras = new Bundle();
                        extras.putString("activity", "NewFile");
                        intent.putExtras(extras);
                        dialog.dismiss();
                        startActivity(intent);
                        finish();
                    }
                });

                existingButton = (Button) dialog.findViewById(R.id.existingButton);
                existingButton.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View arg0){
                        Intent intent = new Intent(ListFile.this, FileChooser.class);
                        Bundle extras = new Bundle();
                        extras.putString("activity", "NewFile");
                        intent.putExtras(extras);
                        dialog.dismiss();
                        startActivity(intent);
                        finish();
                    }
                });
            }
        });

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id)
            {
                String filename = (String) a.getAdapter().getItem(position);
                String fileN = (String) a.getAdapter().getItem(position);

                if (path.endsWith(File.separator)){
                    filename = path + filename;
                }else{
                    filename = path + File.separator + filename;
                }

                if(new File(filename).isDirectory()){
                    Intent intent = new Intent(ListFile.this, ExistingFile.class);
                    intent.putExtra("path", filename);
                    startActivity(intent);
                } else {
                    String sHashedPw = "";
                    String sEncryptedLoc = "";
                    String sCipherText = "";

                    //Read text from file
                    try {
                        BufferedReader br = new BufferedReader(new FileReader(filename));
                        String line = "";

                        while ((line = br.readLine()) != null) {
                            EncText += line;
                        }
                        result = EncText.split(":");
                        // read hashedPw
                            sHashedPw = result[0];
                        // read location
                            sEncryptedLoc = result[1];
                        // read ciphertext
                            sCipherText = result[2];
                        br.close();

                        final Dialog dialog = new Dialog(context);
                        dialog.setContentView(R.layout.activity_password_dialog);
                        dialog.setTitle(fileN);

                        Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonEnter);
                        final EditText passwordText = (EditText) dialog.findViewById(R.id.passwordTextbox);

                        dialog.show();

                        final String finalSHashedPw = sHashedPw;
                        final String finalSEncryptedLoc = sEncryptedLoc;
                        final String finalSCipherText = sCipherText;
                        final String finalFileN = fileN;

                        // Get secret key
                        internalKey = "";
                        FileInputStream fileIn = openFileInput("secretKey.txt");
                        InputStreamReader InputRead = new InputStreamReader(fileIn);

                        char[] inputBuffer = new char[READ_BLOCK_SIZE];
                        int charRead;

                        while ((charRead = InputRead.read(inputBuffer)) > 0) {
                            // char to string conversion
                            String readstring = String.copyValueOf(inputBuffer, 0, charRead);
                            internalKey += readstring;
                        }
                        InputRead.close();

                        dialogButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    String passwordEntry = passwordText.getText().toString();
                                    String hashedPw = SHA1.hash(passwordEntry);
                                    String locSalt = SHA1.hash(hashedPw+passwordEntry);
                                    Encryption locationE = Encryption.getDefault(internalKey, locSalt, new byte[16]);
                                    String locText = locationE.decrypt(finalSEncryptedLoc);
                                    String[] result = locText.split(",");
                                    longitude = Double.parseDouble(result[0]);
                                    latitude = Double.parseDouble(result[1]);
                                    radius = Double.parseDouble(result[2]);
                                    try {
                                        boolean status = checkDistance(latitude, longitude, currentLatitude, currentLongitude, radius);
                                        if (hashedPw.equals(finalSHashedPw)&&status == true) {
                                            dialog.dismiss();
                                            Intent intent = new Intent(ListFile.this, ExistingFile.class);
                                            Bundle extras = new Bundle();

                                            extras.putString("filename", finalFileN);
                                            extras.putString("password", passwordEntry);
                                            extras.putString("hashedPw", finalSHashedPw);
                                            extras.putString("encryptedLoc", finalSEncryptedLoc);
                                            extras.putString("cipherText", finalSCipherText);

                                            intent.putExtras(extras);
                                            startActivity(intent);
                                            finish();
                                        } else{
                                            Toast.makeText(getBaseContext(), "Invalid password/location!", Toast.LENGTH_SHORT).show();
                                        }
                                    }catch(Exception e) {
                                        e.printStackTrace();
                                    }
                                } catch (Exception e) {
                                    Toast.makeText(getBaseContext(), "Invalid password/location!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    boolean checkDistance(double latitude, double longitude, double currentLatitude, double currentLongitude, double radius){
        float[] results = new float[1];
        Location.distanceBetween(latitude, longitude, currentLatitude, currentLongitude,  results);
        if(results[0] <= radius ) {
            return true;
        }else{
            return false;
        }
    }

    @Override
    public void onLocationChanged(Location location){
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();
    }

    public boolean checkPhonePermission() {
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_PHONE_STATE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_PHONE_STATE},
                        MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);


            } else {
                //Request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_PHONE_STATE},
                        MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_PHONE_STATE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                        createSecretKey();
                    }
                } else {
                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    public void createSecretKey(){
        // GET DEVICE ID
        deviceId = Settings.Secure.getString(getContentResolver(),Settings.Secure.ANDROID_ID);

        // GET IMEI NUMBER
        tManager = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
        deviceIMEI = tManager.getDeviceId();
        // create secret key and save to internal storage
        try {
            internalKey = "";
            deviceId = "";
            internalKey = deviceId + deviceIMEI;
            FileOutputStream fileout = openFileOutput("secretKey.txt", MODE_PRIVATE);
            OutputStreamWriter outputWriter = new OutputStreamWriter(fileout);
            outputWriter.write(internalKey);
            outputWriter.close();
            //display successfully created message
            Toast.makeText(getBaseContext(), "Secret Key created successfully!", Toast.LENGTH_SHORT).show();
            count++;
        } catch (Exception f) {
            f.printStackTrace();
        }
    }
}