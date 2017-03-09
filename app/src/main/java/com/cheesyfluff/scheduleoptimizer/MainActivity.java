package com.cheesyfluff.scheduleoptimizer;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;

import com.google.api.services.calendar.CalendarScopes;
import com.google.api.client.util.DateTime;

import com.google.api.services.calendar.model.*;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.TimeUnit;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends Activity
        implements EasyPermissions.PermissionCallbacks {
    GoogleAccountCredential mCredential;
    private TextView mOutputText;
    private Button mCallApiButton;
    private Button mEventInputButton;
    private Button mUpdateCalendarButton;
    private ArrayList<FilledTimes> filledTimes;
    private long startQueryTime;
    private long endQueryTime;
    ProgressDialog mProgress;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private final int START_DAYS_AHEAD = 1;
    private final int END_DAYS_AHEAD = 8;
    private final int WAKEUP_TIME = 8;
    private final int SLEEP_TIME = 22;


    static final String PREFERENCES = "pref";

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { CalendarScopes.CALENDAR };

    /**
     * Create the main activity.
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity);

        mCallApiButton = (Button) findViewById(R.id.callApiButton);
        mCallApiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallApiButton.setEnabled(false);
                mOutputText.setText("");
                getResultsFromApi();
                mCallApiButton.setEnabled(true);
                mUpdateCalendarButton.setEnabled(true);
            }
        });

        mEventInputButton = (Button) findViewById(R.id.inputTasksButton);
        mEventInputButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTasks(v);
            }
        });

        mUpdateCalendarButton = (Button) findViewById(R.id.updateCalendarButton);
        mUpdateCalendarButton.setEnabled(false);
        mUpdateCalendarButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                mUpdateCalendarButton.setEnabled(false);
                mOutputText.setText("");
                addCalendarEvents(v);
            }
        });

        filledTimes = new ArrayList<>();
        startQueryTime = getDayFromTodayInMs(START_DAYS_AHEAD);
        endQueryTime = getDayFromTodayInMs(END_DAYS_AHEAD);

        mOutputText = (TextView) findViewById(R.id.textView);
        mOutputText.setPadding(16, 16, 16, 16);
        mOutputText.setVerticalScrollBarEnabled(true);
        mOutputText.setMovementMethod(new ScrollingMovementMethod());
        mOutputText.setText(
                "Click the \'" + mCallApiButton.getText() +"\' button to test the API.");

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Google Calendar API ...");

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
    }

    public void openTasks(View view)
    {
        Intent intent = new Intent(this, EventInput.class);
        startActivity(intent);
    }

    private long getDayFromTodayInMs(int day)
    {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int year  = cal.get(java.util.Calendar.YEAR);
        int month = cal.get(java.util.Calendar.MONTH);
        int date  = cal.get(java.util.Calendar.DATE);
        cal.clear();
        cal.set(year, month, date);
        return cal.getTimeInMillis()+TimeUnit.DAYS.toMillis(day);
    }

    private ArrayList<CalendarActivity> getSavedActivities()
    {
        ArrayList<CalendarActivity> activityList = new ArrayList<>();
        SharedPreferences prefs = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        for(int i= 0; i<prefs.getInt("numEntries",0); i++)
        {
            activityList.add(new CalendarActivity(prefs.getString("item"+Integer.toString(i)+"name", null),
                    prefs.getInt("item"+Integer.toString(i)+"hours", 0),
                    prefs.getString("item"+Integer.toString(i)+"days", null)));
        }
        return activityList;
    }

    private ArrayList<CalendarEvent> generateCalendarEvents(long startDay, long endDay, long startTime, long endTime,
                                                            ArrayList<CalendarActivity> calendarActivites,
                                                            ArrayList<FilledTimes> fTimes)
    {
        if(calendarActivites==null || calendarActivites.size()==0)
            return new ArrayList<>();
        boolean occupiedTime[] = new boolean[(int)TimeUnit.MILLISECONDS.toHours(endDay-startDay)];
        for(int i=0; i<(int)TimeUnit.MILLISECONDS.toDays(endDay-startDay);i++) {
            long d = startDay+TimeUnit.DAYS.toMillis(i);
            fTimes.add(new FilledTimes(d, d + startTime));
            fTimes.add(new FilledTimes(d + endTime, d+TimeUnit.DAYS.toMillis(1)));
        }
        for(FilledTimes f : fTimes)
        {
            int beginIndex = (int)TimeUnit.MILLISECONDS.toHours(f.getStart()-startDay);
            if(beginIndex<0)
                beginIndex=0;
            int endIndex = (int)TimeUnit.MILLISECONDS.toHours(f.getEnd()-1-startDay)+1;
            if(endIndex>occupiedTime.length)
                endIndex=occupiedTime.length;
            for(int i=beginIndex; i<endIndex; i++)
            {
                occupiedTime[i]=true;
            }
        }
        ArrayList<CalendarEvent> list = new ArrayList<>();
        int i = 0;
        int arrayListIndex=0;
        int hoursRemaining=calendarActivites.get(arrayListIndex).getHours();
        while(i<occupiedTime.length)
        {
            if(!occupiedTime[i])
            {
                if (hoursRemaining > 0)
                {
                    long start = startDay+ TimeUnit.HOURS.toMillis(i);
                    if(list.size()>0 && list.get(list.size()-1).getName()
                            .equals(calendarActivites.get(arrayListIndex).getName())
                        && startDay+TimeUnit.HOURS.toMillis(i)==list.get(list.size()-1).getEndTime())
                    {
                        start = list.get(list.size()-1).getStartTime();
                        list.remove(list.size()-1);
                    }
                    list.add(new CalendarEvent(calendarActivites.get(arrayListIndex).getName(),
                        start,
                        startDay+TimeUnit.HOURS.toMillis(i+1)));
                    hoursRemaining--;
                }
                else if (arrayListIndex<calendarActivites.size()-1)
                {
                    arrayListIndex++;
                    hoursRemaining=calendarActivites.get(arrayListIndex).getHours();
                    list.add(new CalendarEvent(calendarActivites.get(arrayListIndex).getName(),
                            startDay+TimeUnit.HOURS.toMillis(i),
                            startDay+TimeUnit.HOURS.toMillis(i+1)));
                    hoursRemaining--;
                }
                else
                    break;
            }
            i++;
        }
        return list;
    }

    public void addCalendarEvents(View v)
    {
        startQueryTime = getDayFromTodayInMs(START_DAYS_AHEAD);
        endQueryTime = getDayFromTodayInMs(END_DAYS_AHEAD);
        addCalendarEvents(generateCalendarEvents(startQueryTime,
                endQueryTime,
                TimeUnit.HOURS.toMillis(WAKEUP_TIME),
                TimeUnit.HOURS.toMillis(SLEEP_TIME),
                getSavedActivities(),
                filledTimes
        ));
    }

    private void addCalendarEvents(ArrayList<CalendarEvent> list)
    {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            mOutputText.setText("No network connection available.");
        } else {
            new UpdateCalendarTask(mCredential, list).execute();
        }
    }


    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            mOutputText.setText("No network connection available.");
        } else {
            new MakeRequestTask(mCredential).execute();
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    mOutputText.setText(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * An asynchronous task that handles the Google Calendar API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("ScheduleOptimizer")
                    .build();
        }

        /**
         * Background task to call Google Calendar API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of the next 10 events from the primary calendar.
         * @return List of Strings describing returned events.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            // List the next 10 events from the primary calendar.

            List<String> eventStrings = new ArrayList<String>();
            Events events = mService.events().list("primary")
                    .setMaxResults(500)
                    .setTimeMin(new DateTime(startQueryTime))
                    .setTimeMax(new DateTime(endQueryTime))
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            List<Event> items = events.getItems();
            ArrayList<FilledTimes> tempArrayList = new ArrayList<>();
            for (Event event : items) {
                DateTime start = event.getStart().getDateTime();
                if (start == null) {
                    // All-day events don't have start times, so just use
                    // the start date.
                    start = event.getStart().getDate();
                }
                else
                    tempArrayList.add(new FilledTimes(event.getStart().getDateTime().getValue(),
                            event.getEnd().getDateTime().getValue()));
                eventStrings.add(
                        String.format("%s (%s)", event.getSummary(), start));
            }
            filledTimes = tempArrayList;
            return eventStrings;
        }


        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
            if (output == null || output.size() == 0) {
                mOutputText.setText("No results returned.");
            } else {
                output.add(0, "Data retrieved using the Google Calendar API:");
                mOutputText.setText(TextUtils.join("\n", output));
            }
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    mOutputText.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }
    }
    private class UpdateCalendarTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.calendar.Calendar mService = null;
        private ArrayList<CalendarEvent> eventList = null;
        private Exception mLastError = null;

        UpdateCalendarTask(GoogleAccountCredential credential, ArrayList<CalendarEvent> eList) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("ScheduleOptimizer")
                    .build();
            eventList = eList;
        }

        /**
         * Background task to call Google Calendar API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return updateCalendar();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of the next 10 events from the primary calendar.
         * @return List of Strings describing returned events.
         * @throws IOException
         */
        private List<String> updateCalendar() throws IOException {
            List<String> eventStrings = new ArrayList<>();

            for (CalendarEvent e : eventList) {
                Event event = new Event().setSummary(e.getName());
                DateTime startDateTime = new DateTime(e.getStartTime());
                EventDateTime start = new EventDateTime()
                        .setDateTime(startDateTime);
                event.setStart(start);

                DateTime endDateTime = new DateTime(e.getEndTime());
                EventDateTime end = new EventDateTime()
                        .setDateTime(endDateTime);
                event.setEnd(end);
                mService.events().insert("primary", event).execute();
                eventStrings.add(
                        String.format("%s (%s)(%s)", event.getSummary(), start.toString(), end.toString()));
            }
            return eventStrings;
        }


        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
            if (output == null || output.size() == 0) {
                mOutputText.setText("No results returned.");
            } else {
                output.add(0, "Data sent using the Google Calendar API:");
                mOutputText.setText(TextUtils.join("\n", output));
            }
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    mOutputText.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }
    }
}