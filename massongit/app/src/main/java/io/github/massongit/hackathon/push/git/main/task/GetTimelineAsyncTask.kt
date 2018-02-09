package io.github.massongit.hackathon.push.git.main.task

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v4.widget.SwipeRefreshLayout
import android.util.Log
import android.widget.Toast
import com.eclipsesource.json.JsonArray
import com.eclipsesource.json.JsonObject
import com.github.scribejava.core.exceptions.OAuthException
import com.github.scribejava.core.oauth.OAuth20Service
import io.github.massongit.hackathon.push.git.R
import io.github.massongit.hackathon.push.git.main.event.*
import io.github.massongit.hackathon.push.git.main.eventView.EventViewAdapter
import io.github.massongit.hackathon.push.git.main.helper.MainHelper
import org.apache.commons.lang3.time.DateFormatUtils
import java.lang.ref.WeakReference
import java.net.URL


/**
 * タイムラインを取得する非同期タスク
 * @param context Activity
 * @param service GitHub APIのサービス
 * @param swipeRefreshLayout SwipeRefreshLayout
 * @param eventViewAdapter イベントビューのアダプター
 * @param helper Helper
 * @param isCurrent 最新のタイムラインを取得するかどうか
 */
class GetTimelineAsyncTask(context: Context, service: OAuth20Service?, swipeRefreshLayout: SwipeRefreshLayout, private val eventViewAdapter: EventViewAdapter, helper: MainHelper, private val isCurrent: Boolean = true) : RequestAsyncTask<Unit, Unit, List<Event>>(service, helper) {
    companion object {
        /**
         * ログ用タグ
         */
        private val TAG: String? = GetTimelineAsyncTask::class.simpleName

        /**
         * 現在見ているタイムラインのページ番号
         */
        private var page: Int = 1
    }

    /**
     * Activityを保持するWeakReference
     */
    private val contextWeakReference: WeakReference<Context> = WeakReference(context)

    /**
     * SwipeRefreshLayoutを保持するWeakReference
     */
    private val swipeRefreshLayoutWeakReference: WeakReference<SwipeRefreshLayout> = WeakReference(swipeRefreshLayout)

    override fun onPreExecute() {
        Log.v(GetTimelineAsyncTask.TAG, "onPreExecute called")
        Toast.makeText(this.contextWeakReference.get(), this.contextWeakReference.get()?.getString(R.string.getting_user_timeline), Toast.LENGTH_SHORT).show()
        this.swipeRefreshLayoutWeakReference.get()?.isRefreshing = true
    }

    override fun doInBackground(vararg units: Unit): List<Event> {
        Log.v(GetTimelineAsyncTask.TAG, "doInBackground called")
        val events = mutableListOf<Event?>()
        val actorAvatarCache = mutableMapOf<String, Bitmap>()

        try {
            var receivedEventsUrl = "https://api.github.com/users/%s/received_events".format(this.helper.userName)

            if (!this.isCurrent) {
                GetTimelineAsyncTask.page++
                receivedEventsUrl += "?page=%d".format(GetTimelineAsyncTask.page)
            }

            (this.request(receivedEventsUrl, true) as? JsonArray)?.forEach {
                val receivedEvent = it as? JsonObject
                val type = receivedEvent?.get("type")?.asString()
                val actor = receivedEvent?.get("actor") as? JsonObject
                val repo = receivedEvent?.get("repo") as? JsonObject
                val payload = receivedEvent?.get("payload") as? JsonObject
                val repoName = repo?.get("name")?.asString()
                val createdAt = DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse(receivedEvent?.get("created_at")?.asString())
                val actorLogin = actor?.get("login")?.asString()
                val actorHtmlUrl = this.getHtmlUrl(actor)
                val actorAvatarUrl = actor?.get("avatar_url")?.asString()
                val actorAvatar: Bitmap = actorAvatarCache[actorAvatarUrl]
                        ?: URL(actorAvatarUrl).openStream().use {
                            BitmapFactory.decodeStream(it)
                        }
                if (actorAvatarUrl != null && !actorAvatarCache.contains(actorAvatarUrl)) {
                    actorAvatarCache[actorAvatarUrl] = actorAvatar
                }


                if (actorLogin != null && repoName != null) {
                    when (type) {
                        "GollumEvent" -> payload?.get("pages") as? JsonArray
                        "PushEvent" -> payload?.get("commits") as? JsonArray
                        else -> listOf(payload)
                    }?.forEach {
                        try {
                            val payloadElement = it as? JsonObject
                            events.add(when (type) {
                                "GollumEvent", "IssuesEvent", "IssueCommentEvent" -> {
                                    val issue = payloadElement?.get("issue") as? JsonObject
                                    val comment = payloadElement?.get("comment") as? JsonObject
                                    val number = issue?.get("number")?.asInt()
                                    val title = issue?.get("title")?.asString()
                                    val action = payloadElement?.get("action")?.asString()
                                    val eventHtmlUrl = when (type) {
                                        "IssuesEvent" -> this.getHtmlUrl(issue)
                                        "IssueCommentEvent" -> this.getHtmlUrl(comment)
                                        else -> this.getHtmlUrl(payloadElement)
                                    }
                                    if (type == "GollumEvent") {
                                        val wikiTitle = payloadElement?.get("title")?.asString()
                                        if (action == null || wikiTitle == null) {
                                            null
                                        } else {
                                            GollumEvent(actorLogin, repoName, actorHtmlUrl, eventHtmlUrl, actorAvatar, createdAt, action, wikiTitle)
                                        }
                                    } else if (type == "IssueCommentEvent") {
                                        val commentBody = comment?.get("body")?.asString()
                                        if (number == null || commentBody == null || title == null) {
                                            null
                                        } else {
                                            IssueCommentEvent(actorLogin, repoName, actorHtmlUrl, eventHtmlUrl, actorAvatar, createdAt, number, title, commentBody, issue.get("pull_request") != null)
                                        }
                                    } else if (type == "IssuesEvent" && action != null && number != null && title != null) {
                                        IssuesEvent(actorLogin, repoName, actorHtmlUrl, eventHtmlUrl, actorAvatar, createdAt, action, number, title)
                                    } else {
                                        null
                                    }
                                }
                                "PushEvent" -> {
                                    val branch = payload?.get("ref")?.asString()
                                    val commitMessage = payloadElement?.get("message")?.asString()
                                    if (branch == null || commitMessage == null) {
                                        null
                                    } else {
                                        PushEvent(actorLogin, repoName, actorHtmlUrl, this.getHtmlUrl(payloadElement), actorAvatar, createdAt, branch, commitMessage)
                                    }
                                }
                                "ReleaseEvent" -> {
                                    val release = payloadElement?.get("release") as? JsonObject
                                    val version = release?.get("name")?.asString()
                                    if (version == null) {
                                        null
                                    } else {
                                        ReleaseEvent(actorLogin, repoName, actorHtmlUrl, this.getHtmlUrl(release), actorAvatar, createdAt, version)
                                    }
                                }
                                "CreateEvent", "DeleteEvent" -> {
                                    val thingType = payloadElement?.get("ref_type")?.asString()
                                    val eventHtmlUrl = if (type == "CreateEvent") {
                                        this.getHtmlUrl(repo)
                                    } else {
                                        this.getHtmlUrl(payloadElement)
                                    }
                                    if (thingType == null) {
                                        null
                                    } else if (type == "DeleteEvent") {
                                        val thing = payloadElement.get("ref")?.asString()
                                        if (thing == null) {
                                            null
                                        } else {
                                            DeleteEvent(actorLogin, repoName, actorHtmlUrl, eventHtmlUrl, actorAvatar, createdAt, thingType, thing)
                                        }
                                    } else if (type == "CreateEvent" && thingType != "tag") {
                                        CreateEvent(actorLogin, repoName, actorHtmlUrl, eventHtmlUrl, actorAvatar, createdAt, thingType)
                                    } else {
                                        null
                                    }
                                }
                                "WatchEvent" -> WatchEvent(actorLogin, repoName, actorHtmlUrl, this.getHtmlUrl(repo), actorAvatar, createdAt)
                                else -> null
                            })
                        } catch (ignored: OAuthException) {

                        }
                    }
                }
            }
        } catch (ignored: OAuthException) {

        }

        return events.filterNotNull()
    }

    override fun onPostExecute(events: List<Event>) {
        Log.v(GetTimelineAsyncTask.TAG, "onPostExecute called")
        this.eventViewAdapter.addItems(events, this.isCurrent)
        this.swipeRefreshLayoutWeakReference.get()?.isRefreshing = false
        Toast.makeText(this.contextWeakReference.get(), this.contextWeakReference.get()?.getString(R.string.get_user_timeline_completed), Toast.LENGTH_SHORT).show()
    }

    /**
     * イベントのURLを取得する
     * @param jsonObject JsonObject
     * @return イベントのURL
     */
    private fun getHtmlUrl(jsonObject: JsonObject?): Uri {
        Log.v(GetTimelineAsyncTask.TAG, "getEventHtmlUrl called")
        if (jsonObject == null) {
            throw OAuthException("jsonObject is not found")
        } else {
            val htmlUrl = jsonObject.get("html_url")
            return if (htmlUrl == null) {
                val url = jsonObject.get("url")
                if (url == null) {
                    throw OAuthException("url is not found")
                } else {
                    this.getHtmlUrl(this.request(url.asString()) as? JsonObject)
                }
            } else {
                Uri.parse(htmlUrl.asString())
            }
        }
    }
}