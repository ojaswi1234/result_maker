import re

file_path = "/workspaces/result_maker/app/src/main/java/com/example/ui/MarksEntryScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

target = """                val baseSubjects = viewModel.availableSubjects.toMutableList()
                val config = allExamConfigs.find { it.className == className }
                if (config != null && config.additionalSubjectsString.isNotEmpty()) {"""

replacement = """                val config = allExamConfigs.find { it.className == className }
                val baseSubjects = if (config != null && config.mainSubjectsString.isNotEmpty()) {
                    config.mainSubjectsString.split("|").filter { it.isNotEmpty() }.toMutableList()
                } else {
                    viewModel.availableSubjects.toMutableList()
                }
                if (config != null && config.additionalSubjectsString.isNotEmpty()) {"""

new_content = content.replace(target, replacement)
with open(file_path, "w") as f:
    f.write(new_content)
