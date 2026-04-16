package com.technogizguy.voltra.controller.protocol

import com.technogizguy.voltra.controller.model.VoltraReading
import com.technogizguy.voltra.controller.model.VoltraSafetyState
import kotlin.test.Test
import kotlin.test.assertEquals
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

        assertEquals("Isometric Test, Ready", reading.workoutMode)
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
    fun allowsLoadWhileIsometricScreenIsOpen() {
        val reading = VoltraReading(
            batteryPercent = 90,
            activationState = "Activated",
        )
        val safety = VoltraNotificationParser.mergeSafety(
            current = VoltraSafetyState(targetLoadLb = 49.0),
            reading = reading,
                value = "551D04DF10AA421E2000100400893E8500B04F0811503967540801216E".hexToByteArray(),
        )

        assertTrue(safety.canLoad)
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

        assertEquals(76.0, first.isometricCurrentForceN)
        assertEquals(76.0, first.isometricPeakForceN)
        assertEquals(0L, first.isometricElapsedMillis)
        assertEquals(156.8, second.isometricCurrentForceN)
        assertEquals(156.8, second.isometricPeakForceN)
        assertEquals(100L, second.isometricElapsedMillis)
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
        assertEquals(156.8, armedHeartbeat.isometricPeakForceN)
        assertEquals(0L, armedHeartbeat.isometricElapsedMillis)
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
        assertEquals(26.7, second.isometricCurrentForceN!!, 0.1)
        assertEquals(26.7, second.isometricPeakForceN!!, 0.1)
        assertEquals(100L, second.isometricElapsedMillis)
        assertEquals(142.3, peak.isometricCurrentForceN!!, 0.1)
        assertEquals(142.3, peak.isometricPeakForceN!!, 0.1)
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

        assertEquals(26.7, first.isometricCurrentForceN!!, 0.1)
        assertEquals(26.7, first.isometricPeakForceN!!, 0.1)
        assertEquals(0L, first.isometricElapsedMillis)
        assertEquals(31.1, second.isometricCurrentForceN!!, 0.1)
        assertEquals(31.1, second.isometricPeakForceN!!, 0.1)
        assertEquals(100L, second.isometricElapsedMillis)
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
