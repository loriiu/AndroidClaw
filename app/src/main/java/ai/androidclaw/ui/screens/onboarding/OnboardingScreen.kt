package ai.androidclaw.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ai.androidclaw.domain.model.LlmProviderType

/**
 * 引导流程界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 完成时回调
    LaunchedEffect(uiState.currentStep) {
        if (uiState.currentStep == OnboardingStep.COMPLETE) {
            // 等待用户点击完成
        }
    }
    
    Scaffold(
        topBar = {
            if (uiState.currentStep != OnboardingStep.WELCOME && 
                uiState.currentStep != OnboardingStep.COMPLETE) {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.previousStep() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState.currentStep) {
                OnboardingStep.WELCOME -> WelcomeStep(
                    onNext = { viewModel.nextStep() }
                )
                
                OnboardingStep.PROVIDER -> ProviderStep(
                    selectedProvider = uiState.selectedProvider,
                    onSelectProvider = { viewModel.selectProvider(it) },
                    onNext = { viewModel.nextStep() }
                )
                
                OnboardingStep.CREDENTIALS -> CredentialsStep(
                    provider = uiState.selectedProvider ?: LlmProviderType.OPENAI,
                    apiKey = uiState.apiKey,
                    model = uiState.model,
                    baseUrl = uiState.baseUrl,
                    onApiKeyChange = { viewModel.updateApiKey(it) },
                    onModelChange = { viewModel.updateModel(it) },
                    onBaseUrlChange = { viewModel.updateBaseUrl(it) },
                    onComplete = {
                        viewModel.complete()
                        onComplete()
                    }
                )
                
                OnboardingStep.COMPLETE -> CompleteStep(
                    onComplete = {
                        viewModel.complete()
                        onComplete()
                    }
                )
            }
        }
    }
}

/**
 * 欢迎步骤
 */
@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "AndroidClaw",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Your personal AI assistant on Android.\nConfigure your LLM provider to get started.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Started")
        }
    }
}

/**
 * 选择提供商步骤
 */
@Composable
fun ProviderStep(
    selectedProvider: LlmProviderType?,
    onSelectProvider: (LlmProviderType) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Text(
            text = "Select LLM Provider",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Choose the AI provider you want to use",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LlmProviderType.entries.forEach { provider ->
            ProviderCard(
                provider = provider,
                isSelected = selectedProvider == provider,
                onClick = { onSelectProvider(provider) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedProvider != null
        ) {
            Text("Next")
        }
    }
}

@Composable
fun ProviderCard(
    provider: LlmProviderType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = getProviderDescription(provider),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getProviderDescription(provider: LlmProviderType): String {
    return when (provider) {
        LlmProviderType.OPENAI -> "GPT-4o, GPT-4-Turbo, GPT-3.5"
        LlmProviderType.ANTHROPIC -> "Claude 3.5 Sonnet, Claude 3 Opus"
        LlmProviderType.OLLAMA -> "Local models (Llama3, Mistral, etc.)"
        LlmProviderType.GEMINI -> "Gemini Pro, Gemini Flash"
    }
}

/**
 * 配置凭证步骤
 */
@Composable
fun CredentialsStep(
    provider: LlmProviderType,
    apiKey: String,
    model: String,
    baseUrl: String,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Text(
            text = "Configure ${provider.name}",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // API Key (非 Ollama)
        if (provider != LlmProviderType.OLLAMA) {
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("API Key") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 模型
        OutlinedTextField(
            value = model,
            onValueChange = onModelChange,
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Base URL (Ollama 或自定义)
        OutlinedTextField(
            value = baseUrl,
            onValueChange = onBaseUrlChange,
            label = { Text("Base URL") },
            placeholder = { 
                Text(
                    when (provider) {
                        LlmProviderType.OLLAMA -> "http://localhost:11434"
                        else -> "https://api.openai.com/v1"
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Complete Setup")
        }
    }
}

/**
 * 完成步骤
 */
@Composable
fun CompleteStep(onComplete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎉",
            style = MaterialTheme.typography.displayLarge
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Setup Complete!",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "You're all set to start chatting with AndroidClaw.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Chatting")
        }
    }
}
