package com.example.events

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import java.util.*
import android.accounts.AccountManager
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.AccountPicker
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import com.google.api.services.calendar.model.EventReminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId

class MainActivity : AppCompatActivity() {

    // Request code for Google account picker activity
    private val REQUEST_ACCOUNT_PICKER = 1000

    // Request code for Google Play services authorization
    private val REQUEST_AUTHORIZATION = 1001

    // Google Calendar API instance
    private var calendarService: Calendar? = null

    // Google Account credential
    private var credential: GoogleAccountCredential? = null

    private var base = "311107161129-qh10rrh477b896mla7e7q4b2tfvurvhv.apps.googleusercontent.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val scopes = listOf(CalendarScopes.CALENDAR)
        credential = GoogleAccountCredential.usingOAuth2(
            applicationContext, scopes.map { it.toString() }
        )

        // Initialize Google Calendar service
        val transport: HttpTransport = com.google.api.client.http.javanet.NetHttpTransport()
        val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()
        calendarService = Calendar.Builder(
            transport, jsonFactory, credential
        ).setApplicationName("My Calendar App").build()

        Log.e("MainActivity","First works")

        // Set up button to create a new event
        val createEventButton = findViewById<Button>(R.id.create_event_button)
        createEventButton.setOnClickListener {
            // Launch Google account picker to choose account
            val accountIntent = AccountPicker.newChooseAccountIntent(
                null,
                null,
                arrayOf(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE),
                true,
                null,
                null,
                null,
                null
            )
            startActivityForResult(accountIntent, REQUEST_ACCOUNT_PICKER)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ACCOUNT_PICKER && resultCode == RESULT_OK) {
            // Get selected account from account picker
            val accountName = data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            Log.e("MainActivity","Second works")
            Log.e("MainActivity","working $accountName")

            // Set selected account in credential
            credential?.selectedAccountName = accountName

            // Request authorization from the user
            startActivityForResult(
                credential?.newChooseAccountIntent()!!,
                REQUEST_AUTHORIZATION
            )
        } else if (requestCode == REQUEST_AUTHORIZATION && resultCode == RESULT_OK) {
            // Authorization granted, create a new event in the user's calendar
            Log.e("MainActivity","Third works")
            createNewEvent()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNewEvent() = CoroutineScope(Dispatchers.Main).launch {
        Log.e("MainActivity","Fourth works")
        val account = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
        val credential = GoogleAccountCredential.usingOAuth2(
            this@MainActivity, listOf(CalendarScopes.CALENDAR))
            .setSelectedAccount(account?.account)

        Log.e("MainActivity","Fifth works")
        Log.e("MainActivity","Fifth $credential")

        val httpTransport = AndroidHttp.newCompatibleTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()
        val calendar = Calendar.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName("MyApp")
            .build()

        Log.e("MainActivity","Six works")
        Log.e("MainActivity","Six $calendar")

//        val reminder = EventReminder().apply {
//            minutes = 10 // set the number of minutes before the event to trigger the reminder
//            method = "popup" // set the reminder notification method, e.g. "popup", "email", or "sms"
//        }

        // perform long-running operation in the background thread
        val event = withContext(Dispatchers.IO) {

            val startDateTime = LocalDateTime.of(2023, 4, 15, 10, 0) // set your event start time here
            val endDateTime = LocalDateTime.of(2023, 4, 15, 11, 0)// set your event end time here

            val events = Event().apply {
                summary = "Test event"
                description = "This is a test event"
                start = EventDateTime().apply {
                    dateTime = DateTime(startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                    timeZone = TimeZone.getDefault().id
                }
                end = EventDateTime().apply {
                    dateTime = DateTime(endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                    timeZone = TimeZone.getDefault().id
                }
//                reminders = Event.Reminders().apply {
//                    useDefault = false // set to true if you want to use the default reminders for the calendar instead
//                    overrides = listOf(reminder) // add the custom reminder you created to the list of overrides
//                }
            }

            Log.d("MainActivity", "Event created: ${events.id}")


            calendar.events().insert("primary", events).execute()
        }

        // update UI on the main thread
        // display event ID if the event was created successfully
        if (event.id != null) {
            Log.d("MainActivity", "Event created with ID: ${event.id}")
            Toast.makeText(this@MainActivity, "Event created: ${event.id}", Toast.LENGTH_SHORT).show()
        } else {
            Log.e("MainActivity", "Failed to create event")
            Toast.makeText(this@MainActivity, "Failed to create event", Toast.LENGTH_SHORT).show()
        }
    }

}


//class MainActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {
//
//    private val TAG = MainActivity::class.java.simpleName
//    private lateinit var googleApiClient: GoogleApiClient
//    private lateinit var credentials: GoogleAccountCredential
//    private lateinit var binding: ActivityMainBinding
//    private var context = this
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Initialize Google API client and credentials
//
//        // Create a GoogleSignInOptions object with the appropriate scopes and account
//        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//            .requestEmail()
//            .build()
//
//        // Create a GoogleApiClient object with the appropriate API and options
//        googleApiClient = GoogleApiClient.Builder(this)
//            .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
//            .addScope(Scope(CalendarScopes.CALENDAR))
//            .enableAutoManage(this, this)
//            .build()
//
//
//
//        credentials = GoogleAccountCredential.usingOAuth2(
//            applicationContext, Collections.singleton(CalendarScopes.CALENDAR))
//            .setBackOff(ExponentialBackOff())
//    }
//
//    override fun onConnectionFailed(connectionResult: ConnectionResult) {
//        Log.e(TAG, "Connection to Google API client failed")
//    }
//
//    fun createEvent(view: View) {
////        val transport: HttpTransport = AndroidHttp.newCompatibleTransport()
////        val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()
////
////        // Choose an account to authorize the API request with
////        val accountName = credentials.selectedAccountName
////        if (accountName == null) {
////            startActivityForResult(credentials.newChooseAccountIntent(), 0)
////            return
////        }
////
////        // Authorize the API request using the selected account
////        credentials.selectedAccount = Account(accountName, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE)
////
////        // Build the Calendar API client
////        val calendarService = com.google.api.services.calendar.Calendar.Builder(
////            transport, jsonFactory, credentials)
////            .setApplicationName("My Calendar App")
////            .build()
////
////        // Get the event details from the XML fields
////        val summary = binding.summaryEdittext.text.toString()
////        val location = binding.locationEdittext.text.toString()
////        val description = binding.descriptionEdittext.text.toString()
////        val startTime = binding.startTimeEdittext.text.toString()
////        val endTime = binding.endTimeEdittext.text.toString()
////
////        // Set the start and end times for the event
////        val startTimeString = "$startDateT$startTime:00-04:00"
////        val endTimeString = "$endDateT$endTime:00-04:00"
////        val startDateTime = DateTime(startTimeString)
////        val endDateTime = DateTime(endTimeString)
////
////        // Create a new event
////        val event = Event()
////        event.summary = summary
////        event.location = location
////        event.description = description
////        event.start = EventDateTime().setDateTime(startDateTime)
////        event.end = EventDateTime().setDateTime(endDateTime)
////        event.reminders = Event.Reminders().setUseDefault(true)
////
////        // Insert the event into the user's calendar
////        val insertRequest = calendarService.events().insert("primary", event)
////        insertRequest.execute()
//    }
//
//}
