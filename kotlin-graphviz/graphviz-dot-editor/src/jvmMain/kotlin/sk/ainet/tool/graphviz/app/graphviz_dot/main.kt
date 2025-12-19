package sk.ainet.tool.graphviz.app.graphviz_dot

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "graphviz_dot",
    ) {
        App()
    }
}