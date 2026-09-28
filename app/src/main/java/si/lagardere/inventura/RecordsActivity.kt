package si.lagardere.inventura

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import si.lagardere.inventura.data.AppDatabase
import si.lagardere.inventura.data.Record
import si.lagardere.inventura.databinding.ActivityRecordsBinding

class RecordsActivity : AppCompatActivity() {

    private lateinit var b: ActivityRecordsBinding
    private val dao by lazy { AppDatabase.get(this).recordDao() }
    private lateinit var adapter: RecordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRecordsBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.toolbar.setNavigationOnClickListener { finish() }

        adapter = RecordAdapter(mutableListOf()) { record -> confirmDelete(record) }
        b.recycler.layoutManager = LinearLayoutManager(this)
        b.recycler.adapter = adapter
        b.recycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        load()
    }

    private fun load() {
        lifecycleScope.launch {
            val items = dao.all()
            adapter.setItems(items)
            b.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun confirmDelete(record: Record) {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete_title)
            .setMessage(getString(R.string.confirm_delete_msg, record.ean, record.kolicina.toString()))
            .setPositiveButton(R.string.delete) { _, _ ->
                lifecycleScope.launch {
                    dao.delete(record)
                    adapter.remove(record)
                    if (adapter.itemCount == 0) b.tvEmpty.visibility = View.VISIBLE
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
