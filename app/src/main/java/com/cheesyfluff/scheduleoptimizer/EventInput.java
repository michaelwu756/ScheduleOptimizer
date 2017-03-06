package com.cheesyfluff.scheduleoptimizer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class EventInput extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private ListView mainListView;
    private ArrayAdapter<CalendarActivity> listAdapter;

    static final int REQUEST_NEW_EVENT = 1;
    static final String ADD_NAME = "Name";
    static final String ADD_NUMBER = "Number";
    static final String ADD_DAYS = "Days";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_input);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addEvent(view);
            }
        });

        Button saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                saveEvents(view);
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mainListView = (ListView) findViewById(R.id.mainListView);
        mainListView.setOnItemClickListener(this);
        ArrayList<CalendarActivity> eventList=getSavedActivities();
        listAdapter = new ArrayAdapter<>(this, R.layout.list_entry_layout, eventList);
        mainListView.setAdapter(listAdapter);
    }

    public void addEvent(View v) {
        Intent intent = new Intent(this, AddEvent.class);
        startActivityForResult(intent, REQUEST_NEW_EVENT);
    }

    public void saveEvents(View v)
    {
        SharedPreferences.Editor e = getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE).edit();
        e.clear();
        e.putInt("numEntries", listAdapter.getCount());
        for(int i=0; i<listAdapter.getCount(); i++)
        {
            CalendarActivity a = listAdapter.getItem(i);
            e.putString("item"+Integer.toString(i)+"name", a.getName());
            e.putInt("item"+Integer.toString(i)+"hours", a.getHours());
            e.putString("item"+Integer.toString(i)+"days", a.getDays());
        }
        e.commit();
        Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
    }

    private ArrayList<CalendarActivity> getSavedActivities()
    {
        ArrayList<CalendarActivity> activityList = new ArrayList<>();
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE);
        for(int i= 0; i<prefs.getInt("numEntries",0); i++)
        {
            activityList.add(new CalendarActivity(prefs.getString("item"+Integer.toString(i)+"name", null),
                    prefs.getInt("item"+Integer.toString(i)+"hours", 0),
                    prefs.getString("item"+Integer.toString(i)+"days", null)));
        }
        return activityList;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_NEW_EVENT && resultCode == RESULT_OK && data != null) {
            if(!data.getStringExtra(ADD_NAME).trim().isEmpty())
                listAdapter.add(new CalendarActivity(data.getStringExtra(ADD_NAME),
                        data.getIntExtra(ADD_NUMBER,1),
                        data.getStringExtra(ADD_DAYS)));
        }
    }

    @Override
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        listAdapter.remove(listAdapter.getItem(position));
    }
}
