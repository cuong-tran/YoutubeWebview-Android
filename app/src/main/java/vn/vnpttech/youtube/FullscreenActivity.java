package vn.vnpttech.youtube;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

public class FullscreenActivity extends AppCompatActivity {

    /**
     * The JS client
     */
    private MyWebChromeClient mWebChromeClient = null;

    /**
     * The main activity itself
     */
    private FrameLayout mContentView;
    /**
     * The custom view which is requesting to show in fullscreen, contains the video player
     */
    private View mCustomView;
    /**
     * The callback for custom view which is requesting to show in fullscreen, contains the video player
     */
    private WebChromeClient.CustomViewCallback mCustomViewCallback;

    private WebView myWebView;
    private boolean exiting = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        // Hide the action bar (of the app)
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        myWebView = findViewById(R.id.activity_main_webview);
        mContentView = findViewById(R.id.activity_fullscreen);

        mWebChromeClient = new MyWebChromeClient();
        myWebView.setWebChromeClient(mWebChromeClient);

        /* Prevent loading external browser or Youtube app */
        MyWebViewClient myWebViewClient = new MyWebViewClient();
        myWebView.setWebViewClient(myWebViewClient);

        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        // Set User Agent so that Youtube display proper Vietnamese fonts
        webSettings.setUserAgentString("Mozilla/5.0 (ipad; Android 5.0.2; N1 Build/A5CN410) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.76 Safari/537.36");
        myWebView.loadUrl("https://m.youtube.com");
    }

    private class MyWebViewClient extends WebViewClient {
//        @Override
//        public boolean shouldOverrideUrlLoading(WebView view, String url) {
//            // Can use this to load another activity
//            view.loadUrl(url);
//            return true;
//        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Prevent loading external browser. It will request custom view vi WebChromeClient
            return false;
        }

//        @Override
//        public void onPageFinished(WebView view, String url) {
//            super.onPageFinished(view, url);
//
//            Toast.makeText(FullscreenActivity.this, "Loaded",
//                    Toast.LENGTH_SHORT).show();
//        }
    }

    /**
     *
     * Using WebChromeClient allows you to handle Javascript dialogs, favicons, titles, and the progress.
     * Take a look of this example: Adding alert() support to a WebView {https://gist.github.com/sandeepyohans/ce6c5cc596f693733255c3448a896247}
     *
     * At first glance, there are too many differences WebViewClient & WebChromeClient. But, basically:
     * if you are developing a WebView that won't require too many features but rendering HTML, you can just use a WebViewClient.
     * On the other hand, if you want to (for instance) load the favicon of the page you are rendering,
     * you should use a WebChromeClient object and override the onReceivedIcon(WebView view, Bitmap icon).
     *
     * Most of the times, if you don't want to worry about those things... you can just do this:
     *
     * webView= (WebView) findViewById(R.id.webview);
     * webView.setWebChromeClient(new WebChromeClient());
     * webView.setWebViewClient(new WebViewClient());
     * webView.getSettings().setJavaScriptEnabled(true);
     * webView.loadUrl(url);
     * And your WebView will (in theory) have all features implemented (as the android native browser).
     *
     *
     * Setting your own custom WebViewClient lets you handle onPageFinished, shouldOverrideUrlLoading, etc., WebChromeClient lets you handle Javascript's alert() and other functions.
     *
     * Just make your own class, for example:
     *
     * public class MyWebChromeClient extends WebChromeClient {
     * //Handle javascript alerts:
     * Override
     * public boolean onJsAlert(WebView view, String url, String message, final android.webkit.JsResult result)
     * {
     *   Log.d("alert", message);
     *   Toast.makeText(context, message, 3000).show();
     *   result.confirm();
     *   return true;
     * };
     * ...
     * and / or
     *
     * public class MyWebViewClient extends WebViewClient {
     * //Run script on every page, similar to Greasemonkey:
     * Override
     * public void onPageFinished(WebView view, String url) {
     *         view.loadUrl("javascript:alert('hi')");
     *     }
     * ...
     *
     * Say u want to load a page containing a video into your webview. BUT the page is secured by an htaccess file.
     * So,to authenticate u need to use setWebViewClient and overide its onReceivedHttpAuthRequest() method with ur credentials.
     * Then u want to play the video.... but turns out the video doesn't play! Maybe its controlled via JS.
     * Or it needs a plugin. So, in order to make full use of the content and better JS support you ALSO need to setWebChromeClient().
     * The way I see it: setWebViewClient for basic HTTP browser stuff, setWebChromeClient for content-related operations and support.
     */
    private class MyWebChromeClient extends WebChromeClient {

        FrameLayout.LayoutParams LayoutParameters = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

        /**
         * This is only called when it want to show the fullscreen video.
         * On STB which does not support play video in screen, it will open fullscreen automatically.
         *
         * @param view the custom view which is being requested to display fullscreen video
         * @param callback the callback to control the video
         */
        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            // if a view already exists then immediately terminate the new one
            if (mCustomView != null) {
                callback.onCustomViewHidden();

                Toast.makeText(FullscreenActivity.this, "Err: Custom view existed!",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Save the requesting view & callback
            mCustomView = view;
            mCustomViewCallback = callback;

            // Set layout for the custom view which is requesting to show in fullscreen
            view.setLayoutParams(LayoutParameters);

            // Set up the user interaction to manually show or hide the system UI.
            mContentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    toggle();
                }
            });
            // Not working on newer Android as the Video player handles all action. Working on Android 4.0
            view.setOnTouchListener(mDelayHideTouchListener);

            // Trigger the initial hide() shortly after the activity has been
            // created, to briefly hint to the user that UI controls
            // are available.
            hide();

            // Add custom view to current layout and hide the webview
            myWebView.setVisibility(View.GONE);
            // Add the requesting view to custom view container and show it
            mCustomView.setVisibility(View.VISIBLE);
            mContentView.addView(mCustomView);
        }

        @Override
        public void onHideCustomView() {
            if (mCustomView != null) {
                // Dismiss the requested view
                mCustomViewCallback.onCustomViewHidden();

                // Hide the custom view.
                mCustomView.setVisibility(View.GONE);
                // Remove the custom view from its container.
                mContentView.removeView(mCustomView);
                mCustomView = null;

                // Show back the webview
                myWebView.setVisibility(View.VISIBLE);

                // Show system status bar
                show();

                // Reload webpage because somehow it doesn't finish loading when opening video in fullscreen automatically
                myWebView.reload();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mCustomView != null)
            mWebChromeClient.onHideCustomView();
        else if (myWebView.canGoBack()) {
            myWebView.goBack();
            exiting = false;
        }
        else if (!exiting) {
            exiting = true;

            Toast.makeText(FullscreenActivity.this, R.string.exiting_confirmation,
                    Toast.LENGTH_SHORT).show();
        }
        else
            super.onBackPressed();
    }

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    private final Handler mHideHandler = new Handler();

    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            boolean touched = toggle();
//            // Return false so that it would forward touch action to webview
//            return false;
            if (touched)
                view.performClick();
            return touched;
        }
    };

    private boolean toggle() {
        if (mVisible) {
//            hide();
            return false;
        } else {
            show();

            // Auto hide the UI after delay
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
        }
        return true;
    }

    private void hide() {
        // For Android 4.0
        // Hide system navigation bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mVisible = false;

//        // Note that some of these constants are new as of API 16 (Jelly Bean)
//        // and API 19 (KitKat). It is safe to use them, as they are inlined
//        // at compile-time and do nothing on earlier devices.
//        if (mCustomViewContainer != null)
//            mCustomViewContainer.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
//                | View.SYSTEM_UI_FLAG_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private void show() {
        // Don't use this as the System UI will overlap the content view
//        // Show the system bar
//        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        // For Android 4.0
        // Show system navigation bar
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mVisible = true;
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
