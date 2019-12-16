package com.rubyhuntersky.liftlog.story

import com.rubyhuntersky.liftlog.tomex.Mod
import com.rubyhuntersky.liftlog.tomex.WrappingAttribute
import com.rubyhuntersky.liftlog.tomex.getOwners
import com.rubyhuntersky.liftlog.tomex.modsWithEntity
import com.rubyhuntersky.tomedb.Update
import com.rubyhuntersky.tomedb.attributes.*
import com.rubyhuntersky.tomedb.tomicOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.time.ExperimentalTime

@ExperimentalTime
internal class LoggingStoryTest {

    sealed class Edit {
        object AddMovement : Edit()
    }


    object MovementG : AttributeGroup {
        object DIRECTION : Attribute<String> {
            override val groupName: String get() = fallbackGroupName
            override val itemName: String get() = fallbackItemName
            override val cardinality: Cardinality = Cardinality.ONE
            override val description: String = "Direction of movement"
            override val valueType: ValueType<String> = ValueType.STRING
        }

        object FORCE :
            WrappingAttribute<String, Force> {
            override val groupName: String get() = fallbackGroupName
            override val itemName: String get() = fallbackItemName
            override val cardinality: Cardinality = Cardinality.ONE
            override val description: String = "Force of movement"
            override val valueType: ValueType<String> = ValueType.STRING
            override fun wrap(source: String): Force? {
                val parts = source.split(":")
                return when (parts[0]) {
                    "lbs" -> Force.Lbs(parts[1].toInt())
                    else -> null
                }
            }

            override fun unwrap(wrapped: Force): String {
                return when (wrapped) {
                    is Force.Lbs -> "lbs:${wrapped.value}"
                }
            }
        }

        object DURATION : Attribute<String> {
            override val groupName: String get() = fallbackGroupName
            override val itemName: String get() = fallbackItemName
            override val cardinality: Cardinality = Cardinality.ONE
            override val description: String = "Duration of movement"
            override val valueType: ValueType<String> = ValueType.STRING
        }

        object TIME : Attribute<Date> {
            override val groupName: String get() = fallbackGroupName
            override val itemName: String get() = fallbackItemName
            override val cardinality: Cardinality = Cardinality.ONE
            override val description: String = "Time of movement"
            override val valueType: ValueType<Date> = ValueType.INSTANT
        }
    }

    @Test
    fun main() {
        val dir = createTempDir(prefix = "loggingTest")
        println("Db location: $dir")
        val tomic = tomicOf<Edit>(dir) {
            on(Edit.AddMovement::class.java) {
                val now = Date()
                val entity = Random.nextLong().absoluteValue
                val mods = modsWithEntity(entity) {
                    bind(MovementG.DIRECTION, "dip")
                    bind(MovementG.FORCE, Force.Lbs(100))
                    bind(MovementG.DURATION, "reps:10")
                    bind(MovementG.TIME, now)
                }
                val updates = mods.map {
                    require(it is Mod.Set)
                    Update(it.entity, it.attribute, it.value!!, Update.Action.Declare)
                }.toSet()
                write(updates)
            }
            emptyList()
        }
        tomic.write(Edit.AddMovement)
        val owner = tomic.readLatest().getOwners(MovementG.DIRECTION).first()
        val force = owner[MovementG.FORCE] ?: Force.Lbs(200)
        assertEquals(Force.Lbs(100), force)
    }

    @Test
    fun liftLogExists() {
        val vision = LoggingStory.Vision.Loaded(History(emptySet()), emptyList())
        assertNotNull(vision)
    }
}