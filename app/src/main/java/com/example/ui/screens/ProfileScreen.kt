package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.local.UserProfileEntity
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueBg

data class AchievementBadge(
    val title: String,
    val description: String,
    val emoji: String,
    val isUnlocked: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfileEntity,
    onUpdateProfile: (String, String, String, String) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryBlueBg,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Student Profile & Analytics",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Track your study milestones & achievements",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Profile",
                            tint = PrimaryBlue
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp)
                .padding(bottom = 80.dp)
                .testTag("profile_container")
        ) {
            // Student Profile Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar Image Asset
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(R.drawable.student_avatar_1785144682072)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Student Avatar",
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .border(3.dp, PrimaryBlue, CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = userProfile.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${userProfile.major} • ${userProfile.university}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = PrimaryBlueBg,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Target GPA: ",
                                style = MaterialTheme.typography.bodySmall.copy(color = PrimaryBlue)
                            )
                            Text(
                                text = "${userProfile.currentGpa} / 4.0",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Daily Study Streak Section ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_streak_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🔥",
                                fontSize = 28.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Daily Study Streak",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                )
                                Text(
                                    text = "Complete 1+ task daily to keep the fire burning",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Surface(
                            color = PrimaryBlue,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "${userProfile.effectiveStreak} Days",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Streak Status Banner
                    val (statusBg, statusTxt, statusMsg) = when {
                        userProfile.hasCompletedToday -> Triple(
                            AccentEmerald.copy(alpha = 0.15f),
                            AccentEmerald,
                            "🎉 Study Task Completed Today! Streak is Active!"
                        )
                        userProfile.effectiveStreak > 0 -> Triple(
                            PrimaryBlueBg,
                            PrimaryBlue,
                            "⚡ Complete at least 1 study task today to reach ${userProfile.effectiveStreak + 1} days!"
                        )
                        else -> Triple(
                            AccentRose.copy(alpha = 0.15f),
                            AccentRose,
                            "💪 Streak Reset — Complete a study task today to start a new streak!"
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = statusMsg,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = statusTxt
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 7-Day Consistency Visual Indicator
                    Text(
                        text = "Past 7 Days Consistency",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                    val currentDayIndex = (java.time.LocalDate.now().dayOfWeek.value - 1) % 7

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        dayLabels.forEachIndexed { index, dayLabel ->
                            val isCompletedDay = if (userProfile.hasCompletedToday) {
                                index <= currentDayIndex && (currentDayIndex - index) < userProfile.effectiveStreak
                            } else {
                                index < currentDayIndex && (currentDayIndex - 1 - index) < userProfile.effectiveStreak
                            }
                            val isToday = index == currentDayIndex

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = when {
                                        isCompletedDay -> AccentEmerald
                                        isToday -> PrimaryBlueBg
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    border = if (isToday && !isCompletedDay) BorderStroke(2.dp, PrimaryBlue) else null,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isCompletedDay) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else if (isToday) {
                                            Text(
                                                text = "🔥",
                                                fontSize = 14.sp
                                            )
                                        } else {
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dayLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isToday) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Study Statistics Grid
            Text(
                text = "Study Performance Analytics",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Hours Studied",
                    value = "${userProfile.totalHoursStudied}h",
                    icon = Icons.Filled.Timer,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Quizzes Taken",
                    value = "${userProfile.totalQuizzesCompleted}",
                    icon = Icons.Filled.MilitaryTech,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Tasks Completed",
                    value = "${userProfile.totalTasksCompleted}",
                    icon = Icons.Filled.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Active Streak",
                    value = "${userProfile.effectiveStreak} Days 🔥",
                    icon = Icons.Filled.Star,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Achievements & Badges
            Text(
                text = "Academic Achievements & Badges",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(12.dp))

            val badges = listOf(
                AchievementBadge("7-Day Study Streak", "Studied consistently for 7 consecutive days", "🔥", userProfile.effectiveStreak >= 7),
                AchievementBadge("Quiz Master", "Completed 10+ AI quizzes with >80% score", "🏆", true),
                AchievementBadge("AI Explorer", "Asked Gemini AI over 25 academic questions", "🤖", true),
                AchievementBadge("Night Owl", "Completed study session after 10 PM", "🦉", true),
                AchievementBadge("GPA Crusher", "Maintained target GPA above 3.8", "🎓", true),
                AchievementBadge("Perfect Score", "Scored 100% on a hard difficulty quiz", "⚡", false)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                badges.forEach { badge ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (badge.isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = badge.emoji,
                                fontSize = 28.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = badge.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = badge.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            if (badge.isUnlocked) {
                                Surface(
                                    color = AccentEmerald.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Unlocked",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = AccentEmerald,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Locked",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Integration & CI/CD Pipeline Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlueBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "System Integration Status",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Gemini 3.5 Flash AI Engine: Active", style = MaterialTheme.typography.bodySmall.copy(color = PrimaryBlue))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "GitHub CI/CD Automated Build Pipeline: Ready", style = MaterialTheme.typography.bodySmall.copy(color = PrimaryBlue))
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditProfileModal(
            currentProfile = userProfile,
            onDismiss = { showEditDialog = false },
            onSave = { name, uni, major, gpa ->
                onUpdateProfile(name, uni, major, gpa)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = PrimaryBlueBg,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileModal(
    currentProfile: UserProfileEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentProfile.name) }
    var uni by remember { mutableStateOf(currentProfile.university) }
    var major by remember { mutableStateOf(currentProfile.major) }
    var gpa by remember { mutableStateOf(currentProfile.currentGpa) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Student Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_name_input")
                )
                OutlinedTextField(
                    value = uni,
                    onValueChange = { uni = it },
                    label = { Text("University") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_uni_input")
                )
                OutlinedTextField(
                    value = major,
                    onValueChange = { major = it },
                    label = { Text("Major") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_major_input")
                )
                OutlinedTextField(
                    value = gpa,
                    onValueChange = { gpa = it },
                    label = { Text("Target GPA") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_gpa_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, uni, major, gpa) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
