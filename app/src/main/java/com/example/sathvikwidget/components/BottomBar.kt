package com.example.sathvikwidget.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Configure: Route

    @Serializable
    data object Home:Route
}

data class NavBarItems(val route: Route, val icon: ImageVector, val label:String)


@Composable
fun BottomNavBar(navController: NavController){

    val bottomNavItems = listOf<NavBarItems>(
        NavBarItems(Route.Home, icon = Icons.Rounded.Home, label = "Home"),
        NavBarItems(Route.Configure, Icons.Rounded.Build, label = "Configure"),
    )
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination

    NavigationBar{
        bottomNavItems.forEach { item->
            NavigationBarItem(onClick = {
                navController.navigate(item.route){
                    launchSingleTop=true
                    popUpTo(navController.graph.findStartDestination().id){
                        saveState=true
                    }
                    restoreState=true
                } },
                label = { Text(item.label) },
                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                selected = currentRoute?.hierarchy?.any {
                    it.hasRoute(item.route::class)
                }==true
            )

        }
    }

}