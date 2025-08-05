package com.fasal.otpreader;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

/**
 * OTP Reader Cordova Plugin
 * Uses Android SMS User Consent API for automatic OTP reading
 */
public class OTPReader extends CordovaPlugin {
    
    private static final String TAG = "OTPReader";
    private static final int CREDENTIAL_PICKER_REQUEST = 1;
    private static final int SMS_CONSENT_REQUEST = 2;
    
    private CallbackContext otpCallbackContext;
    private CallbackContext phoneNumberCallbackContext;
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
        
        if ("getPhoneNumberHint".equals(action)) {
            this.getPhoneNumberHint(callbackContext);
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
        
        this.otpCallbackContext = callbackContext;
        
        // Register broadcast receiver
        smsReceiver = new SMSBroadcastReceiver(this);
        IntentFilter intentFilter = new IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION);
        cordova.getActivity().registerReceiver(smsReceiver, intentFilter, SmsRetriever.SEND_PERMISSION, null);
        
        // Start SMS User Consent
        Task<Void> task = SmsRetriever.getClient(cordova.getActivity()).startSmsUserConsent(senderPhoneNumber);
        
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                isListening = true;
                Log.d(TAG, "SMS User Consent started successfully");
                
                // Keep the callback for future use
                PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            }
        });
        
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                isListening = false;
                Log.e(TAG, "Failed to start SMS User Consent", e);
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
     * Get phone number hint from user
     */
    private void getPhoneNumberHint(CallbackContext callbackContext) {
        this.phoneNumberCallbackContext = callbackContext;
        
        HintRequest hintRequest = new HintRequest.Builder()
                .setPhoneNumberIdentifierSupported(true)
                .build();
                
        CredentialsClient credentialsClient = Credentials.getClient(cordova.getActivity());
        Intent intent = credentialsClient.getHintPickerIntent(hintRequest);
        
        try {
            cordova.startActivityForResult(this, intent, CREDENTIAL_PICKER_REQUEST);
        } catch (Exception e) {
            Log.e(TAG, "Error starting phone number hint picker", e);
            callbackContext.error("Error getting phone number hint: " + e.getMessage());
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
        
        switch (requestCode) {
            case CREDENTIAL_PICKER_REQUEST:
                handlePhoneNumberResult(resultCode, data);
                break;
                
            case SMS_CONSENT_REQUEST:
                handleSMSConsentResult(resultCode, data);
                break;
        }
    }
    
    /**
     * Handle phone number picker result
     */
    private void handlePhoneNumberResult(int resultCode, Intent data) {
        if (phoneNumberCallbackContext == null) return;
        
        if (resultCode == Activity.RESULT_OK && data != null) {
            Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
            if (credential != null) {
                String phoneNumber = credential.getId();
                phoneNumberCallbackContext.success(phoneNumber);
            } else {
                phoneNumberCallbackContext.error("No phone number selected");
            }
        } else {
            phoneNumberCallbackContext.error("Phone number selection cancelled");
        }
        
        phoneNumberCallbackContext = null;
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
        phoneNumberCallbackContext = null;
    }
}
