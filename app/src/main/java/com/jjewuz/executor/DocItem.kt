package com.jjewuz.executor

sealed class DocSection {
    data class Text(val textRes: Int) : DocSection()
    data class Code(val code: String) : DocSection()
}

data class DocItem(val id: String, val titleRes: Int, val sections: List<DocSection>)
