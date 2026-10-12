package dz.menuisier.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * MenuisierDZ "Bois" identity — warm wood tones with a steel-blue workshop accent.
 * Tokens from Shared/OpenPencil/menuisier-dz/design-v2.html (design source of truth).
 */
private val BoisLight = lightColorScheme(
    primary = Color(0xFF7A4E24),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDCC2),
    onPrimaryContainer = Color(0xFF2C1600),
    secondary = Color(0xFF5B6148),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E6CE),
    onSecondaryContainer = Color(0xFF191E0C),
    tertiary = Color(0xFF006874),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF9EEFFD),
    onTertiaryContainer = Color(0xFF001F24),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF201A16),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF201A16),
    surfaceVariant = Color(0xFFF0E0D2),
    onSurfaceVariant = Color(0xFF51443B),
    outline = Color(0xFF837469),
    outlineVariant = Color(0xFFD3C3B5)
)

private val BoisDark = darkColorScheme(
    primary = Color(0xFFFFB77C),
    onPrimary = Color(0xFF4A2400),
    primaryContainer = Color(0xFF5F3700),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFFC4CAB2),
    onSecondary = Color(0xFF2D331F),
    secondaryContainer = Color(0xFF434934),
    onSecondaryContainer = Color(0xFFE0E6CE),
    tertiary = Color(0xFF4FD8EB),
    onTertiary = Color(0xFF00363B),
    tertiaryContainer = Color(0xFF004F57),
    onTertiaryContainer = Color(0xFF9EEFFD),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF18120E),
    onBackground = Color(0xFFEDE0D8),
    surface = Color(0xFF18120E),
    onSurface = Color(0xFFEDE0D8),
    surfaceVariant = Color(0xFF51443B),
    onSurfaceVariant = Color(0xFFD3C3B5),
    outline = Color(0xFF9C8B80),
    outlineVariant = Color(0xFF51443B)
)

/**
 * MenuisierDZ theme. Brand identity first (no dynamic color) so the workshop
 * look is identical on every device. Dark mode follows the system unless the
 * user overrides it in-app.
 */
@Composable
fun DzTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) BoisDark else BoisLight, content = content)
}

/**
 * Brand app bar. Surface + status-bar padding instead of TopAppBar so it
 * behaves the same across Material3 versions.
 */
@Composable
fun MenuHeader(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, modifier = modifier) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            trailing?.invoke()
        }
    }
}

/** Small uppercase section heading above input groups. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/** Empty state with a tinted icon badge; the icon is decorative. */
@Composable
fun MenuEmptyState(icon: ImageVector, title: String, hint: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(50),
            modifier = Modifier.size(96.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
