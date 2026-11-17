package io.github.algorandecosystem.example.coinflipper.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

var passphraseTextField: String = ""

/** Formats a microAlgo amount as ALGO, e.g. 9938000 -> "9.938 ALGO". */
fun formatAlgos(microAlgos: Long): String {
    val whole = microAlgos / 1_000_000
    val fraction = (microAlgos % 1_000_000).toString().padStart(6, '0').trimEnd('0')
    return if (fraction.isEmpty()) "$whole ALGO" else "$whole.$fraction ALGO"
}

/**
 * A full-height column that supports pull-to-refresh. The content scrolls (needed for the pull
 * gesture) but is at least as tall as the screen, so [verticalArrangement] still spaces it out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshableColumn(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.SpaceEvenly,
    content: @Composable ColumnScope.() -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = horizontalAlignment,
                verticalArrangement = verticalArrangement,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight),
                content = content,
            )
        }
    }
}

@Composable
fun AlgorandButton(
    stringResourceId: StringResource,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        // colors = ButtonDefaults.buttonColors(colorResource(Color.)),
        shape = RoundedCornerShape(8.dp),
        modifier =
            Modifier
                .width(190.dp)
                .height(50.dp),
    ) {
        Text(
            stringResource(resource = stringResourceId),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )
    }
}

@Composable
fun AlgorandDivider() {
    HorizontalDivider(
        modifier =
            Modifier
                .width(300.dp),
        thickness = 1.dp,
        color = Color.Gray,
    )
}

@Composable
fun PassphraseField(
    label: String,
    textData: String,
    placeholder: String? = null,
) {
    var textInput by remember { mutableStateOf(textData) }
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = textInput,
        textStyle = TextStyle.Default.copy(fontSize = 16.sp),
        onValueChange = {
            textInput = it
            passphraseTextField = it
        },
        label = {
            Text(
                text = label,
                color = Color.Black,
            )
        },
        placeholder = placeholder?.let { { Text(it) } },
        // A recovery phrase is a secret: no autocorrect, suggestions or keyboard learning.
        keyboardOptions =
            KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        colors = OutlinedTextFieldDefaults.colors(),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 30.dp, end = 30.dp),
    )
}

@Composable
fun ShowSnackbar(message: String) {
    val snackState = remember { SnackbarHostState() }
    val snackScope = rememberCoroutineScope()

    SnackbarHost(
        modifier = Modifier,
        hostState = snackState,
    ) {
        Snackbar(
            snackbarData = it,
            containerColor = Color.White,
            contentColor = Color.Black,
        )
    }

    LaunchedEffect(Unit) {
        snackScope.launch { snackState.showSnackbar(message) }
        // openSnackbar(false)
    }
}
