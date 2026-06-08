package com.technogizguy.voltra.controller.protocol

import com.technogizguy.voltra.controller.model.VoltraParamValueType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VoltraParamCsvParserTest {
    @Test
    fun parsesQuotedFieldsAndBooleans() {
        val csv = """
            "id","name","description","unit","valueType","length","defaultValue","min","max","needReboot","category","writable","volatile","submodule"
            21722,"EP_WORKOUT_CHAINS_PCT_X100_S32","Chains weight percent, x100, int32_t","-","int32",4,"0","-2147483648","2147483647",0,"BP",1,1,"DEFAULT"
        """.trimIndent()

        val param = VoltraParamCsvParser.parse(csv).single()

        assertEquals(21722, param.id)
        assertEquals("Chains weight percent, x100, int32_t", param.description)
        assertEquals(VoltraParamValueType.INT32, param.valueType)
        assertTrue(param.writable)
        assertTrue(param.volatile)
    }

    @Test
    fun generatedRegistryContainsRecoveredCsvRows() {
        assertTrue(VoltraParamRegistry.all.size >= 2_210)
        assertEquals("FITNESS_REST_TIME_OVERRIDE", VoltraParamRegistry.byId.getValue(21731).name)
    }
}
