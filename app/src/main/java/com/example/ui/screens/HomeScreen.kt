package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ui.MainTab
import com.example.ui.MawaeeViewModel
import com.example.ui.dialogs.AppointmentDialog
import com.example.ui.dialogs.MedicationDialog
import com.example.ui.dialogs.ProfileDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MawaeeViewModel,
    navController: NavController? = null
) {
    val profiles by viewModel.profiles.collectAsState()
    val selectedProfileId by viewModel.selectedProfileId.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showSearch by remember { mutableStateOf(false) }
    var showAddAppointmentDialog by remember { mutableStateOf(false) }
    var showAddMedicationDialog by remember { mutableStateOf(false) }
    var showAddProfileDialog by remember { mutableStateOf(false) }

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("بحث عن موعد، طبيب، أو دواء...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("search_input")
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "مواعيد العلاج",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (showSearch) {
                        IconButton(onClick = {
                            showSearch = false
                            viewModel.updateSearchQuery("")
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    }
                },
                actions = {
                    if (showSearch) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح البحث")
                            }
                        }
                    } else {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, contentDescription = "بحث")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == MainTab.APPOINTMENTS,
                    onClick = { viewModel.selectTab(MainTab.APPOINTMENTS) },
                    icon = { Icon(Icons.Default.EventNote, contentDescription = null) },
                    label = { Text("المواعيد") },
                    modifier = Modifier.testTag("tab_appointments")
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.CALENDAR,
                    onClick = { viewModel.selectTab(MainTab.CALENDAR) },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    label = { Text("التقويم") },
                    modifier = Modifier.testTag("tab_calendar")
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.MEDICATIONS,
                    onClick = { viewModel.selectTab(MainTab.MEDICATIONS) },
                    icon = { Icon(Icons.Default.Medication, contentDescription = null) },
                    label = { Text("الأدوية") },
                    modifier = Modifier.testTag("tab_medications")
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.PROFILES,
                    onClick = { viewModel.selectTab(MainTab.PROFILES) },
                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                    label = { Text("الملفات") },
                    modifier = Modifier.testTag("tab_profiles")
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.STATS,
                    onClick = { viewModel.selectTab(MainTab.STATS) },
                    icon = { Icon(Icons.Default.Insights, contentDescription = null) },
                    label = { Text("نظرة عامة") },
                    modifier = Modifier.testTag("tab_stats")
                )
            }
        },
        floatingActionButton = {
            when (currentTab) {
                MainTab.APPOINTMENTS -> {
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (navController != null) {
                                navController.navigate("add_appointment")
                            } else {
                                showAddAppointmentDialog = true
                            }
                        },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("موعد جديد") },
                        modifier = Modifier.testTag("fab_add_appointment")
                    )
                }
                MainTab.MEDICATIONS -> {
                    ExtendedFloatingActionButton(
                        onClick = { showAddMedicationDialog = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("دواء جديد") },
                        modifier = Modifier.testTag("fab_add_medication")
                    )
                }
                MainTab.CALENDAR -> {
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (navController != null) {
                                navController.navigate("add_appointment")
                            } else {
                                showAddAppointmentDialog = true
                            }
                        },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("موعد جديد") },
                        modifier = Modifier.testTag("fab_calendar_add_appointment")
                    )
                }
                MainTab.PROFILES -> {
                    FloatingActionButton(
                        onClick = { showAddProfileDialog = true },
                        modifier = Modifier.testTag("fab_add_profile")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة فرد جديد")
                    }
                }
                MainTab.STATS -> {}
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Horizontal Profile Filter Bar for Appointments, Calendar, and Medications tabs
            if (currentTab == MainTab.APPOINTMENTS || currentTab == MainTab.MEDICATIONS || currentTab == MainTab.CALENDAR) {
                if (profiles.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("profile_filter_row")
                    ) {
                        item {
                            FilterChip(
                                selected = selectedProfileId == null,
                                onClick = { viewModel.selectProfile(null) },
                                label = { Text("الكل") },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        items(profiles, key = { it.id }) { p ->
                            FilterChip(
                                selected = selectedProfileId == p.id,
                                onClick = { viewModel.selectProfile(p.id) },
                                label = { Text(p.name) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 2.dp)
                                    )
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        item {
                            androidx.compose.material3.InputChip(
                                selected = false,
                                onClick = { showAddProfileDialog = true },
                                label = { Text("+ شخص جديد") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }
            }

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (currentTab) {
                    MainTab.APPOINTMENTS -> {
                        AppointmentsScreen(
                            viewModel = viewModel,
                            profiles = profiles,
                            onAddClick = {
                                if (navController != null) {
                                    navController.navigate("add_appointment")
                                } else {
                                    showAddAppointmentDialog = true
                                }
                            }
                        )
                    }
                    MainTab.CALENDAR -> {
                        CalendarScreen(
                            viewModel = viewModel,
                            profiles = profiles,
                            onAddAppointment = {
                                if (navController != null) {
                                    navController.navigate("add_appointment")
                                } else {
                                    showAddAppointmentDialog = true
                                }
                            }
                        )
                    }
                    MainTab.MEDICATIONS -> {
                        MedicationsScreen(
                            viewModel = viewModel,
                            profiles = profiles,
                            onAddClick = { showAddMedicationDialog = true }
                        )
                    }
                    MainTab.PROFILES -> {
                        ProfilesScreen(
                            viewModel = viewModel,
                            profiles = profiles,
                            onAddClick = { showAddProfileDialog = true }
                        )
                    }
                    MainTab.STATS -> {
                        OverviewScreen(
                            viewModel = viewModel,
                            profiles = profiles,
                            onAddAppointment = {
                                if (navController != null) {
                                    navController.navigate("add_appointment")
                                } else {
                                    showAddAppointmentDialog = true
                                }
                            },
                            onAddMedication = { showAddMedicationDialog = true }
                        )
                    }
                }
            }
        }
    }

    // Add Appointment Dialog
    if (showAddAppointmentDialog) {
        AppointmentDialog(
            appointment = null,
            profiles = profiles,
            initialProfileId = selectedProfileId,
            onDismiss = { showAddAppointmentDialog = false },
            onSave = { _, profileId, title, doctor, dateTime, location, notes, repeat, offsets ->
                viewModel.saveAppointment(
                    profileId = profileId,
                    title = title,
                    doctor = doctor,
                    dateTime = dateTime,
                    locationText = location,
                    notes = notes,
                    repeat = repeat,
                    reminderOffsets = offsets
                )
                showAddAppointmentDialog = false
            }
        )
    }

    // Add Medication Dialog
    if (showAddMedicationDialog) {
        MedicationDialog(
            medication = null,
            profiles = profiles,
            initialProfileId = selectedProfileId,
            onDismiss = { showAddMedicationDialog = false },
            onSave = { _, profileId, name, dose, times, startDate, endDate, frequency ->
                viewModel.saveMedication(
                    profileId = profileId,
                    name = name,
                    dose = dose,
                    times = times,
                    startDate = startDate,
                    endDate = endDate,
                    frequency = frequency
                )
                showAddMedicationDialog = false
            }
        )
    }

    // Add Profile Dialog
    if (showAddProfileDialog) {
        ProfileDialog(
            profile = null,
            onDismiss = { showAddProfileDialog = false },
            onSave = { _, name, relation ->
                viewModel.saveProfile(0L, name, relation)
                showAddProfileDialog = false
            }
        )
    }
}
