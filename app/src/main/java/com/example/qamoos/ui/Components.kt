package com.example.qamoos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qamoos.data.UnifiedEntry
import com.example.qamoos.ui.theme.Manjari
import com.example.qamoos.ui.theme.ScheherazadeNew
import com.example.qamoos.utils.LanguageUtils

fun cleanHtml(text: String?): String {
    if (text == null) return ""
    return text.replace(Regex("<br\\s*/?>"), "\n")
               .replace(Regex("</p>"), "\n")
               .replace(Regex("<[^>]*>"), "") // Remove any other tags
               .replace(Regex("\n{2,}"), "\n") // Collapse multiple newlines into one
               .replace("&nbsp;", " ")
               .replace("&amp;", "&")
               .replace("&lt;", "<")
               .replace("&gt;", ">")
               .replace("&quot;", "\"")
               .replace("&#39;", "'")
               .trim()
}

@Composable
fun DictionaryResultCard(
    entry: UnifiedEntry,
    fontSizeMultiplier: Float,
    isAlternate: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAlternate) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            }
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = entry.dictionaryName,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontFamily = Manjari,
                        fontSize = 10.sp * fontSizeMultiplier
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                
                val useManjariForWord = LanguageUtils.isMalayalam(entry.word) || entry.dictionaryName.contains("Malayalam", ignoreCase = true)
                
                Text(
                    text = entry.word ?: "",
                    style = TextStyle(
                        fontFamily = if (useManjariForWord) Manjari else ScheherazadeNew,
                        fontSize = (if (useManjariForWord) 18 else 22).sp * fontSizeMultiplier,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = if (useManjariForWord) TextAlign.Left else TextAlign.Right,
                        textDirection = if (useManjariForWord) TextDirection.Ltr else TextDirection.Rtl
                    )
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            
            val isLtr = LanguageUtils.isLtrContent(entry)
            val isMal = LanguageUtils.isMalayalam(entry.meaning) || entry.dictionaryName.contains("Malayalam", ignoreCase = true)
            
            val displayMeaning = if (entry.meaning?.contains("[[") == true) {
                if (entry.meaning.contains("[[SUBHEADING]]")) {
                    val sub = entry.meaning.substringAfter("[[SUBHEADING]]", "").substringBefore("[[MEANING]]").trim()
                    val mean = cleanHtml(entry.meaning.substringAfter("[[MEANING]]", ""))
                    if (sub.isNotEmpty()) "$sub: $mean" else mean
                } else {
                    val arPart = entry.meaning.substringAfter("[[ARABIC]]", "").substringBefore("[[MALAYALAM]]").substringBefore("[[ENGLISH]]").replace(Regex("\\[\\[.*?\\]\\]"), "").trim()
                    val malPart = entry.meaning.substringAfter("[[MALAYALAM]]", "").substringBefore("[[ENGLISH]]").substringBefore("[[ARABIC]]").replace(Regex("\\[\\[.*?\\]\\]"), "").trim()
                    val engPart = entry.meaning.substringAfter("[[ENGLISH]]", "").substringBefore("[[ARABIC]]").substringBefore("[[MALAYALAM]]").replace(Regex("\\[\\[.*?\\]\\]"), "").trim()
                    listOfNotNull(
                        if (arPart.isNotEmpty()) arPart else null,
                        if (malPart.isNotEmpty()) malPart else null,
                        if (engPart.isNotEmpty()) engPart else null
                    ).joinToString(" | ")
                }
            } else {
                cleanHtml(entry.meaning)
            }
            
            Text(
                text = displayMeaning,
                style = TextStyle(
                    fontFamily = if (isLtr || isMal) Manjari else ScheherazadeNew,
                    fontSize = (if (isMal) 16 else if (!isLtr) 18 else 14).sp * fontSizeMultiplier,
                    lineHeight = (if (isMal) 22 else if (!isLtr) 24 else 20).sp * fontSizeMultiplier,
                    textAlign = if (isLtr || isMal) TextAlign.Left else TextAlign.Right,
                    textDirection = if (isLtr || isMal) TextDirection.Ltr else TextDirection.Rtl,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun EmptyStateView(icon: ImageVector, message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(100.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontFamily = Manjari,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
