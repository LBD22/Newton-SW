package ru.newton.fieldapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ru.newton.fieldapp.core.ui.theme.NewtonTheme

/**
 * Field Blue text-field per spec §6.6 — micro-cap floating label on top,
 * Title-L mono value as the editable content. The container picks up a brand
 * border + focus ring while focused.
 *
 * Designed for short tokens: point names, codes, antenna heights. For
 * multi-line inputs (notes, descriptions) use a vanilla M3 `OutlinedTextField`.
 */
@Composable
fun FieldTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val colors = NewtonTheme.colors
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (focused) colors.surface else colors.surfaceTint)
            .border(
                width = if (focused) 1.5.dp else 1.dp,
                color = if (focused) colors.brand else colors.hairline,
                shape = shape,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.W800),
                color = if (focused) colors.brand else colors.text2,
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                cursorBrush = SolidColor(colors.brand),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = VisualTransformation.None,
                textStyle = (if (mono) NewtonTheme.mono.monoMedium else MaterialTheme.typography.titleLarge)
                    .copy(
                        color = colors.text,
                        fontWeight = FontWeight.W700,
                    ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            style = (if (mono) NewtonTheme.mono.monoMedium else MaterialTheme.typography.titleLarge)
                                .copy(color = colors.text3),
                        )
                    }
                    inner()
                },
            )
        }
        if (trailingContent != null) {
            trailingContent()
        }
    }
}
