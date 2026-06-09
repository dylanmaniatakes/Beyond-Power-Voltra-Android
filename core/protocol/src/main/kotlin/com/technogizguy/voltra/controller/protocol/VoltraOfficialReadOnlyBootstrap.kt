package com.technogizguy.voltra.controller.protocol

data class VoltraBootstrapPacket(
    val label: String,
    val hex: String,
) {
    val bytes: ByteArray = hex.hexToByteArray()
}

object VoltraOfficialReadOnlyBootstrap {
    private const val APP_HELLO_NAME = "Open Source"

    val packets: List<VoltraBootstrapPacket> = listOf(
        VoltraBootstrapPacket(
            label = "commonHandshake app hello ($APP_HELLO_NAME)",
            hex = VoltraFrameBuilder.build(
                cmd = VoltraControlFrames.CMD_READ_DEVICE_NAME,
                payload = appHelloPayload(APP_HELLO_NAME),
                sender = 0x01,
                receiver = VoltraFrameBuilder.DEVICE_RECEIVER,
                seq = 0x00,
            ).toHexString(),
        ),
        VoltraBootstrapPacket(
            label = "commonConnectRequest",
            hex = "550f0801aad200002000ff00aa0419",
        ),
        VoltraBootstrapPacket(
            label = "handshake finish/check",
            hex = "551f044eaa10000020002781105eab9ef41c864ff5877a9c8c1d5f0d603e86",
        ),
        VoltraBootstrapPacket(
            label = "read common state",
            hex = "550d0433aa10000020007403bc",
        ),
        VoltraBootstrapPacket(
            label = "read firmware page 0",
            hex = "550e0466aa100100200077003889",
        ),
        VoltraBootstrapPacket(
            label = "read firmware page 1",
            hex = "550e0466aa10020020007701cc94",
        ),
        VoltraBootstrapPacket(
            label = "read serial page",
            hex = "550e0466aa100300200019002b7e",
        ),
        VoltraBootstrapPacket(
            label = "read activation/security page",
            hex = "550e0466aa1004002000ab01ad7a",
        ),
        VoltraBootstrapPacket(
            label = "read battery state (BMS_RSOC)",
            hex = VoltraFrameBuilder.build(
                cmd = VoltraControlFrames.CMD_PARAM_READ,
                payload = VoltraControlFrames.readParamsPayload(
                    VoltraControlFrames.PARAM_BMS_RSOC,
                    VoltraControlFrames.PARAM_BMS_RSOC_LEGACY,
                ),
                seq = 0x05,
            ).toHexString(),
        ),
        VoltraBootstrapPacket(
            label = "read mode feature state",
            hex = VoltraFrameBuilder.build(
                cmd = VoltraControlFrames.CMD_PARAM_READ,
                payload = VoltraControlFrames.readParamsPayload(
                    VoltraControlFrames.PARAM_BP_BASE_WEIGHT,
                    VoltraControlFrames.PARAM_RESISTANCE_BAND_MAX_FORCE,
                    VoltraControlFrames.PARAM_RESISTANCE_BAND_ALGORITHM,
                    VoltraControlFrames.PARAM_RESISTANCE_BAND_LEN,
                    VoltraControlFrames.PARAM_RESISTANCE_BAND_LEN_BY_ROM,
                    VoltraControlFrames.PARAM_EP_RESISTANCE_BAND_INVERSE,
                    VoltraControlFrames.PARAM_FEATURE_LIST_01,
                    VoltraControlFrames.PARAM_FEATURE_LIST_02,
                    VoltraControlFrames.PARAM_OVERDRIVE_AVAILABLE,
                    VoltraControlFrames.PARAM_OVERDRIVE_USER_CFG_FORCE_MAX,
                    VoltraControlFrames.PARAM_OVERDRIVE_ACTIVE_STATUS,
                    VoltraControlFrames.PARAM_EP_MAX_ALLOWED_FORCE,
                    VoltraControlFrames.PARAM_EP_MAX_ECCENTRIC_PCT,
                    VoltraControlFrames.PARAM_EP_MAX_CHAINS_PCT,
                    VoltraControlFrames.PARAM_FITNESS_ASSIST_MODE,
                    VoltraControlFrames.PARAM_BP_CHAINS_WEIGHT,
                    VoltraControlFrames.PARAM_BP_ECCENTRIC_WEIGHT,
                    VoltraControlFrames.PARAM_FITNESS_INVERSE_CHAIN,
                    VoltraControlFrames.PARAM_WEIGHT_TRAINING_EXTRA_MODE,
                    VoltraControlFrames.PARAM_BP_SET_FITNESS_MODE,
                    VoltraControlFrames.PARAM_FITNESS_WORKOUT_STATE,
                    VoltraControlFrames.PARAM_ISOMETRIC_MAX_FORCE,
                    VoltraControlFrames.PARAM_ISOMETRIC_MAX_DURATION,
                    VoltraControlFrames.PARAM_APP_CUR_SCR_ID,
                    VoltraControlFrames.PARAM_FITNESS_ONGOING_UI,
                    VoltraControlFrames.PARAM_BP_RUNTIME_POSITION_CM,
                    VoltraControlFrames.PARAM_MC_DEFAULT_OFFLEN_CM,
                    VoltraControlFrames.PARAM_QUICK_CABLE_ADJUSTMENT,
                ),
                seq = 0x06,
            ).toHexString(),
        ),
    )

    private fun appHelloPayload(name: String): ByteArray {
        return VoltraControlFrames.setDeviceNamePayload(name) +
            byteArrayOf(0x84.toByte(), 0xAB.toByte(), 0x1A, 0x5F, 0x29, 0x20, 0x01)
    }
}
