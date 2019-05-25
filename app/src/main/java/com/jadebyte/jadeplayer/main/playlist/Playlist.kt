// Copyright (c) 2019 . Wilberforce Uwadiegwu. All Rights Reserved.

package com.jadebyte.jadeplayer.main.playlist

import android.database.Cursor
import android.os.Parcelable
import android.provider.MediaStore
import kotlinx.android.parcel.Parcelize

/**
 * Created by Wilberforce on 2019-05-19 at 10:13.
 */

@Parcelize
data class Playlist(val id: Long, val name: String) : Parcelable {

    constructor(cursor: Cursor) : this(
        id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID)),
        name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Playlists.NAME))
    )
}