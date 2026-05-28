package com.mettyoung.deconstructchinese.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mettyoung.deconstructchinese.ui.theme.BluePrimary
import com.mettyoung.deconstructchinese.ui.theme.Divider
import com.mettyoung.deconstructchinese.ui.theme.Surface
import com.mettyoung.deconstructchinese.ui.theme.TextPrimary
import com.mettyoung.deconstructchinese.ui.theme.TextSecondary

@Composable
fun ApiKeyDialog(
    currentApiKey: String,
    useSimplified: Boolean,
    onDismiss: () -> Unit,
    onApiKeySubmit: (String) -> Unit,
    onUseSimplifiedChange: (Boolean) -> Unit
) {
    var keyInput by remember { mutableStateOf(currentApiKey) }
    var showKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = MaterialTheme.shapes.large,
        title = {
            Text(
                "Settings", 
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                // Script Preference
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("CHINESE SCRIPT")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(BluePrimary.copy(alpha = 0.04f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                if (useSimplified) "Simplified (简体)" else "Traditional (繁體)",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                if (useSimplified) "Preferred for Mainland China" else "Preferred for Taiwan/HK",
                                color = TextSecondary.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = useSimplified,
                            onCheckedChange = onUseSimplifiedChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BluePrimary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Divider
                            ),
                            modifier = Modifier.size(44.dp, 24.dp)
                        )
                    }
                }

                // API Key
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("OPENAI API KEY")
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        placeholder = { Text("sk-...", color = TextSecondary.copy(alpha = 0.3f)) },
                        visualTransformation = if (showKey) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showKey) "Toggle visibility" else "Toggle visibility",
                                    tint = TextSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BluePrimary,
                            unfocusedBorderColor = Divider.copy(alpha = 0.5f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = BluePrimary
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp)
                    )
                    Text(
                        "Your key is stored locally on your device.",
                        color = TextSecondary.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApiKeySubmit(keyInput.trim()); onDismiss() },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                modifier = Modifier.height(44.dp).padding(horizontal = 8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(44.dp)
            ) {
                Text("Cancel", color = TextSecondary, fontWeight = FontWeight.Medium)
            }
        }
    )
}
