package sample.shared
import sample.common.SharedModel
class AndroidView(context: android.content.Context) : android.view.View(context) { val model = SharedModel() }
