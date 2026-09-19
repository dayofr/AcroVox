package com.acrovox.core.database

import androidx.room.TypeConverter
import com.acrovox.core.model.DownloadStatus
import com.acrovox.core.model.EpisodeActionType
import com.acrovox.core.model.EpisodeState

/** Enums stockés par nom : l'ordre des constantes peut changer sans migration. */
internal class Converters {
    @TypeConverter fun fromEpisodeState(value: EpisodeState): String = value.name

    @TypeConverter fun toEpisodeState(value: String): EpisodeState = EpisodeState.valueOf(value)

    @TypeConverter fun fromDownloadStatus(value: DownloadStatus): String = value.name

    @TypeConverter fun toDownloadStatus(value: String): DownloadStatus = DownloadStatus.valueOf(value)

    @TypeConverter fun fromActionType(value: EpisodeActionType): String = value.name

    @TypeConverter fun toActionType(value: String): EpisodeActionType = EpisodeActionType.valueOf(value)
}
