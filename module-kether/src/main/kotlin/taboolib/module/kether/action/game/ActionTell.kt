package taboolib.module.kether.action.game

import io.izzel.kether.common.api.ParsedAction
import io.izzel.kether.common.loader.types.ArgTypes
import taboolib.module.kether.*
import java.util.concurrent.CompletableFuture

/**
 * @author IzzelAliz
 */
class ActionTell(val message: ParsedAction<*>) : ScriptAction<Void>() {

    override fun run(frame: ScriptFrame): CompletableFuture<Void> {
        return frame.newFrame(message).run<Any>().thenAccept {
            val viewer = frame.script().sender ?: error("No sender selected.")
            viewer.sendMessage(it.toString().trimIndent().replace("@sender", viewer.name))
        }
    }

    override fun toString(): String {
        return "ActionTell(message=$message)"
    }

    companion object {

        @KetherParser(["tell", "send", "message"])
        fun parser() = scriptParser {
            ActionTell(it.next(ArgTypes.ACTION))
        }
    }
}