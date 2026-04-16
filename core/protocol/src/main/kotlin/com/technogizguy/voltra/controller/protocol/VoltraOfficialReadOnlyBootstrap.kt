package com.technogizguy.voltra.controller.protocol

data class VoltraBootstrapPacket(
    val label: String,
    val hex: String,
) {
    val bytes: ByteArray = hex.hexToByteArray()
}

object VoltraOfficialReadOnlyBootstrap {
    val packets: List<VoltraBootstrapPacket> = listOf(
        VoltraBootstrapPacket(
            label = "commonHandshake app hello",
            hex = "552904c90110000020004f69506164000000000000000000000000000000000084ab1a5f292001ea4f",
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
                    VoltraControlFrames.PARAM_FITNESS_ASSIST_MODE,
                    VoltraControlFrames.PARAM_BP_CHAINS_WEIGHT,
                    VoltraControlFrames.PARAM_BP_ECCENTRIC_WEIGHT,
                    VoltraControlFrames.PARAM_FITNESS_INVERSE_CHAIN,
                    VoltraControlFrames.PARAM_WEIGHT_TRAINING_EXTRA_MODE,
                    VoltraControlFrames.PARAM_BP_SET_FITNESS_MODE,
                    VoltraControlFrames.PARAM_FITNESS_WORKOUT_STATE,
                    VoltraControlFrames.PARAM_ISOMETRIC_MAX_FORCE,
                    VoltraControlFrames.PARAM_ISOMETRIC_MAX_DURATION,
                    VoltraControlFrames.PARAM_BP_RUNTIME_POSITION_CM,
                    VoltraControlFrames.PARAM_MC_DEFAULT_OFFLEN_CM,
                    VoltraControlFrames.PARAM_QUICK_CABLE_ADJUSTMENT,
                ),
                seq = 0x06,
            ).toHexString(),
        ),
    )
}
