package com.jetbrains.swift.ift.lesson.editor

import com.jetbrains.swift.ift.SwiftLessonsBundle
import training.learn.lesson.kimpl.KLesson
import training.learn.lesson.kimpl.LessonContext
import training.learn.lesson.kimpl.LessonSample
import training.learn.lesson.kimpl.parseLessonSample

class SwiftQuickPopupsLesson : KLesson("swift.codeassistance.quickpopups",
                                       SwiftLessonsBundle.message("swift.editor.popups.name"), "Swift") {

  private val sample: LessonSample = parseLessonSample("""
import Foundation
import UIKit

class Duplicate: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()

        let x = 0
        let y = 50

        let tableView = UITableView()

        let header = UILabel()
        header.text = "AppCode"
        header.sizeToFit()

        tableView.frame = CGRect(x: x, y: y, width: 320, height: 400)
        tableView.tableHeaderView = header
        self.view.addSubview(tableView)
    }

}""".trimIndent())
  override val lessonContent: LessonContext.() -> Unit = {
    prepareSample(sample)
    caret(18, 34)
    task {
      triggers("ParameterInfo")
      text(SwiftLessonsBundle.message("swift.editor.popups.param.info", action("ParameterInfo")))
    }
    task {
      triggers("EditorEscape")
      text(SwiftLessonsBundle.message("swift.editor.popups.close.param.info", action("EditorEscape")))
    }
    caret(4, 26)
    task {
      triggers("QuickJavaDoc")
      text(SwiftLessonsBundle.message("swift.editor.popups.doc", action("QuickJavaDoc")))
    }
    caret(4, 26)
    task {
      triggers("QuickImplementations")
      text(SwiftLessonsBundle.message("swift.editor.popups.impl", action("QuickImplementations")))
    }
  }
}