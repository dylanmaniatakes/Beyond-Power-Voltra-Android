package com.technogizguy.voltra.controller.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VoltraFrameBuilderTest {
    @Test
    fun buildsConfirmedOfficialEnterWorkoutFrame() {
        val frame = VoltraFrameBuilder.build(
            cmd = 0x11,
            payload = VoltraControlFrames.enterWeightTrainingPayload(),
            seq = 0x13,
        )

        assertEquals(
            "551204C7AA1013002000110100B04F012ED4",
            frame.toHexString(),
        )
    }

    @Test
    fun buildsConfirmedOfficialExitWorkoutFrame() {
        val frame = VoltraFrameBuilder.build(
            cmd = 0x11,
            payload = VoltraControlFrames.exitWeightTrainingPayload(),
            seq = 0x14,
        )

        assertEquals(
            "551204C7AA1014002000110100B04F005201",
            frame.toHexString(),
        )
    }

    @Test
    fun buildsConfirmedOfficialLoadModeFrame() {
        val frame = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_PARAM_WRITE,
            payload = VoltraControlFrames.loadPayload(),
            seq = 0x24,
        )

        assertEquals(
            "55130403AA1024002000110100893E05006C4B",
            frame.toHexString(),
        )
    }

    @Test
    fun buildsConfirmedOfficialEnterIsokineticFrame() {
        val frame = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_PARAM_WRITE,
            payload = VoltraControlFrames.enterIsokineticPayload(),
            seq = 0x13,
        )

        assertEquals(
            "551204C7AA1013002000110100B04F0718B1",
            frame.toHexString(),
        )
    }

    @Test
    fun buildsConfirmedOfficialIsokineticTargetSpeedFrame() {
        val frame = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_PARAM_WRITE,
            payload = VoltraControlFrames.setIsokineticTargetSpeedPayload(1000),
            seq = 0x14,
        )

        assertEquals(
            "551504A9AA10140020001101005053E8030000D59B",
            frame.toHexString(),
        )
    }

    @Test
    fun buildsConfirmedOfficialUnloadModeFrame() {
        val frame = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_PARAM_WRITE,
            payload = VoltraControlFrames.unloadPayload(),
            seq = 0x2A,
        )

        assertEquals(
            "55130403AA102A002000110100893E0400691B",
            frame.toHexString(),
        )
    }

    @Test
    fun buildsConfirmedOfficialFivePoundBaseWeightFrame() {
        val frame = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_PARAM_WRITE,
            payload = VoltraControlFrames.setBaseWeightPayload(5),
            seq = 0x14,
        )

        assertEquals(
            "55130403AA1014002000110100863E05005A6A",
            frame.toHexString(),
        )
    }

    @Test
    fun buildsConfirmedOfficialTenPoundBaseWeightFrame() {
        val frame = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_PARAM_WRITE,
            payload = VoltraControlFrames.setBaseWeightPayload(10),
            seq = 0x22,
        )

        assertEquals(
            "55130403AA1022002000110100863E0A002A8F",
            frame.toHexString(),
        )
    }

    @Test
    fun buildsConfirmedOfficialThirtyPoundChainsFrame() {
        val frame = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_PARAM_WRITE,
            payload = VoltraControlFrames.setChainsWeightPayload(30),
            seq = 0x20,
        )

        assertEquals(
            "55130403AA1020002000110100873E1E0042CA",
            frame.toHexString(),
        )
    }

    @Test
    fun buildsConfirmedOfficialNegativeTwentyPoundEccentricFrame() {
        val frame = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_PARAM_WRITE,
            payload = VoltraControlFrames.setEccentricWeightPayload(-20),
            seq = 0x23,
        )

        assertEquals(
            "55130403AA1023002000110100883EECFFC8C6",
            frame.toHexString(),
        )
    }

    @Test
    fun buildsConfirmedOfficialInverseChainsOffFrame() {
        val frame = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_PARAM_WRITE,
            payload = VoltraControlFrames.setInverseChainsPayload(false),
            seq = 0x27,
        )

        assertEquals(
            "551204C7AA1027002000110100B05300ED37",
            frame.toHexString(),
        )
    }

    @Test
    fun buildsConfirmedOfficialAssistOnFrame() {
        val frame = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_PARAM_WRITE,
            payload = VoltraControlFrames.setAssistModePayload(true),
            seq = 0x14,
        )

        assertEquals(
            "551204C7AA1014002000110100065101C143",
            frame.toHexString(),
        )
    }

    @Test
    fun buildsConfirmedOfficialRenameFrame() {
        val frame = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_SET_DEVICE_NAME,
            payload = VoltraControlFrames.setDeviceNamePayload("Dylans Voltra"),
            seq = 0x27,
        )

        assertEquals(
            "552204EAAA10270020004E44796C616E7320566F6C7472610000000000000000D72F",
            frame.toHexString(),
        )
    }

    @Test
    fun buildsCompactStartupImageHeaderPayloadTrailer() {
        val payload = VoltraControlFrames.startupImageHeaderPayload(
            imageBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04),
            chunkCount = 0x57,
        )

        assertEquals("0201FFFF00000000D002D002000000008142040000005700", payload.toHexString())
    }

    @Test
    fun buildsStartupImageFingerprintWithSizeLow16AndVoltraCrc16() {
        val compactPayload = VoltraControlFrames.startupImageHeaderPayload(
            imageBytes = ByteArray(53_844),
            chunkCount = 117,
            trailer = VoltraControlFrames.STARTUP_IMAGE_HEADER_COMPACT_TRAILER,
        )
        val largePayload = VoltraControlFrames.startupImageHeaderPayload(
            imageBytes = ByteArray(155_320),
            chunkCount = 335,
            trailer = VoltraControlFrames.STARTUP_IMAGE_HEADER_CUSTOM_PHOTO_TRAILER,
        )

        assertEquals("A85554D2", compactPayload.copyOfRange(16, 20).toHexString())
        assertEquals("DB58B85E", largePayload.copyOfRange(16, 20).toHexString())
    }

    @Test
    fun selectsLargeCustomPhotoStartupImageHeaderTrailer() {
        val trailer = VoltraControlFrames.startupImageHeaderTrailer(
            imageBytes = ByteArray(80_000),
            chunkCount = 200,
        )

        assertEquals(VoltraControlFrames.STARTUP_IMAGE_HEADER_CUSTOM_PHOTO_TRAILER, trailer)
    }

    @Test
    fun buildsCapturedVendorStateRefreshFrame() {
        val frame = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_VENDOR,
            payload = VoltraControlFrames.vendorStateRefreshPayload(),
            seq = 0x5C,
        )

        assertEquals("550F04A2AA105C002000AA130186F9", frame.toHexString())
    }

    @Test
    fun buildsRowingSelectorPayloadsAsZeroBasedIndices() {
        assertEquals(
            "0100A75303",
            VoltraControlFrames.setRowingResistanceLevelPayload(4).toHexString(),
        )
        assertEquals(
            "0100AE5307",
            VoltraControlFrames.setRowingSimulatedWearLevelPayload(8).toHexString(),
        )
    }

    @Test
    fun buildsCapturedFiftyMeterRowStartScreenSwitchPayload() {
        assertEquals(
            "01006551063E0001",
            VoltraControlFrames.triggerRowStartScreenPayload(50).toHexString(),
        )
    }

    @Test
    fun mapsRowPresetScreenSwitchActions() {
        assertEquals("01006551043E0001", VoltraControlFrames.selectJustRowScreenPayload().toHexString())
        assertEquals("01006551033E0001", VoltraControlFrames.triggerRowStartScreenPayload(null).toHexString())
        assertEquals("01006551063E0001", VoltraControlFrames.triggerRowStartScreenPayload(50).toHexString())
        assertEquals("01006551073E0001", VoltraControlFrames.triggerRowStartScreenPayload(100).toHexString())
        assertEquals("01006551083E0001", VoltraControlFrames.triggerRowStartScreenPayload(500).toHexString())
        assertEquals("01006551093E0001", VoltraControlFrames.triggerRowStartScreenPayload(1000).toHexString())
        assertEquals("010065510A3E0001", VoltraControlFrames.triggerRowStartScreenPayload(2000).toHexString())
        assertEquals("010065510B3E0001", VoltraControlFrames.triggerRowStartScreenPayload(5000).toHexString())
    }

    @Test
    fun treatsRowingActiveAsLoadedOnlyForRowWorkoutState() {
        assertFalse(VoltraControlFrames.isLoadedFitnessMode(VoltraControlFrames.FITNESS_MODE_ROWING_ACTIVE))
        assertFalse(
            VoltraControlFrames.isLoadEngagedForWorkoutState(
                VoltraControlFrames.FITNESS_MODE_ROWING_ACTIVE,
                VoltraControlFrames.WORKOUT_STATE_ACTIVE,
            ),
        )
        assertTrue(
            VoltraControlFrames.isLoadEngagedForWorkoutState(
                VoltraControlFrames.FITNESS_MODE_ROWING_ACTIVE,
                VoltraControlFrames.WORKOUT_STATE_ROWING,
            ),
        )
    }

    @Test
    fun buildsCapturedIsometricCablePositionReadFrame() {
        val frame = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_PARAM_READ,
            payload = VoltraControlFrames.readIsometricCablePositionPayload(),
            seq = 0x14,
        )

        assertEquals("55130403AA10140020000F02006A50823E1E7A", frame.toHexString())
    }

    @Test
    fun buildsCapturedIsometricArmFrame() {
        val frame = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_PARAM_WRITE,
            payload = VoltraControlFrames.loadIsometricPayload(),
            seq = 0x16,
        )

        assertEquals("55130403AA1016002000110100893E0100E114", frame.toHexString())
    }

    @Test
    fun buildsCapturedCustomCurveEnterFrame() {
        val frame = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_PARAM_WRITE,
            payload = VoltraControlFrames.enterCustomCurvePayload(),
            seq = 0x14,
        )

        assertEquals("551204C7AA1014002000110100B04F066464", frame.toHexString())
    }

    @Test
    fun buildsCapturedCustomCurveNotifySubscribeFrame() {
        val frame = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_PARAM_WRITE,
            payload = VoltraControlFrames.setFitnessDataNotifySubscribePayload(),
            seq = 0x07,
        )

        assertEquals("551504A9AA10070020001101008351F57B65001D85", frame.toHexString())
    }

    @Test
    fun buildsCapturedCustomCurveNotifyHzFrame() {
        val frame = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_PARAM_WRITE,
            payload = VoltraControlFrames.setFitnessDataNotifyHzPayload(),
            seq = 0x09,
        )

        assertEquals("551204C7AA10090020001101008251284FB7", frame.toHexString())
    }

    @Test
    fun buildsStandardWorkoutTelemetryRestorePayloads() {
        assertEquals(
            "0100835100000000",
            VoltraControlFrames.resetFitnessDataNotifySubscribePayload().toHexString(),
        )
        assertEquals(
            "0100825100",
            VoltraControlFrames.resetFitnessDataNotifyHzPayload().toHexString(),
        )
    }

    @Test
    fun buildsCapturedCustomCurveVendorPresetFrame() {
        val frame = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_VENDOR,
            payload = VoltraControlFrames.customCurveVendorPresetPayload(),
            seq = 0x13,
        )

        assertEquals(
            "55550432AA1013002000AA06020000920464006405E64E9CEA0300000000000000004C172D3EEFE3FC3D4C17AD3EEFE37C3E5136013F4DC7D33E0DFBE02B3F518E143F7EF0553F29474A3F0000803F0000803F018F",
            frame.toHexString(),
        )
    }

    @Test
    fun buildsEditableCustomCurveVendorPresetPayload() {
        val payload = VoltraControlFrames.customCurveVendorPresetPayload(
            listOf(0.25f, 0.35f, 0.45f, 0.55f),
        )

        assertEquals(
            "06020000920464006405E64E9CEA0300000000000000004C172D3E3433733E4C17AD3EDEDD8D3E5136013F2322A23E0DFBE02B3F6666B63E7EF0553FABAACA3E0000803FF0EEDE3E",
            payload.toHexString(),
        )
    }

    @Test
    fun buildsEditableCustomCurveVendorPresetPayloadWithResistanceAndRangeOfMotion() {
        val payload = VoltraControlFrames.customCurveVendorPresetPayload(
            points = listOf(0.25f, 0.35f, 0.45f, 0.55f),
            resistanceMinLb = 25,
            resistanceLimitLb = 150,
            rangeOfMotionIn = 80,
        )

        assertEquals(
            "06020000200396009619E64E9CEA0300000000000000004C172D3E0000A03E4C17AD3EAAAABA3E5136013F5555D53E0DFBE02B3FFFFFEF3E7EF0553F5555053F0000803FABAA123F",
            payload.toHexString(),
        )
    }

    @Test
    fun scalesCustomCurveWireValuesToResistanceLimit() {
        val payload = VoltraControlFrames.customCurveVendorPresetPayload(
            resistanceMinLb = 5,
            resistanceLimitLb = 25,
        )

        val finalWireY = ByteBuffer
            .wrap(payload, payload.size - 4, 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .float

        assertEquals("06020000920419001905", payload.take(10).toByteArray().toHexString())
        assertEquals(0.16666667f, finalWireY, 0.0001f)
    }

    @Test
    fun capsCustomCurveWireRangeOfMotionAtCapturedIpadCeiling() {
        val payload = VoltraControlFrames.customCurveVendorPresetPayload(
            rangeOfMotionIn = VoltraControlFrames.MAX_CUSTOM_CURVE_RANGE_OF_MOTION_IN,
        )

        assertEquals("9204", payload.copyOfRange(4, 6).toHexString())
    }

    @Test
    fun buildsCustomCurveMaxAllowedForcePayload() {
        val payload = VoltraControlFrames.setMaxAllowedForcePayload(150)

        assertEquals("010014539600", payload.toHexString())
    }

    @Test
    fun buildsCapturedCustomCurveBulkSubscribeFrame() {
        val frame = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_BULK_PARAM_WRITE,
            payload = VoltraControlFrames.customCurveBulkSubscribePayload(),
            seq = 0x08,
        )

        assertEquals(
            "55D304B7AA1008002000AF024100B04F015053018153015153016E50017F5301A85101C45301115401883E01A75301065101645301853E01315401CF5301145401873E016A5001825201155401E14E01835101DE5401525301823E01675401863E015553018C5401E552012D4E011150011853015B53011353010F5401D25301245101195301893E01035101B653014154018B5401AE53016F5001625301B05301C95301C85301DF54010F5201025101145301B75301C75301125401215401C65301D45401C553018D53011354011054017566",
            frame.toHexString(),
        )
    }

    @Test
    fun buildsCapturedRowBulkSubscribeFrame() {
        val frame = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_BULK_PARAM_WRITE,
            payload = VoltraControlFrames.rowBulkSubscribePayload(),
            seq = 0x08,
        )

        assertEquals(
            "55D304B7AA1008002000AF0241007F5301505301515301A85101525301C75301145301835101245101105401AE5301145401675401DF5401B04F010F5401065101C85301CF5301823E01645301E55201185301125401C45301315401155401A75301863E012D4E01135301893E018252015B5301135401195301815301B65301555301883E01625301215401B05301C95301DE5401873E01E14E010F52018B5401C553018D5301025101415401D454011154016A5001C65301035101853E016F5001115001D253018C54016E5001B753010760",
            frame.toHexString(),
        )
    }

    @Test
    fun buildsExtendedStartupImageChunkFrame() {
        val chunk = ByteArray(VoltraControlFrames.STARTUP_IMAGE_CHUNK_DATA_BYTES) { index ->
            (index and 0xFF).toByte()
        }

        val frame = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_STARTUP_IMAGE,
            payload = VoltraControlFrames.startupImageChunkPayload(
                chunkIndex = 1,
                chunkBytes = chunk,
            ),
            seq = 0x13,
            frameType = VoltraFrameBuilder.FRAME_TYPE_EXTENDED_APP_WRITE,
        )

        assertEquals(480, frame.size)
        assertEquals(0x55.toByte(), frame[0])
        assertEquals(0xE0.toByte(), frame[1])
        assertEquals(0x05.toByte(), frame[2])
        assertTrue(frame.copyOfRange(14, 14 + chunk.size).contentEquals(chunk))
        assertTrue(assertNotNull(VoltraPacketParser.parse(frame)).lengthMatches)
    }

    @Test
    fun buildsConfirmedOfficialStrengthModeFrame() {
        val frame = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_PARAM_WRITE,
            payload = VoltraControlFrames.setStrengthModePayload(),
            seq = 0x1C,
        )

        assertEquals(
            "55130403AA101C002000110100893E0400D17D",
            frame.toHexString(),
        )
    }

    @Test
    fun buildsBatteryReadFrameForConnectBootstrap() {
        val frame = VoltraFrameBuilder.build(
            cmd = VoltraControlFrames.CMD_PARAM_READ,
            payload = VoltraControlFrames.readParamsPayload(
                VoltraControlFrames.PARAM_BMS_RSOC,
                VoltraControlFrames.PARAM_BMS_RSOC_LEGACY,
            ),
            seq = 0x05,
        )

        assertEquals(
            "55130403AA10050020000F02002D4E5D1B8E20",
            frame.toHexString(),
        )
    }
}
