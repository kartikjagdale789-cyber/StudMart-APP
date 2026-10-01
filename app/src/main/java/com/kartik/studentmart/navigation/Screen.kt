package com.kartik.studentmart.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Categories : Screen("categories")
    object ProductDetails : Screen("product_details/{productId}") {
        fun createRoute(productId: String) = "product_details/$productId"
    }
    object SellProduct : Screen("sell_product")
    object MyListings : Screen("my_listings")
    object Wishlist : Screen("wishlist")
    object MyPurchases : Screen("my_purchases")
    object SellerRequests : Screen("seller_requests")
    object Chat : Screen("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
    object MyChats : Screen("my_chats")
    object Offers : Screen("offers")
    object ReceivedOffers : Screen("received_offers")
    object BookExchange : Screen("book_exchange")
    object Notifications : Screen("notifications")
    object Profile : Screen("profile")
    object AdminDashboard : Screen("admin_dashboard")
}
