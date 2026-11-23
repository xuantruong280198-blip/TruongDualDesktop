package com.truong.dualdesktop

import android.app.Activity
import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.input.InputManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Display
import android.view.InputDevice
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), DisplayManager.DisplayListener, InputManager.InputDeviceListener {
    private lateinit var displayManager: DisplayManager
    private lateinit var inputManager: InputManager
    private lateinit var output: TextView
    private lateinit var status: TextView
    private var presentation: ProbePresentation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
        displayManager.registerDisplayListener(this, null)
        inputManager.registerInputDeviceListener(this, null)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(0xFF101820.toInt())
        }
        status = TextView(this).apply {
            text = "TRUONG DUAL DESKTOP · DIAGNOSTIC 0.1"
            textSize = 18f
            setTextColor(0xFF65D6AD.toInt())
        }
        val refresh = Button(this).apply {
            text = "Làm mới thông tin màn hình / chuột"
            setOnClickListener { refreshReport() }
        }
        val test = Button(this).apply {
            text = "Mở thử giao diện riêng trên màn hình ngoài"
            setOnClickListener { openPresentation() }
        }
        val close = Button(this).apply {
            text = "Đóng giao diện thử"
            setOnClickListener { presentation?.dismiss(); presentation = null }
        }
        output = TextView(this).apply {
            textSize = 13f
            typeface = android.graphics.Typeface.MONOSPACE
            setTextColor(0xFFE6EDF3.toInt())
            setTextIsSelectable(true)
        }
        root.addView(status)
        root.addView(refresh)
        root.addView(test)
        root.addView(close)
        root.addView(ScrollView(this).apply { addView(output) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        refreshReport()
    }

    private fun refreshReport() {
        val lines = mutableListOf<String>()
        lines += "DEVICE: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        lines += "ANDROID: ${android.os.Build.VERSION.RELEASE} / SDK ${android.os.Build.VERSION.SDK_INT}"
        lines += "BUILD: ${android.os.Build.DISPLAY}"
        lines += "TIME: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}"
        lines += ""
        val displays = displayManager.displays
        lines += "DISPLAYS (${displays.size})"
        displays.forEach { d ->
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            d.getRealMetrics(metrics)
            lines += "--- Display id=${d.displayId} ---"
            lines += "name=${d.name}"
            lines += "type=${typeName(d)} state=${stateName(d.state)}"
            lines += "flags=0x${d.flags.toString(16)} (${flagNames(d.flags)})"
            lines += "size=${metrics.widthPixels}x${metrics.heightPixels} density=${metrics.densityDpi}dpi"
            lines += "rotation=${d.rotation} refresh=${d.refreshRate}Hz"
            lines += "mode=${d.mode.physicalWidth}x${d.mode.physicalHeight}@${d.mode.refreshRate}Hz"
            lines += "isPresentation=${d.flags and Display.FLAG_PRESENTATION != 0}"
        }
        lines += ""
        lines += "INPUT DEVICES (${inputManager.inputDeviceIds.size})"
        inputManager.inputDeviceIds.forEach { id ->
            val dev = inputManager.getInputDevice(id) ?: return@forEach
            val sources = buildList {
                if (dev.supportsSource(InputDevice.SOURCE_MOUSE)) add("MOUSE")
                if (dev.supportsSource(InputDevice.SOURCE_KEYBOARD)) add("KEYBOARD")
                if (dev.supportsSource(InputDevice.SOURCE_TOUCHSCREEN)) add("TOUCHSCREEN")
                if (dev.supportsSource(InputDevice.SOURCE_TOUCHPAD)) add("TOUCHPAD")
                if (dev.supportsSource(InputDevice.SOURCE_STYLUS)) add("STYLUS")
            }
            lines += "id=$id name=${dev.name} sources=${sources.joinToString()} virtual=${dev.isVirtual}"
        }
        lines += ""
        lines += "NOTE: App API can inspect displays and show app-owned Presentation UI."
        lines += "It cannot by itself change system-wide display topology or move the OS mouse cursor across displays."
        output.text = lines.joinToString("\n")
        status.text = "DIAGNOSTIC · ${displays.size} display(s) · ${inputManager.inputDeviceIds.size} input device(s)"
    }

    private fun openPresentation() {
        val target = displayManager.displays.firstOrNull {
            it.displayId != Display.DEFAULT_DISPLAY && it.flags and Display.FLAG_PRESENTATION != 0
        }
        if (target == null) {
            status.text = "Không tìm thấy Presentation display phụ. Xem báo cáo bên dưới."
            return
        }
        try {
            presentation?.dismiss()
            presentation = ProbePresentation(this, target).also { it.show() }
            status.text = "Đã mở giao diện thử trên display ${target.displayId}"
        } catch (e: Exception) {
            status.text = "Không mở được Presentation: ${e.javaClass.simpleName}: ${e.message}"
        }
    }

    override fun onDisplayAdded(displayId: Int) = refreshReport()
    override fun onDisplayRemoved(displayId: Int) { presentation?.dismiss(); presentation = null; refreshReport() }
    override fun onDisplayChanged(displayId: Int) = refreshReport()
    override fun onInputDeviceAdded(deviceId: Int) = refreshReport()
    override fun onInputDeviceRemoved(deviceId: Int) = refreshReport()
    override fun onInputDeviceChanged(deviceId: Int) = refreshReport()

    override fun onDestroy() {
        displayManager.unregisterDisplayListener(this)
        inputManager.unregisterInputDeviceListener(this)
        presentation?.dismiss()
        super.onDestroy()
    }

    private fun typeName(d: Display) = when (d.type) {
        Display.TYPE_BUILT_IN -> "BUILT_IN"
        Display.TYPE_HDMI -> "HDMI"
        Display.TYPE_WIFI -> "WIFI"
        Display.TYPE_OVERLAY -> "OVERLAY"
        Display.TYPE_VIRTUAL -> "VIRTUAL"
        else -> "OTHER(${d.type})"
    }
    private fun stateName(s: Int) = when (s) {
        Display.STATE_ON -> "ON"; Display.STATE_OFF -> "OFF"; Display.STATE_DOZE -> "DOZE"
        Display.STATE_DOZE_SUSPEND -> "DOZE_SUSPEND"; Display.STATE_UNKNOWN -> "UNKNOWN"; else -> "$s"
    }
    private fun flagNames(f: Int) = buildList {
        if (f and Display.FLAG_DEFAULT_DISPLAY != 0) add("DEFAULT")
        if (f and Display.FLAG_PRESENTATION != 0) add("PRESENTATION")
        if (f and Display.FLAG_SECURE != 0) add("SECURE")
        if (f and Display.FLAG_PRIVATE != 0) add("PRIVATE")
        if (f and Display.FLAG_OWN_CONTENT_ONLY != 0) add("OWN_CONTENT_ONLY")
        if (f and Display.FLAG_AUTO_MIRROR != 0) add("AUTO_MIRROR")
    }.joinToString("|").ifEmpty { "none" }
}

private class ProbePresentation(context: Context, display: Display) : Presentation(context, display) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(0xFF17232E.toInt())
        }
        panel.addView(TextView(context).apply {
            text = "TRUONG DUAL DESKTOP\n\nEXTERNAL DISPLAY TEST\n\nĐây là giao diện riêng do app tạo ra.\nKhông phải màn hình mirror."
            textSize = 24f
            gravity = android.view.Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
        })
        setContentView(panel)
        window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
    }
}
