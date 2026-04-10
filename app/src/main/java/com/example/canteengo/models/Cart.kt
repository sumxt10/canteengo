package com.example.canteengo.models

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CartItem(
    val menuItem: MenuItem,
    var quantity: Int = 1
) {
    val totalPrice: Double get() = menuItem.price * quantity
}

object CartManager {
    private val _items = mutableListOf<CartItem>()
    val items: List<CartItem> get() = _items.toList()

    private var appContext: Context? = null

    val itemCount: Int get() = _items.sumOf { it.quantity }

    val subtotal: Double get() = _items.sumOf { it.totalPrice }

    // Dynamic 10% handling charge
    val handlingCharge: Double get() = subtotal * 0.10

    val total: Double get() = subtotal + handlingCharge

    fun initialize(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        restoreFromStorage()
    }

    fun addItem(menuItem: MenuItem) {
        val existing = _items.find { it.menuItem.id == menuItem.id }
        if (existing != null) {
            existing.quantity++
        } else {
            _items.add(CartItem(menuItem, 1))
        }
        persist()
    }

    fun removeItem(menuItemId: String) {
        _items.removeAll { it.menuItem.id == menuItemId }
        persist()
    }

    fun updateQuantity(menuItemId: String, quantity: Int) {
        if (quantity <= 0) {
            removeItem(menuItemId)
        } else {
            _items.find { it.menuItem.id == menuItemId }?.quantity = quantity
            persist()
        }
    }

    fun incrementQuantity(menuItemId: String) {
        _items.find { it.menuItem.id == menuItemId }?.let {
            it.quantity++
            persist()
        }
    }

    fun decrementQuantity(menuItemId: String) {
        _items.find { it.menuItem.id == menuItemId }?.let {
            if (it.quantity > 1) {
                it.quantity--
                persist()
            } else {
                removeItem(menuItemId)
            }
        }
    }

    fun getQuantity(menuItemId: String): Int {
        return _items.find { it.menuItem.id == menuItemId }?.quantity ?: 0
    }

    fun clear() {
        _items.clear()
        persist()
    }

    fun isEmpty(): Boolean = _items.isEmpty()

    private fun persist() {
        val context = appContext ?: return
        val array = JSONArray()
        _items.forEach { cartItem ->
            val menuItem = cartItem.menuItem
            val itemJson = JSONObject().apply {
                put("quantity", cartItem.quantity)
                put("menuItem", JSONObject().apply {
                    put("id", menuItem.id)
                    put("name", menuItem.name)
                    put("description", menuItem.description)
                    put("price", menuItem.price)
                    put("category", menuItem.category)
                    put("imageUrl", menuItem.imageUrl)
                    put("isVeg", menuItem.isVeg)
                    put("isAvailable", menuItem.isAvailable)
                    put("createdAt", menuItem.createdAt)
                    put("totalRatingSum", menuItem.totalRatingSum)
                    put("totalRatingCount", menuItem.totalRatingCount)
                })
            }
            array.put(itemJson)
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CART_ITEMS, array.toString())
            .apply()
    }

    private fun restoreFromStorage() {
        val context = appContext ?: return
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CART_ITEMS, null)
            ?: return

        runCatching {
            val parsed = JSONArray(stored)
            _items.clear()
            for (index in 0 until parsed.length()) {
                val itemJson = parsed.getJSONObject(index)
                val menuJson = itemJson.getJSONObject("menuItem")

                val menuItem = MenuItem(
                    id = menuJson.optString("id"),
                    name = menuJson.optString("name"),
                    description = menuJson.optString("description"),
                    price = menuJson.optDouble("price", 0.0),
                    category = menuJson.optString("category"),
                    imageUrl = menuJson.optString("imageUrl"),
                    isVeg = menuJson.optBoolean("isVeg", true),
                    isAvailable = menuJson.optBoolean("isAvailable", true),
                    createdAt = menuJson.optLong("createdAt", System.currentTimeMillis()),
                    totalRatingSum = menuJson.optDouble("totalRatingSum", 0.0),
                    totalRatingCount = menuJson.optInt("totalRatingCount", 0)
                )

                val quantity = itemJson.optInt("quantity", 1).coerceAtLeast(1)
                _items.add(CartItem(menuItem, quantity))
            }
        }.onFailure {
            _items.clear()
        }
    }

    private const val PREFS_NAME = "canteengo_cart"
    private const val KEY_CART_ITEMS = "cart_items"
}

