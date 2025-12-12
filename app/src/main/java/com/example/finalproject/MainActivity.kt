package com.example.finalproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.finalproject.data.DinnerDatabase
import com.example.finalproject.data.Dinner
import com.example.finalproject.data.FamilyMember
import com.example.finalproject.ui.DinnerViewModel
import com.example.finalproject.ui.DinnerViewModelFactory
import com.example.finalproject.ui.theme.FinalProjectTheme
import com.example.finalproject.ui.theme.PrimaryGreen
import com.example.finalproject.ui.theme.LightGreen
import com.example.finalproject.ui.theme.DarkGreen
import com.example.finalproject.ui.theme.TextBlack
import com.example.finalproject.ui.theme.TextGray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Database and ViewModel
        val applicationScope = CoroutineScope(SupervisorJob())
        val database = DinnerDatabase.getDatabase(this, applicationScope)
        val viewModel: DinnerViewModel by viewModels {
            DinnerViewModelFactory(database.dinnerDao())
        }

        setContent {
            FinalProjectTheme {
                DinnerApp(viewModel)
            }
        }
    }
}

@Composable
fun DinnerApp(viewModel: DinnerViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen(navController, viewModel) }
            composable("schedule") { ScheduleScreen(viewModel) }
            composable("topics") { TopicsScreen(viewModel) }
            composable("family") { FamilyScreen(viewModel) }
        }
    }
}

// --- Screens ---

@Composable
fun HomeScreen(navController: NavController, viewModel: DinnerViewModel) {
    val dinners by viewModel.allDinners.collectAsStateWithLifecycle()

    // Calculate stats
    val dinnerCount = dinners.size
    val nextDinner = dinners.firstOrNull()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).background(Color(0xFFFAFAFA))) {
        Text("Dinner with the Family", style = MaterialTheme.typography.headlineMedium, color = PrimaryGreen, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // Tonight's Dinner Card
        Card(
            colors = CardDefaults.cardColors(containerColor = LightGreen),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Next Scheduled Dinner", style = MaterialTheme.typography.titleLarge, color = DarkGreen)
                if (nextDinner != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(nextDinner.time, style = MaterialTheme.typography.displayMedium, color = TextBlack, fontWeight = FontWeight.Bold)
                    Text("Date: ${nextDinner.date}", style = MaterialTheme.typography.bodyLarge)
                    Text("Attendees: ${nextDinner.attendees}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { /* Simple interaction */ },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("I'm Ready!") }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text("No dinners scheduled yet.", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { navController.navigate("schedule") },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Schedule Now") }
                }
            }
        }

        // Stats
        Text("Your Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFA000))
                Spacer(Modifier.width(8.dp))
                Text("$dinnerCount dinners planned total! Keep it up!")
            }
        }

        // Quick Actions
        Spacer(Modifier.height(16.dp))
        Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { navController.navigate("topics") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Get Conversation Starter", color = TextBlack) }
    }
}

@Composable
fun ScheduleScreen(viewModel: DinnerViewModel) {
    val dinners by viewModel.allDinners.collectAsStateWithLifecycle()
    val familyMembers by viewModel.allFamilyMembers.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AddDinnerDialog(
            familyMembers = familyMembers,
            onDismiss = { showDialog = false },
            onConfirm = { date, time, attendees ->
                viewModel.addDinner(date, time, attendees)
                showDialog = false
            }
        )
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Schedule Dinners", style = MaterialTheme.typography.headlineSmall, color = PrimaryGreen)
            Spacer(Modifier.height(16.dp))

            if (dinners.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("No dinners found. Schedule one!", color = TextGray)
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dinners) { dinner ->
                    Card(
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(dinner.date, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(dinner.time, color = TextGray)
                                Spacer(Modifier.height(4.dp))
                                Text("With: ${dinner.attendees}", style = MaterialTheme.typography.bodySmall)
                            }
                            SuggestionChip(onClick = {}, label = { Text("Scheduled") })
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showDialog = true },
            containerColor = PrimaryGreen,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Dinner")
        }
    }
}

@Composable
fun TopicsScreen(viewModel: DinnerViewModel) {
    var currentTopic by remember { mutableStateOf("Tap a category below!") }

    // Categories to click
    val categories = listOf("Random", "Gratitude", "Goals", "Creative")
    val colors = listOf(Color(0xFF4285F4), Color(0xFF34A853), Color(0xFFFBBC05), Color(0xFFAA46BB))

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Conversation Starters", style = MaterialTheme.typography.headlineSmall, color = PrimaryGreen)

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(150.dp),
            colors = CardDefaults.cardColors(containerColor = LightGreen),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(
                    currentTopic,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Text("Tap a category to get a new topic:", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))

        // Grid of Categories
        Column {
            val chunked = categories.zip(colors).chunked(2)
            chunked.forEach { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowItems.forEach { (name, color) ->
                        Card(
                            modifier = Modifier.weight(1f).height(100.dp).clickable {
                                // Fetch random topic from DB
                                viewModel.getRandomTopic { topic ->
                                    currentTopic = topic?.text ?: "No topics found in DB!"
                                }
                            },
                            colors = CardDefaults.cardColors(containerColor = color)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun FamilyScreen(viewModel: DinnerViewModel) {
    val familyMembers by viewModel.allFamilyMembers.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddFamilyDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, role ->
                viewModel.addFamilyMember(name, role)
                showAddDialog = false
            }
        )
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Family Members", style = MaterialTheme.typography.headlineSmall, color = PrimaryGreen)
            Spacer(Modifier.height(16.dp))

            if (familyMembers.isEmpty()) {
                Text("No family members added yet. Add someone!", modifier = Modifier.padding(8.dp), color = TextGray)
            }

            LazyColumn {
                items(familyMembers) { member ->
                    FamilyMemberCard(member, viewModel)
                }
            }
        }
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = PrimaryGreen,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = "Add Member")
        }
    }
}

@Composable
fun FamilyMemberCard(member: FamilyMember, viewModel: DinnerViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Surface(
                shape = CircleShape,
                color = if(member.isOnline) PrimaryGreen else Color.LightGray,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(member.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
            Spacer(Modifier.width(16.dp))
            // Info
            Column(Modifier.weight(1f)) {
                Text(member.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(member.role, style = MaterialTheme.typography.bodySmall, color = TextGray)
            }
            // Actions
            IconButton(onClick = { viewModel.toggleMemberStatus(member) }) {
                Icon(
                    imageVector = if(member.isOnline) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Toggle Status",
                    tint = if(member.isOnline) PrimaryGreen else Color.Gray
                )
            }
            IconButton(onClick = { viewModel.deleteFamilyMember(member) }) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}

// --- Dialogs ---

@Composable
fun AddDinnerDialog(familyMembers: List<FamilyMember>, onDismiss: () -> Unit, onConfirm: (String, String, List<String>) -> Unit) {
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }

    // Multi-select state
    val selectedMembers = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Dinner") },
        text = {
            Column {
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (e.g., Oct 3)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time (e.g., 6:30 PM)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Text("Who is attending?", fontWeight = FontWeight.Bold)
                if (familyMembers.isEmpty()) Text("Add family members in the Family tab first!", color = Color.Red, fontSize = 12.sp)

                // Simple list of checkboxes for attendees
                LazyColumn(modifier = Modifier.height(150.dp)) {
                    items(familyMembers) { member ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                            if (selectedMembers.contains(member.name)) selectedMembers.remove(member.name) else selectedMembers.add(member.name)
                        }) {
                            Checkbox(checked = selectedMembers.contains(member.name), onCheckedChange = {
                                if (it) selectedMembers.add(member.name) else selectedMembers.remove(member.name)
                            })
                            Text(member.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if(date.isNotBlank() && time.isNotBlank()) {
                    onConfirm(date, time, selectedMembers.toList())
                }
            }) { Text("Schedule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddFamilyDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Family Member") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role (e.g. Dad, Child)") })
            }
        },
        confirmButton = {
            Button(onClick = {
                if(name.isNotBlank() && role.isNotBlank()) {
                    onConfirm(name, role)
                }
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// --- Navigation Components ---
@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem("Home", "home", Icons.Default.Home),
        BottomNavItem("Schedule", "schedule", Icons.Default.DateRange),
        BottomNavItem("Topics", "topics", Icons.Default.List),
        BottomNavItem("Family", "family", Icons.Default.Person)
    )
    NavigationBar(containerColor = Color.White) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryGreen),
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

data class BottomNavItem(val title: String, val route: String, val icon: ImageVector)