package com.example.musicplayer

import java.io.Serializable

data class Data(
    val album_backlink: Any?,
    val artwork: Artwork?,
    val comment_count: Int?,
    val description: String?,
    val duration: Int?,
    val favorite_count: Int?,
    val genre: String?,
    val id: String?,
    val is_downloadable: Boolean?,
    val is_original_available: Boolean?,
    val is_streamable: Boolean?,
    val isrc: String?,
    val mood: String?,
    val orig_file_cid: String?,
    val orig_filename: String?,
    val permalink: String?,
    val pinned_comment_id: Int?,
    val play_count: Int?,
    val playlists_containing_track: List<Int>?,
    val preview_cid: String?,
    val release_date: String?,
    val remix_of: RemixOf?,
    val repost_count: Int?,
    val tags: String?,
    val title: String?,
    val track_cid: String?,
    val user: UserX?
) : Serializable

data class Artwork(
    val `1000x1000`: String?,
    val `150x150`: String?,
    val `480x480`: String?,
    val mirrors: List<String>?
) : Serializable

data class RemixOf(
    val tracks: List<Track>?
) : Serializable

data class Track(
    val id: String?,
    val title: String?
) : Serializable

data class UserX(
    val name: String?,
    val handle: String?,
    val id: String?,
    val profile_picture: ProfilePicture?
) : Serializable

data class ProfilePicture(
    val `150x150`: String?,
    val `480x480`: String?,
    val `1000x1000`: String?
) : Serializable
