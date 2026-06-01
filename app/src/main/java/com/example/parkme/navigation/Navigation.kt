package com.example.parkme.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkme.models.ParkingLot
import com.example.parkme.models.ParkingLotHolder
import com.example.parkme.models.ReservationHolder
import com.example.parkme.screens.*
import com.example.parkme.viewmodel.AppViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth

enum class AppScreens {
    HomeUser,
    LogIn,
    SignUp,
    IdentityVerification,
    SearchMap,
    ParkingLotDetail,
    RateParkingLot,
    MyActivity,
    Operator,
    UserProfile,
    OperatorProfile,
    HomeOperator,
    MyActivityOperator,
    CreateParking,
    EditParking,
    ChatOp,
    ChatCli,
    MapPicker,
    ChatListCli,
    ChatListOp,
    TrackUserMap,
    ParkingGallery
}

@Composable
fun Navigation() {
    val navController = rememberNavController()
    val viewModel: AppViewModel = viewModel()
    val authState by viewModel.authState.collectAsState()

    if (authState.isCheckingSession) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val startRoute = when {
        authState.isAuthenticated && authState.isVerified -> {
            if (authState.userRole == "Operador") AppScreens.HomeOperator.name else AppScreens.HomeUser.name
        }
        authState.isAuthenticated && !authState.isVerified -> {
            AppScreens.IdentityVerification.name
        }
        else -> AppScreens.LogIn.name
    }

    NavHost(
        navController = navController,
        startDestination = startRoute,
        enterTransition = { fadeIn(animationSpec = tween(0)) },
        exitTransition = { fadeOut(animationSpec = tween(0)) },
        popEnterTransition = { fadeIn(animationSpec = tween(0)) },
        popExitTransition = { fadeOut(animationSpec = tween(0)) }
    ) {

        composable(AppScreens.LogIn.name) { LogIn(navController, viewModel) }
        composable(AppScreens.SignUp.name) { SignUp(navController, viewModel) }
        composable(AppScreens.IdentityVerification.name) { IdentityVerification(navController, viewModel) }
        composable(AppScreens.HomeUser.name) { HomeUser(navController) }
        composable(AppScreens.HomeOperator.name) { HomeOperator(navController) }
        composable(AppScreens.SearchMap.name) { SearchMap(navController, viewModel) }
        composable(AppScreens.RateParkingLot.name) {
            val parkingLotId = navController.previousBackStackEntry?.savedStateHandle?.get<String>("rateParkingId") ?: ""
            val reservationId = navController.previousBackStackEntry?.savedStateHandle?.get<String>("rateReservationId") ?: ""

            RateParkingLot(
                navController = navController,
                parkingLotId = parkingLotId,
                reservationId = reservationId,
                viewModel = viewModel
            )
        }
        composable(AppScreens.MyActivity.name) { MyActivity(navController,viewModel) }
        composable(AppScreens.UserProfile.name) { ProfileScreen(navController, viewModel) }
        composable(AppScreens.OperatorProfile.name) { ProfileScreen(navController, viewModel) }
        composable(AppScreens.ParkingLotDetail.name) {
            val parking = ParkingLotHolder.selected
            if (parking != null) {
                ParkingLotDetail(navController, parking)
            }
        }
        composable(AppScreens.MyActivityOperator.name) { MyActivityOperator(navController) }
        composable(AppScreens.EditParking.name) { EditParkingVisual() }

        composable(AppScreens.ChatCli.name) {
            val reservationId = ReservationHolder.selectedReservationId
            val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: "usuario_desconocido"

            if (reservationId.isNotEmpty()) {
                ChatScreen(
                    chatId = reservationId,
                    myUserId = currentUserUid,
                    isOperator = false,
                    navController = navController,
                    appViewModel = viewModel
                )
            }
        }

        composable(AppScreens.ChatOp.name) {
            val reservationId = ReservationHolder.selectedReservationId
            val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_operator"

            if (reservationId.isNotEmpty()) {
                ChatScreen(
                    chatId = reservationId,
                    myUserId = currentUserUid,
                    isOperator = true,
                    navController = navController,
                    appViewModel = viewModel
                )
            }
        }

        composable(AppScreens.ChatListCli.name) {
            ChatListScreen(navController = navController, isOperator = false)
        }

        composable(AppScreens.ChatListOp.name) {
            ChatListScreen(navController = navController, isOperator = true)
        }

        composable("${AppScreens.EditParking.name}/{parkingId}") { backStackEntry ->
            val parkingId = backStackEntry.arguments?.getString("parkingId") ?: ""
            EditParkingVisual(parkingId = parkingId, navController = navController)
        }

        composable(AppScreens.CreateParking.name) {
            CreateParkingVisual(navController = navController)
        }

        composable(AppScreens.MapPicker.name) { backStackEntry ->
            val previousBackStack = navController.previousBackStackEntry
            MapPickerScreen(
                navController = navController,
                onLocationPicked = { latLng ->
                    previousBackStack?.savedStateHandle?.set("latLng", latLng)
                }
            )
        }

        composable("${AppScreens.TrackUserMap.name}/{chatId}") { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            TrackUserMapScreen(navController = navController, chatId = chatId)
        }
    }

    LaunchedEffect(
        authState.isAuthenticated,
        authState.isVerified,
        authState.isLoading,
        authState.userRole
    ) {
        if (authState.isLoading) return@LaunchedEffect

        val currentRoute = navController.currentBackStackEntry?.destination?.route

        when {
            authState.isAuthenticated && !authState.isVerified -> {
                if (currentRoute != AppScreens.IdentityVerification.name) {
                    navController.navigate(AppScreens.IdentityVerification.name) { popUpTo(0) }
                }
            }

            authState.isAuthenticated && authState.isVerified -> {
                val isOperator = authState.userRole?.equals("Operador", ignoreCase = true) == true
                val destination = if (isOperator) {
                    AppScreens.HomeOperator.name
                } else {
                    AppScreens.HomeUser.name
                }

                if (currentRoute == AppScreens.LogIn.name ||
                    currentRoute == AppScreens.SignUp.name ||
                    currentRoute == AppScreens.IdentityVerification.name) {
                    navController.navigate(destination) { popUpTo(0) }
                }
            }

            !authState.isAuthenticated -> {
                if (currentRoute != AppScreens.LogIn.name && currentRoute != AppScreens.SignUp.name) {
                    navController.navigate(AppScreens.LogIn.name) { popUpTo(0) }
                }
            }
        }
    }
}