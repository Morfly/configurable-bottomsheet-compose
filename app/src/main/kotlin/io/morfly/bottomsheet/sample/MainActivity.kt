package io.morfly.bottomsheet.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import io.morfly.bottomsheet.sample.theme.MultiStateBottomSheetSampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MultiStateBottomSheetSampleTheme {
                Surface {
                    Navigation()
                }
            }
        }
    }
}
