package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.settings.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.KeyboardSettingsViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: KeyboardSettingsViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    "Keyboard Hemat",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                ) { innerPadding ->
                    SettingsScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh statuses when returning from system settings
        viewModel.checkKeyboardStatus()
        viewModel.loadSettings()
    }
}

@Composable
fun SettingsScreen(
    viewModel: KeyboardSettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val activeTheme by viewModel.activeTheme.collectAsState()
    val heightStyle by viewModel.heightStyle.collectAsState()
    val shapeStyle by viewModel.shapeStyle.collectAsState()
    val autocorrectEnabled by viewModel.autocorrectEnabled.collectAsState()
    val predictionEnabled by viewModel.predictionEnabled.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()
    val hapticDurationMs by viewModel.hapticDurationMs.collectAsState()
    val codingBarEnabled by viewModel.codingBarEnabled.collectAsState()
    val cursorArrowsEnabled by viewModel.cursorArrowsEnabled.collectAsState()
    val codeSnippetsEnabled by viewModel.codeSnippetsEnabled.collectAsState()
    val tabUsesSpaces by viewModel.tabUsesSpaces.collectAsStateWithLifecycle()
    
    val vowelOptionalEnabled by viewModel.vowelOptionalEnabled.collectAsStateWithLifecycle()
    val guessMissingLettersEnabled by viewModel.guessMissingLettersEnabled.collectAsStateWithLifecycle()
    val nextWordPredictionEnabled by viewModel.nextWordPredictionEnabled.collectAsStateWithLifecycle()
    val alwaysPredictEnabled by viewModel.alwaysPredictEnabled.collectAsStateWithLifecycle()
    val predictPasswordsEnabled by viewModel.predictPasswordsEnabled.collectAsStateWithLifecycle()

    val wordCount by viewModel.wordCount.collectAsStateWithLifecycle()
    val customWordCount by viewModel.customWordCount.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val dictionaryWords by viewModel.dictionaryWords.collectAsStateWithLifecycle()

    val isEnabled = viewModel.isKeyboardEnabled.value
    val isSelected = viewModel.isKeyboardSelected.value

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Activation & System IME Status Card
        KeyboardStatusCard(
            isEnabled = isEnabled,
            isSelected = isSelected,
            context = context
        )

        // 2. Interactive Live Keyboard Preview Card
        KeyboardPreviewCard(
            activeTheme = activeTheme,
            heightStyle = heightStyle,
            shapeStyle = shapeStyle,
            codingBarEnabled = codingBarEnabled,
            cursorArrowsEnabled = cursorArrowsEnabled,
            codeSnippetsEnabled = codeSnippetsEnabled,
            tabUsesSpaces = tabUsesSpaces
        )

        // 3. Fast Access & Text Editing Tools Card
        CodingBarCard(
            codingBarEnabled = codingBarEnabled,
            onCodingBarChange = { viewModel.updateCodingBar(it) },
            tabUsesSpaces = tabUsesSpaces,
            onTabUsesSpacesChange = { viewModel.updateTabUsesSpaces(it) },
            codeSnippetsEnabled = codeSnippetsEnabled,
            onCodeSnippetsChange = { viewModel.updateCodeSnippets(it) }
        )

        // 4. Keyboard Appearance: Theme, Height, Key Shape Card
        KeyboardAppearanceCard(
            activeTheme = activeTheme,
            onThemeChange = { viewModel.updateTheme(it) },
            heightStyle = heightStyle,
            onHeightChange = { viewModel.updateHeight(it) },
            shapeStyle = shapeStyle,
            onShapeChange = { viewModel.updateShape(it) }
        )

        // 5. Smart Prediction, Vowel Omission & Feedback Card
        SmartPredictionCard(
            predictionEnabled = predictionEnabled,
            onPredictionChange = { viewModel.updatePrediction(it) },
            nextWordPredictionEnabled = nextWordPredictionEnabled,
            onNextWordPredictionChange = { viewModel.updateNextWordPrediction(it) },
            vowelOptionalEnabled = vowelOptionalEnabled,
            onVowelOptionalChange = { viewModel.updateVowelOptional(it) },
            guessMissingLettersEnabled = guessMissingLettersEnabled,
            onGuessMissingLettersChange = { viewModel.updateGuessMissingLetters(it) },
            alwaysPredictEnabled = alwaysPredictEnabled,
            onAlwaysPredictChange = { viewModel.updateAlwaysPredict(it) },
            predictPasswordsEnabled = predictPasswordsEnabled,
            onPredictPasswordsChange = { viewModel.updatePredictPasswords(it) },
            autocorrectEnabled = autocorrectEnabled,
            onAutocorrectChange = { viewModel.updateAutocorrect(it) },
            hapticEnabled = hapticEnabled,
            onHapticChange = { viewModel.updateHaptic(it) },
            hapticDurationMs = hapticDurationMs,
            onHapticDurationChange = { viewModel.updateHapticDuration(it) }
        )

        // 6. Local User Dictionary Room Database Card
        UserDictionaryCard(
            wordCount = wordCount,
            customWordCount = customWordCount,
            searchQuery = searchQuery,
            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
            filterType = filterType,
            onFilterTypeChange = { viewModel.updateFilterType(it) },
            dictionaryWords = dictionaryWords,
            onAddWord = { word, freq -> viewModel.addUserCustomWord(word, freq) },
            onDeleteWord = { word -> viewModel.deleteWord(word) },
            onClearDictionary = { viewModel.clearDictionary() }
        )

        // 7. System Typing Test Area Card
        SystemTypingTestCard(isSelected = isSelected)

        Spacer(modifier = Modifier.height(24.dp))
    }
}
