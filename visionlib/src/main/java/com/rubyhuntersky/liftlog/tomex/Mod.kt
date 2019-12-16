package com.rubyhuntersky.liftlog.tomex

import com.rubyhuntersky.tomedb.attributes.Attribute
import com.rubyhuntersky.tomedb.database.Database
import com.rubyhuntersky.tomedb.database.Entity
import com.rubyhuntersky.tomedb.database.entitiesWith

sealed class Mod<T> {
    data class Set<T : Any>(
        val entity: Long,
        val attribute: Attribute<T>,
        val value: T
    ) : Mod<T>()
}

interface WrappingAttribute<S : Any, W> : Attribute<S> {
    fun wrap(source: S): W?
    fun unwrap(wrapped: W): S
}

inline fun <reified S : Any, T> projectValue(
    entity: Entity<S>,
    attribute: WrappingAttribute<S, T>
): T? = entity(attribute)?.let { attribute.wrap(it) }

interface EntityModScope {
    fun <T : Any> bind(attr: Attribute<T>, value: T)
    fun <T : Any, W> bind(attribute: WrappingAttribute<T, W>, value: W)
}

fun modsWithEntity(entity: Long, init: EntityModScope.() -> Unit): Set<Mod<*>> {
    val mods = mutableSetOf<Mod<*>>()
    object : EntityModScope {
        override fun <T : Any> bind(attr: Attribute<T>, value: T) {
            mods.add(
                Mod.Set(
                    entity,
                    attr,
                    value
                )
            )
        }

        override fun <T : Any, W> bind(attribute: WrappingAttribute<T, W>, value: W) {
            bind(attribute, attribute.unwrap(value))
        }
    }.init()
    return mods
}

interface Owner<S : Any> {
    operator fun <W> get(attribute: WrappingAttribute<S, W>): W?
}

inline fun <reified T : Any> Database.getOwners(propertyAttribute: Attribute<T>): List<Owner<T>> {
    val entities = entitiesWith(propertyAttribute).toList()
    return entities.map<Entity<T>, Owner<T>> { entity ->
        object : Owner<T> {
            override fun <W> get(attribute: WrappingAttribute<T, W>): W? {
                return projectValue(
                    entity,
                    attribute
                )
            }
        }
    }
}

