package com.technogizguy.voltra.controller.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface VoltraClient {
    val state: StateFlow<VoltraSessionState>

    fun scan(): Flow<List<VoltraScanResult>>

    suspend fun connect(deviceId: String)

    suspend fun disconnect()

    suspend fun setTargetLoad(weight: Weight): VoltraCommandResult

    suspend fun setAssistMode(enabled: Boolean): VoltraCommandResult

    suspend fun setChainsWeight(weight: Weight): VoltraCommandResult

    suspend fun setEccentricWeight(weight: Weight): VoltraCommandResult

    suspend fun setInverseChainsEnabled(enabled: Boolean): VoltraCommandResult

    suspend fun setResistanceExperience(intense: Boolean): VoltraCommandResult

    suspend fun setResistanceBandInverse(enabled: Boolean): VoltraCommandResult

    suspend fun setResistanceBandCurveAlgorithm(logarithm: Boolean): VoltraCommandResult

    suspend fun enterResistanceBandMode(): VoltraCommandResult

    suspend fun enterDamperMode(): VoltraCommandResult

    suspend fun enterIsokineticMode(): VoltraCommandResult

    suspend fun enterIsometricMode(): VoltraCommandResult

    suspend fun setDamperLevel(level: Int): VoltraCommandResult

    suspend fun setResistanceBandMaxForce(weight: Weight): VoltraCommandResult

    suspend fun setResistanceBandByRangeOfMotion(enabled: Boolean): VoltraCommandResult

    suspend fun setResistanceBandLengthCm(lengthCm: Int): VoltraCommandResult

    suspend fun setIsokineticMenu(mode: Int): VoltraCommandResult

    suspend fun setIsokineticTargetSpeedMmS(speedMmS: Int): VoltraCommandResult

    suspend fun setIsokineticSpeedLimitMmS(speedMmS: Int): VoltraCommandResult

    suspend fun setIsokineticConstantResistance(weight: Weight): VoltraCommandResult

    suspend fun setIsokineticMaxEccentricLoad(weight: Weight): VoltraCommandResult

    suspend fun loadResistanceBand(): VoltraCommandResult

    suspend fun triggerCableLengthMode(): VoltraCommandResult

    suspend fun setCableOffsetCm(offsetCm: Int): VoltraCommandResult

    suspend fun refreshModeFeatureStatus(): VoltraCommandResult

    suspend fun load(): VoltraCommandResult

    suspend fun unload(): VoltraCommandResult

    suspend fun setStrengthMode(): VoltraCommandResult

    suspend fun exitWorkout(): VoltraCommandResult
}
