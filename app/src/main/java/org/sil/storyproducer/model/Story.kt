package org.sil.storyproducer.model
import android.content.Context
import android.graphics.Rect
import android.os.Environment
import android.support.v4.content.ContextCompat
import android.util.Log

import org.sil.storyproducer.tools.file.FileSystem
import org.sil.storyproducer.tools.file.LogFiles
import org.sil.storyproducer.tools.file.ProjectXML
import org.sil.storyproducer.tools.media.graphics.KenBurnsEffect
import org.sil.storyproducer.tools.media.graphics.RectHelper

import java.io.File
import java.util.*

class Story {
    val path: String = ""
    val projectPath: String
        get() = path + "/project"

}


fun isPathToStory(path: String) {
    //Check if path is path
    //See if there is an xml photostory file there
    //If not, See if there is an html bloom file there
    //if either, yes, it is a project.
}

/*
//Populate templatePaths from files in system
fun populateStories(con: Context) {
    cacheDirectory = con.cacheDir

    //Reset templatePaths
    templatePaths = HashMap()
    projectPaths = HashMap()
    moviesPaths = HashMap()

    // Get the LWC language from preferences (defaults to ENG if none set)
    val prefs = con.getSharedPreferences(LANGUAGE_PREFS, Context.MODE_PRIVATE)
    language = prefs.getString(LWC_LANGUAGE, "ENG")

    //Iterate external files directories.
    val storeDirs = ContextCompat.getExternalFilesDirs(con, null)
    val moviesDirs = ContextCompat.getExternalFilesDirs(con, Environment.DIRECTORY_MOVIES)
    for (i in storeDirs.indices) {
        val currentStoreDir = storeDirs[i]
        val currentMoviesDir = moviesDirs[i]
        if (currentStoreDir != null) {
            //Get templates directory of current external storage directory.
            val templateDir = File(currentStoreDir, TEMPLATES_DIR)

            //If there is no template directory (i.e. there are no templates on this storage
            // device), move on from this storage device.
            if (!templateDir.exists() || !templateDir.isDirectory) {
                continue
            }

            val projectDir = File(currentStoreDir, PROJECT_DIR)
            //Make the project directory if it does not exist.
            //The template creator shouldn't have to remember this step.
            if (!projectDir.isDirectory) {
                projectDir.mkdir()
            }

            val langDirs = templateDir.listFiles(directoryFilter)
            for (currentLangDir in langDirs) {
                val lang = currentLangDir.name

                if (!templatePaths!!.containsKey(lang)) {
                    templatePaths!![lang] = HashMap()
                }
                val storyTemplateMap = templatePaths!![lang]

                val storyDirs = currentLangDir.listFiles(directoryFilter)
                for (currentStoryDir in storyDirs) {
                    val storyName = currentStoryDir.name
                    val storyTemplatePath = currentStoryDir.absolutePath
                    storyTemplateMap.put(storyName, storyTemplatePath)

                    //Make sure the corresponding projects directory exists.
                    val storyWriteDir = File(File(currentStoreDir, PROJECT_DIR), storyName)
                    if (!storyWriteDir.isDirectory) {
                        storyWriteDir.mkdir()
                    }
                    projectPaths!![storyName] = storyWriteDir.absolutePath
                    moviesPaths!![storyName] = currentMoviesDir.absolutePath
                }
            }
        }
    }

    LogFiles.init(con)
}
*/

/**
 * Get the slide from the story template at the specified index.
 * @param story
 * @param index
 * @return requested slide or null if not found.
 */

fun TemplateSlides(story: String, index: Int): TemplateSlide? {
    val slides = createSlidesFromProjectXML(story)
    return if (slides != null && slides.size > index) {
        slides[index]
    } else null
}

/**
 * Convert a story's ProjectXML into a list of TemplateSlide.
 * @param story
 * @return list of story's slides or null if unable to read/parse project.xml.
 */
private fun createSlidesFromProjectXML(story: String?): List<TemplateSlide>? {
    val templatePath = FileSystem.getTemplatePath(story)

    val xml: ProjectXML
    try {
        xml = ProjectXML(story)
    } catch (e: Exception) {
        Log.e("Temaplate", "Error reading or parsing project.xml file!", e)
        return null
    }

    val slides = ArrayList<TemplateSlide>()

    for (i in 0..xml.units.size-1) {
        val unit = xml.units[i]

        val narrationPath = unit.narrationFilename
        val narration = if (narrationPath == null) null else File(templatePath, narrationPath)

        val imagePath = unit.imageInfo.filename

        val width = unit.imageInfo.width
        val height = unit.imageInfo.height
        val imageDimensions = Rect(0, 0, width, height)

        val start = unit.imageInfo.motion.start
        //Ensure the rectangle fits within the image.
        RectHelper.clip(start, imageDimensions)

        val end = unit.imageInfo.motion.end
        //Ensure the rectangle fits within the image.
        RectHelper.clip(end, imageDimensions)

        //TODO: Should we use crop here? (Are start and end relative to crop or absolute?)
        var crop: Rect? = null
        if (unit.imageInfo.edit != null) {
            crop = unit.imageInfo.edit.crop
        }
        val kbfx = KenBurnsEffect(start, end, crop)

        var soundtrack: File? = null
        var soundtrackVolume = 0
        if (i > 0) {
            val previous = slides[i - 1]
            soundtrack = previous.soundtrack
            soundtrackVolume = previous.soundtrackVolume
        }
        if (unit.imageInfo.musicTrack != null) {
            val soundtrackPath = unit.imageInfo.musicTrack.filename
            soundtrack = File(templatePath, soundtrackPath)
            soundtrackVolume = unit.imageInfo.musicTrack.volume
        }

        val currentSlide = TemplateSlide(narration,
                File(imagePath), imageDimensions, kbfx, soundtrack, soundtrackVolume)
        slides.add(currentSlide)
    }

    return slides
}
