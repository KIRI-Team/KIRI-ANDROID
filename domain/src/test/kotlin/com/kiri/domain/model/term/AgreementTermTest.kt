package com.kiri.domain.model.term

import org.junit.Assert.assertEquals
import org.junit.Test

class AgreementTermTest {

    @Test
    fun 필수_약관의_제목에는_필수_표시가_붙는다() {
        val term = AgreementTerm.TERM_1

        assertEquals("[필수] ${term.displayName}", term.title)
    }
}
