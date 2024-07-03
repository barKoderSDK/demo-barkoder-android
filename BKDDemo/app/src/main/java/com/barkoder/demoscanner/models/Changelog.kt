package com.barkoder.demoscanner.models

class Changelog(val releaseVersion: String, private val description: Array<String>) {

    fun descriptionAsString(): String {
        return when {
            description.size == 1 -> {
                StringBuilder().append("- ").appendLine(description[0]).toString()
            }
            description.size > 1 -> {
                val descriptionBuilder = StringBuilder()
                for (item in description) {
                    descriptionBuilder.append("- ").appendLine(item)
                }

                descriptionBuilder.toString()
            }
            else -> ""
        }
    }
}
