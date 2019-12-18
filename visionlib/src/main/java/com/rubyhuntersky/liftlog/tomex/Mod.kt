package com.rubyhuntersky.liftlog.tomex

import com.rubyhuntersky.tomedb.attributes.Attribute

interface WrappingAttribute<S : Any, W> : Attribute<S> {
    fun wrap(source: S): W?
    fun unwrap(wrapped: W): S
}

