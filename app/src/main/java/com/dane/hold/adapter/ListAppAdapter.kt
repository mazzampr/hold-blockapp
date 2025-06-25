package com.dane.hold.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.dane.hold.R
import com.dane.hold.data.AppData
import com.dane.hold.data.LockedAppManager
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Locale

class ListAppAdapter(
    private var fullSortedList: MutableList<AppData>,
    private val onToggleClicked: (packageName: String, isChecked: Boolean, (Boolean) -> Unit) -> Unit,
    private val onDurationClicked: (packageName: String, currentDuration: Int) -> Unit,
) : RecyclerView.Adapter<ListAppAdapter.AppViewHolder>(), Filterable {

    private var displayList: MutableList<AppData> = fullSortedList.toMutableList()

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.image_view_app_icon)
        val appName: TextView = itemView.findViewById(R.id.text_view_app_name)
        val appSwitch: SwitchMaterial = itemView.findViewById(R.id.switch_app_block)
        val durationText: TextView = itemView.findViewById(R.id.text_custom_duration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_block, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val currentApp = displayList[position]
        holder.appIcon.setImageDrawable(currentApp.icon)
        holder.appName.text = currentApp.name

        val isLocked = LockedAppManager.isAppLocked(holder.itemView.context, currentApp.packageName)
        val customDuration = LockedAppManager.getLockedAppDuration(holder.itemView.context, currentApp.packageName)

        // Tampilkan/sembunyikan flag durasi berdasarkan status terkunci
        if (isLocked) {
            holder.durationText.visibility = View.VISIBLE
            holder.durationText.text = "$customDuration sec"
        } else {
            holder.durationText.visibility = View.GONE
        }

        // Tambahkan listener klik untuk flag durasi
        holder.durationText.setOnClickListener {
            onDurationClicked(currentApp.packageName, customDuration)
        }

        // Logika untuk toggle utama (tetap sama, tapi penting)
        holder.appSwitch.setOnCheckedChangeListener(null)
        holder.appSwitch.isChecked = isLocked
        holder.appSwitch.setOnCheckedChangeListener { switchView, isChecked ->
            onToggleClicked(currentApp.packageName, isChecked) { success ->
                if (!success) {
                    switchView.isChecked = !isChecked
                } else {
                    val status = if (isChecked) "locked" else "unlocked"
                    Toast.makeText(holder.itemView.context, "${currentApp.name} is now $status", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return displayList.size
    }

    fun updateFullList(newSortedList: List<AppData>) {
        fullSortedList.clear()
        fullSortedList.addAll(newSortedList)
        displayList.clear()
        displayList.addAll(newSortedList)
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filteredList: List<AppData> = if (constraint.isNullOrEmpty()) {
                    // Jika tidak ada input, kembalikan daftar lengkap
                    fullSortedList
                } else {
                    val filterPattern = constraint.toString().lowercase(Locale.ROOT).trim()
                    // Filter daftar lengkap berdasarkan nama aplikasi
                    fullSortedList.filter {
                        it.name.lowercase(Locale.ROOT).contains(filterPattern)
                    }
                }
                val results = FilterResults()
                results.values = filteredList
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                // Update the list that is actually shown on screen.
                displayList.clear()
                displayList.addAll(results?.values as? List<AppData> ?: emptyList())
                notifyDataSetChanged()
            }
        }
    }
}