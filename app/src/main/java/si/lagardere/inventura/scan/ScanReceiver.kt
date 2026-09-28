package si.lagardere.inventura.scan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives barcodes from Zebra DataWedge (Intent output).
 * DataWedge is configured to broadcast our ACTION with the scanned data in
 * the standard extra. Registered dynamically by MainActivity while it is
 * in the foreground.
 */
class ScanReceiver(
    private val onScan: (barcode: String, symbology: String?) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val data = intent.getStringExtra(EXTRA_DATA_STRING)
            ?: intent.getStringExtra(EXTRA_DATA_STRING_ALT)
        val labelType = intent.getStringExtra(EXTRA_LABEL_TYPE)
        if (!data.isNullOrBlank()) {
            onScan(data.trim(), labelType)
        }
    }

    companion object {
        /** Our custom broadcast action; must match the DataWedge profile. */
        const val ACTION = "si.lagardere.inventura.SCAN"

        // Standard DataWedge intent extras
        const val EXTRA_DATA_STRING = "com.symbol.datawedge.data_string"
        const val EXTRA_DATA_STRING_ALT = "com.motorolasolutions.emdk.datawedge.data_string"
        const val EXTRA_LABEL_TYPE = "com.symbol.datawedge.label_type"
    }
}
