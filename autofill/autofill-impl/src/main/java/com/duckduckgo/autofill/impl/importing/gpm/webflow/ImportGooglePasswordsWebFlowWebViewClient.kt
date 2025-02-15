/*
 * Copyright (c) 2023 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.autofill.impl.importing.gpm.webflow

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import javax.inject.Inject

class ImportGooglePasswordsWebFlowWebViewClient @Inject constructor(
    private val callback: NewPageCallback,
) : WebViewClient() {

    interface NewPageCallback {
        fun onPageStarted(url: String?) {}
        fun onPageFinished(url: String?) {}
    }

    override fun onPageStarted(
        view: WebView?,
        url: String?,
        favicon: Bitmap?,
    ) {
        callback.onPageStarted(url)
    }

    override fun onPageFinished(
        view: WebView?,
        url: String?,
    ) {
        callback.onPageFinished(url)
    }
}
