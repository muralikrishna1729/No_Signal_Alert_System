package com.example.nosignalalertsystem.ui.contacts

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nosignalalertsystem.R
import com.example.nosignalalertsystem.data.AppDatabase
import com.example.nosignalalertsystem.data.EmergencyContact
import com.example.nosignalalertsystem.databinding.FragmentContactsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ContactsFragment : Fragment(R.layout.fragment_contacts) {

    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!

    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var contactDao: com.example.nosignalalertsystem.data.EmergencyContactDao // Full path to avoid ambiguity

    // Activity Result Launcher for SMS permission (needed to send alerts later)
    private val requestSmsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(requireContext(),
                "SMS permission is required to send emergency alerts.",
                Toast.LENGTH_LONG).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentContactsBinding.bind(view)

        // Initialize DAO
        val database = AppDatabase.getDatabase(requireContext())
        contactDao = database.emergencyContactDao()

        setupRecyclerView()
        setupListeners()
        observeContacts()

        // Request SMS permission on fragment load if not granted
        checkAndRequestSmsPermission()
    }

    private fun setupRecyclerView() {
        contactsAdapter = ContactsAdapter { contactToDelete ->
            deleteContact(contactToDelete)
        }
        binding.rvContacts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contactsAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }

    private fun observeContacts() {
        // Collect latest list of contacts from the database Flow
        lifecycleScope.launch {
            contactDao.getAllContacts().collectLatest { contactsList ->
                contactsAdapter.submitList(contactsList)
            }
        }
    }

    private fun setupListeners() {
        binding.btnAddContact.setOnClickListener {
            addContact()
        }
    }

    private fun checkAndRequestSmsPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            requestSmsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }

    private fun addContact() {
        val name = binding.etContactName.text.toString().trim()
        val phone = binding.etPhoneNumber.text.toString().trim()

        if (name.isEmpty() || phone.length != 10 || !phone.matches(Regex("\\d+"))) {
            Toast.makeText(requireContext(), getString(R.string.error_invalid_input), Toast.LENGTH_SHORT).show()
            return
        }

        val newContact = EmergencyContact(name = name, phoneNumber = phone)

        lifecycleScope.launch {
            contactDao.insertContact(newContact)
            Toast.makeText(requireContext(), getString(R.string.toast_contact_added), Toast.LENGTH_SHORT).show()
            // Clear inputs
            binding.etContactName.text.clear()
            binding.etPhoneNumber.text.clear()
        }
    }

    private fun deleteContact(contact: EmergencyContact) {
        lifecycleScope.launch {
            contactDao.deleteContact(contact)
            Toast.makeText(requireContext(), "Contact deleted: ${contact.name}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}