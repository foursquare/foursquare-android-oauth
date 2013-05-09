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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.Window;

import com.foursquare.android.nativeoauth.model.AccessTokenResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A utility {@link Activity} that converts a short-lived auth code into an
 * access token. Do not start this activity directly. Obtain an intent from
 * {@link FoursquareOAuth#getTokenExchangeIntent(android.content.Context, String, String, String)}
 * and start the intent for a result. <br>
 * <br>
 * Add this activity to your AndroidManifest.xml
 * 
 * <pre>
 * {@code
 * <activity android:name="com.foursquare.android.nativeoauth.TokenExchangeActivity"
 *           android:theme="@android:style/Theme.Dialog" />
 * }
 * </pre>
 * 
 * Since we strongly encourage developers to pass the code up to their server
 * and have the server do the code exchange, this {@link Activity} is an
 * optional part of the native Foursquare auth process. <br>
 * <br>
 * 
 * @see <a href="https://developer.foursquare.com/overview/auth#access"
 *      >https://developer.foursquare.com/overview/auth#access</a>
 *      
 * @date 2013-06-01
 */
public final class TokenExchangeActivity extends Activity {
    
    private static final String TAG = TokenExchangeActivity.class.getName();

    public static final String INTENT_EXTRA_CLIENT_ID = TAG + ".INTENT_EXTRA_CLIENT_ID";
    
    public static final String INTENT_EXTRA_CLIENT_SECRET = TAG + ".INTENT_EXTRA_CLIENT_SECRET";

    public static final String INTENT_EXTRA_AUTH_CODE = TAG + ".INTENT_EXTRA_AUTH_CODE";
    
    public static final String INTENT_RESULT_RESPONSE = TAG + ".INTENT_RESULT_RESPONSE";
    
    private static final String INTENT_EXTRA_TOKEN_EXCHANGE_TASK = TAG + ".INTENT_EXTRA_TOKEN_EXCHANGE_TASK";
    
    private static final String HTTP_BASE = "https://foursquare.com/oauth2/access_token?";
    
    private static final String ACCESS_TOKEN_URL = HTTP_BASE
            + "client_id=%s&client_secret=%s&grant_type=authorization_code&code=%s";
    
    private TokenExchangeTask mTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setTheme(getThemeRes());
        setContentView(R.layout.loading);
        
        String clientId = getIntent().getStringExtra(INTENT_EXTRA_CLIENT_ID);
        String clientSecret = getIntent().getStringExtra(INTENT_EXTRA_CLIENT_SECRET);
        String authCode = getIntent().getStringExtra(INTENT_EXTRA_AUTH_CODE);
        
        if (savedInstanceState == null) {
            mTask = new TokenExchangeTask(this);
            mTask.execute(clientId, clientSecret, authCode);
            
        } else {
            mTask = (TokenExchangeTask) savedInstanceState.getSerializable(INTENT_EXTRA_TOKEN_EXCHANGE_TASK);
            mTask.setActivity(this);
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(INTENT_EXTRA_TOKEN_EXCHANGE_TASK, mTask);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Consume all touch events to avoid activity being finished when touched
        return true;
    }
    
    @Override
    public void onBackPressed() {
        // no-op
    }
    
    private void onTokenComplete(AccessTokenResponse response) {
        Intent data = new Intent();
        data.putExtra(INTENT_RESULT_RESPONSE, response);
        
        setResult(RESULT_OK, data);
        finish();
    }
    
    @SuppressLint("InlinedApi")
    private int getThemeRes() {
        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            return android.R.style.Theme_Dialog;
        } else if (VERSION.SDK_INT < VERSION_CODES.ICE_CREAM_SANDWICH) {
            return android.R.style.Theme_Holo_Light_Dialog_NoActionBar_MinWidth;
        } else {
            return android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth;
        }
    }
    
    static class TokenExchangeTask extends AsyncTask<String, Void, AccessTokenResponse> implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        private TokenExchangeActivity mActivity;
        
        public TokenExchangeTask(TokenExchangeActivity activity) {
            mActivity = activity;
        }
        
        public void setActivity(TokenExchangeActivity activity) {
            mActivity = activity;
        }

        @Override
        protected AccessTokenResponse doInBackground(String... params) {
            String accessTokenUrl = String.format(ACCESS_TOKEN_URL, params[0], params[1], params[2]);
            AccessTokenResponse result = null;
            HttpURLConnection connection = null;
            
            try {
                URL url = new URL(accessTokenUrl);
                connection = (HttpURLConnection) url.openConnection();
                InputStream in = connection.getInputStream();
                String json = readStream(in);
                result = parseAccessToken(json);
                
            } catch (MalformedURLException e) {
                result = createErrorResponse(e);
            } catch (IOException e) {
                result = createErrorResponse(e);
            } catch (JSONException e) {
                result = createErrorResponse(e);
            } catch (Exception e) {
                result = createErrorResponse(e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(AccessTokenResponse result) {
            mActivity.onTokenComplete(result);
        }
        
        private String readStream(InputStream in) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                byte[] buffer = new byte[1024];
                for (int count; (count = in.read(buffer)) != -1;) {
                    out.write(buffer, 0, count);
                }
                return new String(out.toByteArray(), "UTF-8");
            } finally {
                closeQuietly(out);
            }
        }
        
        private void closeQuietly(Closeable closeable) {
            try {
                if (closeable != null) {
                    closeable.close();
                }
            } catch (IOException ioe) {
                // no-op
            }
        }
        
        private AccessTokenResponse parseAccessToken(String json) throws JSONException {
            AccessTokenResponse response = new AccessTokenResponse();
            JSONObject obj = new JSONObject(json);
            String errorCode = obj.optString("error");
            
            if (TextUtils.isEmpty(errorCode)) {
                response.setAccessToken(obj.optString("access_token"));
            } else {
                response.setException(new FoursquareOAuthException(errorCode));
            }
            
            return response;
        }
        
        private AccessTokenResponse createErrorResponse(Exception e) {
            AccessTokenResponse response = new AccessTokenResponse();
            response.setException(new FoursquareInternalErrorException(e));
            return response;
        }
    }
}
