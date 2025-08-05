// OTP Verification Template Events and Helpers
Template.otpVerification.onCreated(function() {
  this.otpCode = new ReactiveVar('');
  this.isVerifying = new ReactiveVar(false);
  this.isListening = new ReactiveVar(false);
  this.errorMessage = new ReactiveVar('');
});

Template.otpVerification.helpers({
  otpCode: function() {
    return Template.instance().otpCode.get();
  },
  
  isVerifying: function() {
    return Template.instance().isVerifying.get();
  },
  
  isListening: function() {
    return Template.instance().isListening.get();
  },
  
  errorMessage: function() {
    return Template.instance().errorMessage.get();
  },
  
  phoneNumber: function() {
    return Session.get('phoneNumber') || '+1 (555) ***-****';
  }
});

Template.otpVerification.events({
  'input #otpInput': function(event, template) {
    const value = event.target.value.replace(/\D/g, '').substring(0, 6);
    event.target.value = value;
    template.otpCode.set(value);
    template.errorMessage.set('');
  },
  
  'click #autoFillOTP': function(event, template) {
    if (!Meteor.isCordova) {
      template.errorMessage.set('Auto-fill only available on mobile devices');
      return;
    }
    
    template.isListening.set(true);
    template.errorMessage.set('');
    
    // Start listening for OTP SMS
    cordova.plugins.OTPReader.startListening(
      null, // No specific sender filter
      function(result) {
        console.log('OTP Reader Result:', result);
        
        if (result.success && result.message) {
          // Extract OTP from message
          const otp = cordova.plugins.OTPReader.extractOTP(result.message, 6);
          
          if (otp) {
            template.otpCode.set(otp);
            template.isListening.set(false);
            
            // Auto-verify if OTP is complete
            if (otp.length === 6) {
              template.find('#verifyOTP').click();
            }
          } else {
            template.errorMessage.set('Could not extract OTP from SMS. Please enter manually.');
          }
        } else if (result.userCancelled) {
          template.errorMessage.set('SMS access was cancelled. Please enter OTP manually.');
          template.isListening.set(false);
        } else if (result.timeout) {
          template.errorMessage.set('SMS listening timeout. Please try again or enter manually.');
          template.isListening.set(false);
        }
      },
      function(error) {
        console.error('OTP Reader Error:', error);
        template.errorMessage.set('Error reading SMS: ' + error);
        template.isListening.set(false);
      }
    );
  },
  
  'click #stopListening': function(event, template) {
    if (Meteor.isCordova) {
      cordova.plugins.OTPReader.stopListening(
        function() {
          template.isListening.set(false);
        },
        function(error) {
          console.error('Error stopping OTP listener:', error);
        }
      );
    }
  },
  
  'click #verifyOTP': function(event, template) {
    const otp = template.otpCode.get();
    
    if (!otp || otp.length !== 6) {
      template.errorMessage.set('Please enter a valid 6-digit code');
      return;
    }
    
    template.isVerifying.set(true);
    template.errorMessage.set('');
    
    // Call server method to verify OTP
    Meteor.call('verifyOTP', {
      phoneNumber: Session.get('phoneNumber'),
      otp: otp
    }, function(error, result) {
      template.isVerifying.set(false);
      
      if (error) {
        console.error('OTP verification error:', error);
        template.errorMessage.set(error.reason || 'Verification failed. Please try again.');
      } else if (result.success) {
        // OTP verified successfully
        console.log('OTP verified successfully');
        FlowRouter.go('/dashboard');
      } else {
        template.errorMessage.set(result.message || 'Invalid verification code');
      }
    });
  },
  
  'click #resendOTP': function(event, template) {
    template.errorMessage.set('');
    
    Meteor.call('sendOTP', {
      phoneNumber: Session.get('phoneNumber')
    }, function(error, result) {
      if (error) {
        template.errorMessage.set(error.reason || 'Failed to resend code');
      } else {
        // Show success message temporarily
        template.errorMessage.set('');
        // You might want to show a success toast instead
      }
    });
  }
});

// Phone Number Input Template
Template.phoneNumberInput.onCreated(function() {
  this.phoneNumber = new ReactiveVar('');
  this.isSending = new ReactiveVar(false);
  this.errorMessage = new ReactiveVar('');
});

Template.phoneNumberInput.helpers({
  phoneNumber: function() {
    return Template.instance().phoneNumber.get();
  },
  
  isSending: function() {
    return Template.instance().isSending.get();
  },
  
  errorMessage: function() {
    return Template.instance().errorMessage.get();
  }
});

Template.phoneNumberInput.events({
  'input #phoneInput': function(event, template) {
    const value = event.target.value;
    template.phoneNumber.set(value);
    template.errorMessage.set('');
  },
  
  'click #getPhoneNumber': function(event, template) {
    if (!Meteor.isCordova) {
      template.errorMessage.set('Phone number access only available on mobile devices');
      return;
    }
    
    // Get device phone number
    cordova.plugins.OTPReader.getPhoneNumber(
      function(phoneNumber) {
        console.log('Device phone number:', phoneNumber);
        template.phoneNumber.set(phoneNumber);
      },
      function(error) {
        console.error('Phone number error:', error);
        template.errorMessage.set('Could not get device phone number: ' + error);
      }
    );
  },
  
  'click #sendOTP': function(event, template) {
    const phoneNumber = template.phoneNumber.get();
    
    if (!phoneNumber || phoneNumber.length < 10) {
      template.errorMessage.set('Please enter a valid phone number');
      return;
    }
    
    template.isSending.set(true);
    template.errorMessage.set('');
    
    // Call server method to send OTP
    Meteor.call('sendOTP', {
      phoneNumber: phoneNumber
    }, function(error, result) {
      template.isSending.set(false);
      
      if (error) {
        console.error('Send OTP error:', error);
        template.errorMessage.set(error.reason || 'Failed to send verification code');
      } else if (result.success) {
        // Store phone number and navigate to OTP verification
        Session.set('phoneNumber', phoneNumber);
        FlowRouter.go('/verify-otp');
      } else {
        template.errorMessage.set(result.message || 'Failed to send verification code');
      }
    });
  }
});

// Cleanup on template destruction
Template.otpVerification.onDestroyed(function() {
  if (Meteor.isCordova) {
    cordova.plugins.OTPReader.stopListening(
      function() {
        console.log('Stopped OTP listening on template destroy');
      },
      function(error) {
        console.error('Error stopping OTP listener on destroy:', error);
      }
    );
  }
});
