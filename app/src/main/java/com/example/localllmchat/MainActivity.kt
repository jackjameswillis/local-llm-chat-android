package com.example.localllmchat.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import com.example.localllmchat.model.ModelDownloadState
import com.example.localllmchat.ui.ChatScreen
import com.example.localllmchat.ui.ChatViewModel
import com.example.localllmchat.ui.ModelSelectionScreen
import com.example.localllmchat.ui.ModelSelectionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    val nav = rememberNavController()
                    val modelSelectionVM: ModelSelectionViewModel = viewModel()
                    val chatVM: ChatViewModel = viewModel()

                    NavHost(navController = nav, startDestination = "model_select") {
                        composable("model_select") {
                            val state = modelSelectionVM.state.collectAsState()
                            ModelSelectionScreen(
                                state = state.value,
                                onDownload = { m ->
                                    modelSelectionVM.ensureDownloaded(m) { /* Ready callback */ }
                                },
                                onOpenChat = { model, title ->
                                    val downloaded = modelSelectionVM.isDownloaded(model)
                                    if (downloaded) {
                                        chatVM.initNewSession(model, title)
                                        val ds = state.value.downloadStates[model.id]
                                        if (ds is ModelDownloadState.Ready) {
                                            chatVM.loadModel(ds.path)
                                        }
                                        nav.navigate("chat")
                                    }
                                }
                            )
                        }
                        composable("chat") {
                            val cState = chatVM.state.collectAsState()
                            ChatScreen(
                                state = cState.value,
                                onSend = { chatVM.sendMessage(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
