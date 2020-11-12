foursquare-android-native-oauth
===============================
Foursquare native authentication makes it easier for your app's users to connect with Foursquare. Unlike web-based OAuth, native authentication re-uses the Foursquare app's user credentials, saving users the hassle of re-logging in to Foursquare within your app.

This repo includes an Android library that can be used in your own app. It also includes a simple application as an example of how to use the library.

Native auth will work on Foursquare versions 2013.08.16 and higher---if your users don't have the proper version installed, the library will give them an opportunity to download it in the Play Store (see "Using FoursquareOAuth").

Setting up your app
====================
1. Visit <a href="https://foursquare.com/developers/apps" target="_blank">https://foursquare.com/developers/apps</a>
2. Create a new app or select from the list of apps that you have created.
3. Generate a key hash of your developer certificate using this command: ```keytool -list -v -keystore mystore.keystore```
4. Paste the generated key hash into the Foursquare app console: 
![screenshot](http://f.cl.ly/items/123k1N351y1q3B2v0v1f/Screen%20Shot%202013-07-09%20at%204.28.05%20PM.png)
5. Note that you can add multiple key hashes delimited by commas.
6. Click "Save Changes".
7. Copy the client id and secret as a string into your project. For security reasons, you should encrypt or obfuscate the id and secret.

Download
--------

```groovy
compile 'com.foursquare:foursquare-android-oauth:1.1.1'
```

Using FoursquareOAuth
=============
#### Obtaining an access code
Call `FoursquareOAuth.getConnectIntent()` with your application's client id to retrieve an intent that starts the Foursquare app for authentication or a fallback if the user does not have the Foursquare app installed. Once you have the intent, call the `startActivityForResult()` method with the retrieved intent.
```java
Intent intent = FoursquareOAuth.getConnectIntent(context, CLIENT_ID);
startActivityForResult(intent, REQUEST_CODE_FSQ_CONNECT);
```

When the authorization completes, the `onActivityResult()` method of your initiating `Activity` or `Fragment` will be triggered. Call `FoursquareOAuth.getAuthCodeFromResult()` with the resultCode and data intent to obtain an `AuthCodeResponse` object.
```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
        case REQUEST_CODE_FSQ_CONNECT:
            AuthCodeResponse codeResponse = FoursquareOAuth.getAuthCodeFromResult(resultCode, data);
            /* ... */
            break;
    }
}
```

The `AuthCodeResponse` object has two members:

* `code` - The access code for the user.
* `exception` - Exception of one of the following types:
    * `FoursquareCancelException` - User pressed the back button in the authorization screen.
    * `FoursquareDenyException` - User pressed the deny button in the authorization screen.
    * `FoursquareUnsupportedVersionException` - The version of the Foursquare app installed on the user's device is too old to support native auth.
    * `FoursquareInvalidRequestException` - Malformed connect request uri that the Foursquare app is not able to interpret, such as missing client id or version number. If you are using `FoursquareOAuth.getConnectIntent()` to start the oauth prcoess, you can ignore this exception as FoursquareOAuth creates the connect uri for you.
    * `FoursquareOAuthException` - An error occurred in the OAuth process. Call `FoursquareOAuthException.getErrorCode()` to obtain one of the error codes listed at http://tools.ietf.org/html/rfc6749#section-5.2
    * `FoursquareInternalErrorException` - An internal error occurred during authorization. Call `exception.getCause()` to inspect the original cause of the exception.

#### Obtaining an access token (server-side, recommended)
You should pass the returned access code to your own server and have it contact Foursquare's servers to convert the code to an access token. This is shown in [step 3 in our code flow docs](https://developer.foursquare.com/overview/auth#code), but note that when making the request to `/oauth2/access_token`, you should omit the `redirect_uri` parameter. We recommend conducting the exchange for an access token on the server to avoid including your client secret in your app's binary.

#### Obtaining an access token (client-side)
*WARNING:* For security reasons, it is recommended that you not use the following method if possible. However, this helper method is provided for you to use if this is not possible for your app.

The steps are very similar to obtaining an access code. Call `FoursquareOAuth.getTokenExchangeIntent()` with your application's client id, secret and access code to obtain an intent that starts the `TokenExchangeActivity` to convert a short-lived code into an access token. Then call the `startActivityForResult()` method with the retrieved intent.
```java
Intent intent = FoursquareOAuth.getTokenExchangeIntent(context, CLIENT_ID, CLIENT_SECRET, authCode);
startActivityForResult(intent, REQUEST_CODE_FSQ_TOKEN_EXCHANGE);
```

When the token exchange completes, the `onActivityResult()` method of your initiating `Activity` or `Fragment` will be triggered. Call `FoursquareOAuth.getTokenFromResult()` with the resultCode and data intent to obtain an `AccessTokenResponse` object.
```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
        case REQUEST_CODE_FSQ_TOKEN_EXCHANGE:
            AccessTokenResponse tokenResponse = FoursquareOAuth.getTokenFromResult(resultCode, data);
            /* ... */
            break;
    }
}
```

The `AccessTokenResponse` object has two members:
* `access_token` - The access token of the user.
* `exception` - Exception of one of the following types:
    * `FoursquareOAuthException` - An error occurred in the OAuth process. Call `FoursquareOAuthException.getErrorCode()` to obtain one of the error codes listed at http://tools.ietf.org/html/rfc6749#section-5.2.
    * `FoursquareInternalErrorException` - An internal error occurred while exchanging the code for a token.

License
=======
    Copyright (C) 2020 Foursquare Labs, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

More Information
================
See https://developer.foursquare.com for more information on how to use the Foursquare API. 
