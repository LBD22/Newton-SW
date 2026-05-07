package ru.newton.fieldapp.data.receiver

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.newton.fieldapp.core.logging.AppLog
import ru.newton.fieldapp.domain.receiver.ApplyProgress
import ru.newton.fieldapp.gnss.command.CommandSession
import ru.newton.fieldapp.gnss.command.CommandSessionState
import ru.newton.fieldapp.gnss.command.NewtonCommandBuilder
import ru.newton.fieldapp.gnss.command.OkKind
import javax.inject.Inject

/**
 * Drives the Apply flow (CMD-006 + DevHandbook §6.4): take the current
 * [PendingChangesService.patch], translate to a Newton command sequence,
 * **persist each step in [CommandQueueRepository] for audit**, send via
 * [CommandSession], emit progress, and finalise with `system save`.
 *
 * The queue table records every command's wire text, origin (screen tag if
 * we can infer it), the receiver's reply, and any error — visible to the
 * user on SET-080 «Диагностика». Successful items get cleared on the next
 * Apply; failed items remain so the user can investigate.
 */
class ApplyReceiverConfigUseCase
    @Inject
    constructor(
        private val session: CommandSession,
        private val pendingChanges: PendingChangesService,
        private val commandQueue: CommandQueueRepository,
        private val log: AppLog,
    ) {
        operator fun invoke(): Flow<ApplyProgress> = flow {
            val patch = pendingChanges.patch.value
            val commands = PatchToCommands.build(patch)

            if (commands.isEmpty()) {
                emit(ApplyProgress.Failed("Нет изменений для применения"))
                return@flow
            }

            // Drop previously-applied rows — keep failed ones around so the
            // surveyor can see what didn't take and investigate.
            commandQueue.clearApplied()

            // Enqueue first so even if Apply is killed mid-flight, the
            // intent is durable. Origin screen id is derived from each
            // command's description prefix; falls back to "Apply".
            val ids = commands.map { prepared ->
                commandQueue.enqueue(
                    commandText = prepared.command,
                    originScreenId = inferOrigin(prepared),
                    description = prepared.description,
                )
            }

            // `system save` is the persistence step — always last.
            val total = commands.size + 1
            emit(ApplyProgress.Idle)

            try {
                if (session.state.value !is CommandSessionState.Ready) {
                    session.handshake()
                }

                commands.forEachIndexed { idx, prepared ->
                    val id = ids[idx]
                    commandQueue.markStatus(id, CommandStatus.SENDING)
                    emit(
                        ApplyProgress.Sending(
                            current = idx + 1,
                            total = total,
                            commandText = prepared.command,
                            description = prepared.description,
                        ),
                    )
                    val ok = session.send(prepared.command)
                    if (ok != OkKind.OK_QUEUED) {
                        commandQueue.markStatus(
                            id,
                            CommandStatus.FAILED,
                            replyText = ok.toString(),
                            errorText = "Ожидался OK!, получено $ok",
                        )
                        error("Команда '${prepared.command}' вернула $ok вместо OK!")
                    }
                    commandQueue.markStatus(id, CommandStatus.APPLIED, replyText = ok.toString())
                }

                // Final system save — record it too so the audit log shows
                // explicitly whether the flash write succeeded.
                val saveCmd = NewtonCommandBuilder.systemSave()
                val saveId = commandQueue.enqueue(saveCmd, "Apply", "Сохранение в энергонезависимую память")
                commandQueue.markStatus(saveId, CommandStatus.SENDING)
                emit(
                    ApplyProgress.Sending(
                        current = total,
                        total = total,
                        commandText = saveCmd,
                        description = "Сохранение в энергонезависимую память",
                    ),
                )
                val saveOk = session.send(saveCmd)
                if (saveOk != OkKind.OK_QUEUED) {
                    commandQueue.markStatus(
                        saveId,
                        CommandStatus.FAILED,
                        replyText = saveOk.toString(),
                        errorText = "system save вернул $saveOk",
                    )
                    error("system save вернул $saveOk вместо OK!")
                }
                commandQueue.markStatus(saveId, CommandStatus.APPLIED, replyText = saveOk.toString())

                pendingChanges.clear()
                emit(ApplyProgress.Succeeded)
            } catch (t: Throwable) {
                log.cmd("Apply failed: ${t.message}", t)
                emit(ApplyProgress.Failed(t.message ?: "Ошибка применения"))
            }
        }

        /**
         * Heuristic mapping of "what changed" → screen id, used as origin
         * tag in the audit log. The descriptions our [PatchToCommands]
         * produces carry enough context that we can pick a screen by
         * inspecting the command itself.
         */
        private fun inferOrigin(prepared: PreparedCommand): String = when {
            prepared.command.startsWith("mode set rover") -> "SET-010"
            prepared.command.startsWith("mode set base") -> "SET-020"
            prepared.command.startsWith("mode rtcmid") -> "SET-020"
            prepared.command.startsWith("mode cmrid") -> "SET-020"
            prepared.command.startsWith("output set correction") -> "SET-020"
            prepared.command.startsWith("survey set mask") -> "SET-010"
            prepared.command.startsWith("input set ntripclient") -> "SET-013"
            prepared.command.startsWith("input set") -> "SET-011"
            prepared.command.startsWith("output add message") -> "SET-014"
            prepared.command.startsWith("output add stream") -> "SET-015"
            prepared.command.startsWith("output clear") -> "SET-014"
            prepared.command.startsWith("ppp ") -> "SET-PPP"
            prepared.command.startsWith("gsm ") -> "SET-017"
            prepared.command.startsWith("bluetooth bridge") -> "SET-016"
            prepared.command.startsWith("bluetooth set") -> "SET-019"
            prepared.command.startsWith("coordsystem set geoid") -> "PRJ-006A"
            prepared.command.startsWith("coordsystem set undulation") -> "PRJ-006A"
            else -> "Apply"
        }
    }
