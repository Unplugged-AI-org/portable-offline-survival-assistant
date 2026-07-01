package ai.unplugged.posa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ai.unplugged.posa.data.local.PosaDatabase
import ai.unplugged.posa.ui.PosaApp

class MainActivity : ComponentActivity() {
    private val database by lazy {
        PosaDatabase.create(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PosaApp(database = database)
        }
    }
}
