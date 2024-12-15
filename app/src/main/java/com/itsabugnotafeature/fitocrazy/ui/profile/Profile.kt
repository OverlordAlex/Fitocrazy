package com.itsabugnotafeature.fitocrazy.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.Converters
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class Profile : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val dateLabel = view.findViewById<TextView>(R.id.label_date)
        dateLabel.text = Converters.dateFormatter.format(
            Instant.ofEpochMilli(Instant.now().toEpochMilli()).atZone(ZoneId.systemDefault())
        )
    }
}