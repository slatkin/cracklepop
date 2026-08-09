/*
 *     This file is part of cracklepop
 *     Copyright (C) 2014-2018  Johannes Pohl
 *     Modifications Copyright (C) 2026  cracklepop contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.badaix.snapcast.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.tv.material3.darkColorScheme

/**
 * Everforest Dark Hard palette expressed as Compose ColorScheme tokens.
 * Sized for 10-foot legibility.
 *
 * Palette reference: https://github.com/sainnhe/everforest
 * Dark Hard variant:
 *   bg0:      #1e2326  (harder black)
 *   bg1:      #272e33
 *   bg2:      #2e383c
 *   bg3:      #374145
 *   bg4:      #404c50
 *   bg5:      #495559
 *   bg_visual:#4c3743
 *   fg:       #d3c6aa  (warm off-white)
 *   red:      #e67e80
 *   orange:   #e69875
 *   yellow:   #dbbc7f
 *   green:    #a7c080
 *   aqua:     #83c092
 *   blue:     #7fbbb3
 *   purple:   #d699b6
 *   grey0:    #7a8478
 */
object EverforestColors {
    val bg0 = Color(0xFF1E2326)
    val bg1 = Color(0xFF272E33)
    val bg2 = Color(0xFF2E383C)
    val bg3 = Color(0xFF374145)
    val bg4 = Color(0xFF404C50)
    val bg5 = Color(0xFF495559)
    val fg = Color(0xFFD3C6AA)
    val red = Color(0xFFE67E80)
    val orange = Color(0xFFE69875)
    val yellow = Color(0xFFDBBC7F)
    val green = Color(0xFFA7C080)
    val aqua = Color(0xFF83C092)
    val blue = Color(0xFF7FBBB3)
    val purple = Color(0xFFD699B6)
    val grey0 = Color(0xFF7A8478)

    // Focus indicator — high contrast for 10-foot legibility
    val focusIndicator = Color(0xFFDBBC7F) // yellow
}

/**
 * Dark ColorScheme based on Everforest Dark Hard.
 */
val EverforestDarkScheme = darkColorScheme(
    background = EverforestColors.bg0,
    surface = EverforestColors.bg1,
    surfaceVariant = EverforestColors.bg2,
    onBackground = EverforestColors.fg,
    onSurface = EverforestColors.fg,
    onSurfaceVariant = EverforestColors.grey0,
    primary = EverforestColors.green,
    onPrimary = EverforestColors.bg0,
    secondary = EverforestColors.blue,
    onSecondary = EverforestColors.bg0,
    error = EverforestColors.red,
    onError = EverforestColors.bg0,
)
