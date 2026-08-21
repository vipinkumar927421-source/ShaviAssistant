package com.vipin.shavi.control

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings

/**
 * Executes real, permission-gated device actions. Every method either
 * performs the action directly (where Android allows a third-party app to)
 * or opens the correct system settings screen (where Android 12+ requires
 * the user to confirm the change manually, e.g. Wi-Fi/Bluetooth toggles).
 */
class PhoneControlManager(private val context: Context) {

    data class ActionResult(val success: Boolean, val message: String)

    fun openApp(appName: String): ActionResult {
        val pm = context.packageManager
        val packageMap = mapOf(
            "whatsapp" to "com.whatsapp",
            "instagram" to "com.instagram.android",
            "gmail" to "com.google.android.gm",
            "youtube" to "com.google.android.youtube",
            "facebook" to "com.facebook.katana",
            "chrome" to "com.android.chrome",
            "google" to "com.google.android.googlequicksearchbox"
        )
        val pkg = packageMap[appName.lowercase()] ?: return ActionResult(false, "$appName supported app list me nahi hai.")
        val launchIntent = pm.getLaunchIntentForPackage(pkg)
            ?: return ActionResult(false, "$appName is device par installed nahi lag raha.")
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return ActionResult(true, "$appName khol rahi hoon!")
    }

    /** Android 13+ removed the ability to silently toggle Wi-Fi; opens the panel instead. */
    fun toggleWifi(): ActionResult {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.startActivity(Intent(Settings.Panel.ACTION_WIFI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            ActionResult(true, "Wi-Fi settings khol rahi hoon, aap confirm kar dijiye.")
        } else {
            @Suppress("DEPRECATION")
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
            ActionResult(true, "Wi-Fi toggle kar diya.")
        }
    }

    /** Bluetooth toggling also requires user confirmation on modern Android — opens system panel. */
    fun toggleBluetooth(): ActionResult {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter: BluetoothAdapter? = bluetoothManager.adapter
        if (adapter == null) return ActionResult(false, "Is device me Bluetooth nahi hai.")
        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return ActionResult(true, "Bluetooth settings khol rahi hoon.")
    }

    fun toggleFlashlight(turnOn: Boolean): ActionResult {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull {
                cameraManager.getCameraCharacteristics(it)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return ActionResult(false, "Is device me flashlight support nahi hai.")
            cameraManager.setTorchMode(cameraId, turnOn)
            ActionResult(true, if (turnOn) "Flashlight on kar diya." else "Flashlight off kar diya.")
        } catch (e: Exception) {
            ActionResult(false, "Flashlight control nahi kar payi: ${e.message}")
        }
    }

    fun adjustVolume(streamType: Int = AudioManager.STREAM_MUSIC, raise: Boolean): ActionResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val direction = if (raise) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(streamType, direction, AudioManager.FLAG_SHOW_UI)
        return ActionResult(true, if (raise) "Volume badha diya." else "Volume kam kar diya.")
    }

    fun callContact(phoneNumber: String, hasCallPermission: Boolean): ActionResult {
        if (!hasCallPermission) return ActionResult(false, "Call permission nahi mili hai.")
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return ActionResult(true, "Call kar rahi hoon.")
    }
}
