package org.sil.storyproducer.controller

import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import org.sil.storyproducer.R
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Slide
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.saveLog
import org.sil.storyproducer.tools.BitmapScaler
import org.sil.storyproducer.tools.file.getStoryImage
import org.sil.storyproducer.tools.file.storyRelPathExists
import org.sil.storyproducer.tools.media.AudioPlayer

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
abstract class SlidePhaseFrag : Fragment() {
    protected var rootView: View? = null
    protected var rootViewToolbar: View? = null

    protected var referenceAudioPlayer: AudioPlayer = AudioPlayer()
    protected var referncePlayButton: ImageButton? = null

    protected var slideNum: Int = 0 //gets overwritten
    protected var slide: Slide = Workspace.activeSlide!! //this is a placeholder that gets overwritten in onCreate.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        slideNum = this.arguments.getInt(SLIDE_NUM)
        slide = Workspace.activeStory.slides[slideNum]
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater?,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        rootView = inflater!!.inflate(R.layout.fragment_slide, container, false)

        setUiColors()
        setPic(rootView!!.findViewById<View>(R.id.fragment_image_view) as ImageView)

        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        val item = menu!!.getItem(0)
        super.onCreateOptionsMenu(menu, inflater)
        item.setIcon(R.drawable.ic_draft)
    }


    override fun onResume() {
        super.onResume()

        referenceAudioPlayer = AudioPlayer()
        referenceAudioPlayer.setStorySource(context,Workspace.activePhase.getReferenceAudioFile(slideNum))

        referenceAudioPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener { referncePlayButton!!.setBackgroundResource(R.drawable.ic_menu_play) })
    }

    /**
     * This function serves to stop the audio streams from continuing after the draft has been
     * put on pause.
     */
    override fun onPause() {
        super.onPause()
        referenceAudioPlayer.release()
    }

    /**
     * This function sets the first slide of each story to the blue color in order to prevent
     * clashing of the grey starting picture.
     */
    protected open fun setUiColors() {
        if (slideNum == 0) {
            var rl = rootView!!.findViewById<ViewGroup>(R.id.fragment_envelope)
            rl?.setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDark))
            rl = rootView!!.findViewById(R.id.fragment_text_envelope)
            rl?.setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDark))

            var tv = rootView!!.findViewById<TextView>(R.id.fragment_scripture_text)
            tv?.setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDark))
            tv = rootView!!.findViewById(R.id.fragment_reference_text)
            tv?.setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDark))
        }
    }

    /**
     * This function allows the picture to scale with the phone's screen size.
     *
     * @param slideImage    The ImageView that will contain the picture.
     * @param slideNum The slide number to grab the picture from the files.
     */
    protected fun setPic(slideImage: ImageView) {
        var slidePicture: Bitmap = getStoryImage(context,slideNum)

        //Get the height of the phone.
        val phoneProperties = context.resources.displayMetrics
        var height = phoneProperties.heightPixels
        val scalingFactor = 0.4
        height = (height * scalingFactor).toInt()

        //scale bitmap
        slidePicture = BitmapScaler.scaleToFitHeight(slidePicture, height)

        //Set the height of the image view
        slideImage.layoutParams.height = height
        slideImage.requestLayout()

        slideImage.setImageBitmap(slidePicture)

        //Set up the reference audio and slide number overlays
        referncePlayButton = rootView!!.findViewById(R.id.fragment_reference_audio_button)
        setReferenceAudioButton()

        val slideNumberText = rootView!!.findViewById<TextView>(R.id.slide_number_text)
        slideNumberText.text = slideNum.toString()
    }

    /**
     * Sets the main text of the layout.
     *
     * @param textView The text view that will be filled with the verse's text.
     */
    protected fun setScriptureText(textView: TextView) {
        val words = slide.content.split(" ")
        val newString = SpannableString(slide.content)
        var counter = 0
        for(text in words){
            if(Workspace.keytermsMap.containsKey(text)){
                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(textView : View) {
                        //bundle up the key term to send to new fragment
                        val args = Bundle()
                        args.putParcelable("KeyTerm", Workspace.keytermsMap[text])
                        val fragment = KeyTermView()
                        fragment.arguments = args
                        //fragmentManager.beginTransaction().replace(R.id.frame_id, fragment).addToBackStack("").commit()
                    }
                }
                newString.setSpan(clickableSpan, counter, counter + text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                newString.setSpan(ForegroundColorSpan(Color.BLUE),counter, counter + text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            counter += text.length + 1
        }
        textView.text = newString
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    /**
     * This function sets the reference text.
     *
     * @param textView The view that will be populated with the reference text.
     */
    protected fun setReferenceText(textView: TextView) {
        val titleNamePriority = arrayOf(slide.reference, slide.subtitle, slide.title)

        for (title in titleNamePriority) {
            if (title != "") {
                textView.text = title
                return
            }
        }
        //There is no reference text.
        textView.text = ""
    }

    protected open fun setReferenceAudioButton() {
        referncePlayButton!!.setOnClickListener {
            if (!storyRelPathExists(context,Workspace.activePhase.getReferenceAudioFile(slideNum))) {
                //TODO make "no audio" string work for all phases
                Snackbar.make(rootView!!, R.string.draft_playback_no_lwc_audio, Snackbar.LENGTH_SHORT).show()
            } else {
                if (referenceAudioPlayer.isAudioPlaying) {
                    referenceAudioPlayer.stopAudio()
                    referncePlayButton!!.setBackgroundResource(R.drawable.ic_menu_play)
                } else {
                    //stop other playback streams.
                    referenceAudioPlayer.playAudio()

                    referncePlayButton!!.setBackgroundResource(R.drawable.ic_stop_white_36dp)
                    Toast.makeText(context, R.string.draft_playback_lwc_audio, Toast.LENGTH_SHORT).show()
                    when(Workspace.activePhase.phaseType){
                        PhaseType.DRAFT -> saveLog(activity.getString(R.string.LWC_PLAYBACK))
                        PhaseType.COMMUNITY_CHECK -> saveLog(activity.getString(R.string.DRAFT_PLAYBACK))
                        else -> {}
                    }
                }
            }
        }
    }


    companion object {
        const val SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG"

    }
}
