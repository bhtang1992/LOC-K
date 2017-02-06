package com.ss164e.lock.loc_k;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class NewFile extends AppCompatActivity implements
        ConnectionCallbacks,
        OnConnectionFailedListener,
        LocationListener {

    //Define a request code to send to Google Play services
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private double currentLatitude;
    private double currentLongitude;
    private Button button;
    private String locText;
    EditText ed;
    TextView result;
    String fileName;
    final Context context = this;
    String internalKey;
    static final int READ_BLOCK_SIZE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_file);
        ed = (EditText) findViewById(R.id.contentText);
        result = (TextView) findViewById(R.id.savedText);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                // The next two lines tell the new client that “this” current class will handle connection stuff
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                //fourth line adds the LocationServices API endpoint from GooglePlayServices
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds


        button = (Button) findViewById(R.id.saveButton);

        button.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View arg0) {
                locText = currentLongitude + ", " + currentLatitude;

                final Dialog dialog = new Dialog(context);
                dialog.setContentView(R.layout.activity_save_dialog);
                dialog.setTitle("Encrypt");

                Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonSave);
                final EditText filenameText = (EditText) dialog.findViewById(R.id.filenameTextbox);
                final EditText passwordText = (EditText) dialog.findViewById(R.id.passwordTextbox);

                // if button is clicked, close the custom dialog
                dialogButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        try {
                            // Get secret key
                            FileInputStream fileIn = openFileInput("secretKey.txt");
                            InputStreamReader InputRead = new InputStreamReader(fileIn);

                            char[] inputBuffer = new char[READ_BLOCK_SIZE];
                            int charRead;

                            while ((charRead=InputRead.read(inputBuffer))>0) {
                                // char to string conversion
                                String readstring = String.copyValueOf(inputBuffer,0,charRead);
                                internalKey += readstring;
                            }
                            InputRead.close();


                            fileName = filenameText.getText().toString();
                            String password = passwordText.getText().toString();
                            String hashedPw = SHA1.hash(password);
                            String message = ed.getText().toString();
                            String locSalt = SHA1.hash(hashedPw+password);
                            String textSalt = SHA1.hash(password+locText);

                            Encryption locationE = Encryption.getDefault(internalKey, locSalt, new byte[16]);
                            String encryptedLoc = locationE.encrypt(locText);

                            Encryption textE = Encryption.getDefault(internalKey, textSalt, new byte[16]);
                            String encryptedText = textE.encrypt(message);

                            // this will create a new name everytime and unique
                            File root = new File(Environment.getExternalStorageDirectory(), "LOC-K");
                            // if external memory exists and folder with name Notes
                            /*if (!root.exists()) {
                                root.mkdirs(); // this will create folder.
                            }*/
                            File filepath = new File(root, fileName + ".txt");  // file path to save
                            FileWriter writer = new FileWriter(filepath);
                            writer.append(hashedPw + "\n");
                            writer.append(encryptedLoc);
                            writer.append(encryptedText);
                            writer.flush();
                            writer.close();
                            //String m = "File generated with name " + fileName + ".txt";
                            //result.setText(m);

                        } catch (IOException e) {
                            e.printStackTrace();
                            result.setText(e.getMessage());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        dialog.dismiss();
                        Intent intent = new Intent(NewFile.this, ListFile.class);
                        startActivity(intent);
                        finish();
                    }
                });

                dialog.show();

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Now lets connect to the API
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(this.getClass().getSimpleName(), "onPause()");

        //Disconnect from API onPause()
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        } else {
            //If everything went fine lets get latitude and longitude
            currentLatitude = location.getLatitude();
            currentLongitude = location.getLongitude();

            //Toast.makeText(this, currentLatitude + " WORKS " + currentLongitude + "", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
            /*
             * Google Play services can resolve some errors it detects.
             * If the error has a resolution, try sending an Intent to
             * start a Google Play services activity that can resolve
             * error.
             */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                    /*
                     * Thrown if Google Play services canceled the original
                     * PendingIntent
                     */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
                /*
                 * If no resolution is available, display a dialog to the
                 * user with the error.
                 */
            Log.e("Error", "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();

        //Toast.makeText(this, currentLatitude + " WORKS " + currentLongitude + "", Toast.LENGTH_LONG).show();
    }

    public void onBackPressed() {
        Intent intent = new Intent(this, ListFile.class);
        startActivity(intent);
        finish();
    }

}