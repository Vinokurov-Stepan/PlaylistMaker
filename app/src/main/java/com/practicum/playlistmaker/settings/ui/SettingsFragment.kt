package com.practicum.playlistmaker.settings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.practicum.playlistmaker.R
import com.practicum.playlistmaker.settings.presentation.view_model.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SettingsScreen(viewModel = koinViewModel())
            }
        }
    }
}

@Composable
private fun SettingsScreen(viewModel: SettingsViewModel) {
    val isDarkTheme = isSystemInDarkTheme()

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SettingsTitle()
        ThemeSwitch(
            isDarkTheme = isDarkTheme,
            onThemeChanged = { checked -> viewModel.updateThemeSettings(checked) })
        SettingsButton(
            text = stringResource(R.string.share_app),
            iconRes = R.drawable.to_share,
            onClick = { viewModel.shareState() })
        SettingsButton(
            text = stringResource(R.string.text_to_support),
            iconRes = R.drawable.to_support,
            onClick = { viewModel.supportingState() })
        SettingsButton(
            text = stringResource(R.string.user_agreement),
            iconRes = R.drawable.arrowforward,
            onClick = { viewModel.agreementState() })
    }
}

@Composable
private fun SettingsTitle() {
    val textColor by animateColorAsState(
        targetValue = if (isSystemInDarkTheme()) colorResource(R.color.white) else colorResource(R.color.black)
    )

    Text(
        text = stringResource(R.string.settings),
        modifier = Modifier
            .height(56.dp)
            .padding(start = 16.dp)
            .wrapContentHeight(Alignment.CenterVertically),
        fontSize = 22.sp,
        fontFamily = FontFamily(Font(R.font.ys_display_medium)),
        color = textColor
    )
}

@Composable
private fun ThemeSwitch(
    isDarkTheme: Boolean, onThemeChanged: (Boolean) -> Unit
) {
    val textColor by animateColorAsState(
        targetValue = if (isSystemInDarkTheme()) colorResource(R.color.white) else colorResource(R.color.black)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(
                onClick = { onThemeChanged(!isDarkTheme) })
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.dark_theme),
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            fontFamily = FontFamily(Font(R.font.ys_display_regular)),
            color = textColor
        )
        Switch(
            checked = isDarkTheme,
            onCheckedChange = { onThemeChanged(!isDarkTheme) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = colorResource(R.color.blue),
                uncheckedThumbColor = colorResource(R.color.light_grey),
                checkedTrackColor = colorResource(R.color.light_blue),
                uncheckedTrackColor = colorResource(R.color.grey)
            )
        )
    }
}

@Composable
private fun SettingsButton(
    text: String, iconRes: Int, onClick: () -> Unit
) {
    val textColor by animateColorAsState(
        targetValue = if (isSystemInDarkTheme()) colorResource(R.color.white) else colorResource(R.color.black)
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSystemInDarkTheme()) colorResource(R.color.white) else colorResource(R.color.light_grey)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(61.dp)
            .clickable(
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            fontFamily = FontFamily(Font(R.font.ys_display_regular)),
            color = textColor
        )
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = iconColor
        )
    }
}

@Preview(name = "settingsScreen", showSystemUi = false)
@Composable
private fun SettingsScreenPreview() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SettingsTitle()
        ThemeSwitch(
            isDarkTheme = false,
            onThemeChanged = {}
        )
        SettingsButton(
            text = stringResource(R.string.share_app),
            iconRes = R.drawable.to_share,
            onClick = {}
        )
        SettingsButton(
            text = stringResource(R.string.text_to_support),
            iconRes = R.drawable.to_support,
            onClick = {}
        )
        SettingsButton(
            text = stringResource(R.string.user_agreement),
            iconRes = R.drawable.arrowforward,
            onClick = {}
        )
    }
}
