package com.fasal.otpreader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

/**
 * Broadcast receiver for SMS User Consent API
 * Handles incoming SMS messages containing OTP
 */
public class SMSBroadcastReceiver extends BroadcastReceiver {
    
    private static final String TAG = "SMSBroadcastReceiver";
    private OTPReader otpReader;
    
    public SMSBroadcastReceiver(OTPReader otpReader) {
        this.otpReader = otpReader;
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Broadcast received with action: " + intent.getAction());
        
        if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            if (extras == null) {
                Log.w(TAG, "No extras in SMS_RETRIEVED_ACTION intent");
                return;
            }
            
            Log.d(TAG, "SMS_RETRIEVED_ACTION received with extras");
            
            Status smsRetrieverStatus = (Status) extras.get(SmsRetriever.EXTRA_STATUS);
            if (smsRetrieverStatus == null) {
                Log.w(TAG, "No status in SMS_RETRIEVED_ACTION intent");
                return;
            }
            
            Log.d(TAG, "SMS Retriever Status Code: " + smsRetrieverStatus.getStatusCode());
            
            switch (smsRetrieverStatus.getStatusCode()) {
                case CommonStatusCodes.SUCCESS:
                    Log.d(TAG, "SMS retrieval successful");
                    
                    // Get consent intent to show user consent dialog
                    Intent consentIntent = extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT);
                    if (consentIntent != null) {
                        // Start activity to show consent dialog to user
                        // Activity must be started within 5 minutes, otherwise TIMEOUT will occur
                        otpReader.handleSMSConsent(consentIntent);
                    } else {
                        Log.e(TAG, "No consent intent found in successful SMS retrieval");
                    }
                    break;
                    
                case CommonStatusCodes.TIMEOUT:
                    Log.w(TAG, "SMS retrieval timeout occurred");
                    otpReader.handleSMSTimeout();
                    break;
                    
                default:
                    Log.e(TAG, "SMS retrieval failed with status: " + smsRetrieverStatus.getStatusCode());
                    Log.e(TAG, "Status message: " + smsRetrieverStatus.getStatusMessage());
                    break;
            }
        } else {
            Log.d(TAG, "Received broadcast with different action: " + intent.getAction());
        }
    }
}
