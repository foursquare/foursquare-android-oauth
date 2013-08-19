/*
 * Copyright (C) 2013 Foursquare Labs, Inc.
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
package com.foursquare.android.nativeoauth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.net.Uri;
import android.text.TextUtils;

import com.foursquare.android.nativeoauth.model.AccessTokenResponse;
import com.foursquare.android.nativeoauth.model.AuthCodeResponse;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Main implementation of Foursquare authentication.
 * 
 * @date 2013-06-01
 */
public final class FoursquareOAuth {

    private static final String PACKAGE = "com.joelapenna.foursquared";
    
    private static final String INTENT_RESULT_CODE = PACKAGE
            + ".fragments.OauthWebviewFragment.INTENT_RESULT_CODE";
    
    private static final String INTENT_RESULT_ERROR = PACKAGE
            + ".fragments.OauthWebviewFragment.INTENT_RESULT_ERROR";
    
    private static final String INTENT_RESULT_DENIED = PACKAGE
            + ".fragments.OauthWebviewFragment.INTENT_RESULT_DENIED";
    
    private static final String INTENT_RESULT_ERROR_MESSAGE = PACKAGE
            + ".fragments.OauthWebviewFragment.INTENT_RESULT_ERROR_MESSAGE";

    private static final String URI_SCHEME = "foursquareauth";
    private static final String URI_AUTHORITY = "authorize";
    private static final String PARAM_CLIENT_ID = "client_id";
    private static final String PARAM_SIGNATURE = "androidKeyHash";
    private static final String PARAM_VERSION = "v";
    
    private static final String URI_MARKET_PAGE = "market://details?id=com.joelapenna.foursquared"; 
    private static final String MARKET_REFERRER = "utm_source=foursquare-android-oauth&utm_term=%s";
    
    private static final String ERROR_CODE_UNSUPPORTED_VERSION = "unsupported_version";
    private static final String ERROR_CODE_INVALID_REQUEST = "invalid_request";
    private static final String ERROR_CODE_INTERNAL_ERROR = "internal_error";
    
    private static final int LIB_VERSION = 20130509;

    /**
     * Returns an intent that will start the Foursquare app for authentication
     * or return an intent that directs them to the app store if the app is not
     * present.
     * 
     * @param context 
     *          The context to use. Usually your Application or Activity object.
     * @param clientId
     */
    public static Intent getConnectIntent(Context context, String clientId) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(URI_SCHEME);
        builder.authority(URI_AUTHORITY);
        builder.appendQueryParameter(PARAM_CLIENT_ID, clientId);
        builder.appendQueryParameter(PARAM_VERSION, String.valueOf(LIB_VERSION));
        builder.appendQueryParameter(PARAM_SIGNATURE, getSignatureFingerprint(context));
        
        Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        if (isIntentAvailable(context, intent)) {
            return intent;
        }

        return getPlayStoreIntent(clientId);
    }
    
    /**
     * Obtains the {@link AuthCodeResponse} from the supplied intent result bundle.
     * This method should be called in onActivityResult() of the initiating 
     * {@link Activity} or {@link Fragment}.
     * 
     * @return an AuthCodeResponse object.
     */
    public static AuthCodeResponse getAuthCodeFromResult(int resultCode, Intent data) {
        AuthCodeResponse response = new AuthCodeResponse();
        
        switch (resultCode) {
            case Activity.RESULT_OK:
                boolean denied = data.getBooleanExtra(INTENT_RESULT_DENIED, false);
                String authCode = data.getStringExtra(INTENT_RESULT_CODE);
                String errorCode = data.getStringExtra(INTENT_RESULT_ERROR);
                String errorMessage = data.getStringExtra(INTENT_RESULT_ERROR_MESSAGE);
                
                if (denied) {
                    response.setException(new FoursquareDenyException());
                } else {
                    if (TextUtils.isEmpty(errorCode)) {
                        response.setCode(authCode);
                    } else if (ERROR_CODE_INVALID_REQUEST.equals(errorCode)) {
                        response.setException(new FoursquareInvalidRequestException(errorMessage));
                    } else if (ERROR_CODE_UNSUPPORTED_VERSION.equals(errorCode)) {
                        response.setException(new FoursquareUnsupportedVersionException(errorMessage));
                    } else if (ERROR_CODE_INTERNAL_ERROR.equals(errorCode)) {
                        response.setException(new FoursquareInternalErrorException(errorMessage));
                    } else {
                        response.setException(new FoursquareOAuthException(errorCode));
                    }
                }
                return response;
                
            case Activity.RESULT_CANCELED:
            default:
                // Cancel
                response.setException(new FoursquareCancelException());
                return response;
        }
    }
    
    /**
     * Returns an intent that will start the {@link TokenExchangeActivity} to
     * convert the short-lived auth code into an access token with a longer
     * lifetime. The initiating {@link Activity} or {@link Fragment} will be
     * called back with an {@link AccessTokenResponse} value. <br>
     * <br>
     * We strongly encourage developers to pass the code up to their server and
     * have the server do the code exchange.
     * 
     * @return
     * 
     * @see <a href="https://developer.foursquare.com/overview/auth#access"
     *      >https://developer.foursquare.com/overview/auth#access</a>
     */
    public static Intent getTokenExchangeIntent(Context context, String clientId, String clientSecret, String authCode) {
        Intent intent = new Intent();
        intent.setClass(context, TokenExchangeActivity.class);
        intent.putExtra(TokenExchangeActivity.INTENT_EXTRA_CLIENT_ID, clientId);
        intent.putExtra(TokenExchangeActivity.INTENT_EXTRA_CLIENT_SECRET, clientSecret);
        intent.putExtra(TokenExchangeActivity.INTENT_EXTRA_AUTH_CODE, authCode);
        return intent;
    }
    
    /**
     * Obtains the {@link AccessTokenResponse} from the intent result bundle
     * returned by {@link TokenExchangeActivity}. This method should be called
     * in onActivityResult() of the initiating {@link Activity} or {@link Fragment}.
     * 
     * @param resultCode
     * @param data
     * @return null if the operation is canceled.
     */
    public static AccessTokenResponse getTokenFromResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            return (AccessTokenResponse) data.getSerializableExtra(TokenExchangeActivity.INTENT_RESULT_RESPONSE);
        }

        return null;
    }
    
    /**
     * You can use this method to test if the intent returned by getConnectIntent()
     * would open the Foursquare app detail page on Google Play. This happens when 
     * the user does not have the Foursquare app installed on their device.
     * 
     * @param intent the intent returned by getConnectIntent().
     */
    public static boolean isPlayStoreIntent(Intent intent) {
        final Uri marketUri = Uri.parse(URI_MARKET_PAGE);
        Uri uri = intent.getData();
        
        return intent != null 
                && Intent.ACTION_VIEW.equals(intent.getAction())
                && marketUri.getScheme().equals(uri.getScheme())
                && marketUri.getHost().equals(uri.getHost())
                && marketUri.getQueryParameter("id").equals(uri.getQueryParameter("id"));
    }

    /**
     * Builds an intent that will open the Foursquare app detail page on Google Play.
     * 
     * @param clientId The app's clientId for referral tracking.
     * 
     * @return An intent that will open the Foursquare app detail page on Google Play.
     */
    private static Intent getPlayStoreIntent(String clientId) {
        final String referrer = String.format(MARKET_REFERRER, clientId);
        return new Intent(Intent.ACTION_VIEW, 
                Uri.parse(URI_MARKET_PAGE)
                    .buildUpon()
                    .appendQueryParameter("referrer", referrer)
                    .build());
    }
    
    /**
     * This method queries the package manager for installed packages that can
     * respond to an intent with the specified action.
     * 
     * @return true if a suitable package is found.
     */
    private static boolean isIntentAvailable(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(
                intent, PackageManager.MATCH_DEFAULT_ONLY);

        return resolveInfo.size() > 0;
    }
    
    private static String getSignatureFingerprint(Context context) {
        String callingPackage = context.getApplicationContext().getPackageName();
        PackageManager pm = context.getPackageManager();
        int flags = PackageManager.GET_SIGNATURES;

        PackageInfo callingPackageInfo = null;
        try {
            callingPackageInfo = pm.getPackageInfo(callingPackage, flags);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    
        if (callingPackageInfo != null ){
            Signature[] signatures = callingPackageInfo.signatures;
            if (signatures != null && signatures.length > 0) {
                byte[] cert = signatures[0].toByteArray();
        
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA1");
                    byte[] fingerprint = md.digest(cert);
        
                    StringBuffer hexString = new StringBuffer();
                    for (int i = 0; i < fingerprint.length; i++) {
                        String appendString = Integer.toHexString(0xFF & fingerprint[i]);
                        if (hexString.length() > 0) {
                            hexString.append(":");
                        }
                        if (appendString.length() == 1) hexString.append("0");
                        hexString.append(appendString);
                    }
        
                    String signature = hexString.toString().toUpperCase(); 
                    return signature;
        
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
