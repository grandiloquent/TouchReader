package euphoria.psycho.library

import java.io.File
import java.io.FileFilter
import java.io.Serializable
import java.util.*

class CompositeFilter(val filters: ArrayList<FileFilter>) : FileFilter, Serializable {
    override fun accept(f: File?): Boolean {

        for (filter in filters) {
            if (!filter.accept(f)) {
                return false
            }
        }

        return true
    }

}