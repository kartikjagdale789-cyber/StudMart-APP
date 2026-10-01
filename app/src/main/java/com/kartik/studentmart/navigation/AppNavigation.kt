package com.kartik.studentmart.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kartik.studentmart.ui.components.StudMartBottomNav
import com.kartik.studentmart.ui.screens.*
import com.kartik.studentmart.viewmodel.AuthViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavRoutes = listOf(
        Screen.Home.route,
        Screen.Categories.route,
        Screen.SellProduct.route,
        Screen.Wishlist.route,
        Screen.Profile.route
    )

    val showBottomNav = currentRoute in bottomNavRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomNav) {
                StudMartBottomNav(
                    currentRoute = currentRoute,
                    onItemClick = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    authViewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    authViewModel = authViewModel,
                    onRegisterSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToProductDetails = { productId ->
                        navController.navigate(Screen.ProductDetails.createRoute(productId))
                    },
                    onNavigateToBookExchange = { navController.navigate(Screen.BookExchange.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToChat = { navController.navigate(Screen.MyChats.route) },
                    onNavigateToSell = { navController.navigate(Screen.SellProduct.route) },
                    onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                    onCategoryClick = { _ ->
                        navController.navigate(Screen.Categories.route)
                    },
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) }
                )
            }
            composable(Screen.Categories.route) {
                CategoriesScreen(
                    onCategorySelected = { _ -> },
                    onNavigateToProductDetails = { productId ->
                        navController.navigate(Screen.ProductDetails.createRoute(productId))
                    },
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) }
                )
            }
            composable(
                route = Screen.ProductDetails.route,
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId")
                ProductDetailsScreen(
                    productId = productId,
                    onNavigateToOffers = { navController.navigate(Screen.Offers.route) },
                    onNavigateToChat = { chatId ->
                        navController.navigate(Screen.Chat.createRoute(chatId))
                    },
                    onBack = { navController.popBackStack() },
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                    onNavigateToSell = { navController.navigate(Screen.SellProduct.route) }
                )
            }
            composable(Screen.SellProduct.route) {
                SellProductScreen(
                    onProductSubmitted = {
                        navController.navigate(Screen.MyListings.route) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.MyListings.route) {
                MyListingsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToDetails = { productId ->
                        navController.navigate(Screen.ProductDetails.createRoute(productId))
                    }
                )
            }
            composable(Screen.Wishlist.route) {
                WishlistScreen(
                    onNavigateToProductDetails = { productId ->
                        navController.navigate(Screen.ProductDetails.createRoute(productId))
                    },
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) }
                )
            }
            composable(Screen.MyPurchases.route) {
                MyPurchasesScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToDetails = { productId ->
                        navController.navigate(Screen.ProductDetails.createRoute(productId))
                    },
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) }
                )
            }
            composable(Screen.SellerRequests.route) {
                SellerRequestsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToDetails = { productId ->
                        navController.navigate(Screen.ProductDetails.createRoute(productId))
                    }
                )
            }
            composable(Screen.MyChats.route) {
                MyChatsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToChat = { chatId ->
                        navController.navigate(Screen.Chat.createRoute(chatId))
                    },
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) }
                )
            }
            composable(
                route = Screen.Chat.route,
                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("productId") ?: backStackEntry.arguments?.getString("chatId")
                ChatScreen(
                    chatId = chatId,
                    onBack = { navController.popBackStack() },
                    onNavigateToSell = { navController.navigate(Screen.SellProduct.route) }
                )
            }
            composable(Screen.Offers.route) {
                OffersScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToDetails = { productId ->
                        navController.navigate(Screen.ProductDetails.createRoute(productId))
                    },
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) }
                )
            }
            composable(Screen.ReceivedOffers.route) {
                ReceivedOffersScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToDetails = { productId ->
                        navController.navigate(Screen.ProductDetails.createRoute(productId))
                    }
                )
            }
            composable(Screen.BookExchange.route) {
                BookExchangeScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToChat = { chatId ->
                        navController.navigate(Screen.Chat.createRoute(chatId))
                    },
                    onNavigateToProductDetails = { productId ->
                        navController.navigate(Screen.ProductDetails.createRoute(productId))
                    },
                    onNavigateToOffers = { navController.navigate(Screen.Offers.route) },
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    authViewModel = authViewModel,
                    onNavigateToListings = { navController.navigate(Screen.MyListings.route) },
                    onNavigateToPurchases = { navController.navigate(Screen.MyPurchases.route) },
                    onNavigateToOffers = { navController.navigate(Screen.Offers.route) },
                    onNavigateToReceivedOffers = { navController.navigate(Screen.ReceivedOffers.route) },
                    onNavigateToSellerRequests = { navController.navigate(Screen.SellerRequests.route) },
                    onNavigateToMyChats = { navController.navigate(Screen.MyChats.route) },
                    onNavigateToWishlist = { navController.navigate(Screen.Wishlist.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToAdmin = { navController.navigate(Screen.AdminDashboard.route) },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.AdminDashboard.route) {
                AdminDashboardScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
