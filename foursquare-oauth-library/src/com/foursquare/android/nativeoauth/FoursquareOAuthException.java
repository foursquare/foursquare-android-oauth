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

/**
 * An exception indicating that an error occured during the Foursqure OAuth
 * process. The error code details can be found <a
 * href="http://tools.ietf.org/html/rfc6749#section-5.2">here</a>
 * 
 * @date 2013-06-01
 */
public class FoursquareOAuthException extends Exception {
    
    private static final long serialVersionUID = 1L;
    private String mErrorCode;

    public FoursquareOAuthException(String errorCode) {
        super("An error occurred during authorization.");
        mErrorCode = errorCode;
    }

    public FoursquareOAuthException(Throwable throwable) {
        super(throwable);
    }

    public FoursquareOAuthException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
    
    public String getErrorCode() {
        return mErrorCode;
    }
}
