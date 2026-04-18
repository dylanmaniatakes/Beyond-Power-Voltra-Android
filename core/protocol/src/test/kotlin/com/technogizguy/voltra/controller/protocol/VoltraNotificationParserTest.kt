package com.technogizguy.voltra.controller.protocol

import com.technogizguy.voltra.controller.model.VoltraReading
import com.technogizguy.voltra.controller.model.VoltraSafetyState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VoltraNotificationParserTest {
    @Test
    fun extractsSerialWithMPrefixFromCapturedFrame() {
        val reading = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "552F08C010AA030020001900014D423130323637413235303931333032353600000000000000000000000000009323"
                .hexToByteArray(),
            nowMillis = 1234L,
        )

        assertEquals("MB10267A2509130256", reading.serialNumber)
        assertEquals(1234L, reading.lastUpdatedMillis)
    }

    @Test
    fun doesNotMisreadCommonStateCrcAsBatteryAndExtractsActivation() {
        val afterCommonState = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "5529086A10AA00002000740000000610000011000012000013000014000015000000000000000032C1"
                .hexToByteArray(),
            nowMillis = 2000L,
        )
        val withActivation = VoltraNotificationParser.mergeReading(
            current = afterCommonState,
            value = "552408E310AA04002000AB000101DC59D4695EAB9EF41C864FF5877A9C8C1D5F0D600921"
                .hexToByteArray(),
            nowMillis = 3000L,
        )

        assertNull(withActivation.batteryPercent)
        assertEquals("Activated", withActivation.activationState)
        assertEquals(3000L, withActivation.lastUpdatedMillis)
    }

    @Test
    fun extractsBatteryFromAsyncBmsRsocParamUpdate() {
        val reading = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "551204C710AA5C0220001001002D4E28E6EA".hexToByteArray(),
            nowMillis = 4000L,
        )

        assertEquals(40, reading.batteryPercent)
        assertEquals(4000L, reading.lastUpdatedMillis)
    }

    @Test
    fun extractsBatteryFromLegacyBmsRsocParamUpdate() {
        val reading = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = VoltraFrameBuilder.build(
                cmd = VoltraControlFrames.CMD_PARAM_READ,
                payload = byteArrayOf(
                    0x01,
                    0x00,
                    0x5D,
                    0x1B,
                    0x26,
                ),
                seq = 1,
            ),
            nowMillis = 4500L,
        )

        assertEquals(38, reading.batteryPercent)
        assertEquals(4500L, reading.lastUpdatedMillis)
    }

    @Test
    fun extractsTargetWeightAndWorkoutModeFromCapturedStateUpdate() {
        val reading = VoltraNotificationParser.mergeReading(
            current = VoltraReading(activationState = "Activated"),
            value = "552E04A710AA8C022000100900863E0A00873E0000883E0000893E0400025103035109B04F01E14E01245100350B"
                .hexToByteArray(),
            nowMillis = 5000L,
        )

        assertEquals(10.0, reading.weightLb)
        assertEquals(0.0, reading.chainsWeightLb)
        assertEquals(0.0, reading.eccentricWeightLb)
        assertEquals("Strength ready, session active", reading.workoutMode)
        assertEquals(5000L, reading.lastUpdatedMillis)
    }

    @Test
    fun extractsSignedEccentricWeightFromCapturedStateUpdate() {
        val reading = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "552E04A710AA34002000100900863E5600873E0000883EECFF893E0400025103035109B04F01E14E012451000421"
                .hexToByteArray(),
            nowMillis = 5500L,
        )

        assertEquals(86.0, reading.weightLb)
        assertEquals(0.0, reading.chainsWeightLb)
        assertEquals(-20.0, reading.eccentricWeightLb)
        assertEquals("Strength ready, session active", reading.workoutMode)
        assertEquals(5500L, reading.lastUpdatedMillis)
    }

    @Test
    fun extractsInverseChainsFromParamUpdate() {
        val reading = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = VoltraFrameBuilder.build(
                cmd = 0x10,
                payload = VoltraControlFrames.setInverseChainsPayload(true),
                seq = 2,
            ),
            nowMillis = 5600L,
        )

        assertEquals(true, reading.inverseChains)
        assertEquals(5600L, reading.lastUpdatedMillis)
    }

    @Test
    fun extractsAssistModeFromCapturedParamUpdates() {
        val enabled = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "551204C710AAFF252000100100065101100E".hexToByteArray(),
            nowMillis = 5650L,
        )
        val disabled = VoltraNotificationParser.mergeReading(
            current = enabled,
            value = "551204C710AA0C262000100100065108D5BC".hexToByteArray(),
            nowMillis = 5660L,
        )

        assertEquals(true, enabled.assistModeEnabled)
        assertEquals(false, disabled.assistModeEnabled)
        assertEquals(5660L, disabled.lastUpdatedMillis)
    }

    @Test
    fun extractsExperimentalRepCountFromWeightTrainingTelemetry() {
        val reading = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "553A047010AAB1062000AA812B030300030028003A024600590280FE0C000000000000000000FFC070004303000000000000140000005000AF53"
                .hexToByteArray(),
            nowMillis = 6000L,
        )

        assertEquals(3, reading.setCount)
        assertEquals(3, reading.repCount)
        assertEquals("Return", reading.repPhase)
        assertEquals(6000L, reading.lastUpdatedMillis)
    }

    @Test
    fun extractsSetCountFromThreeByFourWorkoutTelemetry() {
        val reading = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "553A047010AA94092000AA812B030300040032004E022D0032029EFD0C000000000000000000FE2E340075030000000000005F000000320038AF"
                .hexToByteArray(),
            nowMillis = 6100L,
        )

        assertEquals(3, reading.setCount)
        assertEquals(4, reading.repCount)
        assertEquals("Return", reading.repPhase)
        assertEquals(6100L, reading.lastUpdatedMillis)
    }

    @Test
    fun labelsResistanceBandWorkoutStateFromDeviceMenu() {
        val reading = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "552E04A710AAF30F2000100900863E0500873E0000883E0000893E0400025103035109B04F02E14E0124510017C2"
                .hexToByteArray(),
            nowMillis = 6200L,
        )

        assertEquals("Resistance Band, Ready", reading.workoutMode)
        assertEquals(6200L, reading.lastUpdatedMillis)
    }

    @Test
    fun labelsDamperWorkoutStateFromCapturedIpadSequence() {
        val reading = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "552E04A710AA4E162000100900863E0500873E0000883E0000893E0400025102035100B04F04E14E0124510057DD"
                .hexToByteArray(),
            nowMillis = 6250L,
        )

        assertEquals("Damper, Ready", reading.workoutMode)
        assertEquals(0, reading.damperLevelIndex)
        assertEquals(6250L, reading.lastUpdatedMillis)
    }

    @Test
    fun labelsIsokineticWorkoutStateFromAndroidCapture() {
        val reading = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "552E04A710AA371B2000100900863E3100873E0000883E0000893E0401025102035100B04F07E14E012451007BBA"
                .hexToByteArray(),
            nowMillis = 6275L,
        )

        assertEquals("Isokinetic, Ready", reading.workoutMode)
        assertEquals(6275L, reading.lastUpdatedMillis)
    }

    @Test
    fun labelsIsometricWorkoutStateFromCapturedIpadSequence() {
        val reading = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "551D04DF10AA421E2000100400893E8500B04F0811503967540801216E".hexToByteArray(),
            nowMillis = 6278L,
        )

        assertEquals("Isometric Test, Loaded", reading.workoutMode)
        assertEquals(6278L, reading.lastUpdatedMillis)
    }

    @Test
    fun labelsReadyIsometricStateAfterUnload() {
        val reading = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "552E04A710AA273D2000100900863E0500873E0000883E0000893E0400025100035103B04F08E14E012451002711"
                .hexToByteArray(),
            nowMillis = 6279L,
        )

        assertEquals("Isometric Test, Ready", reading.workoutMode)
        assertEquals(6279L, reading.lastUpdatedMillis)
    }

    @Test
    fun preservesLiveIsometricForceWhenLoadedScreenFrameArrivesAfterLoadedSample() {
        val active = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "551504A910AA83490000B42000020000003200DCBE".hexToByteArray(),
            nowMillis = 6280L,
        )
        val loaded = VoltraNotificationParser.mergeReading(
            current = active,
            value = "551D04DF10AA421E2000100400893E8500B04F0811503967540801216E".hexToByteArray(),
            nowMillis = 6281L,
        )

        assertEquals(lbToNewtons(32.0), loaded.isometricCurrentForceN!!, 0.1)
        assertEquals(lbToNewtons(32.0), loaded.isometricPeakForceN!!, 0.1)
        assertEquals(0L, loaded.isometricElapsedMillis)
    }

    @Test
    fun extractsResistanceExperienceToggleFromCapturedParamUpdate() {
        val reading = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = VoltraFrameBuilder.build(
                cmd = 0x10,
                payload = VoltraControlFrames.setResistanceExperiencePayload(true),
                seq = 8,
            ),
            nowMillis = 6280L,
        )

        assertEquals(true, reading.resistanceExperienceIntense)
        assertEquals(6280L, reading.lastUpdatedMillis)
    }

    @Test
    fun extractsIsokineticSettingsFromCapturedParamUpdates() {
        val withMenu = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = VoltraFrameBuilder.build(
                cmd = 0x10,
                payload = VoltraControlFrames.setIsokineticMenuPayload(VoltraControlFrames.ISOKINETIC_MENU_ISOKINETIC),
                seq = 9,
            ),
            nowMillis = 6285L,
        )
        val withSpeed = VoltraNotificationParser.mergeReading(
            current = withMenu,
            value = VoltraFrameBuilder.build(
                cmd = 0x10,
                payload = VoltraControlFrames.setIsokineticSpeedLimitPayload(2000),
                seq = 10,
            ),
            nowMillis = 6290L,
        )
        val withConstantResistance = VoltraNotificationParser.mergeReading(
            current = withSpeed,
            value = VoltraFrameBuilder.build(
                cmd = 0x10,
                payload = VoltraControlFrames.setIsokineticConstantResistancePayload(5),
                seq = 11,
            ),
            nowMillis = 6295L,
        )
        val withEccentricLoad = VoltraNotificationParser.mergeReading(
            current = withConstantResistance,
            value = VoltraFrameBuilder.build(
                cmd = 0x10,
                payload = VoltraControlFrames.setIsokineticMaxEccentricLoadPayload(145),
                seq = 12,
            ),
            nowMillis = 6300L,
        )

        assertEquals(VoltraControlFrames.ISOKINETIC_MENU_ISOKINETIC, withEccentricLoad.isokineticMode)
        assertEquals(2000, withEccentricLoad.isokineticSpeedLimitMmS)
        assertEquals(5.0, withEccentricLoad.isokineticConstantResistanceLb)
        assertEquals(145.0, withEccentricLoad.isokineticMaxEccentricLoadLb)
        assertEquals(6300L, withEccentricLoad.lastUpdatedMillis)
    }

    @Test
    fun extractsIsokineticTargetSpeedFromCleanIpadCapture() {
        val reading = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "551504A910AA132320001001005053E80300005A94".hexToByteArray(),
            nowMillis = 6310L,
        )

        assertEquals(1000, reading.isokineticTargetSpeedMmS)
        assertEquals(6310L, reading.lastUpdatedMillis)
    }

    @Test
    fun extractsResistanceBandForceAndCableOffsetFromCleanIpadCapture() {
        val resistanceForce = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "5513040310AA4510200010010062533200ED9E".hexToByteArray(),
            nowMillis = 6300L,
        )
        val cable = VoltraNotificationParser.mergeReading(
            current = resistanceForce,
            value = "5518088310AA180020000F0002006A502200823E2200BA1E".hexToByteArray(),
            nowMillis = 6400L,
        )

        assertEquals(50.0, cable.resistanceBandMaxForceLb)
        assertEquals(34.0, cable.cableOffsetCm)
        assertEquals(34.0, cable.cableLengthCm)
        assertEquals(6400L, cable.lastUpdatedMillis)
    }

    @Test
    fun extractsResistanceBandCableOptionsFromParamUpdate() {
        val reading = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = VoltraFrameBuilder.build(
                cmd = 0x10,
                payload = byteArrayOf(
                    0x04,
                    0x00,
                    0xB7.toByte(),
                    0x53,
                    0xB4.toByte(),
                    0x00,
                    0xB6.toByte(),
                    0x53,
                    0x01,
                    0xE3.toByte(),
                    0x52,
                    0x01,
                    0xBC.toByte(),
                    0x54,
                    0x00,
                ),
                seq = 7,
            ),
            nowMillis = 6500L,
        )

        assertEquals(180.0, reading.resistanceBandLengthCm)
        assertEquals(true, reading.resistanceBandByRangeOfMotion)
        assertEquals(true, reading.resistanceBandInverse)
        assertEquals(false, reading.quickCableAdjustment)
        assertEquals(6500L, reading.lastUpdatedMillis)
    }

    @Test
    fun opensSafetyGateForConfirmedStrengthModeUnloadedState() {
        val reading = VoltraReading(
            batteryPercent = 40,
            activationState = "Activated",
        )
        val safety = VoltraNotificationParser.mergeSafety(
            current = VoltraSafetyState(),
            reading = reading,
            value = "552E04A710AA97022000100900863E0A00873E0000883E0000893E0400025103035109B04F01E14E01245100437E"
                .hexToByteArray(),
        )

        assertTrue(safety.canLoad)
        assertEquals(4, safety.fitnessMode)
        assertEquals(1, safety.workoutState)
        assertEquals(10.0, safety.targetLoadLb)
    }

    @Test
    fun opensSafetyGateForIsokineticReadyModeWithExtendedFitnessValue() {
        val reading = VoltraReading(
            batteryPercent = 90,
            activationState = "Activated",
        )
        val safety = VoltraNotificationParser.mergeSafety(
            current = VoltraSafetyState(),
            reading = reading,
            value = "552E04A710AA371B2000100900863E3100873E0000883E0000893E0401025102035100B04F07E14E012451007BBA"
                .hexToByteArray(),
        )

        assertTrue(safety.canLoad)
        assertEquals(260, safety.fitnessMode)
        assertEquals(7, safety.workoutState)
        assertEquals(49.0, safety.targetLoadLb)
    }

    @Test
    fun treatsIsometricScreenAsLoadedState() {
        val reading = VoltraReading(
            batteryPercent = 90,
            activationState = "Activated",
        )
        val safety = VoltraNotificationParser.mergeSafety(
            current = VoltraSafetyState(targetLoadLb = 49.0),
            reading = reading,
                value = "551D04DF10AA421E2000100400893E8500B04F0811503967540801216E".hexToByteArray(),
        )

        assertFalse(safety.canLoad)
        assertEquals(133, safety.fitnessMode)
        assertEquals(8, safety.workoutState)
    }

    @Test
    fun opensSafetyGateForReadyIsometricState() {
        val reading = VoltraReading(
            batteryPercent = 90,
            activationState = "Activated",
        )
        val safety = VoltraNotificationParser.mergeSafety(
            current = VoltraSafetyState(targetLoadLb = 5.0),
            reading = reading,
            value = "552E04A710AA273D2000100900863E0500873E0000883E0000893E0400025100035103B04F08E14E012451002711"
                .hexToByteArray(),
        )

        assertTrue(safety.canLoad)
        assertEquals(4, safety.fitnessMode)
        assertEquals(8, safety.workoutState)
    }

    @Test
    fun extractsIsometricTelemetryFromCapturedIpadFrames() {
        val first = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "553A047010AA8F382000AA812B00000000000000000002000200000000000000000000000000D2968201000000000000000000000000F802C369"
                .hexToByteArray(),
            nowMillis = 7000L,
        )
        val second = VoltraNotificationParser.mergeReading(
            current = first,
            value = "553A047010AA90382000AA812B000000000000000000010002000000000000000000000000003697820100000000000000000000000020066976"
                .hexToByteArray(),
            nowMillis = 7100L,
        )

        assertNull(first.isometricCurrentForceN)
        assertNull(first.isometricPeakForceN)
        assertNull(first.isometricElapsedMillis)
        assertEquals(81.1, first.isometricCarrierForceN!!, 0.1)
        assertNull(second.isometricCurrentForceN)
        assertNull(second.isometricPeakForceN)
        assertNull(second.isometricElapsedMillis)
        assertEquals(167.3, second.isometricCarrierForceN!!, 0.1)
    }

    @Test
    fun ignoresArmedHeartbeatFramesAsLiveIsometricTelemetry() {
        val active = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "553A047010AA90382000AA812B000000000000000000010002000000000000000000000000003697820100000000000000000000000020066976"
                .hexToByteArray(),
            nowMillis = 7200L,
        )
        val armedHeartbeat = VoltraNotificationParser.mergeReading(
            current = active,
            value = "553A047010AAE9412000AA812B00000000000000000011000A000000000000000000000000007AD8A301000000000000000000000000A00F7D63"
                .hexToByteArray(),
            nowMillis = 7300L,
        )

        assertNull(armedHeartbeat.isometricCurrentForceN)
        assertNull(armedHeartbeat.isometricPeakForceN)
        assertNull(armedHeartbeat.isometricElapsedMillis)
    }

    @Test
    fun clearsStaleIsometricMetricsWhenReadyHeartbeatArrivesWithoutCollectedSamples() {
        val stale = VoltraReading(
            isometricPeakForceN = 400.0,
            isometricElapsedMillis = 4_000L,
        )
        val readyHeartbeat = VoltraNotificationParser.mergeReading(
            current = stale,
            value = "553A047010AAE9412000AA812B00000000000000000011000A000000000000000000000000007AD8A301000000000000000000000000A00F7D63"
                .hexToByteArray(),
            nowMillis = 7_300L,
        )

        assertNull(readyHeartbeat.isometricCurrentForceN)
        assertNull(readyHeartbeat.isometricPeakForceN)
        assertNull(readyHeartbeat.isometricElapsedMillis)
    }

    @Test
    fun ignoresLegacyIsometricSummaryPacketWithoutLiveTelemetry() {
        val stale = VoltraReading(
            isometricPeakForceN = 400.0,
            isometricPeakRelativeForcePercent = 55.0,
            isometricElapsedMillis = 2_200L,
        )

        val summary = VoltraNotificationParser.mergeReading(
            current = stale,
            value = "553404AC10AA08082000AA8025050005000000000000000000005400000000000000050300010000640001EC0200000000006583"
                .hexToByteArray(),
            nowMillis = 12_000L,
        )

        assertNull(summary.isometricCurrentForceN)
        assertEquals(400.0, summary.isometricPeakForceN!!, 0.1)
        assertEquals(55.0, summary.isometricPeakRelativeForcePercent!!, 0.1)
        assertEquals(2_200L, summary.isometricElapsedMillis)
    }

    @Test
    fun clearsStaleIsometricMetricsWhenEnteringFreshIsometricScreen() {
        val stale = VoltraReading(
            isometricPeakForceN = 400.0,
            isometricElapsedMillis = 2_200L,
            workoutMode = "Strength ready, session inactive",
        )

        val entered = VoltraNotificationParser.mergeReading(
            current = stale,
            value = "551D04DF10AA421E2000100400893E8500B04F0811503967540801216E".hexToByteArray(),
            nowMillis = 12_500L,
        )

        assertNull(entered.isometricCurrentForceN)
        assertNull(entered.isometricPeakForceN)
        assertNull(entered.isometricPeakRelativeForcePercent)
        assertNull(entered.isometricElapsedMillis)
        assertTrue(entered.isometricWaveformSamplesN.isEmpty())
        assertEquals("Isometric Test, Loaded", entered.workoutMode)
    }

    @Test
    fun doesNotLetLegacyIsometricSummaryPacketReplaceClearedAttempt() {
        val entered = VoltraNotificationParser.mergeReading(
            current = VoltraReading(
                isometricPeakForceN = 400.0,
                isometricPeakRelativeForcePercent = 55.0,
                isometricElapsedMillis = 2_200L,
                workoutMode = "Strength ready, session inactive",
            ),
            value = "551D04DF10AA421E2000100400893E8500B04F0811503967540801216E".hexToByteArray(),
            nowMillis = 12_500L,
        )
        val summarized = VoltraNotificationParser.mergeReading(
            current = entered,
            value = "553404AC10AA08082000AA8025050005000000000000000000005400000000000000050300010000640001EC0200000000006583"
                .hexToByteArray(),
            nowMillis = 12_600L,
        )
        val exited = VoltraNotificationParser.mergeReading(
            current = summarized,
            value = "552E04A710AA0B082000100900863E0500873E0000883E0000893E0400025100035108B04F00E14E0124510014AB"
                .hexToByteArray(),
            nowMillis = 12_700L,
        )

        assertNull(exited.isometricPeakForceN)
        assertNull(exited.isometricPeakRelativeForcePercent)
        assertNull(exited.isometricElapsedMillis)
        assertEquals("Strength ready, session inactive", exited.workoutMode)
    }

    @Test
    fun treatsLegacy812bHeartbeatFramesAsStateCarriersInsteadOfRealtimeForce() {
        val ready = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "553A047010AAD76B2000AA812B00000000000000000011000A000000000000000000000000006B175F00000000000000000000000000A00FD84A"
                .hexToByteArray(),
            nowMillis = 20_000L,
        )
        val active = VoltraNotificationParser.mergeReading(
            current = ready,
            value = "553A047010AAE86B2000AA812B0000000000000000000600020000000000000000000000000038275F000000000000000000000000001003A8AB"
                .hexToByteArray(),
            nowMillis = 20_100L,
        )

        assertNull(active.isometricCurrentForceN)
        assertNull(active.isometricPeakForceN)
        assertNull(active.isometricElapsedMillis)
        assertEquals(83.7, active.isometricCarrierForceN!!, 0.1)
        assertEquals(6, active.isometricCarrierStatusPrimary)
        assertEquals(2, active.isometricCarrierStatusSecondary)
        assertTrue(active.isometricWaveformSamplesN.isEmpty())
    }

    @Test
    fun extractsLiveForceFromLegacy812bPullFrames() {
        val first = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "553A047010AA93282000AA812B0000000000000000006C000C00000000000000000000000000BA503E00000000000000000000000000A00F1BB5"
                .hexToByteArray(),
            nowMillis = 30_000L,
        )
        val second = VoltraNotificationParser.mergeReading(
            current = first,
            value = "553A047010AA94282000AA812B00000000000000000068000C000000000000000000000000001E513E00000000000000000000000000A00F468D"
                .hexToByteArray(),
            nowMillis = 30_100L,
        )
        val third = VoltraNotificationParser.mergeReading(
            current = second,
            value = "553A047010AA95282000AA812B00000000000000000061000C0000000000000000000000000082513E00000000000000000000000000A00F64A8"
                .hexToByteArray(),
            nowMillis = 30_200L,
        )

        assertEquals(48.0, first.isometricCurrentForceN!!, 0.1)
        assertEquals(48.0, first.isometricPeakForceN!!, 0.1)
        assertEquals(0L, first.isometricElapsedMillis)
        assertEquals(listOf(48.0), first.isometricWaveformSamplesN.map { (it * 10.0).toInt() / 10.0 })

        assertEquals(46.2, second.isometricCurrentForceN!!, 0.1)
        assertEquals(48.0, second.isometricPeakForceN!!, 0.1)
        assertEquals(100L, second.isometricElapsedMillis)
        assertEquals(
            listOf(48.0, 46.2),
            second.isometricWaveformSamplesN.map { (it * 10.0).toInt() / 10.0 },
        )

        assertEquals(43.1, third.isometricCurrentForceN!!, 0.1)
        assertEquals(48.0, third.isometricPeakForceN!!, 0.1)
        assertEquals(200L, third.isometricElapsedMillis)
        assertEquals(
            listOf(48.0, 46.2, 43.1),
            third.isometricWaveformSamplesN.map { (it * 10.0).toInt() / 10.0 },
        )
        assertEquals(12, third.isometricCarrierStatusSecondary)
    }

    @Test
    fun extractsCoarseLiveForceFromLegacy812bCarrierFrames() {
        val first = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "553A047010AA872E2000AA812B000000000000000000000001000000000000000000000000007B32F3000000000000000000000000000803C204"
                .hexToByteArray(),
            nowMillis = 40_000L,
        )
        val second = VoltraNotificationParser.mergeReading(
            current = first,
            value = "553A047010AA882E2000AA812B00000000000000000000000100000000000000000000000000DF32F3000000000000000000000000003006D6CE"
                .hexToByteArray(),
            nowMillis = 40_100L,
        )
        val third = VoltraNotificationParser.mergeReading(
            current = second,
            value = "553A047010AA892E2000AA812B000000000000000000000001000000000000000000000000004333F30000000000000000000000000058098E67"
                .hexToByteArray(),
            nowMillis = 40_200L,
        )

        assertEquals(82.8, first.isometricCurrentForceN!!, 0.1)
        assertEquals(82.8, first.isometricPeakForceN!!, 0.1)
        assertEquals(0L, first.isometricElapsedMillis)
        assertEquals(listOf(82.7), first.isometricWaveformSamplesN.map { (it * 10.0).toInt() / 10.0 })

        assertEquals(169.0, second.isometricCurrentForceN!!, 0.1)
        assertEquals(169.0, second.isometricPeakForceN!!, 0.1)
        assertEquals(100L, second.isometricElapsedMillis)
        assertEquals(
            listOf(82.7, 169.0),
            second.isometricWaveformSamplesN.map { (it * 10.0).toInt() / 10.0 },
        )

        assertEquals(255.2, third.isometricCurrentForceN!!, 0.1)
        assertEquals(255.2, third.isometricPeakForceN!!, 0.1)
        assertEquals(200L, third.isometricElapsedMillis)
        assertEquals(
            listOf(82.7, 169.0, 255.2),
            third.isometricWaveformSamplesN.map { (it * 10.0).toInt() / 10.0 },
        )
        assertEquals(1, third.isometricCarrierStatusSecondary)
    }

    @Test
    fun legacy812bHeartbeatDoesNotEraseCollectedLiveIsometricSamples() {
        val live = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = VoltraFrameBuilder.build(
                cmd = 0xB4,
                payload = byteArrayOf(
                    0x12,
                    0x00,
                    0x03,
                    0x00,
                    0x00,
                    0x00,
                    0x32,
                    0x00,
                ),
                seq = 0x122,
            ),
            nowMillis = 11_500L,
        )
        val heartbeat = VoltraNotificationParser.mergeReading(
            current = live,
            value = "553A047010AA1B802000AA812B00000000000000000002000200000000000000000000000000791A6D000000000000000000000000000803187D"
                .hexToByteArray(),
            nowMillis = 11_600L,
        )

        assertEquals(lbToNewtons(18.0), heartbeat.isometricCurrentForceN!!, 0.1)
        assertEquals(lbToNewtons(18.0), heartbeat.isometricPeakForceN!!, 0.1)
        assertEquals(0L, heartbeat.isometricElapsedMillis)
        assertEquals(
            listOf(roundToTenth(lbToNewtons(18.0))),
            heartbeat.isometricWaveformSamplesN.map { (it * 10.0).toInt() / 10.0 },
        )
    }

    @Test
    fun extractsIsometricTelemetryFromB4StreamFrames() {
        val first = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "551504A910AAED480000B40000020002002C00F95D".hexToByteArray(),
            nowMillis = 8000L,
        )
        val second = VoltraNotificationParser.mergeReading(
            current = first,
            value = "551504A910AA06490000B4060002000000320026C0".hexToByteArray(),
            nowMillis = 8100L,
        )
        val peak = VoltraNotificationParser.mergeReading(
            current = second,
            value = "551504A910AA83490000B42000020000003200DCBE".hexToByteArray(),
            nowMillis = 8300L,
        )

        assertEquals(0.0, first.isometricCurrentForceN)
        assertEquals(0.0, first.isometricPeakForceN)
        assertEquals(0L, first.isometricElapsedMillis)
        assertEquals(lbToNewtons(6.0), second.isometricCurrentForceN!!, 0.1)
        assertEquals(lbToNewtons(6.0), second.isometricPeakForceN!!, 0.1)
        assertEquals(100L, second.isometricElapsedMillis)
        assertEquals(lbToNewtons(32.0), peak.isometricCurrentForceN!!, 0.1)
        assertEquals(lbToNewtons(32.0), peak.isometricPeakForceN!!, 0.1)
        assertEquals(300L, peak.isometricElapsedMillis)
    }

    @Test
    fun extractsIsometricTelemetryFromB4VariantOneStreamFrames() {
        val first = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "551504A910AAF21D0000B40600010000003200A2D7".hexToByteArray(),
            nowMillis = 9000L,
        )
        val second = VoltraNotificationParser.mergeReading(
            current = first,
            value = "551504A910AA241E0000B407000100000032003E33".hexToByteArray(),
            nowMillis = 9100L,
        )

        assertEquals(lbToNewtons(6.0), first.isometricCurrentForceN!!, 0.1)
        assertEquals(lbToNewtons(6.0), first.isometricPeakForceN!!, 0.1)
        assertEquals(0L, first.isometricElapsedMillis)
        assertEquals(lbToNewtons(7.0), second.isometricCurrentForceN!!, 0.1)
        assertEquals(lbToNewtons(7.0), second.isometricPeakForceN!!, 0.1)
        assertEquals(100L, second.isometricElapsedMillis)
    }

    @Test
    fun extractsIsometricTelemetryFromCapturedB4VariantThreeFrames() {
        val first = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = VoltraFrameBuilder.build(
                cmd = 0xB4,
                payload = byteArrayOf(
                    0x0D,
                    0x00,
                    0x03,
                    0x00,
                    0x00,
                    0x00,
                    0x32,
                    0x00,
                ),
                seq = 0x120,
            ),
            nowMillis = 11_000L,
        )
        val second = VoltraNotificationParser.mergeReading(
            current = first,
            value = VoltraFrameBuilder.build(
                cmd = 0xB4,
                payload = byteArrayOf(
                    0x12,
                    0x00,
                    0x03,
                    0x00,
                    0x00,
                    0x00,
                    0x32,
                    0x00,
                ),
                seq = 0x121,
            ),
            nowMillis = 11_100L,
        )

        assertEquals(lbToNewtons(13.0), first.isometricCurrentForceN!!, 0.1)
        assertEquals(lbToNewtons(13.0), first.isometricPeakForceN!!, 0.1)
        assertEquals(0L, first.isometricElapsedMillis)
        assertEquals(lbToNewtons(18.0), second.isometricCurrentForceN!!, 0.1)
        assertEquals(lbToNewtons(18.0), second.isometricPeakForceN!!, 0.1)
        assertEquals(100L, second.isometricElapsedMillis)
    }

    @Test
    fun extractsLegacyIsometricPullTelemetryFromLiveForceCarrierFrames() {
        val reading = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = VoltraFrameBuilder.build(
                cmd = 0xAA,
                payload = byteArrayOf(
                    0x81.toByte(),
                    0x2B,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x96.toByte(),
                    0x00,
                    0x0C,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x64,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0xA0.toByte(),
                    0x0F,
                ),
                seq = 0x130,
            ),
            nowMillis = 12_000L,
        )

        assertEquals(66.7, reading.isometricCurrentForceN!!, 0.1)
        assertEquals(66.7, reading.isometricPeakForceN!!, 0.1)
        assertEquals(0L, reading.isometricElapsedMillis)
        assertEquals(150, reading.isometricCarrierStatusPrimary)
        assertEquals(12, reading.isometricCarrierStatusSecondary)
    }

    @Test
    fun continuesLegacyIsometricLivePullAcrossSecondaryMarkersThirteenThroughFifteen() {
        val first = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "553A047010AA02502000AA812B00000000000000000082000C0000000000000000000000000011773100000000000000000000000000A00F2494"
                .hexToByteArray(),
            nowMillis = 50_000L,
        )
        val second = VoltraNotificationParser.mergeReading(
            current = first,
            value = "553A047010AA05502000AA812B000000000000000000ED000D000000000000000000000000003D783100000000000000000000000000A00FB299"
                .hexToByteArray(),
            nowMillis = 50_100L,
        )
        val third = VoltraNotificationParser.mergeReading(
            current = second,
            value = "553A047010AA1A502000AA812B00000000000000000024020F0000000000000000000000000071803100000000000000000000000000A00F0F0B"
                .hexToByteArray(),
            nowMillis = 50_200L,
        )

        assertEquals(57.8, first.isometricCurrentForceN!!, 0.1)
        assertEquals(57.8, first.isometricPeakForceN!!, 0.1)
        assertEquals(105.4, second.isometricCurrentForceN!!, 0.1)
        assertEquals(105.4, second.isometricPeakForceN!!, 0.1)
        assertEquals(243.8, third.isometricCurrentForceN!!, 0.1)
        assertEquals(243.8, third.isometricPeakForceN!!, 0.1)
        assertEquals(2_400L, third.isometricElapsedMillis)
        assertEquals(3, third.isometricWaveformSamplesN.size)
        assertEquals(57.8, third.isometricWaveformSamplesN.first(), 0.1)
        assertEquals(243.8, third.isometricWaveformSamplesN.last(), 0.1)
        assertEquals(15, third.isometricCarrierStatusSecondary)
    }

    @Test
    fun legacyCompletedMarkerClearsCurrentForceButKeepsCollectedAttempt() {
        val current = VoltraReading(
            isometricCurrentForceN = 191.254,
            isometricPeakForceN = 253.088,
            isometricElapsedMillis = 5_400L,
            isometricTelemetryTick = 1_000L,
            isometricTelemetryStartTick = 900L,
            isometricCarrierForceN = 426.8,
            isometricCarrierStatusPrimary = 133,
            isometricCarrierStatusSecondary = 12,
        )

        val completed = VoltraNotificationParser.mergeReading(
            current = current,
            value = "553A047010AADA9B2000AA812B00000000000000000016000B0000000000000000000000000068BF2C01000000000000000000000000A00FE0F5"
                .hexToByteArray(),
            nowMillis = 12_500L,
        )

        assertNull(completed.isometricCurrentForceN)
        assertEquals(253.088, completed.isometricPeakForceN!!, 0.001)
        assertEquals(5_400L, completed.isometricElapsedMillis)
        assertEquals(22, completed.isometricCarrierStatusPrimary)
        assertEquals(11, completed.isometricCarrierStatusSecondary)
    }

    @Test
    fun letsHigherSummaryPacketFinalizeCollectedIsometricTelemetry() {
        val live = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = VoltraFrameBuilder.build(
                cmd = 0xB4,
                payload = byteArrayOf(
                    0x12,
                    0x00,
                    0x03,
                    0x00,
                    0x00,
                    0x00,
                    0x32,
                    0x00,
                ),
                seq = 0x122,
            ),
            nowMillis = 11_500L,
        )

        val summary = VoltraNotificationParser.mergeReading(
            current = live,
            value = "553404AC10AA08082000AA8025050005000000000000000000005400000000000000050300010000640001EC0200000000006583"
                .hexToByteArray(),
            nowMillis = 11_600L,
        )

        assertEquals(lbToNewtons(18.0), summary.isometricCurrentForceN!!, 0.1)
        assertEquals(77.3, summary.isometricPeakForceN!!, 0.1)
        assertEquals(10.0, summary.isometricPeakRelativeForcePercent!!, 0.1)
        assertEquals(2_000L, summary.isometricElapsedMillis)
    }

    @Test
    fun ignoresLowerSummaryPacketAfterStrongerCollectedIsometricTelemetry() {
        val live = VoltraReading(
            isometricCurrentForceN = 116.3,
            isometricPeakForceN = 157.9,
            isometricElapsedMillis = 3_900L,
            isometricTelemetryTick = 39_000L,
            isometricTelemetryStartTick = 35_100L,
            isometricWaveformSamplesN = listOf(82.7, 169.0, 157.9),
        )

        val summary = VoltraNotificationParser.mergeReading(
            current = live,
            value = "553404AC10AA08082000AA8025050005000000000000000000005400000000000000050300010000640001EC0200000000006583"
                .hexToByteArray(),
            nowMillis = 11_600L,
        )

        assertEquals(116.3, summary.isometricCurrentForceN!!, 0.1)
        assertEquals(157.9, summary.isometricPeakForceN!!, 0.1)
        assertNull(summary.isometricPeakRelativeForcePercent)
        assertEquals(3_900L, summary.isometricElapsedMillis)
    }

    @Test
    fun letsSummaryOverrideSparseCarrierOnlyLegacyTrace() {
        val coarseLegacyTrace = VoltraReading(
            isometricCurrentForceN = null,
            isometricPeakForceN = 241.6,
            isometricElapsedMillis = 1_000L,
            isometricTelemetryTick = 39_000L,
            isometricTelemetryStartTick = 38_000L,
            isometricCarrierForceN = 426.8,
            isometricCarrierStatusPrimary = 18,
            isometricCarrierStatusSecondary = 10,
            isometricWaveformSamplesN = listOf(82.8, 169.0, 241.6),
        )

        val summary = VoltraNotificationParser.mergeReading(
            current = coarseLegacyTrace,
            value = "553404AC10AA08082000AA8025050005000000000000000000005400000000000000050300010000640001EC0200000000006583"
                .hexToByteArray(),
            nowMillis = 11_600L,
        )

        assertNull(summary.isometricCurrentForceN)
        assertEquals(77.3, summary.isometricPeakForceN!!, 0.1)
        assertEquals(10.0, summary.isometricPeakRelativeForcePercent!!, 0.1)
        assertEquals(2_000L, summary.isometricElapsedMillis)
        assertEquals(
            listOf(26.4, 54.0, 77.3),
            summary.isometricWaveformSamplesN.map(::roundToTenth),
        )
    }

    @Test
    fun extractsIsometricTelemetryFromModernB4StreamFrames() {
        val first = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "551504A910AA3B4D0000B412010D000000A00FB129".hexToByteArray(),
            nowMillis = 10_000L,
        )
        val second = VoltraNotificationParser.mergeReading(
            current = first,
            value = "551504A910AA864D0000B413010D000000A00FADBF".hexToByteArray(),
            nowMillis = 10_100L,
        )
        val descending = VoltraNotificationParser.mergeReading(
            current = second,
            value = "551504A910AA72610000B400010D000000A00F1816".hexToByteArray(),
            nowMillis = 10_300L,
        )

        assertEquals(274.0, first.isometricCurrentForceN!!, 0.1)
        assertEquals(274.0, first.isometricPeakForceN!!, 0.1)
        assertEquals(0L, first.isometricElapsedMillis)
        assertEquals(listOf(274.0), first.isometricWaveformSamplesN.map { (it * 10.0).toInt() / 10.0 })
        assertEquals(275.0, second.isometricCurrentForceN!!, 0.1)
        assertEquals(275.0, second.isometricPeakForceN!!, 0.1)
        assertEquals(100L, second.isometricElapsedMillis)
        assertEquals(listOf(274.0, 275.0), second.isometricWaveformSamplesN.map { (it * 10.0).toInt() / 10.0 })
        assertEquals(256.0, descending.isometricCurrentForceN!!, 0.1)
        assertEquals(275.0, descending.isometricPeakForceN!!, 0.1)
        assertEquals(300L, descending.isometricElapsedMillis)
        assertEquals(
            listOf(274.0, 275.0, 256.0),
            descending.isometricWaveformSamplesN.map { (it * 10.0).toInt() / 10.0 },
        )
    }

    @Test
    fun extractsIsometricTelemetryFromLowTrailingModernB4Frames() {
        val first = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "551504A910AA79B50000B41300000000003A00F1BB".hexToByteArray(),
            nowMillis = 12_000L,
        )
        val second = VoltraNotificationParser.mergeReading(
            current = first,
            value = "551504A910AA92B50000B41700000000004F009F10".hexToByteArray(),
            nowMillis = 12_100L,
        )

        assertEquals(19.0, first.isometricCurrentForceN!!, 0.1)
        assertEquals(19.0, first.isometricPeakForceN!!, 0.1)
        assertEquals(0L, first.isometricElapsedMillis)
        assertEquals(listOf(19.0), first.isometricWaveformSamplesN.map { (it * 10.0).toInt() / 10.0 })

        assertEquals(23.0, second.isometricCurrentForceN!!, 0.1)
        assertEquals(23.0, second.isometricPeakForceN!!, 0.1)
        assertEquals(100L, second.isometricElapsedMillis)
        assertEquals(
            listOf(19.0, 23.0),
            second.isometricWaveformSamplesN.map { (it * 10.0).toInt() / 10.0 },
        )
    }

    @Test
    fun extractsIsometricTelemetryFromHighStatusModernB4Frames() {
        val first = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "551504A910AA951D0000B44A00440000004A00D16F".hexToByteArray(),
            nowMillis = 13_000L,
        )
        val second = VoltraNotificationParser.mergeReading(
            current = first,
            value = "551504A910AA251F0000B44300410054004700BEEB".hexToByteArray(),
            nowMillis = 13_100L,
        )

        assertEquals(74.0, first.isometricCurrentForceN!!, 0.1)
        assertEquals(74.0, first.isometricPeakForceN!!, 0.1)
        assertEquals(0L, first.isometricElapsedMillis)
        assertEquals(listOf(74.0), first.isometricWaveformSamplesN.map { (it * 10.0).toInt() / 10.0 })

        assertEquals(67.0, second.isometricCurrentForceN!!, 0.1)
        assertEquals(74.0, second.isometricPeakForceN!!, 0.1)
        assertEquals(100L, second.isometricElapsedMillis)
        assertEquals(
            listOf(74.0, 67.0),
            second.isometricWaveformSamplesN.map { (it * 10.0).toInt() / 10.0 },
        )
    }

    @Test
    fun extractsIsometricTelemetryFromExtendedModernB4Frames() {
        val first = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "551904E410AA0E0A0200B42C002E01C600020003002E000CE0".hexToByteArray(),
            nowMillis = 14_000L,
        )
        val second = VoltraNotificationParser.mergeReading(
            current = first,
            value = "551904E410AA400A0100B42A002301F200000022012C007700".hexToByteArray(),
            nowMillis = 14_100L,
        )

        assertEquals(44.0, first.isometricCurrentForceN!!, 0.1)
        assertEquals(44.0, first.isometricPeakForceN!!, 0.1)
        assertEquals(0L, first.isometricElapsedMillis)
        assertEquals(2, first.isometricCarrierStatusPrimary)
        assertEquals(3, first.isometricCarrierStatusSecondary)
        assertEquals(listOf(44.0), first.isometricWaveformSamplesN.map { (it * 10.0).toInt() / 10.0 })

        assertEquals(42.0, second.isometricCurrentForceN!!, 0.1)
        assertEquals(44.0, second.isometricPeakForceN!!, 0.1)
        assertEquals(100L, second.isometricElapsedMillis)
        assertEquals(0, second.isometricCarrierStatusPrimary)
        assertEquals(290, second.isometricCarrierStatusSecondary)
        assertEquals(
            listOf(44.0, 42.0),
            second.isometricWaveformSamplesN.map { (it * 10.0).toInt() / 10.0 },
        )
    }

    @Test
    fun accumulatesChunkedIsometricWaveformFramesForRealtimeGraph() {
        val first = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = VoltraFrameBuilder.build(
                cmd = 0xAA,
                payload = byteArrayOf(
                    0x93.toByte(),
                    0xCC.toByte(),
                    0x01,
                    0x03,
                    0x03,
                    0x00,
                    0x0A,
                    0x00,
                    0x14,
                    0x00,
                    0x1E,
                    0x00,
                ),
                seq = 20,
            ),
            nowMillis = 9200L,
        )
        val second = VoltraNotificationParser.mergeReading(
            current = first,
            value = VoltraFrameBuilder.build(
                cmd = 0xAA,
                payload = byteArrayOf(
                    0x93.toByte(),
                    0xCC.toByte(),
                    0x02,
                    0x03,
                    0x02,
                    0x00,
                    0x28,
                    0x00,
                    0x32,
                    0x00,
                ),
                seq = 21,
            ),
            nowMillis = 9300L,
        )

        assertEquals(
            listOf(4.4, 8.8, 13.3),
            first.isometricWaveformSamplesN.map { (it * 10.0).toInt() / 10.0 },
        )
        assertEquals(1, first.isometricWaveformLastChunkIndex)
        assertEquals(
            listOf(4.4, 8.8, 13.3, 17.7, 22.2),
            second.isometricWaveformSamplesN.map { (it * 10.0).toInt() / 10.0 },
        )
        assertEquals(2, second.isometricWaveformLastChunkIndex)
    }

    @Test
    fun acceptsCapturedWaveformTrailerVariantsForIsometricGraph() {
        val base = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "55db04c110aae5012000aa93cc010c6400040004000500070009000a000a000b000b000b000b000c000e00100013001300150018001b001f0021002300250028003300390039003600360036003700370037003700370038003800380039003a003b003c003d003f00410044004600470049004b004c004d004f0050005100520054005500560057005800590059005b005c005d005e0060006100610063006400660066006800680069006a006a006a006c006c006d006e006f007000700071007100720072007200720072007300730074007400750075006637"
                .hexToByteArray(),
            nowMillis = 9400L,
        )
        val trailer = VoltraNotificationParser.mergeReading(
            current = base,
            value = "559104bd10aaf0012000aa93820c0c3f009600950094009300910090008e008e008c008b008a00890087008500830082007f007d007b00790076007300720070006e006c006b006b006b006900660062005f005f005e005e005d005c005b005b005900590058005700560055005400520051004f004d004b004a00480046004500430041003f003e003a003600320020e6"
                .hexToByteArray(),
            nowMillis = 9500L,
        )

        assertEquals(100, base.isometricWaveformSamplesN.size)
        assertEquals(163, trailer.isometricWaveformSamplesN.size)
        assertEquals(12, trailer.isometricWaveformLastChunkIndex)
    }

    @Test
    fun mergesFirmwarePartsAcrossFrames() {
        val first = VoltraNotificationParser.mergeReading(
            current = VoltraReading(),
            value = "554708EC10AA0100200077000045500000000000000000000000000000425000000000000000000000000000004550312E30000000000000000000000042010701000000012306"
                .hexToByteArray(),
            nowMillis = 1L,
        )
        val second = VoltraNotificationParser.mergeReading(
            current = first,
            value = VoltraFrameBuilder.build(
                cmd = 0x77,
                payload = "BP1.0 MainControlv1.6".encodeToByteArray(),
                seq = 2,
            ),
            nowMillis = 2L,
        )

        val firmware = second.firmwareVersion.orEmpty()
        assertTrue(firmware.contains("EP1.0"), firmware)
        assertTrue(firmware.contains("BP1.0"), firmware)
        assertTrue(firmware.contains("MainControlv1.6"), firmware)
        assertTrue("........" !in firmware, firmware)
        assertEquals(2L, second.lastUpdatedMillis)
    }
}

private fun lbToNewtons(pounds: Double): Double = pounds * 4.4482216152605

private fun roundToTenth(value: Double): Double = (value * 10.0).toInt() / 10.0
