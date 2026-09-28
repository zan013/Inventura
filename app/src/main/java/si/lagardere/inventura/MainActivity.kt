package si.lagardere.inventura

import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import si.lagardere.inventura.data.AppDatabase
import si.lagardere.inventura.data.Record
import si.lagardere.inventura.databinding.ActivityMainBinding
import si.lagardere.inventura.export.Exporter
import si.lagardere.inventura.scan.DataWedgeHelper
import si.lagardere.inventura.scan.ScanReceiver
import si.lagardere.inventura.util.Prefs

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var prefs: Prefs
    private val dao by lazy { AppDatabase.get(this).recordDao() }

    private var tone: ToneGenerator? = null
    private var pendingExportSkladisce: String? = null
    private var pendingClearAfter: Boolean = true

    private val scanReceiver = ScanReceiver { barcode, _ ->
        runOnUiThread { onScanned(barcode) }
    }

    private val requestStorage = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) doExport() else toast(getString(R.string.export_failed, "ni dovoljenja za shranjevanje"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)
        prefs = Prefs(this)

        try { tone = ToneGenerator(AudioManager.STREAM_MUSIC, 80) } catch (_: Exception) {}

        b.etPopisovalec.setText(prefs.popisovalec)
        b.swOnlyEan.isChecked = prefs.onlyEan
        applyOnlyEan(prefs.onlyEan)

        b.swOnlyEan.setOnCheckedChangeListener { _, checked ->
            prefs.onlyEan = checked
            applyOnlyEan(checked)
        }

        // Hardware Enter / IME "done" on EAN behaves like a scan
        b.etEan.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                onScanned(b.etEan.text?.toString()?.trim().orEmpty()); true
            } else false
        }
        b.etKolicina.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { saveRecord(); true } else false
        }
        b.btnSave.setOnClickListener { saveRecord() }

        // Configure DataWedge once per install (best-effort on Zebra devices)
        if (!prefs.dwConfigured) {
            DataWedgeHelper.configure(this)
            prefs.dwConfigured = true
        }

        updateCounts()
        b.etEan.requestFocus()
    }

    private fun applyOnlyEan(only: Boolean) {
        b.tilKolicina.visibility = if (only) LinearLayout.GONE else LinearLayout.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(ScanReceiver.ACTION)
        // DataWedge (a separate app) sends this broadcast, so it must be EXPORTED.
        ContextCompat.registerReceiver(
            this, scanReceiver, filter, ContextCompat.RECEIVER_EXPORTED
        )
        updateCounts()
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(scanReceiver) } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        tone?.release()
    }

    /** A barcode arrived (from DataWedge or manual Enter). */
    private fun onScanned(barcode: String) {
        if (barcode.isEmpty()) return
        b.etEan.setText(barcode)
        if (b.swOnlyEan.isChecked) {
            saveRecord()
        } else {
            b.etKolicina.requestFocus()
            b.etKolicina.selectAll()
        }
    }

    private fun saveRecord() {
        val popisovalec = b.etPopisovalec.text?.toString()?.trim().orEmpty()
        val ean = b.etEan.text?.toString()?.trim().orEmpty()
        if (popisovalec.isEmpty()) { toast(getString(R.string.err_no_popisovalec)); b.etPopisovalec.requestFocus(); return }
        if (ean.isEmpty()) { toast(getString(R.string.err_no_ean)); b.etEan.requestFocus(); return }

        val kolicina = if (b.swOnlyEan.isChecked) 1
        else b.etKolicina.text?.toString()?.trim()?.toIntOrNull()?.takeIf { it > 0 } ?: 1

        prefs.popisovalec = popisovalec
        val record = Record(popisovalec = popisovalec, ean = ean, kolicina = kolicina)

        lifecycleScope.launch {
            dao.insert(record)
            beep()
            b.etEan.setText("")
            b.etKolicina.setText("1")
            b.etEan.requestFocus()
            updateCounts()
        }
    }

    private fun updateCounts() {
        lifecycleScope.launch {
            val count = dao.count()
            val last = dao.last()
            b.tvCount.text = getString(R.string.records_count, count)
            b.tvLast.text = if (last != null)
                getString(R.string.last_scan, last.ean, last.kolicina.toString()) else ""
        }
    }

    private fun beep() {
        try { tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 120) } catch (_: Exception) {}
        try {
            val v = getSystemService(VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") v?.vibrate(40)
            }
        } catch (_: Exception) {}
    }

    // ---------- menu ----------
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu); return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_records -> { startActivity(android.content.Intent(this, RecordsActivity::class.java)); true }
        R.id.action_export -> { exportDialog(); true }
        R.id.action_setup_scanner -> {
            DataWedgeHelper.configure(this); prefs.dwConfigured = true
            toast(getString(R.string.dw_configured)); true
        }
        else -> super.onOptionsItemSelected(item)
    }

    // ---------- export ----------
    private fun exportDialog() {
        lifecycleScope.launch {
            if (dao.count() == 0) { toast(getString(R.string.export_none)); return@launch }

            val pad = (16 * resources.displayMetrics.density).toInt()
            val container = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(pad, pad / 2, pad, 0)
            }
            val etSkl = EditText(this@MainActivity).apply {
                hint = getString(R.string.export_skladisce)
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setText(prefs.skladisce)
            }
            val cb = CheckBox(this@MainActivity).apply {
                text = getString(R.string.export_clear_after)
                isChecked = true
            }
            container.addView(etSkl)
            container.addView(cb)

            AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.export_title)
                .setView(container)
                .setPositiveButton(R.string.export_do) { _, _ ->
                    prefs.skladisce = etSkl.text?.toString()?.trim().orEmpty()
                    pendingExportSkladisce = prefs.skladisce
                    pendingClearAfter = cb.isChecked
                    ensurePermissionThenExport()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun ensurePermissionThenExport() {
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.M..Build.VERSION_CODES.P) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) { requestStorage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE); return }
        }
        doExport()
    }

    private fun doExport() {
        lifecycleScope.launch {
            try {
                val records = dao.all()
                val result = Exporter.export(this@MainActivity, records, pendingExportSkladisce)
                if (pendingClearAfter) dao.clear()
                updateCounts()
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.export_ok_title)
                    .setMessage(getString(R.string.export_ok_msg, result.displayPath))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            } catch (e: Exception) {
                toast(getString(R.string.export_failed, e.message ?: "napaka"))
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
