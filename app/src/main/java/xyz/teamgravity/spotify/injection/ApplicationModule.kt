package xyz.teamgravity.spotify.injection

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import xyz.teamgravity.spotify.R
import xyz.teamgravity.spotify.helper.util.MusicServiceConnection
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    @Singleton
    @Provides
    fun provideGlide(@ApplicationContext context: Context) = Glide.with(context).setDefaultRequestOptions(
        RequestOptions()
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.placeholder_error)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
    )

    @Singleton
    @Provides
    fun provideMusicServiceConnection(@ApplicationContext context: Context) = MusicServiceConnection(context)
}