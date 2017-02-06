package com.ss164e.lock.loc_k;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;




public class ListFile extends AppCompatActivity {

    private String path;
    private Button button;
    String internalKey;
    static final int READ_BLOCK_SIZE = 100;
    public final static String EXTRA_MESSAGE = "";
    final Context context = this;
    static int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_file);

        //reading secret key from file
        try {
            FileInputStream fileIn = openFileInput("secretKey.txt");

            if (count == 0){
                Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                count++;
            }

            else{
                Toast.makeText(this,"",Toast.LENGTH_SHORT).cancel();
            }

        } catch (Exception e) {

            // create secret key and save to internal storage
            try {
                // GET DEVICE ID
                final String deviceId = Settings.Secure.getString(getContentResolver(),Settings.Secure.ANDROID_ID);

                // GET IMEI NUMBER
                TelephonyManager tManager = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
                String deviceIMEI = tManager.getDeviceId();

                internalKey = deviceId + deviceIMEI;

                FileOutputStream fileout = openFileOutput("secretKey.txt", MODE_PRIVATE);
                OutputStreamWriter outputWriter = new OutputStreamWriter(fileout);
                outputWriter.write(internalKey);
                outputWriter.close();

                //display successfully created message
                Toast.makeText(getBaseContext(), "Secret Key created successfully!", Toast.LENGTH_SHORT).show();

            } catch (Exception f) {
                f.printStackTrace();
            }
        }

        //if there is no SD card, create new directory objects to make directory on device
        if(Environment.getExternalStorageState() == null){
            //create new file directory object
            File directory = new File(Environment.getDataDirectory() + "/LOC-K/");
            if(!directory.exists()){
                directory.mkdir();
            }
        }else if(Environment.getExternalStorageState() != null){  // if phone DOES have sd card
            // search for directory on SD card
            File directory = new File(Environment.getExternalStorageDirectory() + "/LOC-K/");
            // if no directory exists, create new directory
            if(!directory.exists()){
                directory.mkdir();
            }
        }// end of SD card checking

        // Use the current directory as title
        path = Environment.getExternalStorageDirectory() + "/LOC-K/";
        if(getIntent().hasExtra("path")){
            path = getIntent().getStringExtra("path");
        }
        //setTitle(path);

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
                Intent intent = new Intent(ListFile.this, NewFile.class);
                startActivity(intent);
                finish();
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
                }
                else{
                    int count = 0;
                    String sHashedPw = "";

                    String sEncryptedLoc = "";
                    String sCipherText = "";



                    //Read text from file
                    final StringBuilder text = new StringBuilder();

                    try {
                        BufferedReader br = new BufferedReader(new FileReader(filename));
                        String line;

                        while ((line = br.readLine()) != null) {
                            //text.append(line);
                            //text.append('\n');

                            // read hashedPw
                            if (count == 0) {
                                sHashedPw = line;
                                count++;
                            }
                            // read location
                            else if (count == 1) {

                                sEncryptedLoc = line;
                                count++;
                            }
                            // read ciphertext
                            else {
                                sCipherText += line;

                                count++;
                            }
                        }
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

                        dialogButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                try {
                                    String passwordEntry = passwordText.getText().toString();
                                    String hashedPw = SHA1.hash(passwordEntry);

                                    if (hashedPw.equals(finalSHashedPw)) {
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
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        //br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
