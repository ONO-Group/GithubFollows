package com.skydoves.githubfollows.repository

import android.arch.lifecycle.LiveData
import com.skydoves.githubfollows.api.ApiResponse
import com.skydoves.githubfollows.api.GithubService
import com.skydoves.githubfollows.models.Envelope
import com.skydoves.githubfollows.models.Follower
import com.skydoves.githubfollows.models.GithubUser
import com.skydoves.githubfollows.models.Resource
import com.skydoves.githubfollows.preference.PreferenceComponent_PrefAppComponent
import com.skydoves.githubfollows.preference.Preference_UserProfile
import com.skydoves.githubfollows.room.FollowersDao
import com.skydoves.githubfollows.room.GithubUserDao
import com.skydoves.preferenceroom.InjectPreference
import org.jetbrains.anko.doAsync
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**s
 * Created by skydoves on 2018. 3. 6.
 * Copyright (c) 2018 skydoves All rights reserved.
 */

@Singleton
class GithubUserRepository @Inject
constructor(val githubUserDao: GithubUserDao, val followersDao: FollowersDao, val service: GithubService) {

    @InjectPreference lateinit var profile: Preference_UserProfile

    init {
        Timber.d("Injection GithubUserRepository")
        PreferenceComponent_PrefAppComponent.getInstance().inject(this)
    }

    fun refreshUser(user: String) {
        profile.putName(user)
        doAsync { githubUserDao.truncateGithubUser() }
    }

    fun loadUser(user: String): LiveData<Resource<GithubUser>> {
        return object: NetworkBoundRepository<GithubUser, GithubUser>() {
            override fun saveFetchData(item: GithubUser) {
                doAsync { githubUserDao.insertGithubUser(item) }
            }

            override fun shouldFetch(data: GithubUser?): Boolean {
                return data == null
            }

            override fun loadFromDb(): LiveData<GithubUser> {
                return githubUserDao.getGithubUser(user)
            }

            override fun fetchService(): LiveData<ApiResponse<GithubUser>> {
                return service.fetchGithubUser(user)
            }

            override fun onFetchFailed(envelope: Envelope?) {
                Timber.d("onFetchFailed : $envelope")
            }
        }.asLiveData()
    }

    fun loadFollowers(user: String, page: Int, isFollowers: Boolean): LiveData<Resource<List<Follower>>> {
        return object : NetworkBoundRepository<List<Follower>, List<Follower>>() {
            override fun saveFetchData(items: List<Follower>) {
                doAsync {
                    for(item in items) {
                        item.owner = user
                        item.page = page
                        item.isFollower = isFollowers
                    }
                    followersDao.insertFollowers(items)
                }
            }

            override fun shouldFetch(data: List<Follower>?): Boolean {
                return data == null || data.isEmpty()
            }

            override fun loadFromDb(): LiveData<List<Follower>> {
                return followersDao.getFollowers(user, page, isFollowers)
            }

            override fun fetchService(): LiveData<ApiResponse<List<Follower>>> {
                if(isFollowers) return service.fetchFollowers(user, page, per_page)
                return service.fetchFollowings(user, page, per_page)
            }

            override fun onFetchFailed(envelope: Envelope?) {
                Timber.d("onFetchFailed : $envelope")
            }
        }.asLiveData()
    }

    fun getUserKeyName(): String = profile.nameKeyName()

    fun getPreferenceMenuPosition(): Int = profile.menuPosition

    fun getUserName(): String = profile.name

    fun putPreferenceMenuPosition(position: Int) = profile.putMenuPosition(position)

    companion object {
        const val per_page = 10
    }
}
