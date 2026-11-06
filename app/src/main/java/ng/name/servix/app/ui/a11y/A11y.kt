package ng.name.servix.app.ui.a11y

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

/** Marks a text node as a heading so TalkBack can jump between sections. */
fun Modifier.heading(): Modifier = this.semantics { heading() }
