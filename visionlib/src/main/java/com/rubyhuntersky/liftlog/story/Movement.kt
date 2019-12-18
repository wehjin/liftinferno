package com.rubyhuntersky.liftlog.story

import com.rubyhuntersky.tomedb.Mod
import com.rubyhuntersky.tomedb.attributes.AttributeInObject
import com.rubyhuntersky.tomedb.attributes.DateScriber
import com.rubyhuntersky.tomedb.attributes.Scriber
import com.rubyhuntersky.tomedb.modEnt
import java.util.*

object Movement {

    object WHEN : AttributeInObject<Date>() {
        override val description: String = "Time the movement was performed"
        override val scriber: Scriber<Date> = DateScriber
    }

    object DIRECTION : AttributeInObject<Direction>() {
        override val description: String = "Direction of the movement"
        override val scriber: Scriber<Direction> = object : Scriber<Direction> {
            override val emptyScript: String = Direction.Squats.name
            override fun scribe(quant: Direction): String = quant.name
            override fun unscribe(script: String): Direction = Direction.valueOf(script)
        }
    }

    object FORCE : AttributeInObject<Force>() {
        override val description: String = "Force of the movement"
        override val scriber: Scriber<Force> = object : Scriber<Force> {
            override val emptyScript: String = "lbs:0"

            override fun scribe(quant: Force): String = when (quant) {
                is Force.Lbs -> "lbs:${quant.value}"
            }

            override fun unscribe(script: String): Force {
                val parts = script.split(":")
                return when (parts[0]) {
                    "lbs" -> Force.Lbs(parts[1].toInt())
                    else -> TODO()
                }
            }
        }
    }

    object DISTANCE : AttributeInObject<Distance>() {
        override val description: String = "Distance of the movement"
        override val scriber: Scriber<Distance> = object : Scriber<Distance> {
            override val emptyScript: String = "reps:0"

            override fun scribe(quant: Distance): String = when (quant) {
                is Distance.Reps -> "reps:${quant.count}"
                is Distance.Seconds -> "secs:${quant.count}"
            }

            override fun unscribe(script: String): Distance {
                val parts = script.split(":")
                return when (parts[0]) {
                    "reps" -> Distance.Reps(parts[1].toInt())
                    "secs" -> Distance.Seconds(parts[1].toInt())
                    else -> TODO()
                }
            }
        }
    }
}

fun modMovement(
    ent: Long,
    date: Date,
    direction: Direction,
    force: Force,
    distance: Distance
): List<Mod<*>> = modEnt(ent) {
    Movement.WHEN set date
    Movement.DIRECTION set direction
    Movement.FORCE set force
    Movement.DISTANCE set distance
}
