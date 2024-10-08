package org.sil.bloom.reader;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;

// This class is used by BloomLibraryActivity to configure its WebView. It passes on the page title
// when the page is loaded and overrides another method to prevent the system from loading certain
// URLs in the default system browser instead of within the webview.
public class BloomLibraryWebViewClient extends WebViewClient {

    Activity mOwner;
    public BloomLibraryWebViewClient(Activity owner) {
        mOwner = owner;
    }

    // This deprecated version of the method is needed to support Android 6
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return shouldOverrideUrlLoadingInternal(view, Uri.parse(url));
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return shouldOverrideUrlLoadingInternal(view, request.getUrl());
    }

    private boolean shouldOverrideUrlLoadingInternal(WebView view, Uri uri) {
        // Note, the webview never sees links when they are handled by the blorg router.

        final String externalStr = uri.getQueryParameter("external");
        if (externalStr != null && externalStr.equals("true")) {
            // Open in external browser
            final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            mOwner.startActivity(intent);
            return true;
        } else {
            // load this url in the webView itself
            return false;
        }
    }

    // The data we need to save if caching a web response. We don't currently do this,
    // and as a result the content field is not currently used, but we might have another go.
//    class Response {
//        byte[] content;
//        String contentType;
//        String contentEncoding;
//        Map<String, String> headers;
//    }
//    static Dictionary<String, Response> sCache = new Hashtable<String, Response>();

    @Nullable
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        // For now, we don't implement this ourselves. See comments below for a couple reasons we tried it
        // in the past. I'm leaving the code here, commented out, because it represents quite a few things
        // we had to figure out and which may be necessary if we go back to trying to cache resources locally.
        //
        // But the most recent thing this code was doing was handling gzipping ourselves.
        // That caused issues with Android 6, at least, when we started targeting Android 13.
        // And we now believe that we are, in fact, getting gzipping for free, anyway.
        //
        // Note that if we go back to implementing this method, we probably need to start sending our
        // modified user-agent string again in BloomLibraryActivity's onCreate. (See comment there.)
        return super.shouldInterceptRequest(view, request);

//        // This code started out as an attempt to cache HTTP requests better than the browser does.
//        // One idea was to give the browser what we've saved, but occasionally send the request
//        // in the background and update the cache. However, what we most want to improve is first-time
//        // load. To do that we'd have to ship BR with some version of the BL files. We might try that
//        // sometime.
//        // Along the way I discovered that WebView is not providing the header that allows the server
//        // to zip data. So the main thing this code currently achieves is to add that header and
//        // add a layer to the result stream that unzips it. We pass on the response headers, so
//        // the browser can cache things as directed.
//        // If we decide to try our own caching again, the next step would be to save the cache data
//        // somewhere more persistent (there's something like context.getCache() that returns a good
//        // place for cache-type data that the OS can delete if it needs space).
//        // And then we have to think about how long to keep it and whether some or all queries
//        // should be redone in the background while we return the cached data. For example, we might
//        // often update the root HTML document, which allows the whole website to be updated since
//        // it uses new hashed names for everything else. Parse queries should be updated often
//        // enough to see new information. Conceivably we could implement some mechanism to update
//        // the UI if a background query produces different data.
//        // Note that some of the same effects could be produced, perhaps with somewhat less control,
//        // using a service worker on the web side of things.
//        Uri uri = request.getUrl();
//        String path = uri.getPath();
//        String url = uri.toString();
//        Response data = null; // = sCache.get(url); // restore for real caching
//        InputStream result = null;
//        if (data == null) { // currently will always be true
//            if ( url.startsWith("https://cdn")) {
//                // caching these isn't working yet, at least in some cases, but it doesn't matter much:
//                // they are small requests and only needed when we go to a child page.
//                // Based on experience with the parse reqeusts, the problem is probably
//                // that we are not getting the handshake that says we are allowed to have
//                // the required request header that identifies us to the server, at least for Contentful.
//                // If we decide to try to get these working, note that we needed changes at both ends
//                // for parse queries: it was essential both that the browser NOT attempt to send
//                // the headers (somehow doing so made the whole request a security problem when
//                // cached), and also that code here should provide them (since they are essential
//                // for the query to work).
//                // For now, returning null tells the browser to get the data normally.
//                return null;
//            }
//            try {
//                data = new Response();
//                data.content = new byte[0];
//                data.headers = new HashMap<String, String>();
//                String modUrl = url;
//
//                URL url1 = new java.net.URL(modUrl);
//                HttpURLConnection connection = (HttpURLConnection)url1.openConnection();
//                if (url1.getHost().equals("analytics.bloomlibrary.org"))
//                    return null; // caching is not relevant here, and we get CORS errors if we intercept
//
//                if (path.startsWith("/parse/")) {
//                    connection.setRequestProperty("X-Parse-Application-Id", "R6qNTeumQXjJCMutAJYAwPtip1qBulkFyLefkCE5");
//                    // If we're working with one of the versions of BL that uses the dev parse server,
//                    // this needs to change to "yrXftBF6mbAuVu3fO6LnhCJiHxZPIdE7gl1DUVGR".
//                }
//                // copy all headers from the original request over
//                for (Map.Entry<String, String> kvp:request.getRequestHeaders().entrySet())
//                {
//                    connection.addRequestProperty(kvp.getKey(), kvp.getValue());
//                }
//                // This is currently the main point of having shouldInterceptRequest: to allow
//                // the server to send data gzipped.
//                connection.addRequestProperty("accept-encoding", "gzip");
//                InputStream in = connection.getInputStream();
//                String encoding = connection.getContentEncoding();
//                data.contentEncoding = encoding;
//                // Since webview isn't setting a header to say it handles gzip, we'd better handle it
//                // ourselves if we get gzipped data.
//                if ("gzip".equals(encoding)) { // don't reverse this, encoding might be null
//                    in = new GZIPInputStream(in);
//                    data.contentEncoding = null; // don't tell the browser it's zipped after we unzipped it.
//                }
//
//                if (data.content == null) {
//                    data.content = new byte[0];
//                }
//                data.contentType = connection.getContentType();
//                if (data.contentType.startsWith("text/html")) {
//                    // sometimes we get, at least from localhost, something like text/html; charset=utf-8.
//                    // that is not valid, and causes the webview to display the HTML source instead
//                    // of the web page.
//                    data.contentType = "text/html";
//                }
//                // I'm not actually sure whether this is helpful. contentEncoding seems to be more
//                // about things like gzip than character set. However, it doesn't seem to hurt.
//                if (data.contentEncoding == null) {
//                    int index = data.contentType.indexOf("charset=");
//                    if (index >= 0) {
//                        data.contentEncoding = data.contentType.substring(index + "charset=".length());
//                    }
//                }
//                result = in;
//                for(int i = 0; true; i++) {
//                    String key = connection.getHeaderFieldKey(i);
//                    if (key == null)
//                        break;
//                    if (key == "Content-Encoding")
//                        continue; // we're messing with this, don't copy the original over.
//                    String val = connection.getHeaderField(i);
//                    data.headers.put(key,val);
//                }
//                // For now I am not actually caching anything here. We're copying the response headers over,
//                // so the browser can obey the caching directions in the response.
//                // If we decide to do our own caching here,
//                // 1. We probably really want to save it on disk somewhere; sCache was just a proof of concept
//                // 2. We have to second-guess what we made the server say regarding how long things should be cached.
//                // 3. Also uncomment getting the response stream from data.content, below.
//                //data.content = IOUtils.toByteArray(in);
//                //sCache.put(url, data);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        // We need to do this if we are reading the original stream into data.content.
//        //result = new ByteArrayInputStream(data.content);
//
//        WebResourceResponse response = new WebResourceResponse(data.contentType, data.contentEncoding, result);
//        Map<String, String> headers = new HashMap<String, String>();
//        // This predated figuring out how to copy the original response headers, so it may no longer
//        // be needed. SOMETHING has to tell the browser we're allowed to use parse data even if it
//        // comes from a different domain.
//        headers.put("Access-Control-Allow-Origin", "*");
//        for (Map.Entry<String, String> kvp:data.headers.entrySet()) {
//            headers.put(kvp.getKey(), kvp.getValue());
//        }
//        response.setResponseHeaders(headers);
//        return response;
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        super.onReceivedSslError(view, handler, error);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        mOwner.setTitle(view.getTitle());
    }
}
