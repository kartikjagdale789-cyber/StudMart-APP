package com.kartik.studentmart.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.kartik.studentmart.navigation.Screen

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem(Screen.Home.route, "Home", Icons.Default.Home)
    object Categories : BottomNavItem(Screen.Categories.route, "Categories", Icons.Default.List)
    object Sell : BottomNavItem(Screen.SellProduct.route, "Sell", Icons.Default.AddCircle)
    object Wishlist : BottomNavItem(Screen.Wishlist.route, "Wishlist", Icons.Default.Favorite)
    object Profile : BottomNavItem(Screen.Profile.route, "Profile", Icons.Default.Person)
}

@Composable
fun StudMartBottomNav(
    currentRoute: String?,
    onItemClick: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Categories,
        BottomNavItem.Sell,
        BottomNavItem.Wishlist,
        BottomNavItem.Profile
    )

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        onItemClick(item.route)
                    }
                }
            )
        }
    }
}
