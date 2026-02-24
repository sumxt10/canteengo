# 🍔 CanteenGo

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform">
  <img src="https://img.shields.io/badge/Language-Kotlin-blue.svg" alt="Language">
  <img src="https://img.shields.io/badge/Min%20SDK-24-yellow.svg" alt="Min SDK">
  <img src="https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore-orange.svg" alt="Firebase">
</p>

A modern **College Canteen Food Ordering App** built with Kotlin and XML for Android. CanteenGo streamlines the food ordering process in college canteens by connecting students directly with canteen staff, eliminating queues and improving efficiency.

---

## 📱 Features

### For Students
- **Browse Menu**: View food items categorized by type (South Indian, Snacks, Beverages, etc.)
- **Smart Search**: Real-time search functionality to quickly find food items
- **Cart Management**: Add, remove, and update quantities of items in cart
- **Order Placement**: Place orders with ASAP or preferred pickup time
- **Unique Token & QR Code**: Each order generates a unique token and QR code for easy identification
- **Real-time Order Tracking**: Track order status (Pending → Accepted → Preparing → Ready → Collected)
- **Order History**: View past orders and their details
- **Spending Statistics**: View daily, weekly, and monthly spending analytics
- **Profile Management**: Edit profile details and manage account

### For Admins (Canteen Staff)
- **Dashboard Overview**: View pending orders, today's earnings, and order statistics
- **Order Management**: Accept, prepare, mark ready, and complete orders
- **Real-time Sync**: Multi-admin support with instant order synchronization
- **Menu Management**: Add, edit, delete menu items with image uploads
- **Availability Toggle**: Instantly toggle food item availability
- **Profile Management**: Manage admin account details

---

## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | Kotlin |
| **UI** | XML with ViewBinding |
| **Architecture** | Repository Pattern |
| **Authentication** | Firebase Auth (Email/Password + Google Sign-In) |
| **Database** | Cloud Firestore |
| **Image Storage** | Cloudinary (Free Tier) |
| **Image Loading** | Coil |
| **QR Generation** | ZXing |
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 36 |

---

## 📁 Project Structure

```
app/src/main/java/com/example/canteengo/
├── activities/
│   ├── admin/                    # Admin-specific activities
│   │   ├── AddEditMenuItemActivity.kt
│   │   ├── AdminDashboardActivity.kt
│   │   ├── AdminLoginActivity.kt
│   │   ├── AdminMenuActivity.kt
│   │   ├── AdminOrderDetailsActivity.kt
│   │   ├── AdminOrdersActivity.kt
│   │   ├── AdminProfileActivity.kt
│   │   └── AdminSignupActivity.kt
│   ├── student/                  # Student-specific activities
│   │   ├── CartActivity.kt
│   │   ├── FoodDetailsActivity.kt
│   │   ├── OrderSuccessActivity.kt
│   │   ├── PickupTimeActivity.kt
│   │   ├── StudentHomeActivity.kt
│   │   ├── StudentLoginActivity.kt
│   │   ├── StudentOrderDetailsActivity.kt
│   │   ├── StudentOrdersActivity.kt
│   │   ├── StudentProfileActivity.kt
│   │   └── StudentSignupActivity.kt
│   ├── GoogleSignupDetailsActivity.kt
│   ├── OnboardingActivity.kt
│   ├── RoleSelectionActivity.kt
│   └── SplashActivity.kt
├── adapters/                     # RecyclerView adapters
├── models/                       # Data models
├── repository/                   # Data repositories
└── utils/                        # Utility classes
```

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Arctic Fox or later
- JDK 11 or higher
- Android device/emulator with API 24+
- Firebase project with Authentication and Firestore enabled
- Cloudinary account (free tier)

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/CanteenGo.git
   cd CanteenGo
   ```

2. **Firebase Setup**
   - Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
   - Enable **Email/Password** and **Google Sign-In** authentication methods
   - Create a **Cloud Firestore** database
   - Download `google-services.json` and place it in the `app/` directory
   - Add your app's SHA-1 fingerprint for Google Sign-In

3. **Cloudinary Setup**
   - Create a free account at [Cloudinary](https://cloudinary.com/)
   - Create an unsigned upload preset named `canteengo_menu`
   - Update the cloud name in `AddEditMenuItemActivity.kt`

4. **Build the project**
   ```bash
   ./gradlew assembleDebug
   ```

5. **Run on device**
   - Connect your Android device via USB with debugging enabled
   - Click Run in Android Studio or use:
   ```bash
   ./gradlew installDebug
   ```

---

## 🔥 Firebase Structure

### Collections

```
users/
├── {userId}/
│   ├── name: string
│   ├── email: string
│   ├── phone: string
│   ├── role: "student" | "admin"
│   ├── libraryCard: string (students only)
│   ├── department: string (students only)
│   └── division: string (students only)

menu_items/
├── {itemId}/
│   ├── name: string
│   ├── description: string
│   ├── price: number
│   ├── category: string
│   ├── imageUrl: string
│   └── isAvailable: boolean

orders/
├── {orderId}/
│   ├── token: string
│   ├── studentId: string
│   ├── studentName: string
│   ├── items: array
│   ├── subtotal: number
│   ├── handlingCharge: number
│   ├── totalAmount: number
│   ├── pickupTime: string
│   ├── status: string
│   ├── acceptedByPhone: string
│   ├── qrString: string
│   └── createdAt: timestamp
```

---

## 📸 Screenshots

| Splash | Role Selection | Student Dashboard |
|--------|----------------|-------------------|
| *Splash Screen* | *Choose Role* | *Browse Menu* |

| Cart | Order Success | Admin Dashboard |
|------|---------------|-----------------|
| *Manage Cart* | *Token & QR* | *Manage Orders* |

---

## 🔒 Security Notes

- Never commit `google-services.json` to public repositories
- Use environment variables for sensitive API keys
- Configure Firestore security rules for production
- Review Firebase Authentication settings before deployment

---

## 📋 Order Status Flow

```
PENDING → ACCEPTED → PREPARING → READY → COLLECTED
                  ↘ REJECTED
```

---

## 🎯 Future Enhancements

- [ ] Push notifications for order status updates
- [ ] Payment gateway integration (UPI, Cards)
- [ ] Favorite items and reorder functionality
- [ ] Rating and review system
- [ ] Multiple canteen support
- [ ] Admin analytics dashboard
- [ ] Offline mode with local caching

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

**Sumit**

- GitHub: [@yourusername](https://github.com/yourusername)

---

## 🙏 Acknowledgments

- Firebase for backend services
- Cloudinary for image hosting
- ZXing for QR code generation
- Material Design Components for UI

---

<p align="center">
  Made with ❤️ for College Canteens
</p>

