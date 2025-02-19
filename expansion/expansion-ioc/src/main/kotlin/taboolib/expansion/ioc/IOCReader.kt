package taboolib.expansion.ioc

import taboolib.common.LifeCycle
import taboolib.common.env.RuntimeDependencies
import taboolib.common.env.RuntimeDependency
import taboolib.common.platform.Awake
import taboolib.common.platform.Schedule
import taboolib.expansion.ioc.annotation.Component
import taboolib.expansion.ioc.database.IOCDatabase
import taboolib.expansion.ioc.database.impl.IOCDatabaseYaml
import taboolib.expansion.ioc.event.DataReadEvent
import taboolib.expansion.ioc.serialization.SerializationManager
import java.util.concurrent.ConcurrentHashMap

@RuntimeDependencies(
    RuntimeDependency(value = "!com.google.code.gson:gson:2.8.7", test = "!com.google.gson.JsonElement")
)
object IOCReader {

    val databaseMap = ConcurrentHashMap<String, IOCDatabase>()

    val dataMap = ConcurrentHashMap<String, ConcurrentHashMap<String, Any>>()

    fun readRegister(classes: List<Class<*>>, defaultIOCDatabase: IOCDatabase = IOCDatabaseYaml()) {
        classes.forEach { clazz: Class<*> ->
            if (!clazz.isAnnotationPresent(Component::class.java)) {
                return@forEach
            }
            val event = DataReadEvent(clazz, defaultIOCDatabase)
            if (event.isCancelled) {
                return@forEach
            }
            val database = this.databaseMap.getOrPut(event.data.name) {
                event.iocDatabase.init(clazz)
            }
            database.getDataAll().forEach { (_, value) ->
                value?.let {
                    SerializationManager.deserialize(it, clazz, clazz)?.let { it1 ->
                        val save = dataMap.getOrPut(clazz.name) { ConcurrentHashMap<String, Any>() }
                        save[IndexReader.getIndexId(it1)] = it1
                    }
                }
            }

        }
    }


    @Schedule(period = 2400, async = true)
    @Awake(LifeCycle.DISABLE)
    fun write() {
        dataMap.forEach { (t, u) ->
            val database = databaseMap[t] ?: return@forEach
            database.resetDatabase()
            u.forEach { (k, v) ->
                database.writeData(k, v)
            }
            database.saveDatabase()
        }
    }

}