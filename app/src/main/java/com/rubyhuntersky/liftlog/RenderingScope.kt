package com.rubyhuntersky.liftlog

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

interface RenderingScope {

    fun EditText.render(text: String, onTextChange: (String) -> Unit = {}) {
        (getTag(R.id.text_watcher_key) as? TextWatcher)?.let {
            removeTextChangedListener(it)
        }
        if (text != getText().toString()) {
            setText(text)
        }
        object : TextWatcher {
            private var oldText = text

            override fun afterTextChanged(s: Editable?) {
                require(s != null)
                val newText = s.toString()
                if (newText != oldText) {
                    oldText = newText.also { onTextChange(newText) }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }.let {
            addTextChangedListener(it)
            setTag(R.id.text_watcher_key, it)
        }
    }
}
