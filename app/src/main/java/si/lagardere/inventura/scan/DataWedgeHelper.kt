package si.lagardere.inventura.scan

import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * Programmatically creates a DataWedge profile bound to this app so scans are
 * delivered to us as a broadcast Intent (ScanReceiver.ACTION). This means IT
 * does not have to hand-configure DataWedge on every Zebra device.
 *
 * Best-effort: on non-Zebra devices these broadcasts are simply ignored.
 * If it fails, you can still configure DataWedge manually (see README).
 */
object DataWedgeHelper {

    private const val DW_API = "com.symbol.datawedge.api.ACTION"
    private const val EXTRA_CREATE = "com.symbol.datawedge.api.CREATE_PROFILE"
    private const val EXTRA_SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG"
    private const val PROFILE_NAME = "Inventura"

    fun configure(context: Context) {
        try {
            // 1) Ensure the profile exists
            send(context, EXTRA_CREATE, PROFILE_NAME)

            // 2) Configure it: associate our app, enable barcode, output via Intent broadcast
            val profileConfig = Bundle().apply {
                putString("PROFILE_NAME", PROFILE_NAME)
                putString("PROFILE_ENABLED", "true")
                putString("CONFIG_MODE", "UPDATE")

                // Associate this app (all activities)
                val app = Bundle().apply {
                    putString("PACKAGE_NAME", context.packageName)
                    putStringArray("ACTIVITY_LIST", arrayOf("*"))
                }
                putParcelableArray("APP_LIST", arrayOf(app))

                val plugins = arrayListOf<Bundle>()

                // BARCODE input on
                plugins.add(Bundle().apply {
                    putString("PLUGIN_NAME", "BARCODE")
                    putString("RESET_CONFIG", "true")
                    putBundle("PARAM_LIST", Bundle().apply {
                        putString("scanner_selection", "auto")
                        putString("scanner_input_enabled", "true")
                    })
                })

                // INTENT output on (broadcast our action)
                plugins.add(Bundle().apply {
                    putString("PLUGIN_NAME", "INTENT")
                    putString("RESET_CONFIG", "true")
                    putBundle("PARAM_LIST", Bundle().apply {
                        putString("intent_output_enabled", "true")
                        putString("intent_action", ScanReceiver.ACTION)
                        putString("intent_delivery", "2") // 2 = Broadcast
                    })
                })

                // KEYSTROKE output off (avoid the EAN also being typed)
                plugins.add(Bundle().apply {
                    putString("PLUGIN_NAME", "KEYSTROKE")
                    putString("RESET_CONFIG", "true")
                    putBundle("PARAM_LIST", Bundle().apply {
                        putString("keystroke_output_enabled", "false")
                    })
                })

                putParcelableArrayList("PLUGIN_CONFIG", plugins)
            }

            val intent = Intent().apply {
                action = DW_API
                putExtra(EXTRA_SET_CONFIG, profileConfig)
                setPackage("com.symbol.datawedge")
            }
            context.sendBroadcast(intent)
        } catch (_: Exception) {
            // Non-Zebra device or DataWedge absent; ignore.
        }
    }

    private fun send(context: Context, extra: String, value: String) {
        val i = Intent().apply {
            action = DW_API
            putExtra(extra, value)
            setPackage("com.symbol.datawedge")
        }
        context.sendBroadcast(i)
    }
}
