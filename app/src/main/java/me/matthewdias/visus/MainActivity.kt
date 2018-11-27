package me.matthewdias.visus

import android.media.MediaPlayer
import android.os.AsyncTask
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var mp : MediaPlayer
    private var mpAllocated = false
    private lateinit var tts : TextToSpeech
    private lateinit var nexts : Array<String>
    private lateinit var directions : Array<String>

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_destinations -> {
                hideNavigate()
                hideEmergency()

                destList.visibility = View.VISIBLE
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_navigate -> {
                hideDestinations()
                hideEmergency()

                message.text = "ECSS 2.203"
                startButton.visibility = View.VISIBLE

                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_emergency -> {
                hideDestinations()
                hideNavigate()

                message.text = getString(R.string.title_help)
                stopButton.visibility = View.VISIBLE

                mp = MediaPlayer.create(this, R.raw.alert)
                mpAllocated = true
                mp.isLooping = true
                mp.start()
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        val viewAdapter = DestAdapter(arrayOf(
                "ECSS 2.203",
                "ECSS 2.204",
                "ECSS 2.205",
                "ECSS 2.206",
                "ECSS 2.207",
                "Press for more rooms"
        ), this)

        findViewById<RecyclerView>(R.id.destList).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        directions = arrayOf(
                "navigating to ecss 2.203.",
                "exit room, then turn left in hallway.",
                "walk straight for 1000 feet, then turn left into hallway.",
                "your destination is on the right."
        )

        nexts = arrayOf(
                "ECSS 2.203 🏁",
                "hallway 👈",
                "hallway 👈",
                "ECSS 2.203 🏁"
        )

        tts = TextToSpeech(this, TextToSpeech.OnInitListener {
            tts.language = Locale.ENGLISH
        })

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                runOnUiThread {
                    val index = utteranceId!!.toInt()
                    message.text = (index + 1).toString() + ". " + nexts[index]
                }
            }
            override fun onDone(utteranceId: String?) {
                val next = utteranceId!!.toInt() + 1
                if (next < directions.size) {
                    speak(next)
                }
            }
            override fun onError(utteranceId: String?) { }
        })

        startButton.setOnClickListener { navigate() }
        stopButton.setOnClickListener { stopAlert() }
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }

    private fun hideDestinations() {
        destList.visibility = View.GONE
    }

    private fun hideNavigate() {
        startButton.visibility = View.GONE
        tts.stop()
    }

    private fun hideEmergency() {
        stopButton.visibility = View.GONE
        stopAlert()
    }

    private fun stopAlert() {
        if (mpAllocated) {
            mp.stop()
            mp.release()
            mpAllocated = false
        }
    }

    private fun navigate() {
        startButton.visibility = View.GONE
        speak(0)
    }

    private fun speak(i : Int) {
        DoAsync {
            tts.speak(directions[i], TextToSpeech.QUEUE_FLUSH, null, i.toString())
        }.execute()
    }

    fun goToNavigate() {
        val navigateTab : View = navigation.findViewById(R.id.navigation_navigate)
        navigateTab.performClick()
        navigate()
    }
}

class DoAsync(private val handler : () -> Unit) : AsyncTask<Void, Void, Void>() {
    override fun doInBackground(vararg params: Void?) : Void? {
        handler()
        return null
    }
}

class DestAdapter(private val data: Array<String>, activity: MainActivity) : RecyclerView.Adapter<DestAdapter.DestViewHolder>() {
    private val act : MainActivity = activity

    class DestViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DestViewHolder {
        val textView = LayoutInflater.from(parent.context)
                .inflate(R.layout.dest_text_view, parent, false) as TextView
        return DestViewHolder(textView)
    }

    override fun onBindViewHolder(holder: DestViewHolder, position: Int) {
        holder.textView.text = data[position]
        holder.textView.setOnClickListener { act.goToNavigate() }
    }

    override fun getItemCount() = data.size
}