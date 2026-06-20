package top.thinapps.brightflashlight.ui

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import top.thinapps.brightflashlight.R

class PowerHaloView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
  private var powerStateView: TextView? = null

  private val powerStateWatcher = object : TextWatcher {
    override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit
    override fun afterTextChanged(text: Editable?) = syncVisibility()
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    post {
      attachPowerStateView()
      syncVisibility()
    }
  }

  override fun onDetachedFromWindow() {
    powerStateView?.removeTextChangedListener(powerStateWatcher)
    powerStateView = null
    super.onDetachedFromWindow()
  }

  private fun attachPowerStateView() {
    val stateView = rootView.findViewById<TextView>(R.id.txtPowerState) ?: return
    if (stateView === powerStateView) return

    powerStateView?.removeTextChangedListener(powerStateWatcher)
    powerStateView = stateView
    stateView.addTextChangedListener(powerStateWatcher)
  }

  private fun syncVisibility() {
    val activeLabel = context.getString(R.string.action_torch_off)
    visibility = if (powerStateView?.text?.toString() == activeLabel) VISIBLE else INVISIBLE
  }
}
