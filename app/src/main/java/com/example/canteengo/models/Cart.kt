package com.example.canteengo.models

data class CartItem(
    val menuItem: MenuItem,
    var quantity: Int = 1
) {
    val totalPrice: Double get() = menuItem.price * quantity
}

object CartManager {
    private val _items = mutableListOf<CartItem>()
    val items: List<CartItem> get() = _items.toList()

    val itemCount: Int get() = _items.sumOf { it.quantity }

    val subtotal: Double get() = _items.sumOf { it.totalPrice }

    // Dynamic 10% handling charge
    val handlingCharge: Double get() = subtotal * 0.10

    val total: Double get() = subtotal + handlingCharge

    fun addItem(menuItem: MenuItem) {
        val existing = _items.find { it.menuItem.id == menuItem.id }
        if (existing != null) {
            existing.quantity++
        } else {
            _items.add(CartItem(menuItem, 1))
        }
    }

    fun removeItem(menuItemId: String) {
        _items.removeAll { it.menuItem.id == menuItemId }
    }

    fun updateQuantity(menuItemId: String, quantity: Int) {
        if (quantity <= 0) {
            removeItem(menuItemId)
        } else {
            _items.find { it.menuItem.id == menuItemId }?.quantity = quantity
        }
    }

    fun incrementQuantity(menuItemId: String) {
        _items.find { it.menuItem.id == menuItemId }?.let {
            it.quantity++
        }
    }

    fun decrementQuantity(menuItemId: String) {
        _items.find { it.menuItem.id == menuItemId }?.let {
            if (it.quantity > 1) {
                it.quantity--
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
    }

    fun isEmpty(): Boolean = _items.isEmpty()
}

