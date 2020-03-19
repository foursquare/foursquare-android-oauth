/*
 * Copyright (C) 2020 Foursquare Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.foursquare.android.sample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.foursquare.android.nativeoauth.FoursquareCancelException
import com.foursquare.android.nativeoauth.FoursquareDenyException
import com.foursquare.android.nativeoauth.FoursquareInvalidRequestException
import com.foursquare.android.nativeoauth.FoursquareOAuth
import com.foursquare.android.nativeoauth.FoursquareOAuthException
import com.foursquare.android.nativeoauth.FoursquareUnsupportedVersionException

/**
 * A sample activity demonstrating usage of the Foursquare auth library.
 *
 * @date 2013-06-01
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ensureUi()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_FSQ_CONNECT -> onCompleteConnect(resultCode, data)
            REQUEST_CODE_FSQ_TOKEN_EXCHANGE -> onCompleteTokenExchange(resultCode, data)
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    /**
     * Update the UI. If we already fetched a token, we'll just show a success
     * message.
     */
    private fun ensureUi() {
        val isAuthorized = !TextUtils.isEmpty(ExampleTokenStore.token)
        val tvMessage = findViewById<TextView>(R.id.tvMessage)
        tvMessage.visibility = if (isAuthorized) View.VISIBLE else View.GONE
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        btnLogin.visibility = if (isAuthorized) View.GONE else View.VISIBLE
        btnLogin.setOnClickListener {
            // Start the native auth flow.
            val intent = FoursquareOAuth.getConnectIntent(this@MainActivity, CLIENT_ID)
            // If the device does not have the Foursquare app installed, we'd
            // get an intent back that would open the Play Store for download.
            // Otherwise we start the auth flow.
            if (FoursquareOAuth.isPlayStoreIntent(intent)) {
                toastMessage(this@MainActivity, getString(R.string.app_not_installed_message))
                startActivity(intent)
            } else {
                startActivityForResult(intent, REQUEST_CODE_FSQ_CONNECT)
            }
        }
    }

    private fun onCompleteConnect(resultCode: Int, data: Intent?) {
        val codeResponse = FoursquareOAuth.getAuthCodeFromResult(resultCode, data)
        val exception = codeResponse.exception
        if (exception == null) { // Success.
            val code = codeResponse.code
            performTokenExchange(code)
        } else {
            if (exception is FoursquareCancelException) { // Cancel.
                toastMessage(this, "Canceled")
            } else if (exception is FoursquareDenyException) { // Deny.
                toastMessage(this, "Denied")
            } else if (exception is FoursquareOAuthException) { // OAuth error.
                val errorMessage = exception.message
                val errorCode = exception.errorCode
                toastMessage(this, "$errorMessage [$errorCode]")
            } else if (exception is FoursquareUnsupportedVersionException) { // Unsupported Fourquare app version on the device.
                toastError(this, exception)
            } else if (exception is FoursquareInvalidRequestException) { // Invalid request.
                toastError(this, exception)
            } else { // Error.
                toastError(this, exception)
            }
        }
    }

    private fun onCompleteTokenExchange(resultCode: Int, data: Intent?) {
        val tokenResponse = FoursquareOAuth.getTokenFromResult(resultCode, data)
        val exception = tokenResponse.exception
        if (exception == null) {
            val accessToken = tokenResponse.accessToken
            // Success.
            toastMessage(this, "Access token: $accessToken")
            // Persist the token for later use.
            ExampleTokenStore.token = accessToken
            // Refresh UI.
            ensureUi()
        } else {
            if (exception is FoursquareOAuthException) { // OAuth error.
                val errorMessage = exception.message
                val errorCode = exception.errorCode
                toastMessage(this, "$errorMessage [$errorCode]")
            } else { // Other exception type.
                toastError(this, exception)
            }
        }
    }

    /**
     * Exchange a code for an OAuth Token. Note that we do not recommend you
     * do this in your app, rather do the exchange on your server. Added here
     * for demo purposes.
     *
     * @param code
     * The auth code returned from the native auth flow.
     */
    private fun performTokenExchange(code: String) {
        val intent = FoursquareOAuth.getTokenExchangeIntent(this, CLIENT_ID, CLIENT_SECRET, code)
        startActivityForResult(intent, REQUEST_CODE_FSQ_TOKEN_EXCHANGE)
    }

    companion object {
        private const val REQUEST_CODE_FSQ_CONNECT = 200
        private const val REQUEST_CODE_FSQ_TOKEN_EXCHANGE = 201
        /**
         * Obtain your client id and secret from:
         * https://foursquare.com/developers/apps
         */
        private const val CLIENT_ID = "YOUR_CLIENT_ID_HERE"
        private const val CLIENT_SECRET = "YOUR_CLIENT_SECRET_HERE"
        fun toastMessage(context: Context?, message: String?) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        fun toastError(context: Context?, t: Throwable) {
            Toast.makeText(context, t.message, Toast.LENGTH_SHORT).show()
        }
    }
}