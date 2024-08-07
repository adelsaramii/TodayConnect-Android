package com.today.connect.adapter

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
            binding.itemSliderTvTitle.text = "Online chat any time"
            binding.itemSliderTvDescription.text =
                "You can chat with your colleagues any timer and anywhere "
            binding.itemSliderImage.setImageResource(R.drawable.chat_svg)
        }
        if (position == 1) {
            binding.itemSliderTvTitle.text = "Today Ai Assistant"
            binding.itemSliderTvDescription.text =
                "Ask any question around your organisation and workflows "
            binding.itemSliderImage.setImageResource(R.drawable.robot_svg)
        }
        if (position == 2) {
            binding.itemSliderTvTitle.text = "High Message Security"
            binding.itemSliderTvDescription.text =
                "Keep your private conversation safe and secure "
            binding.itemSliderImage.setImageResource(R.drawable.secure_svg)
        }
        if (position == 3) {
            binding.itemSliderTvTitle.text = "Video conference"
            binding.itemSliderTvDescription.text =
                "Hold video and audio meetings with others easily "
            binding.itemSliderImage.setImageResource(R.drawable.meeting_svg)
        }
    }
}

class PagerVH(itemView: View) : RecyclerView.ViewHolder(itemView)