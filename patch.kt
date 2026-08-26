import android.view.ViewGroup
import android.widget.FrameLayout

// in onCreateInputView:
val frameLayout = FrameLayout(this).apply {
    layoutParams = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
}
val composeView = ComposeView(this).apply {
    layoutParams = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setViewTreeLifecycleOwner(this@KeyboardIME)
    // ...
