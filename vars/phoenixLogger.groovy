import com.lbg.workflow.sandbox.ColorHandler

def call(int level, String message, String formatType) {
    String border = createSymbolString(message.length()+4, formatType)
    String logLine = LogLevel(level, message, false)
    println border
    println " ${logLine} "
    println border
}

String createSymbolString(int logLength, String formatType) {
    if (logLength <= 0) {
        return ""
    } else {
        switch (formatType) {
            case 'equal':
                return '=' + createSymbolString(--logLength, formatType)
                break
            case 'underline':
                return '_' + createSymbolString(--logLength, formatType)
                break
            case 'dash':
                return '-' + createSymbolString(--logLength, formatType)
                break
            case 'star':
                return '*' + createSymbolString(--logLength, formatType)
                break
            default:
                return ""
                break
        }
    }
}

String LogLevel(int level, String message, boolean colorStatus) {
    if (colorStatus) {
        echo "Install AnsiColorBuildWrapper before enabling color support"
        echo "and delete this message"
    }
    def color = new ColorHandler()
    switch (level) {
        case 1:
            if (colorStatus) {
                return "${color.red()} Error :: " + message + color.reset()
            }
            return "Error :: " + message
            break
        case 2:
            if (colorStatus) {
                return "${color.lgt_red()} Warning :: " + message + color.reset()
            }
            return "Warning :: " + message
            break
        case 3:
            if (colorStatus) {
                return "${color.blue()} Notice :: " + message + color.reset()
            }
            return "Notice :: " + message
            break
        case 4:
            if (colorStatus) {
                return "${color.green()} Info :: " + message + color.reset()
            }
            return "Info :: " + message
            break
        case 5:
            if (colorStatus) {
                return "${color.cyan()} Debug :: " + message + color.reset()
            }
            return "Debug :: " + message
            break
        default:
            return message
            break
    }
}

