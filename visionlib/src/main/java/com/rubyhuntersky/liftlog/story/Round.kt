package com.rubyhuntersky.liftlog.story

import com.rubyhuntersky.tomedb.Owner
import java.util.*

data class Round(
    val epoch: Long,
    val movements: List<Owner<Date>>
)