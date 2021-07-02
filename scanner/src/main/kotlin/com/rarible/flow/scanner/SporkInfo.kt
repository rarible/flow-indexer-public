package com.rarible.flow.scanner

/**
 * Created by TimochkinEA at 01.07.2021
 *
 * Information about Flow Spork
 *
 * @property name               spork name
 * @property nodeUrl            url to Access API
 * @property firstBlockHeight   height of first block in spork
 * @property lastBlockHeight    height of first block in spork, default -1, that means than current spork
 */
data class SporkInfo(
    val name: String,
    val nodeUrl: String,
    val firstBlockHeight: Long,
    val lastBlockHeight: Long = -1
)
