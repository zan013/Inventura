package si.lagardere.inventura

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import si.lagardere.inventura.data.Record
import si.lagardere.inventura.databinding.ItemRecordBinding

class RecordAdapter(
    private val items: MutableList<Record>,
    private val onDelete: (Record) -> Unit
) : RecyclerView.Adapter<RecordAdapter.VH>() {

    inner class VH(val b: ItemRecordBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]
        val ctx = holder.b.root.context
        holder.b.tvMain.text = ctx.getString(R.string.record_line, r.ean, r.kolicina.toString())
        holder.b.tvSub.text = ctx.getString(R.string.record_sub, r.popisovalec)
        holder.b.btnDelete.setOnClickListener { onDelete(r) }
    }

    override fun getItemCount(): Int = items.size

    fun setItems(newItems: List<Record>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun remove(record: Record) {
        val idx = items.indexOfFirst { it.id == record.id }
        if (idx >= 0) {
            items.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }
}
