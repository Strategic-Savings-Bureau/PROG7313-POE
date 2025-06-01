package com.ssba.strategic_savings_budget_app.helpers

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.ssba.strategic_savings_budget_app.AuthActivity
import com.ssba.strategic_savings_budget_app.interfaces.BiometricAuthListener

/**
 * Utility object for handling biometric authentication logic.
 *
 * This helper provides functions to:
 * - Check if the device supports biometric authentication.
 * - Display a biometric prompt with customizable UI.
 * - Handle biometric authentication callbacks through a listener interface.
 *
 * Code Attribution:
 * Author: Saqib Ahmed
 * Source: [Biometric Authentication in Android Kotlin – Medium](https://saqibvnb.medium.com/biometric-authentication-in-android-kotlin-2178cd227afb)
 * Accessed: 1 June 2025
 */
object BiometricUtils
{

    /**
     * Checks whether the device supports biometric authentication
     * using strong biometrics or device credentials.
     *
     * @param context The context used to retrieve the BiometricManager.
     * @return One of the BiometricManager status codes indicating support.
     */
    private fun hasBiometricCapability(context: Context): Int
    {
        return BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
    }

    /**
     * Determines if the device is ready for biometric authentication.
     *
     * @param context The current context.
     * @return `true` if biometric authentication is available and configured, `false` otherwise.
     */
    fun isBiometricReady(context: Context): Boolean =
        hasBiometricCapability(context) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Builds and returns a [BiometricPrompt.PromptInfo] instance with provided UI content.
     *
     * @param title Title displayed in the prompt.
     * @param description Additional info displayed in the dialog.
     * @return A configured [BiometricPrompt.PromptInfo] object.
     */
    private fun setBiometricPromptInfo(title: String, description: String): BiometricPrompt.PromptInfo
    {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setDescription(description)
            .setNegativeButtonText("Use account")
            .build()
    }

    /**
     * Initializes and returns a [BiometricPrompt] with authentication callbacks.
     *
     * @param activity The hosting [AppCompatActivity].
     * @param listener A listener to handle biometric authentication events.
     * @return A configured [BiometricPrompt] instance.
     */
    private fun initBiometricPrompt(activity: AppCompatActivity, listener: BiometricAuthListener): BiometricPrompt
    {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback()
        {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence)
            {
                super.onAuthenticationError(errorCode, errString)
                listener.onBiometricAuthenticateError(errorCode, errString.toString())
            }

            override fun onAuthenticationFailed()
            {
                super.onAuthenticationFailed()

                // Optional: You may notify user here if needed.
                Toast.makeText(activity, "Authentication failed", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult)
            {
                super.onAuthenticationSucceeded(result)
                listener.onBiometricAuthenticateSuccess(result)
            }
        }

        return BiometricPrompt(activity, executor, callback)
    }

    /**
     * Displays the biometric prompt dialog to the user.
     *
     * @param title The title for the biometric dialog.
     * @param description Additional description text in the prompt.
     * @param activity The current activity context.
     * @param listener A callback interface to handle authentication outcomes.
     * @param cryptoObject Optional cryptographic object for securing authentication.
     */
    fun showBiometricPrompt(
        title: String = "Biometric Authentication",
        description: String = "Use biometrics to login",
        activity: AppCompatActivity,
        listener: AuthActivity,
        cryptoObject: BiometricPrompt.CryptoObject? = null,
    )
    {
        val promptInfo = setBiometricPromptInfo(title, description)

        val biometricPrompt = initBiometricPrompt(activity, listener)

        biometricPrompt.apply {
            if (cryptoObject == null) authenticate(promptInfo)
            else authenticate(promptInfo, cryptoObject)
        }
    }
}