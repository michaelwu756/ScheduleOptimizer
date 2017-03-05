package com.cheesyfluff.scheduleoptimizer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

public class AddEvent extends AppCompatActivity {

    private Button addButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        addButton=(Button) findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                sendResult(view);
            }
        });
    }

    public void sendResult(View v)
    {
        EditText nameField = (EditText) findViewById(R.id.newEntryName);
        EditText numberField = (EditText) findViewById(R.id.newEntryNumber);
        CheckBox days[]= new CheckBox[7];
        days[0]= (CheckBox) findViewById(R.id.sundayCheckBox);
        days[1]= (CheckBox) findViewById(R.id.mondayCheckBox);
        days[2]= (CheckBox) findViewById(R.id.tuesdayCheckBox);
        days[3]= (CheckBox) findViewById(R.id.wednesdayCheckBox);
        days[4]= (CheckBox) findViewById(R.id.thursdayCheckBox);
        days[5]= (CheckBox) findViewById(R.id.fridayCheckBox);
        days[6]= (CheckBox) findViewById(R.id.saturdayCheckBox);
        StringBuilder dayStr = new StringBuilder();
        for(int i=0;i<7;i++)
            if(days[i].isChecked())
                dayStr.append("1");
            else
                dayStr.append("0");
        Intent output = new Intent();
        output.putExtra(EventInput.ADD_NAME, nameField.getText().toString());
        if(!numberField.getText().toString().equals(""))
            output.putExtra(EventInput.ADD_NUMBER, Integer.parseInt(numberField.getText().toString()));
        output.putExtra(EventInput.ADD_DAYS, dayStr.toString());
        setResult(RESULT_OK, output);
        finish();
    }
}
