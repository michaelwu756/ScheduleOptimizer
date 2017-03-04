package com.example.quickstart;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
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
        Intent output = new Intent();
        output.putExtra(EventInput.ADD_NAME, nameField.getText().toString());
        if(!numberField.getText().toString().equals(""))
            output.putExtra(EventInput.ADD_NUMBER, Integer.parseInt(numberField.getText().toString()));
        setResult(RESULT_OK, output);
        finish();
    }
}
