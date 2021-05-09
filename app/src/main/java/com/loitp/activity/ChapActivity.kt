package com.loitp.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.annotation.IsFullScreen
import com.annotation.IsSwipeActivity
import com.annotation.LogTag
import com.core.base.BaseApplication
import com.core.base.BaseFontActivity
import com.core.utilities.*
import com.loitp.R
import com.loitp.adapter.ChapAdapter
import com.loitp.adapter.LoadingAdapter
import com.loitp.adapter.StoryOverViewAdapter
import com.loitp.db.Db
import com.loitp.model.Story
import com.loitp.service.StoryApiConfiguration
import com.loitp.viewmodels.ChapViewModel
import com.views.layout.swipeback.SwipeBackLayout.OnSwipeBackListener
import kotlinx.android.synthetic.main.activity_chap.*
import kotlinx.android.synthetic.main.activity_chap.indicatorView
import kotlinx.android.synthetic.main.activity_chap.ivBackground
import kotlinx.android.synthetic.main.activity_chap.recyclerView
import kotlinx.android.synthetic.main.frm_home.*
import kotlinx.android.synthetic.main.view_row_item_story.view.*

@LogTag("ChapActivity")
@IsFullScreen(true)
@IsSwipeActivity(true)
class ChapActivity : BaseFontActivity() {

    companion object {
        const val KEY_STORY = "KEY_STORY"
    }

    private var story: Story? = null
    private var chapViewModel: ChapViewModel? = null
    private var pageIndex = 0
    private var totalPage = Int.MAX_VALUE
    private var concatAdapter = ConcatAdapter()
    private val loadingAdapter = LoadingAdapter()
    private var storyOverViewAdapter = StoryOverViewAdapter()
    private var chapAdapter: ChapAdapter? = null

    override fun setLayoutResourceId(): Int {
        return R.layout.activity_chap
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupData()
        setupViews()
        setupViewModels()

        //save last story
        story?.getImgSource()?.let {
            Db.setLastBackground(it)
        }

        getListChap()
    }

    private fun getListChap() {
        story?.id?.let { comicId ->
            chapViewModel?.getListChap(
                comicId = comicId,
                pageSize = StoryApiConfiguration.PAGE_SIZE,
                pageIndex = pageIndex
            )
        }
    }

    private fun setupData() {
        val tmpStory = intent.getSerializableExtra(KEY_STORY)
        if (tmpStory != null && tmpStory is Story) {
            this.story = tmpStory
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupViews() {

        fun setupDataInRecyclerView() {
            recyclerView.layoutManager = LinearLayoutManager(this)

            chapAdapter = ChapAdapter(ArrayList())
            chapAdapter?.let { na ->
                na.onClickRootListener = { chap, _ ->
                    //TODO
                    val intent = Intent(this, ReadActivity::class.java)
//                    intent.putExtra(ChapActivity.KEY_STORY, story)
                    startActivity(intent)
                    LActivityUtil.tranIn(this)
                }
            }

            storyOverViewAdapter.onClickBackListener = {
                onBackPressed()
            }
            storyOverViewAdapter.onClickShareListener = {
                LSocialUtil.shareApp(this)
            }

            chapAdapter?.let {
                concatAdapter.addAdapter(it)
            }

            recyclerView.adapter = concatAdapter

            LUIUtil.setScrollChange(
                recyclerView = recyclerView,
                onTop = {
                    logD("onTop")
                },
                onBottom = {
                    logD("loitpp onBottom")
                    loadMore()
                }
            )
        }

        LImageUtil.load(
            context = this,
            any = story?.getImgSource(),
            imageView = ivBackground
        )

        swipeBackLayout.setSwipeBackListener(object : OnSwipeBackListener {
            override fun onViewPositionChanged(
                mView: View?,
                swipeBackFraction: Float,
                swipeBackFactor: Float
            ) {
            }

            override fun onViewSwipeFinished(mView: View?, isEnd: Boolean) {
                if (isEnd) {
                    finish()
                    LActivityUtil.transActivityNoAnimation(this@ChapActivity)
                }
            }
        })
        setupDataInRecyclerView()
    }

    private fun setupViewModels() {
        chapViewModel = getViewModel(ChapViewModel::class.java)
        chapViewModel?.let { mvm ->
            mvm.listStoryLiveData.observe(this, Observer { actionData ->
                logD("loitpp <<<listStoryLiveData " + BaseApplication.gson.toJson(actionData.data))
                val isDoing = actionData.isDoing
                val isSuccess = actionData.isSuccess
                actionData.totalPages?.let {
                    totalPage = it
                }

                if (isDoing == true) {
                    if (actionData?.page == 0) {
                        indicatorView.smoothToShow()
                    }
                } else {

                    //set ui for story over
                    setupStoryOverviewUI()

                    if (actionData?.page == 0) {
                        indicatorView.smoothToHide()
                    }

                    if (isSuccess == true) {
                        concatAdapter.removeAdapter(loadingAdapter)
                        val listChap = actionData.data
                        if (listChap.isNullOrEmpty()) {
                            //do nothing
                        } else {
                            chapAdapter?.addData(
                                listChap = listChap
                            )
                        }
                    } else {
                        val error = actionData.errorResponse
                        showDialogError(error?.message ?: getString(R.string.err_unknow), Runnable {
                            //do nothing
                        })
                    }
                }
            })
        }

    }

    private fun isLoading(): Boolean {
        concatAdapter.adapters.forEach { childAdapter ->
            if (childAdapter == loadingAdapter) {
                return true
            }
        }
        return false
    }

    private fun isHasStoryOverViewAdapter(): Boolean {
        concatAdapter.adapters.forEach { childAdapter ->
            if (childAdapter == storyOverViewAdapter) {
                return true
            }
        }
        return false
    }

    private fun setupStoryOverviewUI() {
        if (!isHasStoryOverViewAdapter()) {
            storyOverViewAdapter.setData(story)
            concatAdapter.addAdapter(0, storyOverViewAdapter)
        }
    }

    private fun loadMore() {
        logE("loitpp loadMore pageIndex $pageIndex, totalPage $totalPage, isLoading() ${isLoading()}")
        if (pageIndex >= totalPage) {
            return
        }
        if (!isLoading()) {
            concatAdapter.addAdapter(loadingAdapter)
            concatAdapter.itemCount.let {
                recyclerView.scrollToPosition(it - 1)
            }
            pageIndex++
            getListChap()
        }
    }

}