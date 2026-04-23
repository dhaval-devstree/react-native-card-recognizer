package com.reactnativecardrecognizer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.BaseActivityEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import lens24.camera.widget.CardDetectionStateView
import lens24.intent.Card
import lens24.intent.ScanCardCallback
import lens24.intent.ScanCardIntent
import lens24.sdk.R as Lens24R
import lens24.ui.ScanCardActivity
import lens24.ui.ScanCardRequest

class CardScannerModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    companion object {
        private const val MODULE_NAME = "CardScannerModule"
        private const val REQUEST_CODE_SCAN_CARD = 49201
        private const val DEFAULT_HINT = "Align your card inside the frame"
        private const val DEFAULT_TOOLBAR_TITLE = "Scan card"
        private const val LENS24_SCAN_REQUEST_KEY = "lens24.ui.ScanCardActivity.SCAN_CARD_REQUEST"
    }

    private var pendingPromise: Promise? = null
    private var activityResultCallback: ActivityResultCallback<ActivityResult>? = null

    private val activityEventListener: ActivityEventListener =
        object : BaseActivityEventListener() {
            override fun onActivityResult(
                activity: Activity, requestCode: Int, resultCode: Int, data: Intent?
            ) {
                if (requestCode != REQUEST_CODE_SCAN_CARD) return

                val callback = activityResultCallback
                if (callback == null) {
                    pendingPromise?.reject("E_NO_CALLBACK", "Card scan callback not set")
                    clearPending()
                    return
                }
                callback.onActivityResult(ActivityResult(resultCode, data))
            }
        }

    init {
        reactContext.addActivityEventListener(activityEventListener)
    }

    override fun getName(): String = MODULE_NAME

    @ReactMethod
    fun scanCard(promise: Promise) {
        scanCardWithOptions(null, promise)
    }

    @ReactMethod
    fun scanCardWithOptions(options: ReadableMap?, promise: Promise) {
        val activity: Activity = reactContext.currentActivity ?: run {
            promise.reject("E_NO_ACTIVITY", "No current Activity. Is the app in foreground?")
            return
        }
        if (pendingPromise != null) {
            promise.reject("E_IN_PROGRESS", "Card scan already in progress")
            return
        }

        pendingPromise = promise

        activityResultCallback = ScanCardCallback.Builder().setOnSuccess { card: Card, _ ->
            val result = Arguments.createMap().apply {
                putString("cardNumber", card.cardNumber)
                putString("cardNumberRedacted", card.cardNumberRedacted)
                putString("cardHolderName", card.cardHolderName)
                putString("expirationDate", card.expirationDate)
            }
            pendingPromise?.resolve(result)
            clearPending()
        }.setOnBackPressed {
            pendingPromise?.reject("E_CANCELED", "Card scan canceled (back pressed)")
            clearPending()
        }.setOnManualInput {
            pendingPromise?.reject("E_CANCELED", "Card scan canceled (manual input)")
            clearPending()
        }.setOnError {
            pendingPromise?.reject("E_SCAN_FAILED", "Card scan failed")
            clearPending()
        }.build()

        val androidOptions =
            if (options != null && options.hasKey("android") && !options.isNull("android")) {
                options.getMap("android")
            } else {
                options
            }
        val hint = androidOptions.getNonBlankString("hint") ?: DEFAULT_HINT
        val toolbarTitle = androidOptions.getNonBlankString("toolbarTitle") ?: DEFAULT_TOOLBAR_TITLE
        val manualInputButtonText = androidOptions.getNonBlankString("manualInputButtonText")
        val cardFrameColor = try {
            androidOptions.getOptionalAndroidMainColor(activity)
                ?: options.getOptionalAndroidMainColor(activity) // allow cross-platform root-level shortcut
        } catch (e: IllegalArgumentException) {
            pendingPromise?.reject("E_BAD_OPTIONS", e.message, e)
            clearPending()
            return
        }

        // Lens24 uses a static color inside CardDetectionStateView to tint the card frame.
        // Some Lens24 flows don't apply ScanCardRequest.cardFrameColor to the frame, so set it proactively.
        if (cardFrameColor != null) {
            try {
                CardDetectionStateView(activity).setMainColor(activity, cardFrameColor)
            } catch (_: Throwable) {
                // Best-effort: don't block scanning if the SDK internals change.
            }
        }

        val intent = if (cardFrameColor != null || manualInputButtonText != null) {
            Intent(activity, ScanCardActivity::class.java).apply {
                val resolvedMainColor =
                    cardFrameColor ?: ContextCompat.getColor(activity, Lens24R.color.lens24_primary_color)
                val request = ScanCardRequest(
                    /* enableVibration */ true,
                    /* scanExpirationDate */ true,
                    /* scanCardHolder */ true,
                    /* grabCardImage */ false,
                    /* hint */ hint,
                    /* title */ toolbarTitle,
                    /* manualInputButtonLabel */ manualInputButtonText,
                    /* cardFrameColor */ resolvedMainColor,
                    /* bottomHint */ null
                )
                putExtra(LENS24_SCAN_REQUEST_KEY, request)
            }
        } else {
            ScanCardIntent.Builder(activity).setScanCardHolder(true).setScanExpirationDate(true)
                .setSaveCard(false).setVibrationEnabled(true).setHint(hint)
                .setToolbarTitle(toolbarTitle).build()
        }

        try {
            activity.startActivityForResult(intent, REQUEST_CODE_SCAN_CARD)
        } catch (e: Exception) {
            pendingPromise?.reject("E_START_FAILED", e.message, e)
            clearPending()
        }
    }

    private fun clearPending() {
        pendingPromise = null
        activityResultCallback = null
    }

    private fun ReadableMap?.getNonBlankString(key: String): String? {
        if (this == null) return null
        if (!this.hasKey(key) || this.isNull(key)) return null
        val value = this.getString(key) ?: return null
        val trimmed = value.trim()
        return trimmed.ifEmpty { null }
    }

    private fun ReadableMap?.getOptionalAndroidMainColor(context: Context): Int? {
        if (this == null) return null
        val key = when {
            this.hasKey("cardFrameColor") && !this.isNull("cardFrameColor") -> "cardFrameColor"
            this.hasKey("mainColor") && !this.isNull("mainColor") -> "mainColor" // backward-compatible alias
            else -> return null
        }

        return when (this.getType(key)) {
            com.facebook.react.bridge.ReadableType.String -> {
                val raw = this.getString(key)?.trim().orEmpty()
                if (raw.isEmpty()) {
                    throw IllegalArgumentException("android.cardFrameColor must be a non-empty string")
                }
                if (raw.startsWith("#") || raw.startsWith("rgb", ignoreCase = true)) {
                    try {
                        Color.parseColor(raw)
                    } catch (e: IllegalArgumentException) {
                        throw IllegalArgumentException("android.cardFrameColor must be a valid color string (e.g. \"#0A84FF\")")
                    }
                } else {
                    val name = raw
                        .removePrefix("@color/")
                        .removePrefix("color/")
                        .removePrefix("R.color.")
                        .trim()
                    if (name.isEmpty()) {
                        throw IllegalArgumentException("android.cardFrameColor must be a valid @color resource name (e.g. \"@color/primary_color_dark\")")
                    }
                    val resId = context.resources.getIdentifier(name, "color", context.packageName)
                    if (resId == 0) {
                        throw IllegalArgumentException("android.cardFrameColor references unknown color resource: \"$raw\"")
                    }
                    ContextCompat.getColor(context, resId)
                }
            }

            com.facebook.react.bridge.ReadableType.Number -> {
                // React Native may pass numbers as Double; accept both Int and Double.
                val value = try {
                    this.getInt(key)
                } catch (_: Throwable) {
                    this.getDouble(key).toLong().toInt()
                }
                value
            }

            else -> {
                throw IllegalArgumentException("android.cardFrameColor must be a string or number")
            }
        }
    }
}
