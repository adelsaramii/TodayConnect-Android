package com.today.connect.ui.activity.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.today.connect.R
import com.today.connect.databinding.ItemSliderSpelashBinding

class ViewPagerAdapter : RecyclerView.Adapter<PagerVH>() {

    //array of colors to change the background color of screen
    private val colors = intArrayOf(
        android.R.color.black,
        android.R.color.holo_red_light,
        android.R.color.holo_blue_dark,
        android.R.color.holo_purple
    )

    lateinit var binding: ItemSliderSpelashBinding
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