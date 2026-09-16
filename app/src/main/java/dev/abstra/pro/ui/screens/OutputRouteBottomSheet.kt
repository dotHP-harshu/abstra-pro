package dev.abstra.pro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abstra.pro.ui.theme.LocalAbstraColors

data class AvailableRoute(
    val id: String,
    val name: String,
    val isSelected: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutputRouteBottomSheet(
    routes: List<AvailableRoute>,
    onRouteSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAbstraColors.current
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.bgBackground
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Play on…", 
                fontSize = 17.sp, 
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(Modifier.height(12.dp))
            routes.forEach { route ->
                ListItem(
                    headlineContent = { Text(route.name, color = colors.textPrimary) },
                    trailingContent = {
                        if (route.isSelected) Icon(Icons.Default.Check, null, tint = colors.primary)
                    },
                    modifier = Modifier.clickable {
                        onRouteSelected(route.id)
                        onDismiss()
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = colors.bgBackground
                    )
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
