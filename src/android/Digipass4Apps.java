/**
 */
package com.vasco.digipass4apps;

import android.content.Context;

import org.apache.cordova.*;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import com.vasco.digipass.sdk.DigipassSDK;
import com.vasco.digipass.sdk.DigipassSDKConstants;
import com.vasco.digipass.sdk.DigipassSDKReturnCodes;
import com.vasco.digipass.sdk.responses.ActivationResponse;
import com.vasco.digipass.sdk.responses.GenerationResponse;
import com.vasco.digipass.sdk.responses.SecureChannelParseResponse;
import com.vasco.digipass.sdk.responses.MultiDeviceLicenseActivationResponse;

import com.vasco.digipass.sdk.utils.securestorage.SecureStorageSDK;
import com.vasco.digipass.sdk.utils.securestorage.SecureStorageSDKErrorCodes;
import com.vasco.digipass.sdk.utils.securestorage.SecureStorageSDKException;

import com.vasco.digipass.sdk.utils.biometricsensor.BiometricSensorSDK;
import com.vasco.digipass.sdk.utils.biometricsensor.BiometricSensorSDKDialogParams;
import com.vasco.digipass.sdk.utils.biometricsensor.BiometricSensorSDKErrorCodes;
import com.vasco.digipass.sdk.utils.biometricsensor.BiometricSensorSDKException;

import com.vasco.digipass.sdk.utils.devicebinding.DeviceBindingSDK;
import com.vasco.digipass.sdk.utils.devicebinding.DeviceBindingSDKErrorCodes;
import com.vasco.digipass.sdk.utils.devicebinding.DeviceBindingSDKException;

import com.vasco.message.client.SecureMessagingSDKClient;
import com.vasco.message.client.TransactionData;
import com.vasco.message.exception.SecureMessagingSDKException;
import com.vasco.message.model.FormattedText;
import com.vasco.message.model.KeyValue;

import android.util.Log;
import jdk.nashorn.api.scripting.JSObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

public class Digipass4Apps extends CordovaPlugin {
  private static final String TAG = "Digipass4Apps";
  private SecureStorageSDK secureStorage;

  // m1
  private String secureChannelMessageMultiDeviceLicenseActivation = "";
  // m2
  private String secureChannelMessageMultiDeviceInstanceActivation = "";

  private String staticVector = "";
  private String platformFingerprint = "";
  private byte jailbreakStatus = DigipassSDKConstants.JAILBREAK_STATUS_NA;
  private long clientServerTimeShift = 0;
  private String password = "myPass";

  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);

    Log.d(TAG, "Inicializando Digipass4Apps");
  }

  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

    Context context = this.cordova.getActivity().getApplicationContext();

    if (action.equals("deviceFingerprint")) {

      JSONObject respuesta = new JSONObject();
      respuesta = getDeviceFingerprint(callbackContext);

      callbackContext.success(respuesta);

    } else if (action.equals("licenseActivation")) {

      JSONObject respuesta = new JSONObject();

      JSONObject obj = args.optJSONObject(0);
      if (obj != null) {
        secureChannelMessageMultiDeviceInstanceActivation = obj.optString("m2");
      } else {
        callbackContext.error("Falló el tema del args de la instancia");
        return true;
      }

      initiateSecureStorage(callbackContext);

      SecureChannelParseResponse secureChannelParseResponse = DigipassSDK
          .parseSecureChannelMessage(secureChannelMessageMultiDeviceLicenseActivation);

      MultiDeviceLicenseActivationResponse multiDeviceActivateLicenseResponse = DigipassSDK.multiDeviceActivateLicense(
          secureChannelParseResponse.getMessage(), staticVector, platformFingerprint, jailbreakStatus,
          clientServerTimeShift);

      if (true) {
        // secureHardwareResultTV.setText(getResources().getString(secureHardwareResultTextID));

        // --------------------------------------------------------------------------------------
        // Step 5 => Parse a secure channel message related to a multi-device instance
        // activation
        // --------------------------------------------------------------------------------------

        secureChannelParseResponse = DigipassSDK
            .parseSecureChannelMessage(secureChannelMessageMultiDeviceInstanceActivation);

        if (secureChannelParseResponse.getReturnCode() != DigipassSDKReturnCodes.SUCCESS) {
          callbackContext
              .error("Parse secure channel message 1 FAILED - [ " + secureChannelParseResponse.getReturnCode() + ": "
                  + DigipassSDK.getMessageForReturnCode(secureChannelParseResponse.getReturnCode()) + " ]");
        } else {
          try {
            respuesta.put("parsedMessageInstance", "[DIGIPASS]Parse secure channel message SUCCEEDED\n");
          } catch (JSONException e) {
            Log.d(TAG, "This should never happen");
          }
        }

        // --------------------------------------------------------------------------------------
        // Step 6 => Second step of the multi-device activation process. Instance
        // activation.
        // --------------------------------------------------------------------------------------

        ActivationResponse activationResponse = DigipassSDK.multiDeviceActivateInstance(
            multiDeviceActivateLicenseResponse.getStaticVector(), multiDeviceActivateLicenseResponse.getDynamicVector(),
            secureChannelParseResponse.getMessage(), password, platformFingerprint);

        if (activationResponse.getReturnCode() != DigipassSDKReturnCodes.SUCCESS) {
          callbackContext.error("Multi-device instance activation FAILED 2 - [ " + activationResponse.getReturnCode()
              + ": " + DigipassSDK.getMessageForReturnCode(activationResponse.getReturnCode()) + " ]\n");
        } else {
          try {
            respuesta.put("instanceActivated", "Multi-device instance activation SUCCEEDED");
          } catch (JSONException e) {
            Log.d(TAG, "This should never happen");
          }
        }

        // --------------------------------------------------------------------------------------
        // Step 7 => Store the dynamic vector in local storage
        // !! Retrieve it from the activationResponse object !!

        // volver a guardar la informacion en secure storage
        // --------------------------------------------------------------------------------------

        byte[] dynamicVectorByte = activationResponse.getDynamicVector();
        byte[] staticVectorByte = activationResponse.getStaticVector();

        try {
          // Put the key/value pair in the secure storage
          if (secureStorage != null) {
            secureStorage.putBytes("sv", staticVectorByte);
            secureStorage.putBytes("dv", dynamicVectorByte);
            try {
              respuesta.put("addData", "[SecureStorageSDK] onAddDataClicked SUCCESS");
            } catch (JSONException e) {
              Log.d(TAG, "This should never happen");
            }
          }
        } catch (SecureStorageSDKException e) {
          callbackContext.error("[SecureStorageSDK] onAddDataClicked FAILED to put data");

        }

        GenerationResponse generationResponse = DigipassSDK.generateSignatureFromSecureChannelMessage(
            multiDeviceActivateLicenseResponse.getStaticVector(), activationResponse.getDynamicVector(),
            secureChannelParseResponse.getMessage(), password, clientServerTimeShift,
            DigipassSDKConstants.CRYPTO_APPLICATION_INDEX_APP_2, platformFingerprint);

        if (generationResponse.getReturnCode() != DigipassSDKReturnCodes.SUCCESS) {
          callbackContext
              .error("Generate signature from secure channel message FAILED - [ " + generationResponse.getReturnCode()
                  + ": " + DigipassSDK.getMessageForReturnCode(generationResponse.getReturnCode()) + " ]");
        } else {
          try {
            respuesta.put("signSuccess", "Generate signature from secure channel message SUCCEEDED");
            respuesta.put("signature", generationResponse.getResponse());
          } catch (JSONException e) {
            Log.d(TAG, "This should never happen");
          }
        }

        dynamicVectorByte = generationResponse.getDynamicVector();

        try {
          // Put the key/value pair in the secure storage
          if (secureStorage != null) {
            secureStorage.putBytes("dv", dynamicVectorByte);
            secureStorage.write(getStorageFingerprint(), getIterationNumber(), context);
            try {
              respuesta.put("addDynamic", "[SecureStorageSDK] Dynamic vector added SUCCESS");
            } catch (JSONException e) {
              Log.d(TAG, "This should never happen");
            }

          }
        } catch (SecureStorageSDKException e) {
          callbackContext.error("[SecureStorageSDK] onAddDataClicked FAILED to put data");

        }

      } else {
        callbackContext.error("[SecureStorageSDK] The SDK Secure Storage indicate that System IS NOT SECURE");

      }

      callbackContext.success(respuesta);
    }

    else if (action.equals("getDeviceCode")) {

      JSONObject respuesta = new JSONObject();

      respuesta = getDeviceCode(args, callbackContext);

      callbackContext.success(respuesta);

    }

    else if (action.equals("saveInSecureStorage")) {

      JSONObject respuesta = new JSONObject();
      respuesta = saveInSecureStorage(args, callbackContext);

      callbackContext.success(respuesta);

    }

    else if (action.equals("getFromSecureStorage")) {

      JSONObject respuesta = new JSONObject();
      respuesta = getFromSecureStorage(args, callbackContext);

      callbackContext.success(respuesta);

    }

    else if (action.equals("deleteSecureStorage")) {

      JSONObject respuesta = new JSONObject();
      respuesta = deleteSecureStorage(callbackContext);

      callbackContext.success(respuesta);

    }

    else if (action.equals("generateOTP")) {

      JSONObject respuesta = new JSONObject();
      respuesta = getDeviceFingerprint(callbackContext);

      initiateSecureStorage(callbackContext);

      byte[] dynamicVectorByte = {};
      byte[] staticVectorByte = {};

      if (true) {

        try {
          // Put the key/value pair in the secure storage
          if (secureStorage != null) {

            dynamicVectorByte = secureStorage.getBytes("dv");
            staticVectorByte = secureStorage.getBytes("sv");
            secureStorage.write(getStorageFingerprint(), getIterationNumber(), context);
            try {
              respuesta.put("getData", "[SecureStorageSDK] getData SUCCESS");
            } catch (JSONException e) {
              Log.d(TAG, "This should never happen");
            }
          }
        } catch (SecureStorageSDKException e) {
          callbackContext.error("[SecureStorageSDK] FAILED to get data");

        }

        GenerationResponse generateResponse = DigipassSDK.generateResponseOnly(staticVectorByte, dynamicVectorByte,
            password, clientServerTimeShift, DigipassSDKConstants.CRYPTO_APPLICATION_INDEX_APP_1, platformFingerprint);

        if (generateResponse.getReturnCode() != DigipassSDKReturnCodes.SUCCESS) {
          callbackContext.error("The password generation has FAILED - [ " + generateResponse.getReturnCode() + ": "
              + DigipassSDK.getMessageForReturnCode(generateResponse.getReturnCode()) + " ]");
        } else {
          try {
            respuesta.put("otpSuccess", "The password generation has SUCCEEDED");
            respuesta.put("otpCode", generateResponse.getResponse());
          } catch (JSONException e) {
            Log.d(TAG, "This should never happen");
          }
        }

        // --------------------------------------------------------------------------------------
        // Step 7 => Store the dynamic vector in local storage
        // !! Retrieve it from the activationResponse object !!

        // volver a guardar la informacion en secure storage
        // --------------------------------------------------------------------------------------

        dynamicVectorByte = generateResponse.getDynamicVector();

        try {
          // Put the key/value pair in the secure storage
          if (secureStorage != null) {

            secureStorage.putBytes("dv", dynamicVectorByte);
            secureStorage.write(getStorageFingerprint(), getIterationNumber(), context);
            try {
              respuesta.put("addData", "[SecureStorageSDK] onAddDataClicked SUCCESS");
            } catch (JSONException e) {
              Log.d(TAG, "This should never happen");
            }
          }
        } catch (SecureStorageSDKException e) {
          callbackContext.error("[SecureStorageSDK] onAddDataClicked FAILED to put data");

        }

      } else {
        callbackContext.error("[SecureStorageSDK] The SDK Secure Storage indicate that System IS NOT SECURE");

      }

      callbackContext.success(respuesta);

    }

    return true;

  }

  /**
   * First step for the license activation This is used also for the getDeviceCode
   * and generateOTP
   * 
   * @return Respuesta with the platformFingerprint
   */
  private JSONObject getDeviceFingerprint(final CallbackContext callbackContext) throws JSONException {

    Context context = this.cordova.getActivity().getApplicationContext();
    JSONObject respuesta = new JSONObject();
    try {
      // Get fingerprint hash value
      platformFingerprint = DeviceBindingSDK.getDeviceFingerprint("", context);

      try {
        respuesta.put("deviceFingerprint", "Device Fingerprint:" + platformFingerprint);
      } catch (JSONException e) {
        Log.d(TAG, "This should never happen");
      }

      return respuesta;

    } catch (DeviceBindingSDKException e) {
      // Display error
      switch (e.getErrorCode()) {
      case DeviceBindingSDKErrorCodes.CONTEXT_NULL:
        Log.e(TAG, "Provided context is null");
        callbackContext.error("Provided context is null");
        break;
      case DeviceBindingSDKErrorCodes.INTERNAL_ERROR:
        Log.e(TAG, "Internal error", e);
        callbackContext.error("Internal error");
        break;
      case DeviceBindingSDKErrorCodes.PERMISSION_DENIED:
        Log.e(TAG, "Permission denied");
        callbackContext.error("Permission denied");
        break;
      }
    }
    return respuesta;
  }

  /**
   * Activates the license and generates a Device code from the static vector and
   * the platform fingerprint
   * 
   * @return respuesta with the platformFingerprint and the result of the lincense
   *         activation
   */
  private JSONObject getDeviceCode(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Context context = this.cordova.getActivity().getApplicationContext();
    JSONObject respuesta = new JSONObject();
    respuesta = getDeviceFingerprint(callbackContext);
    JSONObject obj = args.optJSONObject(0);
    if (obj != null) {
      secureChannelMessageMultiDeviceLicenseActivation = obj.optString("m1");
      staticVector = obj.optString("staticVector");
    } else {
      callbackContext.error("Falló el tema del args");
      return respuesta;
    }
    SecureChannelParseResponse secureChannelParseResponse = DigipassSDK
        .parseSecureChannelMessage(secureChannelMessageMultiDeviceLicenseActivation);

    if (secureChannelParseResponse.getReturnCode() != DigipassSDKReturnCodes.SUCCESS) {
      callbackContext.error("Parse secure channel message FAILED - [ " + secureChannelParseResponse.getReturnCode()
          + ": " + DigipassSDK.getMessageForReturnCode(secureChannelParseResponse.getReturnCode()) + " ]");
    } else {

      try {
        respuesta.put("parsedMessageLicense", "Parse secure channel message SUCCEEDED");
      } catch (JSONException e) {
        Log.d(TAG, "This should never happen");
      }
    }

    MultiDeviceLicenseActivationResponse multiDeviceActivateLicenseResponse = DigipassSDK.multiDeviceActivateLicense(
        secureChannelParseResponse.getMessage(), staticVector, platformFingerprint, jailbreakStatus,
        clientServerTimeShift);

    if (multiDeviceActivateLicenseResponse.getReturnCode() != DigipassSDKReturnCodes.SUCCESS) {
      callbackContext.error(
          "Multi-device license activation FAILED +++++- [ " + multiDeviceActivateLicenseResponse.getReturnCode() + ": "
              + DigipassSDK.getMessageForReturnCode(multiDeviceActivateLicenseResponse.getReturnCode()) + " ]\n");
    } else {

      try {
        respuesta.put("licenseActivation", "Multi-device license activation SUCCEEDED");
        respuesta.put("generatedCode", multiDeviceActivateLicenseResponse.getDeviceCode());
      } catch (JSONException e) {
        Log.d(TAG, "This should never happen");
      }
    }

    return respuesta;
  }

  /**
   * Activates the license and generates a Device code from the static vector and
   * the platform fingerprint
   * 
   * @return respuesta with the platformFingerprint and the result of the lincense
   *         activation
   */
  private void initiateSecureStorage(final CallbackContext callbackContext) throws JSONException {
    Context context = this.cordova.getActivity().getApplicationContext();

    try {
      // Initialize a secure storage
      try {
        secureStorage = SecureStorageSDK.init("IDC001234rSwgeSoigh8238", getStorageFingerprint(), getIterationNumber(),
            context);
      } catch (SecureStorageSDKException e) {
        e.printStackTrace();
      }

    } catch (Error e) {

      callbackContext.error("[SecureStorageSDK] Failed to initialize Secure Storage FAILED");
    }

  }

  /**
   * Saves information in the secure storage
   * 
   * @return respuesta with the success of the secureStorage
   */
  private JSONObject saveInSecureStorage(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Context context = this.cordova.getActivity().getApplicationContext();
    JSONObject respuesta = new JSONObject();
    String key = "";
    String value = "";

    JSONObject obj = args.optJSONObject(0);
    if (obj != null) {
      key = obj.optString("key");
      value = obj.optString("value");
    } else {
      callbackContext.error("Falló el tema del args saveInSecureStorage");
      return respuesta;
    }

    initiateSecureStorage(callbackContext);

    if (secureStorage != null) {

      try {
        secureStorage.putString(key, value);
        secureStorage.write(getStorageFingerprint(), getIterationNumber(), context);
      } catch (SecureStorageSDKException e) {
        callbackContext.error("Failed to store data");
      }
      try {
        respuesta.put("stored", "[SecureStorageSDK] data saved SUCCESSFULLY");
      } catch (JSONException e) {
        Log.d(TAG, "This should never happen");
      }

    }

    return respuesta;

  }


  /**
   * Saves information in the secure storage
   * 
   * @return respuesta with the success of the secureStorage
   */
  private JSONObject getFromSecureStorage(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Context context = this.cordova.getActivity().getApplicationContext();
    JSONObject respuesta = new JSONObject();
    String key = "";

    JSONObject obj = args.optJSONObject(0);
    if (obj != null) {
      key = obj.optString("key");
    } else {
      callbackContext.error("Falló el tema del args getFromSecureStorage");
      return respuesta;
    }

    initiateSecureStorage(callbackContext);

    if (secureStorage != null) {

      try {
        respuesta.put("resultado", secureStorage.getString(key));
        secureStorage.write(getStorageFingerprint(), getIterationNumber(), context);
      } catch (SecureStorageSDKException e) {
        callbackContext.error("Failed to get data");
      }
      try {
        respuesta.put("stored", "[SecureStorageSDK] data got SUCCESSFULLY");
      } catch (JSONException e) {
        Log.d(TAG, "This should never happen");
      }
    }

    return respuesta;

  }

   /**
   * Saves information in the secure storage
   * 
   * @return respuesta with the success of the secureStorage
   */
  private JSONObject deleteSecureStorage(final CallbackContext callbackContext)
      throws JSONException {
    Context context = this.cordova.getActivity().getApplicationContext();
    JSONObject respuesta = new JSONObject();
    
    initiateSecureStorage(callbackContext);

    if (secureStorage != null) {

      try {
        secureStorage.delete("IDC001234rSwgeSoigh8238", context);
      } catch (SecureStorageSDKException e) {
        callbackContext.error("Failed to remove data");
      }
      try {
        respuesta.put("stored", "[SecureStorageSDK] key removed SUCCESSFULLY");
      } catch (JSONException e) {
        Log.d(TAG, "This should never happen");
      }
    }

    return respuesta;

  }

  /**
   * SECURE STORAGE FUNCTION Return the recommended iteration number to support
   * old Android devices. If you don't need the support of old devices the
   * recommended iteration number is 8000.
   *
   * @return The recommended iteration number to support old Android devices.
   */
  private int getIterationNumber() {
    return 300;
  }

  /**
   * SECURE STORAGE FUNCTION Return the storage fingerprint. The storage
   * fingerprint must be a unique identifier generated by the DeviceBindingSDK
   *
   * @return the storage fingerprint as String.
   * @see DeviceBindingSDK#getDeviceFingerprint
   */
  private String getStorageFingerprint() {
    return platformFingerprint;
  }

}
