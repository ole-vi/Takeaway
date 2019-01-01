package org.ole.planet.myplanet;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.opencsv.CSVReader;

import org.ole.planet.myplanet.utilities.Utilities;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;


public class CSVViewerActivity extends AppCompatActivity {

    private TextView mCSVNameTitle;
    private TextView mCSVContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_csvviewer);
        declareElements();
        renderCSVFile();
    }

    private void declareElements() {
        mCSVNameTitle = (TextView) findViewById(R.id.csvFileName);
        mCSVContent = (TextView) findViewById(R.id.csvFileContent);
    }

    private void renderCSVFile() {
        // File name to be viewed

        Intent imageOpenIntent = getIntent();
        String fileName = imageOpenIntent.getStringExtra("TOUCHED_FILE");

        if (fileName != null && !fileName.isEmpty()) {
            mCSVNameTitle.setText(fileName);
            mCSVNameTitle.setVisibility(View.VISIBLE);
        }

        try {
            CSVReader reader = new CSVReader(new FileReader(new File(Utilities.SD_PATH, fileName)), ',', '"');

            //Get all lines from CSV file
            List<String[]> allRows = reader.readAll();

            //Read List "allRows" into textview line by line
            for (String[] row : allRows) {
                mCSVContent.append(Arrays.toString(row));
                mCSVContent.append("\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
