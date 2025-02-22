package org.oppia.android.app.topic.practice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import org.oppia.android.app.fragment.FragmentScope
import org.oppia.android.app.recyclerview.BindableAdapter
import org.oppia.android.app.topic.RouteToQuestionPlayerListener
import org.oppia.android.app.topic.practice.practiceitemviewmodel.TopicPracticeFooterViewModel
import org.oppia.android.app.topic.practice.practiceitemviewmodel.TopicPracticeHeaderViewModel
import org.oppia.android.app.topic.practice.practiceitemviewmodel.TopicPracticeItemViewModel
import org.oppia.android.app.topic.practice.practiceitemviewmodel.TopicPracticeSubtopicViewModel
import org.oppia.android.databinding.TopicPracticeFooterViewBinding
import org.oppia.android.databinding.TopicPracticeFragmentBinding
import org.oppia.android.databinding.TopicPracticeHeaderViewBinding
import org.oppia.android.databinding.TopicPracticeSubtopicBinding
import org.oppia.android.domain.oppialogger.OppiaLogger
import javax.inject.Inject

/** The presenter for [TopicPracticeFragment]. */
@FragmentScope
class TopicPracticeFragmentPresenter @Inject constructor(
  private val activity: AppCompatActivity,
  private val fragment: Fragment,
  private val oppiaLogger: OppiaLogger,
  private val viewModel: TopicPracticeViewModel
) : SubtopicSelector {
  private lateinit var binding: TopicPracticeFragmentBinding
  private lateinit var linearLayoutManager: LinearLayoutManager
  lateinit var selectedSubtopicIdList: ArrayList<Int>
  var skillIdHashMap = HashMap<Int, MutableList<String>>()
  private lateinit var topicId: String
  private lateinit var topicPracticeFooterViewBinding: TopicPracticeFooterViewBinding
  private val routeToQuestionPlayerListener = activity as RouteToQuestionPlayerListener

  fun handleCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    subtopicList: ArrayList<Int>,
    selectedSkillId: HashMap<Int, MutableList<String>>,
    internalProfileId: Int,
    topicId: String
  ): View? {
    this.topicId = topicId
    viewModel.setTopicId(this.topicId)
    viewModel.setInternalProfileId(internalProfileId)

    selectedSubtopicIdList = subtopicList
    skillIdHashMap = selectedSkillId
    binding = TopicPracticeFragmentBinding.inflate(
      inflater,
      container,
      /* attachToRoot= */ false
    )

    binding.apply {
      this.viewModel = this@TopicPracticeFragmentPresenter.viewModel
      lifecycleOwner = fragment
    }

    linearLayoutManager = LinearLayoutManager(activity.applicationContext)

    binding.topicPracticeSkillList.apply {
      layoutManager = linearLayoutManager
      adapter = createRecyclerViewAdapter()
    }

    return binding.root
  }

  private fun createRecyclerViewAdapter(): BindableAdapter<TopicPracticeItemViewModel> {
    return BindableAdapter.MultiTypeBuilder
      .newBuilder<TopicPracticeItemViewModel, ViewType> { viewModel ->
        when (viewModel) {
          is TopicPracticeHeaderViewModel -> ViewType.VIEW_TYPE_HEADER
          is TopicPracticeSubtopicViewModel -> ViewType.VIEW_TYPE_SKILL
          is TopicPracticeFooterViewModel -> ViewType.VIEW_TYPE_FOOTER
          else -> throw IllegalArgumentException("Encountered unexpected view model: $viewModel")
        }
      }
      .registerViewDataBinder(
        viewType = ViewType.VIEW_TYPE_HEADER,
        inflateDataBinding = TopicPracticeHeaderViewBinding::inflate,
        setViewModel = TopicPracticeHeaderViewBinding::setViewModel,
        transformViewModel = { it as TopicPracticeHeaderViewModel }
      )
      .registerViewDataBinder(
        viewType = ViewType.VIEW_TYPE_SKILL,
        inflateDataBinding = TopicPracticeSubtopicBinding::inflate,
        setViewModel = this::bindSkillView,
        transformViewModel = { it as TopicPracticeSubtopicViewModel }
      )
      .registerViewDataBinder(
        viewType = ViewType.VIEW_TYPE_FOOTER,
        inflateDataBinding = TopicPracticeFooterViewBinding::inflate,
        setViewModel = this::bindFooterView,
        transformViewModel = { it as TopicPracticeFooterViewModel }
      )
      .build()
  }

  private fun bindSkillView(
    binding: TopicPracticeSubtopicBinding,
    model: TopicPracticeSubtopicViewModel
  ) {
    binding.viewModel = model
    binding.isChecked = selectedSubtopicIdList.contains(model.subtopic.subtopicId)
    binding.subtopicCheckBox.setOnCheckedChangeListener { _, isChecked ->
      if (isChecked) {
        subtopicSelected(model.subtopic.subtopicId, model.subtopic.skillIdsList)
      } else {
        subtopicUnselected(model.subtopic.subtopicId, model.subtopic.skillIdsList)
      }
    }
  }

  private fun bindFooterView(
    binding: TopicPracticeFooterViewBinding,
    model: TopicPracticeFooterViewModel
  ) {
    topicPracticeFooterViewBinding = binding
    binding.viewModel = model
    binding.isSubmitButtonActive = selectedSubtopicIdList.isNotEmpty()
    binding.topicPracticeStartButton.setOnClickListener {
      val skillIdList = ArrayList(skillIdHashMap.values)
      oppiaLogger.d("TopicPracticeFragmentPresenter", "Skill Ids = " + skillIdList.flatten())
      routeToQuestionPlayerListener.routeToQuestionPlayer(
        skillIdList.flatten() as ArrayList<String>
      )
    }
  }

  private enum class ViewType {
    VIEW_TYPE_HEADER,
    VIEW_TYPE_SKILL,
    VIEW_TYPE_FOOTER
  }

  override fun subtopicSelected(subtopicId: Int, skillIdList: MutableList<String>) {
    if (!selectedSubtopicIdList.contains(subtopicId)) {
      selectedSubtopicIdList.add(subtopicId)
      skillIdHashMap[subtopicId] = skillIdList
    }

    if (::topicPracticeFooterViewBinding.isInitialized) {
      topicPracticeFooterViewBinding.isSubmitButtonActive = skillIdHashMap.isNotEmpty()
    }
  }

  override fun subtopicUnselected(subtopicId: Int, skillIdList: MutableList<String>) {
    if (selectedSubtopicIdList.contains(subtopicId)) {
      selectedSubtopicIdList.remove(subtopicId)
      skillIdHashMap.remove(subtopicId)
    }
    if (::topicPracticeFooterViewBinding.isInitialized) {
      topicPracticeFooterViewBinding.isSubmitButtonActive = skillIdHashMap.isNotEmpty()
    }
  }
}
