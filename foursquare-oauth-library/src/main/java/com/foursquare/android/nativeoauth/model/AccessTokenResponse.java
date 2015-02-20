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
package com.foursquare.android.nativeoauth.model;

import java.io.Serializable;

/**
 * Stores the access token reply for token exchange.
 * 
 * Note that we recommend doing the token exchange on your server
 * instead of on the client.
 * 
 * @date 2013-06-01
 */
public class AccessTokenResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private String accessToken;
    private Exception exception;
    
    public String getAccessToken() {
    	return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
    	this.accessToken = accessToken;
    }
    
    public Exception getException() {
    	return exception;
    }
    
    public void setException(Exception exception) {
    	this.exception = exception;
    }
}
