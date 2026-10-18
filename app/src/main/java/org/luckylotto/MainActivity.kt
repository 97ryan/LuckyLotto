// SPDX-License-Identifier: GPL-3.0-or-later
package org.luckylotto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import org.luckylotto.ui.LottoApp
import org.luckylotto.ui.LottoViewModel

class MainActivity : ComponentActivity() {
    private val model: LottoViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LottoApp(model) }
    }
    override fun onResume() { super.onResume(); model.tick() }
}
