package com.example.nosignalalertsystem.ui.contacts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nosignalalertsystem.R
import com.example.nosignalalertsystem.data.EmergencyContact
import com.example.nosignalalertsystem.databinding.ItemContactBinding

class ContactsAdapter(private val onDeleteClick: (EmergencyContact) -> Unit) :
    ListAdapter<EmergencyContact, ContactsAdapter.ContactViewHolder>(ContactsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ContactViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: EmergencyContact) {
            binding.tvContactName.text = contact.name
            binding.tvContactPhone.text = contact.phoneNumber

            binding.btnDelete.setOnClickListener {
                onDeleteClick(contact)
            }
        }
    }
}

private class ContactsDiffCallback : DiffUtil.ItemCallback<EmergencyContact>() {
    override fun areItemsTheSame(oldItem: EmergencyContact, newItem: EmergencyContact): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: EmergencyContact, newItem: EmergencyContact): Boolean {
        return oldItem == newItem
    }
}