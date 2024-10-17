package com.barkoder.demoscanner.utils

import com.barkoder.demoscanner.models.RecentScan2

sealed class ResultItem {
    data class Header(val date: String) : ResultItem()
    data class Row(val scanResult: RecentScan2) : ResultItem()
}
