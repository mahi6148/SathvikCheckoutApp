package com.example.sathvikwidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sathvikwidget.components.Route
import com.example.sathvikwidget.components.WidgetUpdateWorker
import com.example.sathvikwidget.screens.ConfigureScreen
import com.example.sathvikwidget.screens.HomeScreen
import com.example.sathvikwidget.ui.theme.SathvikWidgetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WidgetUpdateWorker.enqueuePeriodicWork(this)
        setContent {
            SathvikWidgetTheme {
                val navController = rememberNavController()
                Scaffold {innerPadding ->
                    NavHost(navController, startDestination = Route.Home){
                        composable<Route.Home> {
                            HomeScreen(navController,Modifier.padding(innerPadding))
                        }
                        composable<Route.Configure> {
                            ConfigureScreen(navController,Modifier.padding(innerPadding))
                        }
                    }
                }

            }
        }
    }
}