package com.example.canteengo.activities.admin

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.canteengo.databinding.ActivityAddEditMenuItemBinding
import com.example.canteengo.models.FoodCategory
import com.example.canteengo.models.MenuItem
import com.example.canteengo.repository.MenuRepository
import com.example.canteengo.utils.toast
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AddEditMenuItemActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditMenuItemBinding
    private val menuRepo = MenuRepository()

    private var itemId: String? = null
    private var existingItem: MenuItem? = null
    private var selectedImageUri: Uri? = null
    private var uploadedImageUrl: String = ""

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.ivItemImage.load(it) {
                crossfade(true)
            }
            binding.uploadPlaceholder.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditMenuItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        itemId = intent.getStringExtra("item_id")

        setupToolbar()
        setupCategoryDropdown()
        setupButtons()

        if (itemId != null) {
            binding.tvTitle.text = "Edit Menu Item"
            loadExistingItem()
        } else {
            binding.tvTitle.text = "Add Menu Item"
        }
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupCategoryDropdown() {
        val categories = FoodCategory.values().map { it.displayName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.actvCategory.setAdapter(adapter)
    }

    private fun setupButtons() {
        // Image selection click listeners
        binding.btnSelectImage.setOnClickListener {
            launchImagePicker()
        }

        binding.ivItemImage.setOnClickListener {
            launchImagePicker()
        }

        binding.uploadPlaceholder.setOnClickListener {
            launchImagePicker()
        }

        binding.btnSave.setOnClickListener {
            saveItem()
        }
    }

    private fun launchImagePicker() {
        imagePicker.launch("image/*")
    }

    private fun loadExistingItem() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                // For now, we'll need to get item from list
                val items = menuRepo.getAllMenuItems()
                existingItem = items.find { it.id == itemId }

                existingItem?.let { item ->
                    binding.etName.setText(item.name)
                    binding.etDescription.setText(item.description)
                    binding.etPrice.setText(item.price.toInt().toString())
                    binding.actvCategory.setText(
                        FoodCategory.values().find { it.name == item.category }?.displayName ?: item.category,
                        false
                    )
                    binding.switchVeg.isChecked = item.isVeg
                    binding.switchAvailable.isChecked = item.isAvailable

                    // Load existing image
                    if (item.imageUrl.isNotEmpty()) {
                        uploadedImageUrl = item.imageUrl
                        binding.ivItemImage.load(item.imageUrl) {
                            crossfade(true)
                        }
                        binding.uploadPlaceholder.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                toast("Failed to load item")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun saveItem() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val description = binding.etDescription.text?.toString()?.trim().orEmpty()
        val priceStr = binding.etPrice.text?.toString()?.trim().orEmpty()
        val categoryDisplay = binding.actvCategory.text?.toString()?.trim().orEmpty()
        val isVeg = binding.switchVeg.isChecked
        // New items are always available by default
        // Existing items keep their current availability status
        val isAvailable = existingItem?.isAvailable ?: true

        // Validation
        if (name.isBlank()) {
            binding.tilName.error = "Name is required"
            return
        }
        if (priceStr.isBlank()) {
            binding.tilPrice.error = "Price is required"
            return
        }
        if (categoryDisplay.isBlank()) {
            binding.tilCategory.error = "Category is required"
            return
        }

        val price = priceStr.toDoubleOrNull()
        if (price == null || price <= 0) {
            binding.tilPrice.error = "Enter valid price"
            return
        }

        // Find category enum
        val category = FoodCategory.values().find { it.displayName == categoryDisplay }?.name ?: categoryDisplay.uppercase()

        setLoading(true)

        lifecycleScope.launch {
            try {
                // Upload image if selected
                val imageUrl = if (selectedImageUri != null) {
                    uploadImageToStorage(selectedImageUri!!)
                } else {
                    uploadedImageUrl // Use existing image URL
                }

                val menuItem = MenuItem(
                    id = itemId ?: "",
                    name = name,
                    description = description,
                    price = price,
                    category = category,
                    imageUrl = imageUrl,
                    isVeg = isVeg,
                    isAvailable = isAvailable
                )

                if (itemId != null) {
                    menuRepo.updateMenuItem(menuItem)
                    toast("Item updated successfully")
                } else {
                    menuRepo.addMenuItem(menuItem)
                    toast("Item added successfully")
                }
                finish()
            } catch (e: Exception) {
                toast("Failed to save: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    private suspend fun uploadImageToStorage(uri: Uri): String {
        return try {
            FirebaseApp.getInstance()
            val storage = FirebaseStorage.getInstance()
            val fileName = "menu_images/${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child(fileName)

            binding.uploadProgress.visibility = View.VISIBLE

            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await()

            binding.uploadProgress.visibility = View.GONE

            downloadUrl.toString()
        } catch (e: Exception) {
            binding.uploadProgress.visibility = View.GONE
            throw Exception("Image upload failed: ${e.message}")
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !loading
    }
}
