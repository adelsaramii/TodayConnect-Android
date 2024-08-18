package com.today.connect.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.today.connect.R
import com.today.connect.databinding.ItemSliderSpelashBinding

class ViewPagerAdapter : RecyclerView.Adapter<PagerVH>() {

    private lateinit var binding: ItemSliderSpelashBinding
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerVH {
        binding = ItemSliderSpelashBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PagerVH(binding.root)
    }

    //get the size of color array
    override fun getItemCount(): Int = 4

    //binding the screen with view
    override fun onBindViewHolder(holder: PagerVH, position: Int) = holder.itemView.run {
        if (position == 0) {
            binding.itemSliderTvTitle.text = resources.getString(R.string.online_chat_any_time)
            binding.itemSliderTvDescription.text =
                resources.getString(R.string.online_chat_any_time_discription)
            binding.itemSliderImage.setImageResource(R.drawable.chat_svg)
        }
        if (position == 1) {
            binding.itemSliderTvTitle.text = resources.getString(R.string.today_ai_assistant)
            binding.itemSliderTvDescription.text =
                resources.getString(R.string.today_ai_assistant_discription)
            binding.itemSliderImage.setImageResource(R.drawable.robot_svg)
        }
        if (position == 2) {
            binding.itemSliderTvTitle.text = resources.getString(R.string.high_message_security)
            binding.itemSliderTvDescription.text =
                resources.getString(R.string.high_message_security_discription)
            binding.itemSliderImage.setImageResource(R.drawable.secure_svg)
        }
        if (position == 3) {
            binding.itemSliderTvTitle.text = resources.getString(R.string.video_conference)
            binding.itemSliderTvDescription.text =
                resources.getString(R.string.video_conference_discription)
            binding.itemSliderImage.setImageResource(R.drawable.meeting_svg)
        }
    }
}

class PagerVH(itemView: View) : RecyclerView.ViewHolder(itemView)