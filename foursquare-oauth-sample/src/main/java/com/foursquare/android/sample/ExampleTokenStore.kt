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

/**
 * Used as an example of holding onto a fetched token. You'd want to persist
 * the token in a real application so the user does not have to authenticate
 * every time they use the app.
 *
 * Note that you should encrypt the token when persisting.
 *
 * @date 2013-06-01
 */
object ExampleTokenStore {
    var token: String? = null
}