package com.rubyhuntersky.liftlog.story

import com.rubyhuntersky.tomedb.Tomic
import com.rubyhuntersky.tomedb.Update
import com.rubyhuntersky.tomedb.attributes.*
import com.rubyhuntersky.tomedb.database.Entity
import com.rubyhuntersky.tomedb.database.entitiesWith
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

    interface WrappingAttribute<S : Any, W> : Attribute<S> {
        fun wrap(source: S): W?
        fun unwrap(wrapped: W): S
    }

    object MovementG : AttributeGroup {
        object DIRECTION : Attribute<String> {
            override val groupName: String get() = fallbackGroupName
            override val itemName: String get() = fallbackItemName
            override val cardinality: Cardinality = Cardinality.ONE
            override val description: String = "Direction of movement"
            override val valueType: ValueType<String> = ValueType.STRING
        }

        object FORCE : WrappingAttribute<String, Force> {
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

    interface Hive

    fun <E : Any, T : Any> Tomic<E>.hive(attribute: Attribute<T>): Hive {
        return object : Hive {

        }
    }

    sealed class Collective {
        data class Owners<T : Any>(val field: Attribute<T>) : Collective()
    }

    sealed class Mod<T> {
        data class Clear<T : Any>(
            val entity: Long,
            val attribute: Attribute<T>
        ) : Mod<T>()

        data class Set<T : Any>(
            val entity: Long,
            val attribute: Attribute<T>,
            val value: T
        ) : Mod<T>()
    }

    interface EntityModScope {
        fun <T : Any> bind(attr: Attribute<T>, value: T)
        fun <T : Any, W> bind(attribute: WrappingAttribute<T, W>, value: W)
        infix fun <T : Any> Attribute<T>.set(value: T)
        infix fun <T : Any, W> WrappingAttribute<T, W>.set(value: W)
    }

    fun modEntity(entity: Long, init: EntityModScope.() -> Unit): Set<Mod<*>> {
        val mods = mutableSetOf<Mod<*>>()
        object : EntityModScope {
            override fun <T : Any> bind(attr: Attribute<T>, value: T) {
                mods.add(Mod.Set(entity, attr, value))
            }

            override fun <T : Any, W> bind(attribute: WrappingAttribute<T, W>, value: W) {
                bind(attribute, attribute.unwrap(value))
            }

            override fun <T : Any> Attribute<T>.set(value: T) {
                bind(this, value)
            }

            override infix fun <T : Any, W> WrappingAttribute<T, W>.set(value: W) {
                bind(this, value)
            }
        }.init()
        return mods
    }

    inline operator fun <reified S : Any, T> WrappingAttribute<S, T>.get(entity: Entity<S>): T? {
        return project(entity, this)
    }

    inline operator fun <reified S : Any, T> Entity<S>.get(attribute: WrappingAttribute<S, T>): T? {
        return this(attribute)?.let { attribute.wrap(it) }
    }

    inline fun <reified S : Any, T> project(
        entity: Entity<S>,
        attribute: WrappingAttribute<S, T>
    ): T? = entity(attribute)?.let { attribute.wrap(it) }

    @Test
    fun main() {
        val dir = createTempDir(prefix = "loggingTest")
        println("Db location: $dir")
        val tomic = tomicOf<Edit>(dir) {
            on(Edit.AddMovement::class.java) {
                val now = Date()
                val entity = Random.nextLong().absoluteValue
                val mods = modEntity(entity) {
                    MovementG.DIRECTION set "dip"
                    MovementG.FORCE set Force.Lbs(100)
                    MovementG.DURATION set "reps:10"
                    MovementG.TIME set now
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
        val movements = tomic.readLatest().entitiesWith(MovementG.DIRECTION).toList()
        val movement = movements.first()
        val force = movement[MovementG.FORCE] ?: Force.Lbs(200)
        assertEquals(Force.Lbs(100), force)
    }


    @Test
    fun liftLogExists() {
        val vision = LoggingStory.Vision.Loaded(History(emptySet()), emptyList())
        assertNotNull(vision)
    }
}