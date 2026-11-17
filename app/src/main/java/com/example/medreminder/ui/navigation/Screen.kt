package com.example.medreminder.ui.navigation

sealed class Screen(val route : String, val title : String){
    object Schedule : Screen("schedule","Today's Schedule")
    object Medications : Screen("medications","My Medications")
    object Logs : Screen("logs","Health Logs")
    object Detail : Screen("detail/{medId}","Medication Detail"){
        fun createRoute(medId : Long) = "detail/$medId"
    }
    object Profile : Screen("profile","Profile")
}
