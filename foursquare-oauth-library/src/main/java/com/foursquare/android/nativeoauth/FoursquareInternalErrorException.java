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
 * Thrown when there is an authentication problem on Foursquare's side.
 * 
 * @date 2013-06-01
 */
public class FoursquareInternalErrorException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public FoursquareInternalErrorException() {
    }

    public FoursquareInternalErrorException(String detailMessage) {
        super(detailMessage);
    }

    public FoursquareInternalErrorException(Throwable throwable) {
        super(throwable);
    }

    public FoursquareInternalErrorException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}
