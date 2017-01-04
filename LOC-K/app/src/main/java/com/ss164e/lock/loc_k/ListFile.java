package com.ss164e.lock.loc_k;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ListFile extends ListActivity {

    private String path;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_file);

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
        setTitle(path);

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

        // Put the data into the list
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_2, android.R.id.text1, values);
        setListAdapter(adapter);


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
    }
    public final static String EXTRA_MESSAGE = "temp_message";

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id){
        String filename = (String) getListAdapter().getItem(position);
        if (path.endsWith(File.separator)){
            filename = path + filename;
        }else{
            filename = path + File.separator + filename;
        }
        if(new File(filename).isDirectory()){
            Intent intent = new Intent(this, ExistingFile.class);
            intent.putExtra("path", filename);
            startActivity(intent);
        }else{
            //Read text from file
            StringBuilder text = new StringBuilder();

            try {
                BufferedReader br = new BufferedReader(new FileReader(filename));
                String line;

                while ((line = br.readLine()) != null) {
                    text.append(line);
                    text.append('\n');
                }
                br.close();
                //Toast.makeText(this, filename, Toast.LENGTH_LONG).show();
                //Find the view by its id
                Intent intent = new Intent(this, ExistingFile.class);
                intent.putExtra(EXTRA_MESSAGE, text.toString());
                startActivity(intent);
                finish();
                //br.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
