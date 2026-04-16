package com.technogizguy.voltra.controller.protocol

import kotlin.test.Test
import kotlin.test.assertEquals

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
