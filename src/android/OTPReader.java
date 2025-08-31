package org.apache.cordova.otpreader;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

/**
 * OTP Reader Cordova Plugin
 * Uses Android SMS User Consent API for automatic OTP reading
 */
public class OTPReader extends CordovaPlugin {
    
    private static final String TAG = "OTPReader";
    private static final int SMS_CONSENT_REQUEST = 2;
    
    private CallbackContext otpCallbackContext;
    private SMSBroadcastReceiver smsReceiver;
    private boolean isListening = false;
    
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        
        if ("startListening".equals(action)) {
            String senderPhoneNumber = args.isNull(0) ? null : args.getString(0);
            this.startListening(senderPhoneNumber, callbackContext);
            return true;
        }
        
        if ("stopListening".equals(action)) {
            this.stopListening(callbackContext);
            return true;
        }
        
        if ("getDebugInfo".equals(action)) {
            this.getDebugInfo(callbackContext);
            return true;
        }
        
        return false;
    }
    
    /**
     * Start listening for SMS messages with OTP
     */
    private void startListening(String senderPhoneNumber, CallbackContext callbackContext) {
        if (isListening) {
            callbackContext.error("Already listening for SMS messages");
            return;
        }
        
        // Start listening directly
        this.startListeningWithPermissions(senderPhoneNumber, callbackContext);
    }
    
    /**
     * Start listening with permissions already granted
     */
    private void startListeningWithPermissions(String senderPhoneNumber, CallbackContext callbackContext) {
        
        this.otpCallbackContext = callbackContext;
        
        // Register broadcast receiver with better error handling
        try {
            smsReceiver = new SMSBroadcastReceiver(this);
            IntentFilter intentFilter = new IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION);
            
            // Handle Android 13+ (API 33) receiver export requirements
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                cordova.getActivity().registerReceiver(smsReceiver, intentFilter, SmsRetriever.SEND_PERMISSION, null, Context.RECEIVER_EXPORTED);
            } else {
                cordova.getActivity().registerReceiver(smsReceiver, intentFilter, SmsRetriever.SEND_PERMISSION, null);
            }
            
            Log.d(TAG, "SMS Broadcast Receiver registered successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to register SMS receiver", e);
            callbackContext.error("Failed to register SMS receiver: " + e.getMessage());
            return;
        }
        
        // Start SMS User Consent
        Log.d(TAG, "=== STARTING SMS USER CONSENT ===");
        Log.d(TAG, "Sender phone number parameter: " + (senderPhoneNumber != null ? "'" + senderPhoneNumber + "'" : "null (any sender)"));
        Log.d(TAG, "NOTE: SMS must be sent AFTER this point to be detected");
        
        Task<Void> task = SmsRetriever.getClient(cordova.getActivity()).startSmsUserConsent(senderPhoneNumber);
        
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                isListening = true;
                Log.d(TAG, "=== SMS USER CONSENT STARTED SUCCESSFULLY ===");
                Log.d(TAG, "Listening for SMS from: " + (senderPhoneNumber != null ? "'" + senderPhoneNumber + "'" : "any sender"));
                Log.d(TAG, "⚠️  CRITICAL: Send your OTP SMS NOW! SMS sent before this point will NOT be detected");
                Log.d(TAG, "SMS User Consent will timeout after 5 minutes if no SMS received");
                
                // Return immediate success to indicate listening started
                try {
                    JSONObject result = new JSONObject();
                    result.put("listening", true);
                    result.put("message", "Started listening for SMS. Send OTP now.");
                    result.put("senderFilter", senderPhoneNumber != null ? senderPhoneNumber : "any");
                    
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
                    pluginResult.setKeepCallback(true);
                    callbackContext.sendPluginResult(pluginResult);
                } catch (JSONException e) {
                    Log.e(TAG, "Error creating start listening result", e);
                }
            }
        });
        
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                isListening = false;
                Log.e(TAG, "=== FAILED TO START SMS USER CONSENT ===");
                Log.e(TAG, "Failed to start SMS User Consent", e);
                Log.e(TAG, "Error message: " + e.getMessage());
                callbackContext.error("Failed to start SMS listening: " + e.getMessage());
            }
        });
    }
    
    /**
     * Stop listening for SMS messages
     */
    private void stopListening(CallbackContext callbackContext) {
        if (!isListening) {
            callbackContext.error("Not currently listening for SMS messages");
            return;
        }
        
        try {
            if (smsReceiver != null) {
                cordova.getActivity().unregisterReceiver(smsReceiver);
                smsReceiver = null;
            }
            isListening = false;
            otpCallbackContext = null;
            callbackContext.success("Stopped listening for SMS messages");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping SMS listener", e);
            callbackContext.error("Error stopping SMS listener: " + e.getMessage());
        }
    }
    
    /**
     * Get debug information about the plugin state and environment
     */
    private void getDebugInfo(CallbackContext callbackContext) {
        try {
            JSONObject debugInfo = new JSONObject();
            debugInfo.put("isListening", isListening);
            debugInfo.put("hasReceiver", smsReceiver != null);
            debugInfo.put("androidVersion", Build.VERSION.SDK_INT);
            debugInfo.put("androidRelease", Build.VERSION.RELEASE);
            
            // Check Google Play Services availability
            try {
                Context context = cordova.getActivity();
                int playServicesStatus = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
                debugInfo.put("playServicesAvailable", playServicesStatus == ConnectionResult.SUCCESS);
                debugInfo.put("playServicesStatusCode", playServicesStatus);
            } catch (Exception e) {
                debugInfo.put("playServicesError", e.getMessage());
            }
            
            // Check permissions - No permissions required for SMS User Consent API
            try {
                JSONObject permissionStatus = new JSONObject();
                permissionStatus.put("note", "SMS User Consent API doesn't require any dangerous permissions");
                permissionStatus.put("RECEIVE_SMS", "Not required - uses SMS User Consent API");
                permissionStatus.put("READ_PHONE_STATE", "Not required - getPhoneNumber() removed");
                debugInfo.put("permissions", permissionStatus);
            } catch (Exception e) {
                debugInfo.put("permissionError", e.getMessage());
            }
            
            callbackContext.success(debugInfo);
        } catch (Exception e) {
            Log.e(TAG, "Error getting debug info", e);
            callbackContext.error("Error getting debug info: " + e.getMessage());
        }
    }
    
    /**
     * Handle consent dialog for SMS reading
     */
    public void handleSMSConsent(Intent consentIntent) {
        try {
            cordova.startActivityForResult(this, consentIntent, SMS_CONSENT_REQUEST);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Activity not found for SMS consent", e);
            if (otpCallbackContext != null) {
                otpCallbackContext.error("SMS consent dialog could not be shown");
            }
        }
    }
    
    /**
     * Handle activity results
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == SMS_CONSENT_REQUEST) {
            handleSMSConsentResult(resultCode, data);
        }
    }
    
    /**
     * Handle SMS consent result
     */
    private void handleSMSConsentResult(int resultCode, Intent data) {
        if (otpCallbackContext == null) return;
        
        if (resultCode == Activity.RESULT_OK && data != null) {
            String message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE);
            if (message != null) {
                try {
                    JSONObject result = new JSONObject();
                    result.put("message", message);
                    result.put("success", true);
                    
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
                    pluginResult.setKeepCallback(true);
                    otpCallbackContext.sendPluginResult(pluginResult);
                } catch (JSONException e) {
                    Log.e(TAG, "Error creating JSON result", e);
                    otpCallbackContext.error("Error processing SMS message");
                }
            } else {
                otpCallbackContext.error("No SMS message received");
            }
        } else {
            // User denied consent or cancelled
            try {
                JSONObject result = new JSONObject();
                result.put("success", false);
                result.put("userCancelled", true);
                result.put("message", "User denied SMS access or cancelled");
                
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
                pluginResult.setKeepCallback(true);
                otpCallbackContext.sendPluginResult(pluginResult);
            } catch (JSONException e) {
                Log.e(TAG, "Error creating JSON result", e);
                otpCallbackContext.error("User denied SMS access");
            }
        }
    }
    
    /**
     * Handle timeout from SMS receiver
     */
    public void handleSMSTimeout() {
        if (otpCallbackContext != null) {
            try {
                JSONObject result = new JSONObject();
                result.put("success", false);
                result.put("timeout", true);
                result.put("message", "SMS listening timeout occurred");
                
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
                pluginResult.setKeepCallback(true);
                otpCallbackContext.sendPluginResult(pluginResult);
            } catch (JSONException e) {
                Log.e(TAG, "Error creating JSON result", e);
                otpCallbackContext.error("SMS listening timeout");
            }
        }
    }
    
    /**
     * Cleanup when plugin is destroyed
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isListening && smsReceiver != null) {
            try {
                cordova.getActivity().unregisterReceiver(smsReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering receiver on destroy", e);
            }
        }
        isListening = false;
        otpCallbackContext = null;
    }
}
