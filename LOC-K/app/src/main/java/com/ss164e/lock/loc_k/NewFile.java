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

public class NewFile extends AppCompatActivity {

    //Define a request code to send to Google Play services
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private double currentLatitude;
    private double currentLongitude;
    private double radius;
    private Button button;
    private String locText;
    EditText ed;
    TextView result;
    String fileName;
    final Context context = this;
    String internalKey;
    static final int READ_BLOCK_SIZE = 100;




    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_file);
        ed = (EditText) findViewById(R.id.contentText);

        button = (Button) findViewById(R.id.saveButton);

        button.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View arg0) {
                Intent intent = getIntent();
                Bundle extras = intent.getExtras();
                currentLatitude = extras.getDouble("latitude");
                currentLongitude = extras.getDouble("longitude");
                radius = extras.getDouble("radius");
                locText = currentLongitude + "," + currentLatitude + "," + radius;

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
                            internalKey = "";
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
                            writer.append(":\n");
                            writer.append(encryptedLoc);
                            writer.append(":\n");
                            writer.append(encryptedText);
                            writer.flush();
                            writer.close();
                            //String m = "File generated with name " + fileName + ".txt";
                            //result.setText(m);

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
}